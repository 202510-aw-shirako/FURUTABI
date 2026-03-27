package com.furutabi.config;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MapRecordFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpMapData() {
        Timestamp now = Timestamp.from(Instant.parse("2026-03-25T00:00:00Z"));

        jdbcTemplate.update("DELETE FROM chat_messages");
        jdbcTemplate.update("DELETE FROM chat_threads");
        jdbcTemplate.update("DELETE FROM notification_delivery_logs");
        jdbcTemplate.update("DELETE FROM notifications");
        jdbcTemplate.update("DELETE FROM support_request_status_history");
        jdbcTemplate.update("DELETE FROM support_requests");
        jdbcTemplate.update("DELETE FROM proposal_application_status_history");
        jdbcTemplate.update("DELETE FROM proposal_applications");
        jdbcTemplate.update("DELETE FROM proposal_tags");
        jdbcTemplate.update("DELETE FROM proposals");
        jdbcTemplate.update("DELETE FROM map_record_comments");
        jdbcTemplate.update("DELETE FROM map_record_images");
        jdbcTemplate.update("DELETE FROM map_records");
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM users");

        insertUser(100L, "owner@example.com", "owner-user", now);
        insertUser(101L, "viewer@example.com", "viewer-user", now);

        insertRole(100L, "USER", now);
        insertRole(101L, "USER", now);

        insertMapRecord(501L, 100L, "Public owner record", "Public body", "public", false, now);
        insertMapRecord(502L, 100L, "Private owner record", "Private body", "private", false, now);
        insertMapRecord(503L, 100L, "Limited owner record", "Limited body", "limited", false, now);
        insertMapRecord(504L, 100L, "Draft owner record", "Draft body", "private", true, now);
        insertMapRecord(505L, 100L, "Deleted owner record", "Deleted body", "public", false, now);
        jdbcTemplate.update("UPDATE map_records SET deleted_at = ?, updated_at = ? WHERE id = ?", now, now, 505L);

        jdbcTemplate.update(
            "INSERT INTO map_record_images (map_record_id, file_path, sort_order, created_at) VALUES (?, ?, ?, ?)",
            501L,
            "/images/map-501.jpg",
            0,
            now
        );
        jdbcTemplate.update(
            """
                INSERT INTO map_record_comments (map_record_id, user_id, body, is_hidden, created_at, updated_at, deleted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
            501L,
            101L,
            "Looks good",
            false,
            now,
            now,
            null
        );
    }

    @Test
    @DisplayName("Authenticated owner can view visible map record list")
    void ownerCanViewVisibleMapRecordList() throws Exception {
        mockMvc.perform(get("/app/map-records").with(user("owner@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Public owner record")))
            .andExpect(content().string(containsString("Private owner record")))
            .andExpect(content().string(containsString("Limited owner record")))
            .andExpect(content().string(not(containsString("Draft owner record"))))
            .andExpect(content().string(not(containsString("Deleted owner record"))));
    }

    @Test
    @DisplayName("Authenticated viewer sees only visible map records in list")
    void viewerSeesOnlyVisibleMapRecordsInList() throws Exception {
        mockMvc.perform(get("/app/map-records").with(user("viewer@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Public owner record")))
            .andExpect(content().string(not(containsString("Private owner record"))))
            .andExpect(content().string(not(containsString("Limited owner record"))))
            .andExpect(content().string(not(containsString("Deleted owner record"))))
            .andExpect(content().string(not(containsString(">編集する<"))));
    }

    @Test
    @DisplayName("Authenticated owner can open create page")
    void ownerCanOpenCreatePage() throws Exception {
        mockMvc.perform(get("/app/map-records/new").with(user("owner@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("わたしの地図の記録を追加")));
    }

    @Test
    @DisplayName("Authenticated owner can create published map record")
    void ownerCanCreatePublishedMapRecord() throws Exception {
        mockMvc.perform(
                post("/app/map-records")
                    .with(user("owner@example.com").roles("USER"))
                    .with(csrf())
                    .param("title", "New trip record")
                    .param("body", "Record body for Kamakura")
                    .param("visibility", "PUBLIC")
                    .param("locationName", "Kamakura")
                    .param("locationPrecisionLevel", "town")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/app/map-records/*?saved"));

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM map_records WHERE user_id = ? AND title = ? AND deleted_at IS NULL",
            Integer.class,
            100L,
            "New trip record"
        );
        Assertions.assertEquals(1, count);
    }

    @Test
    @DisplayName("Authenticated owner can edit existing map record")
    void ownerCanEditExistingMapRecord() throws Exception {
        mockMvc.perform(
                post("/app/map-records/501")
                    .with(user("owner@example.com").roles("USER"))
                    .with(csrf())
                    .param("title", "Updated public record")
                    .param("body", "Updated body")
                    .param("visibility", "PRIVATE")
                    .param("locationName", "Yuigahama")
                    .param("locationPrecisionLevel", "point")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/map-records/501?saved"));

        String updatedTitle = jdbcTemplate.queryForObject(
            "SELECT title FROM map_records WHERE id = ?",
            String.class,
            501L
        );
        String updatedVisibility = jdbcTemplate.queryForObject(
            "SELECT visibility FROM map_records WHERE id = ?",
            String.class,
            501L
        );
        Assertions.assertEquals("Updated public record", updatedTitle);
        Assertions.assertEquals("private", updatedVisibility);
    }

    @Test
    @DisplayName("Visibility changes are reflected in list access")
    void visibilityChangesAreReflectedInListAccess() throws Exception {
        mockMvc.perform(
                post("/app/map-records/501")
                    .with(user("owner@example.com").roles("USER"))
                    .with(csrf())
                    .param("title", "Updated public record")
                    .param("body", "Updated body")
                    .param("visibility", "PRIVATE")
                    .param("locationName", "Yuigahama")
                    .param("locationPrecisionLevel", "point")
            )
            .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/app/map-records").with(user("viewer@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("Updated public record"))));
    }

    @Test
    @DisplayName("Authenticated owner can save draft and stay in edit page")
    void ownerCanSaveDraft() throws Exception {
        mockMvc.perform(
                post("/app/map-records/501")
                    .with(user("owner@example.com").roles("USER"))
                    .with(csrf())
                    .param("title", "Draft update")
                    .param("body", "Draft body")
                    .param("visibility", "PRIVATE")
                    .param("locationName", "Kamakura")
                    .param("locationPrecisionLevel", "area")
                    .param("draft", "true")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/map-records/501/edit?savedDraft"));

        Boolean draftFlag = jdbcTemplate.queryForObject(
            "SELECT is_draft FROM map_records WHERE id = ?",
            Boolean.class,
            501L
        );
        Assertions.assertEquals(Boolean.TRUE, draftFlag);
    }

    @Test
    @DisplayName("Authenticated owner can soft delete map record")
    void ownerCanSoftDeleteMapRecord() throws Exception {
        mockMvc.perform(
                post("/app/map-records/501/delete")
                    .with(user("owner@example.com").roles("USER"))
                    .with(csrf())
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/map-records?deleted"));

        Timestamp deletedAt = jdbcTemplate.queryForObject(
            "SELECT deleted_at FROM map_records WHERE id = ?",
            Timestamp.class,
            501L
        );
        Assertions.assertNotNull(deletedAt);
    }

    @Test
    @DisplayName("Deleted map record detail is hidden")
    void deletedMapRecordDetailIsHidden() throws Exception {
        mockMvc.perform(get("/app/map-records/505").with(user("owner@example.com").roles("USER")))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Public map record detail is available to another authenticated user")
    void publicMapRecordDetailIsAvailable() throws Exception {
        mockMvc.perform(get("/app/map-records/501").with(user("viewer@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Public owner record")))
            .andExpect(content().string(containsString("一般公開")));
    }

    @Test
    @DisplayName("Private map record detail is hidden from another authenticated user")
    void privateMapRecordDetailIsHiddenFromOtherUser() throws Exception {
        mockMvc.perform(get("/app/map-records/502").with(user("viewer@example.com").roles("USER")))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Limited map record detail stays owner-centered in current task")
    void limitedMapRecordDetailStaysOwnerCentered() throws Exception {
        mockMvc.perform(get("/app/map-records/503").with(user("viewer@example.com").roles("USER")))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Other authenticated user cannot edit someone else's map record")
    void nonOwnerCannotEditOtherUsersRecord() throws Exception {
        mockMvc.perform(
                post("/app/map-records/501")
                    .with(user("viewer@example.com").roles("USER"))
                    .with(csrf())
                    .param("title", "Not allowed")
                    .param("visibility", "PUBLIC")
                    .param("locationPrecisionLevel", "area")
            )
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Other authenticated user cannot delete someone else's map record")
    void nonOwnerCannotDeleteOtherUsersRecord() throws Exception {
        mockMvc.perform(
                post("/app/map-records/501/delete")
                    .with(user("viewer@example.com").roles("USER"))
                    .with(csrf())
            )
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Unauthenticated map record list still redirects to login")
    void unauthenticatedMapRecordListRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/app/map-records"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    private void insertUser(long id, String email, String nickname, Timestamp now) {
        jdbcTemplate.update(
            """
                INSERT INTO users (
                    id, email, password_hash, nickname, name, name_kana, birthday, gender,
                    phone_number, address, sms_verified, additional_verification_status,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            id,
            email,
            "{noop}unused",
            nickname,
            nickname,
            nickname,
            Date.valueOf("1990-01-01"),
            "NO_ANSWER",
            "090-0000-0000",
            "Test address",
            true,
            "UNREQUESTED",
            now,
            now
        );
    }

    private void insertRole(long userId, String roleName, Timestamp now) {
        jdbcTemplate.update(
            "INSERT INTO user_roles (user_id, role_name, created_at) VALUES (?, ?, ?)",
            userId,
            roleName,
            now
        );
    }

    private void insertMapRecord(
        long mapRecordId,
        long ownerUserId,
        String title,
        String body,
        String visibility,
        boolean isDraft,
        Timestamp now
    ) {
        jdbcTemplate.update(
            """
                INSERT INTO map_records (
                    id, user_id, title, body, visibility, location_name, latitude, longitude,
                    location_precision_level, is_draft, created_at, updated_at, visibility_updated_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            mapRecordId,
            ownerUserId,
            title,
            body,
            visibility,
            "Kamakura",
            null,
            null,
            "area",
            isDraft,
            now,
            now,
            now,
            null
        );
    }
}
