package com.pipeline.crm.avatar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pipeline.crm.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Students' Telegram photos end to end, against a local stand-in for t.me: fetched after create / handle change /
 * lead conversion, served with the student's own visibility rules, refreshed when due, dropped when hidden.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AvatarIntegrationTest extends AbstractIntegrationTest {

    private static final TelegramStub STUB = new TelegramStub();

    @DynamicPropertySource
    static void avatarProps(DynamicPropertyRegistry registry) {
        registry.add("app.avatars.enabled", () -> "true");
        registry.add("app.avatars.telegram-base-url", STUB::baseUrl);
        registry.add("app.avatars.allowed-image-hosts", () -> "127.0.0.1");
        registry.add("app.avatars.request-interval", () -> "0s");
        registry.add("app.avatars.startup-delay", () -> "1h");
    }

    @AfterAll
    static void stopStub() {
        STUB.stop();
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired StudentAvatarService avatarService;
    @Autowired StudentAvatarRepository avatarRepository;

    private String adminToken;
    private String curatorToken;
    private String otherCuratorToken;
    private UUID firstStageId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = login("admin", "admin123");
        curatorToken = login("anya.t", "curator1");
        otherCuratorToken = login("igor.l", "curator2");
        firstStageId = UUID.fromString(getJson("/api/v1/pipeline-stages", adminToken).get(0).get("id").asText());
    }

    @Test
    void newStudentGetsTheirPhotoInTheBackground() throws Exception {
        String handle = handle();
        STUB.photo(handle, Color.RED);
        UUID id = createStudent("@" + handle);

        String version = awaitStudent(id, s -> !s.get("avatarVersion").isNull()).get("avatarVersion").asText();

        MvcResult photo = mockMvc.perform(get("/api/v1/students/" + id + "/avatar?v=" + version)
                        .header("Authorization", "Bearer " + curatorToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andReturn();
        String cacheControl = photo.getResponse().getHeader("Cache-Control");
        assertThat(cacheControl).contains("max-age=").contains("private");
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(photo.getResponse().getContentAsByteArray()));
        assertThat(img.getWidth()).isEqualTo(128);

        // the board list carries the same version
        JsonNode list = getJson("/api/v1/students?search=" + handle, curatorToken);
        assertThat(list.get(0).get("avatarVersion").asText()).isEqualTo(version);
    }

    @Test
    void anotherCuratorsStudentPhotoIsNotFound() throws Exception {
        String handle = handle();
        STUB.photo(handle, Color.BLUE);
        UUID id = createStudent(handle);
        awaitChecked(id);

        mockMvc.perform(get("/api/v1/students/" + id + "/avatar").header("Authorization", "Bearer " + otherCuratorToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/students/" + id + "/avatar/refresh").header("Authorization", "Bearer " + otherCuratorToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/students/" + id + "/avatar").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void noPublicPhotoMeansInitials() throws Exception {
        String handle = handle();
        STUB.set(handle, TelegramStub.Mode.NO_PHOTO);
        UUID id = createStudent(handle);
        awaitChecked(id);

        assertThat(getStudent(id).get("avatarVersion").isNull()).isTrue();
        mockMvc.perform(get("/api/v1/students/" + id + "/avatar").header("Authorization", "Bearer " + curatorToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void newPhotoChangesTheVersionAndHidingItDropsOurCopy() throws Exception {
        String handle = handle();
        STUB.photo(handle, Color.RED);
        UUID id = createStudent(handle);
        awaitChecked(id);
        String first = getStudent(id).get("avatarVersion").asText();

        STUB.photo(handle, Color.GREEN);
        avatarService.refresh(id);
        String second = getStudent(id).get("avatarVersion").asText();
        assertThat(second).isNotEqualTo(first);

        STUB.set(handle, TelegramStub.Mode.NO_PHOTO);
        avatarService.refresh(id);
        assertThat(getStudent(id).get("avatarVersion").isNull()).isTrue();
        assertThat(avatarRepository.findById(id).orElseThrow().getImage()).isNull();
    }

    @Test
    void failedCheckKeepsTheLastPhotoAndRetriesNextDay() throws Exception {
        String handle = handle();
        STUB.photo(handle, Color.RED);
        UUID id = createStudent(handle);
        awaitChecked(id);
        String version = getStudent(id).get("avatarVersion").asText();

        STUB.set(handle, TelegramStub.Mode.ERROR);
        avatarService.refresh(id);

        assertThat(getStudent(id).get("avatarVersion").asText()).isEqualTo(version);
        Instant nextCheck = avatarRepository.findById(id).orElseThrow().getNextCheckAt();
        assertThat(nextCheck).isBetween(Instant.now().plus(Duration.ofHours(23)), Instant.now().plus(Duration.ofHours(25)));
    }

    @Test
    void regularCheckIsTwoWeeksAwayWithASpread() throws Exception {
        String handle = handle();
        STUB.photo(handle, Color.RED);
        UUID id = createStudent(handle);
        awaitChecked(id);

        Instant nextCheck = avatarRepository.findById(id).orElseThrow().getNextCheckAt();
        assertThat(nextCheck).isBetween(Instant.now().plus(Duration.ofDays(14)).minusSeconds(60),
                Instant.now().plus(Duration.ofDays(17)));
    }

    @Test
    void changingOrClearingTheHandleRefetches() throws Exception {
        String photoHandle = handle();
        STUB.photo(photoHandle, Color.RED);
        UUID id = createStudent(null);
        assertThat(getStudent(id).get("avatarVersion").isNull()).isTrue();

        patchStudent(id, "{\"telegramUsername\":\"" + photoHandle + "\"}");
        awaitStudent(id, s -> !s.get("avatarVersion").isNull());

        patchStudent(id, "{\"telegramUsername\":\"\"}");
        awaitStudent(id, s -> s.get("avatarVersion").isNull());
        assertThat(avatarRepository.findById(id)).isEmpty();
    }

    @Test
    void convertedLeadGetsTheirPhoto() throws Exception {
        String handle = handle();
        STUB.photo(handle, Color.ORANGE);
        MvcResult lead = mockMvc.perform(post("/api/v1/leads")
                        .header("Authorization", "Bearer " + curatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Lead " + handle + "\",\"telegramUsername\":\"@" + handle + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        String leadId = objectMapper.readTree(lead.getResponse().getContentAsString()).get("id").asText();

        MvcResult converted = mockMvc.perform(post("/api/v1/leads/" + leadId + "/convert")
                        .header("Authorization", "Bearer " + curatorToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andReturn();
        UUID studentId = UUID.fromString(objectMapper.readTree(converted.getResponse().getContentAsString()).get("id").asText());

        awaitStudent(studentId, s -> !s.get("avatarVersion").isNull());
    }

    @Test
    void refreshButtonWorksOncePerMinute() throws Exception {
        String handle = handle();
        STUB.set(handle, TelegramStub.Mode.NO_PHOTO);
        UUID id = createStudent(handle);
        awaitChecked(id);

        STUB.photo(handle, Color.MAGENTA);
        MvcResult refreshed = mockMvc.perform(post("/api/v1/students/" + id + "/avatar/refresh")
                        .header("Authorization", "Bearer " + curatorToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(objectMapper.readTree(refreshed.getResponse().getContentAsString()).get("avatarVersion").isNull()).isFalse();

        mockMvc.perform(post("/api/v1/students/" + id + "/avatar/refresh").header("Authorization", "Bearer " + curatorToken))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void nightlyJobOnlyTouchesStudentsThatAreDue() throws Exception {
        String dueHandle = handle();
        String freshHandle = handle();
        STUB.photo(dueHandle, Color.RED);
        STUB.photo(freshHandle, Color.RED);
        UUID due = createStudent(dueHandle);
        UUID fresh = createStudent(freshHandle);
        awaitChecked(due);
        awaitChecked(fresh);
        String dueVersion = getStudent(due).get("avatarVersion").asText();
        String freshVersion = getStudent(fresh).get("avatarVersion").asText();

        StudentAvatar row = avatarRepository.findById(due).orElseThrow();
        row.setNextCheckAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        avatarRepository.save(row);
        STUB.photo(dueHandle, Color.CYAN);
        STUB.photo(freshHandle, Color.CYAN);
        int freshHitsBefore = STUB.pageHits(freshHandle);

        avatarService.refreshDue();

        assertThat(getStudent(due).get("avatarVersion").asText()).isNotEqualTo(dueVersion);
        assertThat(getStudent(fresh).get("avatarVersion").asText()).isEqualTo(freshVersion);
        assertThat(STUB.pageHits(freshHandle)).isEqualTo(freshHitsBefore);
    }

    @Test
    void deletingTheStudentDropsTheirPhoto() throws Exception {
        String handle = handle();
        STUB.photo(handle, Color.RED);
        UUID id = createStudent(handle);
        awaitChecked(id);
        assertThat(avatarRepository.findById(id)).isPresent();

        mockMvc.perform(delete("/api/v1/students/" + id).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
        assertThat(avatarRepository.findById(id)).isEmpty();
    }

    private static String handle() {
        return "st_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private UUID createStudent(String telegram) throws Exception {
        String body = "{\"fullName\":\"Avatar " + UUID.randomUUID() + "\",\"currentStageId\":\"" + firstStageId + "\""
                + (telegram != null ? ",\"telegramUsername\":\"" + telegram + "\"" : "") + "}";
        MvcResult result = mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer " + curatorToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private void patchStudent(UUID id, String body) throws Exception {
        mockMvc.perform(patch("/api/v1/students/" + id)
                        .header("Authorization", "Bearer " + curatorToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    private JsonNode getStudent(UUID id) throws Exception {
        return getJson("/api/v1/students/" + id, curatorToken);
    }

    /**
     * Waits for the background check that creating a student with a handle triggers, so later direct
     * refreshes in a test can't be overtaken by it.
     */
    private void awaitChecked(UUID id) throws Exception {
        long deadline = System.currentTimeMillis() + 10_000;
        while (avatarRepository.findById(id).isEmpty()) {
            assertThat(System.currentTimeMillis()).as("waited for the background photo check").isLessThan(deadline);
            Thread.sleep(50);
        }
    }

    /** Background fetches run after the commit on another thread; poll for a few seconds. */
    private JsonNode awaitStudent(UUID id, Predicate<JsonNode> condition) throws Exception {
        long deadline = System.currentTimeMillis() + 10_000;
        JsonNode student = getStudent(id);
        while (!condition.test(student)) {
            assertThat(System.currentTimeMillis()).as("waited for the background photo fetch").isLessThan(deadline);
            Thread.sleep(100);
            student = getStudent(id);
        }
        return student;
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private JsonNode getJson(String url, String token) throws Exception {
        MvcResult result = mockMvc.perform(get(url).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
