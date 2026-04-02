package com.furutabi.config;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
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
class MapRecordEngagementFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        Timestamp now = Timestamp.from(Instant.parse("2026-04-02T00:00:00Z"));

        jdbcTemplate.update("DELETE FROM chat_messages");
        jdbcTemplate.update("DELETE FROM chat_threads");
        jdbcTemplate.update("DELETE FROM notification_delivery_logs");
        jdbcTemplate.update("DELETE FROM notifications");
        jdbcTemplate.update("DELETE FROM access_logs");
        jdbcTemplate.update("DELETE FROM permission_change_logs");
        jdbcTemplate.update("DELETE FROM support_note_reports");
        jdbcTemplate.update("DELETE FROM support_notes");
        jdbcTemplate.update("DELETE FROM partner_assignments");
        jdbcTemplate.update("DELETE FROM permission_rules");
        jdbcTemplate.update("DELETE FROM role_policies");
        jdbcTemplate.update("DELETE FROM user_role_states");
        jdbcTemplate.update("DELETE FROM region_scoped_settings");
        jdbcTemplate.update("DELETE FROM support_request_status_history");
        jdbcTemplate.update("DELETE FROM support_requests");
        jdbcTemplate.update("DELETE FROM map_record_bookmarks");
        jdbcTemplate.update("DELETE FROM map_record_reactions");
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
        insertUser(102L, "bridge@example.com", "bridge-user", now);
        insertUser(103L, "other@example.com", "other-user", now);

        insertRole(100L, "USER", now);
        insertRole(101L, "USER", now);
        insertRole(102L, "BRIDGE", now);
        insertRole(103L, "USER", now);

        insertMapRecord(701L, 100L, "Public footprint A", "public", Timestamp.from(now.toInstant().minusSeconds(3600)));
        insertMapRecord(702L, 100L, "Private footprint", "private", Timestamp.from(now.toInstant().minusSeconds(1800)));
        insertMapRecord(703L, 101L, "Viewer public footprint", "public", now);

        jdbcTemplate.update(
            "INSERT INTO map_record_reactions (map_record_id, user_id, reaction_type, created_at) VALUES (?, ?, ?, ?)",
            701L,
            103L,
            "like",
            now
        );
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM map_record_bookmarks");
        jdbcTemplate.update("DELETE FROM map_record_reactions");
        jdbcTemplate.update("DELETE FROM proposal_application_status_history");
        jdbcTemplate.update("DELETE FROM proposal_applications");
        jdbcTemplate.update("DELETE FROM proposal_tags");
        jdbcTemplate.update("DELETE FROM proposals");
        jdbcTemplate.update("DELETE FROM chat_messages");
        jdbcTemplate.update("DELETE FROM chat_threads");
    }

    @Test
    @DisplayName("viewer can filter footprints by owner and sees filter banner")
    void viewerCanFilterByOwner() throws Exception {
        mockMvc.perform(get("/app/footprints?userId=100").with(user("viewer@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("owner-user")))
            .andExpect(content().string(containsString("さんのピンだけ表示中です。")))
            .andExpect(content().string(containsString("Public footprint A")))
            .andExpect(content().string(not(containsString("Viewer public footprint"))));
    }

    @Test
    @DisplayName("user can toggle like on visible footprint")
    void userCanToggleLike() throws Exception {
        mockMvc.perform(
                post("/app/footprints/701/reaction")
                    .with(user("viewer@example.com").roles("USER"))
                    .with(csrf())
                    .param("returnTo", "/app/footprints/701")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/footprints/701"));

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM map_record_reactions WHERE map_record_id = ? AND user_id = ? AND reaction_type = 'like'",
            Integer.class,
            701L,
            101L
        );
        Assertions.assertEquals(1, count);

        mockMvc.perform(
                post("/app/footprints/701/reaction")
                    .with(user("viewer@example.com").roles("USER"))
                    .with(csrf())
                    .param("returnTo", "/app/footprints/701")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/footprints/701"));

        Integer removed = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM map_record_reactions WHERE map_record_id = ? AND user_id = ? AND reaction_type = 'like'",
            Integer.class,
            701L,
            101L
        );
        Assertions.assertEquals(0, removed);
    }

    @Test
    @DisplayName("bridge can toggle thanks on visible footprint")
    void bridgeCanToggleThanks() throws Exception {
        mockMvc.perform(
                post("/app/footprints/701/reaction")
                    .with(user("bridge@example.com").roles("BRIDGE"))
                    .with(csrf())
                    .param("returnTo", "/app/footprints/701")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/footprints/701"));

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM map_record_reactions WHERE map_record_id = ? AND user_id = ? AND reaction_type = 'thanks'",
            Integer.class,
            701L,
            102L
        );
        Assertions.assertEquals(1, count);
    }

    @Test
    @DisplayName("viewer can toggle bookmark on visible footprint")
    void viewerCanToggleBookmark() throws Exception {
        mockMvc.perform(
                post("/app/footprints/701/bookmark")
                    .with(user("viewer@example.com").roles("USER"))
                    .with(csrf())
                    .param("returnTo", "/app/footprints")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/footprints"));

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM map_record_bookmarks WHERE map_record_id = ? AND user_id = ?",
            Integer.class,
            701L,
            101L
        );
        Assertions.assertEquals(1, count);
    }

    @Test
    @DisplayName("non visible records do not allow reaction or bookmark toggles")
    void hiddenRecordsDoNotAllowEngagement() throws Exception {
        mockMvc.perform(
                post("/app/footprints/702/reaction")
                    .with(user("viewer@example.com").roles("USER"))
                    .with(csrf())
                    .param("returnTo", "/app/footprints/702")
            )
            .andExpect(status().isNotFound());

        mockMvc.perform(
                post("/app/footprints/702/bookmark")
                    .with(user("viewer@example.com").roles("USER"))
                    .with(csrf())
                    .param("returnTo", "/app/footprints/702")
            )
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("reaction counts are shown without changing visible ordering")
    void reactionCountsShownWithoutChangingOrdering() throws Exception {
        mockMvc.perform(get("/app/footprints").with(user("viewer@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("いいね 1")))
            .andExpect(content().string(containsString("Public footprint A")))
            .andExpect(content().string(containsString("Viewer public footprint")));
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

    private void insertMapRecord(long mapRecordId, long ownerUserId, String title, String visibility, Timestamp now) {
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
            title + " body",
            visibility,
            "Kamakura",
            null,
            null,
            "area",
            false,
            now,
            now,
            now,
            null
        );
    }
}
