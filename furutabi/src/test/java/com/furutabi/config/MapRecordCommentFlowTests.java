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
class MapRecordCommentFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpCommentData() {
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

        insertMapRecord(501L, 100L, "Public record", "public", false, now);
        insertMapRecord(502L, 100L, "Private record", "private", false, now);

        insertComment(801L, 501L, 101L, "Visible comment", false, now);
        insertComment(802L, 501L, 102L, "Hidden comment", true, now);
        insertComment(803L, 502L, 100L, "Private owner comment", false, now);
    }

    @Test
    @DisplayName("visible viewers see visible comments only")
    void visibleViewersSeeVisibleCommentsOnly() throws Exception {
        mockMvc.perform(get("/app/map-records/501").with(user("viewer@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Visible comment")))
            .andExpect(content().string(not(containsString("Hidden comment"))));
    }

    @Test
    @DisplayName("visible viewers can post comments on visible map records")
    void visibleViewersCanPostComments() throws Exception {
        mockMvc.perform(
                post("/app/map-records/501/comments")
                    .with(user("viewer@example.com").roles("USER"))
                    .with(csrf())
                    .param("body", "New public comment")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/map-records/501?commentSaved"));

        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM map_record_comments
                WHERE map_record_id = ? AND body = ? AND deleted_at IS NULL AND is_hidden = FALSE
                """,
            Integer.class,
            501L,
            "New public comment"
        );
        Assertions.assertEquals(1, count);
    }

    @Test
    @DisplayName("non viewers cannot post comments on hidden map records")
    void nonViewersCannotPostComments() throws Exception {
        mockMvc.perform(
                post("/app/map-records/502/comments")
                    .with(user("viewer@example.com").roles("USER"))
                    .with(csrf())
                    .param("body", "Should fail")
            )
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("owner can hide visible comment")
    void ownerCanHideComment() throws Exception {
        mockMvc.perform(
                post("/app/map-records/501/comments/801/hide")
                    .with(user("owner@example.com").roles("USER"))
                    .with(csrf())
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/map-records/501?commentHidden"));

        Boolean hidden = jdbcTemplate.queryForObject(
            "SELECT is_hidden FROM map_record_comments WHERE id = ?",
            Boolean.class,
            801L
        );
        Assertions.assertEquals(Boolean.TRUE, hidden);
    }

    @Test
    @DisplayName("admin can hide visible comment without being owner")
    void adminCanHideComment() throws Exception {
        mockMvc.perform(
                post("/app/map-records/501/comments/801/hide")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .with(csrf())
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/map-records/501?commentHidden"));
    }

    @Test
    @DisplayName("non owner and non admin cannot hide comment")
    void nonOwnerCannotHideComment() throws Exception {
        mockMvc.perform(
                post("/app/map-records/501/comments/801/hide")
                    .with(user("viewer@example.com").roles("USER"))
                    .with(csrf())
            )
            .andExpect(status().isNotFound());
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
