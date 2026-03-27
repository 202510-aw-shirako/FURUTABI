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
@SuppressWarnings("null")
class FootprintCommentFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpFootprintCommentData() {
        Timestamp now = Timestamp.from(Instant.parse("2026-03-27T00:00:00Z"));

        jdbcTemplate.update("DELETE FROM chat_messages");
        jdbcTemplate.update("DELETE FROM chat_threads");
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
        insertUser(102L, "other@example.com", "other-user", now);
        insertUser(103L, "admin@example.com", "admin-user", now);

        insertRole(100L, "USER", now);
        insertRole(101L, "USER", now);
        insertRole(102L, "USER", now);
        insertRole(103L, "ADMIN", now);

        insertMapRecord(601L, 100L, "Public footprint", "public", false, now);
        insertMapRecord(602L, 100L, "Limited footprint", "limited", false, now);
        insertMapRecord(603L, 100L, "Private map only", "private", false, now);

        insertComment(901L, 601L, 101L, "Visible footprint comment", false, now);
        insertComment(902L, 601L, 102L, "Hidden footprint comment", true, now);
    }

    @Test
    @DisplayName("viewer can open footprint list and detail")
    void viewerCanOpenFootprintListAndDetail() throws Exception {
        mockMvc.perform(get("/app/footprints").with(user("viewer@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Public footprint")))
            .andExpect(content().string(not(containsString("Limited footprint"))))
            .andExpect(content().string(not(containsString("Private map only"))));

        mockMvc.perform(get("/app/footprints/601").with(user("viewer@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Visible footprint comment")))
            .andExpect(content().string(not(containsString("Hidden footprint comment"))));
    }

    @Test
    @DisplayName("viewer can post footprint comment on visible footprint")
    void viewerCanPostFootprintComment() throws Exception {
        mockMvc.perform(
                post("/app/footprints/601/comments")
                    .with(user("viewer@example.com").roles("USER"))
                    .with(csrf())
                    .param("body", "New footprint comment")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/footprints/601?commentSaved"));

        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM map_record_comments
                WHERE map_record_id = ? AND body = ? AND deleted_at IS NULL AND is_hidden = FALSE
                """,
            Integer.class,
            601L,
            "New footprint comment"
        );
        Assertions.assertEquals(1, count);
    }

    @Test
    @DisplayName("non viewer cannot post comment on private map through footprint route")
    void nonViewerCannotPostPrivateFootprintComment() throws Exception {
        mockMvc.perform(
                post("/app/footprints/603/comments")
                    .with(user("viewer@example.com").roles("USER"))
                    .with(csrf())
                    .param("body", "Should fail")
            )
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("owner and admin can hide footprint comments")
    void ownerAndAdminCanHideFootprintComments() throws Exception {
        mockMvc.perform(
                post("/app/footprints/601/comments/901/hide")
                    .with(user("owner@example.com").roles("USER"))
                    .with(csrf())
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/footprints/601?commentHidden"));

        jdbcTemplate.update("UPDATE map_record_comments SET is_hidden = FALSE WHERE id = ?", 901L);

        mockMvc.perform(
                post("/app/footprints/601/comments/901/hide")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .with(csrf())
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/footprints/601?commentHidden"));
    }

    @Test
    @DisplayName("footprint supplementation does not break existing map comment route")
    void footprintSupplementDoesNotBreakMapCommentRoute() throws Exception {
        mockMvc.perform(get("/app/map-records/601").with(user("viewer@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Visible footprint comment")));
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

    private void insertMapRecord(long mapRecordId, long ownerUserId, String title, String visibility, boolean draft, Timestamp now) {
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
            draft,
            now,
            now,
            now,
            null
        );
    }

    private void insertComment(long commentId, long mapRecordId, long userId, String body, boolean hidden, Timestamp now) {
        jdbcTemplate.update(
            """
                INSERT INTO map_record_comments (
                    id, map_record_id, user_id, body, is_hidden, created_at, updated_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
            commentId,
            mapRecordId,
            userId,
            body,
            hidden,
            now,
            now,
            null
        );
    }
}
