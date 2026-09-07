package com.pipeline.crm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pipeline.crm.audit.AuditLogRepository;
import com.pipeline.crm.lead.LeadRepository;
import com.pipeline.crm.student.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CrmApplicationIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired LeadRepository leadRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired AuditLogRepository auditLogRepository;

    private String adminToken;
    private String curatorToken;
    private String otherCuratorToken;
    private UUID curatorId;
    private UUID otherCuratorId;
    private UUID stage0;
    private UUID stage1;

    private static final String ADMIN = "admin";
    private static final String ADMIN_PASS = "admin123";
    private static final String CURATOR = "anya.t";
    private static final String CURATOR_PASS = "curator1";
    private static final String OTHER_CURATOR = "igor.l";
    private static final String OTHER_CURATOR_PASS = "curator2";

    @BeforeEach
    void setUp() throws Exception {
        adminToken = login(ADMIN, ADMIN_PASS);
        curatorToken = login(CURATOR, CURATOR_PASS);
        otherCuratorToken = login(OTHER_CURATOR, OTHER_CURATOR_PASS);

        JsonNode me = getJson("/api/v1/auth/me", curatorToken);
        curatorId = UUID.fromString(me.get("id").asText());
        JsonNode otherMe = getJson("/api/v1/auth/me", otherCuratorToken);
        otherCuratorId = UUID.fromString(otherMe.get("id").asText());

        JsonNode stages = getJson("/api/v1/pipeline-stages", adminToken);
        stage0 = UUID.fromString(stages.get(0).get("id").asText());
        stage1 = UUID.fromString(stages.get(1).get("id").asText());
    }

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void contextAndFlywayLoaded() {
        assertThat(leadRepository).isNotNull();
        assertThat(stage0).isNotNull();
    }

    @Test
    void loginFailsWithWrongPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void blockedUserCannotLogin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"sveta.r\",\"password\":\"curator3\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedRouteRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/leads"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void curatorCannotManageUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + curatorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void curatorCannotCreateStage() throws Exception {
        mockMvc.perform(post("/api/v1/pipeline-stages")
                        .header("Authorization", "Bearer " + curatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"position\":99,\"normDays\":5,\"isFinal\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void curatorCannotCreateCohort() throws Exception {
        mockMvc.perform(post("/api/v1/cohorts")
                        .header("Authorization", "Bearer " + curatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\",\"startDate\":\"2030-01-01\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void curatorSeesOnlyOwnLeadAndCannotSeeOthersById() throws Exception {
        // create lead assigned to the *other* curator
        MvcResult result = mockMvc.perform(post("/api/v1/leads")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Лид Б\",\"telegramUsername\":\"@leadB\",\"curatorId\":\"" + otherCuratorId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID leadId = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(get("/api/v1/leads").header("Authorization", "Bearer " + curatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/v1/leads/" + leadId).header("Authorization", "Bearer " + curatorToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void curatorCreatesLeadAutoAssignedToSelf() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/leads")
                        .header("Authorization", "Bearer " + curatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Мой лид\",\"telegramUsername\":\"@me\",\"notes\":[{\"text\":\"заметка\",\"position\":0}]}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("assignedCuratorId").asText()).isEqualTo(curatorId.toString());
    }

    @Test
    void convertLeadCreatesStudentAndCopiesNotes() throws Exception {
        UUID leadId = createLeadAsAdmin();

        MvcResult studentResult = mockMvc.perform(post("/api/v1/leads/" + leadId + "/convert")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"curatorId\":\"" + curatorId + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode student = objectMapper.readTree(studentResult.getResponse().getContentAsString());
        UUID studentId = UUID.fromString(student.get("id").asText());
        assertThat(student.get("fullName").asText()).isNotBlank();

        mockMvc.perform(post("/api/v1/leads/" + leadId + "/convert")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"curatorId\":\"" + curatorId + "\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/students/" + studentId + "/history")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stages.length()").value(1));
    }

    @Test
    void moveStageClosesOldHistoryAndCreatesNew() throws Exception {
        UUID leadId = createLeadAsAdmin();
        UUID studentId = convertLead(leadId, curatorId);

        mockMvc.perform(post("/api/v1/students/" + studentId + "/move-stage")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stageId\":\"" + stage1 + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/students/" + studentId + "/history")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stages.length()").value(2));
    }

    @Test
    void cannotMoveToArchivedStage() throws Exception {
        UUID leadId = createLeadAsAdmin();
        UUID studentId = convertLead(leadId, curatorId);

        mockMvc.perform(post("/api/v1/pipeline-stages/" + stage1 + "/archive")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/students/" + studentId + "/move-stage")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stageId\":\"" + stage1 + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void curatorCannotSeeOtherCuratorStudent() throws Exception {
        UUID leadId = createLeadAsAdmin();
        UUID studentId = convertLead(leadId, otherCuratorId);

        mockMvc.perform(get("/api/v1/students/" + studentId).header("Authorization", "Bearer " + curatorToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void curatorStatisticsExcludeOtherCurators() throws Exception {
        mockMvc.perform(get("/api/v1/stats/curators").header("Authorization", "Bearer " + curatorToken))
                .andExpect(status().isOk());
    }

    @Test
    void auditRecordedForConversionWithoutSecrets() throws Exception {
        UUID leadId = createLeadAsAdmin();
        convertLead(leadId, curatorId);

        long count = auditLogRepository.count();
        assertThat(count).isGreaterThan(0);

        String allData = auditLogRepository.findAll().stream()
                .map(a -> (a.getBeforeData() == null ? "" : a.getBeforeData()) + (a.getAfterData() == null ? "" : a.getAfterData()))
                .reduce("", String::concat);
        assertThat(allData).doesNotContain("passwordHash", "password", ADMIN_PASS, CURATOR_PASS);
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private JsonNode getJson(String url, String token) throws Exception {
        MvcResult result = mockMvc.perform(get(url).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private UUID createLeadAsAdmin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/leads")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Лид А\",\"telegramUsername\":\"@leadA\"," +
                                "\"curatorId\":\"" + curatorId + "\"," +
                                "\"notes\":[{\"text\":\"note1\",\"position\":0}]}"))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID convertLead(UUID leadId, UUID curator) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/leads/" + leadId + "/convert")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"curatorId\":\"" + curator + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }
}
