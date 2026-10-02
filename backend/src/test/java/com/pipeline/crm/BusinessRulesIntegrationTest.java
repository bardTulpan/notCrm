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

import java.nio.charset.StandardCharsets;
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
        // Students created without a cohort auto-create "<Month> <Year>" cohorts starting on the 1st;
        // avoid colliding with one of those on the unique cohort start_date.
        if (LocalDate.now(ZoneOffset.UTC).minusDays(elapsedDays).getDayOfMonth() == 1) {
            elapsedDays += 1;
        }
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

    @Test
    void curatorCreatedStudentGetsCohortOfStartMonthEvenIfNoneExistedYet() throws Exception {
        String first = createStudentAsCurator(stageIds.get(0), "2026-01-15T00:00:00Z");
        String second = createStudentAsCurator(stageIds.get(0), "2026-01-28T00:00:00Z");

        JsonNode firstStudent = objectMapper.readTree(first);
        assertThat(firstStudent.get("cohortId").isNull()).isFalse();
        assertThat(objectMapper.readTree(second).get("cohortId").asText()).isEqualTo(firstStudent.get("cohortId").asText());

        JsonNode cohorts = getJson("/api/v1/cohorts", adminToken);
        boolean found = false;
        for (JsonNode c : cohorts) {
            if (c.get("id").asText().equals(firstStudent.get("cohortId").asText())) {
                assertThat(c.get("name").asText()).isEqualTo("Январь 2026");
                assertThat(c.get("startDate").asText()).isEqualTo("2026-01-01");
                found = true;
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    void movingBackToThePreviousStageRestoresDaysOnStage() throws Exception {
        UUID studentId = createStudent(stageIds.get(0), daysAgo(12), daysAgo(12));
        String originalEnteredAt = getStudent(studentId, adminToken).get("stageEnteredAt").asText();

        moveStage(studentId, stageIds.get(1));
        assertThat(getStudent(studentId, adminToken).get("daysOnStage").asInt()).isZero();

        moveStage(studentId, stageIds.get(0));
        JsonNode back = getStudent(studentId, adminToken);
        assertThat(Instant.parse(back.get("stageEnteredAt").asText())).isEqualTo(Instant.parse(originalEnteredAt));
        assertThat(back.get("daysOnStage").asInt()).isEqualTo(12);

        JsonNode history = getJson("/api/v1/students/" + studentId + "/history", adminToken).get("stages");
        assertThat(history).hasSize(1);
        assertThat(history.get(0).get("exitedAt").isNull()).isTrue();
    }

    @Test
    void auditLogIsAdminOnlyAndDescribesStageMoves() throws Exception {
        UUID studentId = createStudent(stageIds.get(0), daysAgo(1), daysAgo(1));
        moveStage(studentId, stageIds.get(1));

        mockMvc.perform(get("/api/v1/audit-log").header("Authorization", "Bearer " + curatorToken))
                .andExpect(status().isForbidden());

        JsonNode page = getJson("/api/v1/audit-log?entityType=student&size=20", adminToken);
        boolean found = false;
        for (JsonNode item : page.get("items")) {
            if (item.get("entityId").asText().equals(studentId.toString()) && item.get("action").asText().equals("move-stage")) {
                assertThat(item.get("description").asText()).contains("перенёс ученика");
                found = true;
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    void reorderingStagesSwapsPositionsWithoutUniqueViolation() throws Exception {
        List<UUID> original = new ArrayList<>(stageIds);
        List<UUID> swapped = new ArrayList<>(original);
        UUID tmp = swapped.get(2);
        swapped.set(2, swapped.get(3));
        swapped.set(3, tmp);

        reorderStages(swapped);
        JsonNode after = getJson("/api/v1/pipeline-stages", adminToken);
        assertThat(after.get(2).get("id").asText()).isEqualTo(swapped.get(2).toString());
        assertThat(after.get(3).get("id").asText()).isEqualTo(swapped.get(3).toString());

        reorderStages(original); // leave the shared test DB as we found it
        assertThat(getJson("/api/v1/pipeline-stages", adminToken).get(2).get("id").asText()).isEqualTo(original.get(2).toString());
    }

    @Test
    void curatorWorkloadIsAdminOnlyAndCountsStudentsByHealth() throws Exception {
        UUID studentId = createStudent(stageIds.get(0), daysAgo(45), daysAgo(45)); // red on a 30-day stage

        mockMvc.perform(get("/api/v1/stats/curator-workload").header("Authorization", "Bearer " + curatorToken))
                .andExpect(status().isForbidden());

        JsonNode rows = getJson("/api/v1/stats/curator-workload", adminToken);
        JsonNode mine = null;
        for (JsonNode r : rows) {
            if (r.get("curatorId").asText().equals(curatorId.toString())) mine = r;
        }
        assertThat(mine).isNotNull();
        assertThat(mine.get("students").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(mine.get("red").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(mine.get("green").asLong() + mine.get("yellow").asLong() + mine.get("red").asLong() + mine.get("paused").asLong())
                .isEqualTo(mine.get("students").asLong());
        assertThat(studentId).isNotNull();
    }

    @Test
    void auditLogCanBeFilteredByStudentAndPeriod() throws Exception {
        UUID studentId = createStudent(stageIds.get(0), daysAgo(1), daysAgo(1));
        moveStage(studentId, stageIds.get(1));

        JsonNode byStudent = getJson("/api/v1/audit-log?entityId=" + studentId, adminToken);
        assertThat(byStudent.get("total").asLong()).isEqualTo(2); // create + move-stage
        for (JsonNode item : byStudent.get("items")) {
            assertThat(item.get("entityId").asText()).isEqualTo(studentId.toString());
        }

        String future = Instant.now().plus(1, ChronoUnit.DAYS).toString();
        assertThat(getJson("/api/v1/audit-log?entityId=" + studentId + "&from=" + future, adminToken).get("total").asLong()).isZero();
        assertThat(getJson("/api/v1/audit-log?entityId=" + studentId + "&to=" + future, adminToken).get("total").asLong()).isEqualTo(2);
    }

    @Test
    void invalidInputGivesRealErrorCodesNotMaskedUnauthorized() throws Exception {
        UUID someStage = stageIds.get(0);
        String base = "\"fullName\":\"Validation Test\",\"currentStageId\":\"" + someStage + "\"";

        // postpay outside 0..100 -> 400 (used to hit a DB check constraint and surface as 401/500)
        mockMvc.perform(post("/api/v1/students").header("Authorization", "Bearer " + curatorToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{" + base + ",\"postpayPercent\":150}"))
                .andExpect(status().isBadRequest());
        // unknown cohort -> 404, not an FK violation
        mockMvc.perform(post("/api/v1/students").header("Authorization", "Bearer " + curatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + base + ",\"cohortId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isNotFound());
        // "on stage since" in the future -> 400
        mockMvc.perform(post("/api/v1/students").header("Authorization", "Bearer " + curatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + base + ",\"stageEnteredAt\":\"" + Instant.now().plus(10, ChronoUnit.DAYS) + "\"}"))
                .andExpect(status().isBadRequest());
        // broken JSON and a non-UUID path id -> 400
        mockMvc.perform(post("/api/v1/students").header("Authorization", "Bearer " + curatorToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{bad"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/students/not-a-uuid").header("Authorization", "Bearer " + curatorToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void studentsCanOnlyBeAssignedToActiveCurators() throws Exception {
        UUID studentId = createStudent(stageIds.get(0), daysAgo(1), daysAgo(1));
        UUID adminId = UUID.fromString(getJson("/api/v1/auth/me", adminToken).get("id").asText());

        for (UUID bad : List.of(UUID.randomUUID(), adminId)) {
            mockMvc.perform(post("/api/v1/students/" + studentId + "/assign-curator")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"curatorId\":\"" + bad + "\"}"))
                    .andExpect(status().isBadRequest());
        }
        assertThat(getStudent(studentId, adminToken).get("curatorId").asText()).isEqualTo(curatorId.toString());
    }

    @Test
    void curatorCanReadNamesOnlyDirectoryButNotTheUserList() throws Exception {
        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + curatorToken))
                .andExpect(status().isForbidden());
        JsonNode directory = getJson("/api/v1/directory/users", curatorToken);
        assertThat(directory.size()).isGreaterThanOrEqualTo(2);
        assertThat(directory.get(0).has("username")).isFalse();
        assertThat(directory.get(0).has("fullName")).isTrue();
    }

    private void reorderStages(List<UUID> ids) throws Exception {
        String body = "{\"ids\":[" + ids.stream().map(id -> "\"" + id + "\"").collect(java.util.stream.Collectors.joining(",")) + "]}";
        mockMvc.perform(put("/api/v1/pipeline-stages/order")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful());
    }

    private String createStudentAsCurator(UUID stageId, String startedAt) throws Exception {
        return mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer " + curatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Cohort Test " + UUID.randomUUID() + "\",\"currentStageId\":\"" + stageId
                                + "\",\"startedAt\":\"" + startedAt + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private void moveStage(UUID studentId, UUID stageId) throws Exception {
        mockMvc.perform(post("/api/v1/students/" + studentId + "/move-stage")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stageId\":\"" + stageId + "\"}"))
                .andExpect(status().isOk());
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
        // The JSON response has no charset; MockMvc would otherwise decode it as ISO-8859-1 and garble Cyrillic.
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
