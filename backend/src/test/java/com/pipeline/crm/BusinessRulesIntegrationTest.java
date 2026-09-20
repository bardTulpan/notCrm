package com.pipeline.crm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers business rules from DEVELOPMENT_BACKLOG.md that are not exercised by
 * CrmApplicationIntegrationTest: health-tier thresholds, onlyOverdue excluding
 * paused students, cohort funnel planned-stage/on-track/behind computation,
 * lead ping-date filters and the postpone-ping "not earlier than today" rule.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BusinessRulesIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String adminToken;
    private String curatorToken;
    private String otherCuratorToken;
    private UUID curatorId;
    private UUID otherCuratorId;
    private List<UUID> stageIds;
    private List<Integer> stageNormDays;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = login("admin", "admin123");
        curatorToken = login("anya.t", "curator1");
        otherCuratorToken = login("igor.l", "curator2");

        JsonNode me = getJson("/api/v1/auth/me", curatorToken);
        curatorId = UUID.fromString(me.get("id").asText());
        JsonNode otherMe = getJson("/api/v1/auth/me", otherCuratorToken);
        otherCuratorId = UUID.fromString(otherMe.get("id").asText());

        JsonNode stages = getJson("/api/v1/pipeline-stages", adminToken);
        stageIds = new ArrayList<>();
        stageNormDays = new ArrayList<>();
        for (JsonNode s : stages) {
            stageIds.add(UUID.fromString(s.get("id").asText()));
            stageNormDays.add(s.get("normDays").isNull() ? null : s.get("normDays").asInt());
        }
    }

    @Test
    void healthTransitionsFromGreenToYellowToRed() throws Exception {
        // stage 0 (GoPractice) has a 30 day norm
        UUID studentId = createStudent(stageIds.get(0), daysAgo(10), daysAgo(10));
        assertThat(getStudent(studentId, adminToken).get("health").asText()).isEqualTo("green");

        patchStageEnteredAt(studentId, daysAgo(22)); // 22/30 = 73.3% -> yellow
        assertThat(getStudent(studentId, adminToken).get("health").asText()).isEqualTo("yellow");

        patchStageEnteredAt(studentId, daysAgo(31)); // 31/30 > 100% -> red
        assertThat(getStudent(studentId, adminToken).get("health").asText()).isEqualTo("red");
    }

    @Test
    void pausedStudentIsAlwaysGreenHealthAndNeverOverdue() throws Exception {
        UUID studentId = createStudent(stageIds.get(0), daysAgo(45), daysAgo(45));
        assertThat(getStudent(studentId, adminToken).get("health").asText()).isEqualTo("red");

        mockMvc.perform(post("/api/v1/students/" + studentId + "/pause")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertThat(getStudent(studentId, adminToken).get("health").asText()).isEqualTo("paused");
    }

    @Test
    void onlyOverdueExcludesPausedStudents() throws Exception {
        UUID overdue = createStudent(stageIds.get(0), daysAgo(40), daysAgo(40));
        UUID overduePaused = createStudent(stageIds.get(0), daysAgo(40), daysAgo(40));
        mockMvc.perform(post("/api/v1/students/" + overduePaused + "/pause")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/v1/students?onlyOverdue=true")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode list = objectMapper.readTree(result.getResponse().getContentAsString());
        List<String> ids = new ArrayList<>();
        list.forEach(n -> ids.add(n.get("id").asText()));

        assertThat(ids).contains(overdue.toString());
        assertThat(ids).doesNotContain(overduePaused.toString());
    }

    @Test
    void cohortFunnelComputesPlannedStageAndOnTrackBehind() throws Exception {
        // Compute expectations from the currently active stages (fetched live) rather than
        // hardcoding norms, so this test stays correct even if another test archived a stage.
        JsonNode stages = getJson("/api/v1/pipeline-stages", adminToken);
        List<UUID> ids = new ArrayList<>();
        List<Integer> positions = new ArrayList<>();
        List<Integer> norms = new ArrayList<>();
        for (JsonNode s : stages) {
            ids.add(UUID.fromString(s.get("id").asText()));
            positions.add(s.get("position").asInt());
            norms.add(s.get("normDays").isNull() ? null : s.get("normDays").asInt());
        }
        int targetIdx = 2; // third active stage; never the final (norm-less) one on a 10-stage pipeline
        long cumulativeBeforeTarget = 0;
        for (int i = 0; i < targetIdx; i++) {
            cumulativeBeforeTarget += norms.get(i);
        }
        long elapsedDays = cumulativeBeforeTarget + (norms.get(targetIdx) / 2);
        int expectedPlannedPosition = positions.get(targetIdx);

        LocalDate startDate = LocalDate.now(ZoneOffset.UTC).minusDays(elapsedDays);
        String cohortName = "Test cohort " + UUID.randomUUID();

        MvcResult cohortResult = mockMvc.perform(post("/api/v1/cohorts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + cohortName + "\",\"startDate\":\"" + startDate + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID cohortId = UUID.fromString(objectMapper.readTree(cohortResult.getResponse().getContentAsString()).get("id").asText());

        Instant startedAt = Instant.now().minus(elapsedDays, ChronoUnit.DAYS);
        UUID onTrackStudent = createStudentWithCohort(ids.get(targetIdx), startedAt, startedAt, cohortId);
        UUID behindStudent = createStudentWithCohort(ids.get(0), startedAt, startedAt, cohortId);

        MvcResult detailResult = mockMvc.perform(get("/api/v1/stats/cohorts/" + cohortId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode detail = objectMapper.readTree(detailResult.getResponse().getContentAsString());

        assertThat(detail.get("total").asLong()).isEqualTo(2);
        assertThat(detail.get("plannedStagePosition").asInt()).isEqualTo(expectedPlannedPosition);
        assertThat(detail.get("onTrackCount").asLong()).isEqualTo(1);
        assertThat(detail.get("behindCount").asLong()).isEqualTo(1);
        assertThat(onTrackStudent).isNotEqualTo(behindStudent);
    }

    @Test
    void leadPingFiltersRestrictResults() throws Exception {
        String near = Instant.now().plus(1, ChronoUnit.DAYS).toString();
        String far = Instant.now().plus(20, ChronoUnit.DAYS).toString();

        UUID nearLead = createLead("Near lead " + UUID.randomUUID(), near);
        UUID farLead = createLead("Far lead " + UUID.randomUUID(), far);

        String pingTo = Instant.now().plus(5, ChronoUnit.DAYS).toString();
        MvcResult result = mockMvc.perform(get("/api/v1/leads?pingTo=" + pingTo)
                        .header("Authorization", "Bearer " + curatorToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode list = objectMapper.readTree(result.getResponse().getContentAsString());
        List<String> ids = new ArrayList<>();
        list.forEach(n -> ids.add(n.get("id").asText()));

        assertThat(ids).contains(nearLead.toString());
        assertThat(ids).doesNotContain(farLead.toString());
    }

    @Test
    void postponePingRejectsDateBeforeTodayButAcceptsToday() throws Exception {
        UUID leadId = createLead("Ping lead " + UUID.randomUUID(), Instant.now().plus(1, ChronoUnit.DAYS).toString());

        Instant startOfToday = Instant.now().truncatedTo(ChronoUnit.DAYS);
        String yesterday = startOfToday.minusSeconds(1).toString();
        mockMvc.perform(post("/api/v1/leads/" + leadId + "/postpone-ping")
                        .header("Authorization", "Bearer " + curatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nextPingAt\":\"" + yesterday + "\"}"))
                .andExpect(status().isConflict());

        String laterToday = startOfToday.plusSeconds(3600).toString();
        mockMvc.perform(post("/api/v1/leads/" + leadId + "/postpone-ping")
                        .header("Authorization", "Bearer " + curatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nextPingAt\":\"" + laterToday + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void adminConvertWithoutCuratorIdIsRejectedButCuratorSelfAssigns() throws Exception {
        UUID adminLeadId = createLead("Admin lead " + UUID.randomUUID(), null);
        mockMvc.perform(post("/api/v1/leads/" + adminLeadId + "/convert")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        MvcResult curatorLead = mockMvc.perform(post("/api/v1/leads")
                        .header("Authorization", "Bearer " + curatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Own lead " + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID curatorLeadId = UUID.fromString(objectMapper.readTree(curatorLead.getResponse().getContentAsString()).get("id").asText());

        MvcResult converted = mockMvc.perform(post("/api/v1/leads/" + curatorLeadId + "/convert")
                        .header("Authorization", "Bearer " + curatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode student = objectMapper.readTree(converted.getResponse().getContentAsString());
        assertThat(student.get("curatorId").asText()).isEqualTo(curatorId.toString());
        assertThat(student.get("health").asText()).isEqualTo("green");
    }

    @Test
    void curatorStatisticsOnlyContainOwnRecord() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/stats/curators")
                        .header("Authorization", "Bearer " + curatorToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode list = objectMapper.readTree(result.getResponse().getContentAsString());

        assertThat(list.size()).isEqualTo(1);
        assertThat(list.get(0).get("curatorId").asText()).isEqualTo(curatorId.toString());
        List<String> ids = new ArrayList<>();
        list.forEach(n -> ids.add(n.get("curatorId").asText()));
        assertThat(ids).doesNotContain(otherCuratorId.toString());
    }

    @Test
    void cannotDeleteCuratorWithActiveStudents() throws Exception {
        createStudent(stageIds.get(0), daysAgo(1), daysAgo(1));

        mockMvc.perform(delete("/api/v1/users/" + curatorId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }

    @Test
    void cannotBlockOrDeleteLastActiveAdmin() throws Exception {
        JsonNode me = getJson("/api/v1/auth/me", adminToken);
        UUID adminId = UUID.fromString(me.get("id").asText());

        mockMvc.perform(post("/api/v1/users/" + adminId + "/block")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/v1/users/" + adminId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }

    private String daysAgo(long days) {
        return Instant.now().minus(days, ChronoUnit.DAYS).toString();
    }

    private UUID createStudent(UUID stageId, String stageEnteredAt, String startedAt) throws Exception {
        return createStudentWithCohort(stageId, Instant.parse(stageEnteredAt), Instant.parse(startedAt), null);
    }

    private UUID createStudentWithCohort(UUID stageId, Instant stageEnteredAt, Instant startedAt, UUID cohortId) throws Exception {
        String body = "{\"fullName\":\"Student " + UUID.randomUUID() + "\"," +
                "\"curatorId\":\"" + curatorId + "\"," +
                (cohortId != null ? "\"cohortId\":\"" + cohortId + "\"," : "") +
                "\"currentStageId\":\"" + stageId + "\"," +
                "\"stageEnteredAt\":\"" + stageEnteredAt + "\"," +
                "\"startedAt\":\"" + startedAt + "\"}";
        MvcResult result = mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private void patchStageEnteredAt(UUID studentId, String stageEnteredAt) throws Exception {
        mockMvc.perform(patch("/api/v1/students/" + studentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stageEnteredAt\":\"" + stageEnteredAt + "\"}"))
                .andExpect(status().isOk());
    }

    private JsonNode getStudent(UUID id, String token) throws Exception {
        return getJson("/api/v1/students/" + id, token);
    }

    private UUID createLead(String name, String nextPingAt) throws Exception {
        String body = "{\"name\":\"" + name + "\"" +
                (nextPingAt != null ? ",\"nextPingAt\":\"" + nextPingAt + "\"" : "") + "}";
        MvcResult result = mockMvc.perform(post("/api/v1/leads")
                        .header("Authorization", "Bearer " + curatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
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
}
