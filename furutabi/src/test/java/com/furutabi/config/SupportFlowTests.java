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
@SuppressWarnings("null")
class SupportFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpSupportData() {
        Timestamp now = Timestamp.from(Instant.parse("2026-03-27T00:00:00Z"));

        jdbcTemplate.update("DELETE FROM support_request_status_history");
        jdbcTemplate.update("DELETE FROM support_requests");
        jdbcTemplate.update("DELETE FROM notification_delivery_logs");
        jdbcTemplate.update("DELETE FROM notifications");
        jdbcTemplate.update("DELETE FROM chat_messages");
        jdbcTemplate.update("DELETE FROM chat_threads");
        jdbcTemplate.update("DELETE FROM proposal_application_status_history");
        jdbcTemplate.update("DELETE FROM proposal_applications");
        jdbcTemplate.update("DELETE FROM proposal_tags");
        jdbcTemplate.update("DELETE FROM proposals");
        jdbcTemplate.update("DELETE FROM map_record_comments");
        jdbcTemplate.update("DELETE FROM map_record_images");
        jdbcTemplate.update("DELETE FROM map_records");
        jdbcTemplate.update("DELETE FROM sms_verifications");
        jdbcTemplate.update("DELETE FROM contact_preferences");
        jdbcTemplate.update("DELETE FROM user_profiles");
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM users");

        insertUser(100L, "requester@example.com", "requester", now);
        insertUser(101L, "admin@example.com", "admin", now);
        insertUser(102L, "other@example.com", "other", now);
        insertUser(103L, "local@example.com", "local", now);

        insertRole(100L, "USER", now);
        insertRole(101L, "ADMIN", now);
        insertRole(102L, "USER", now);
        insertRole(103L, "LOCAL", now);

        insertSupportRequest(801L, 100L, "general", "account", "/app/account", "Need help with account", "received", null, now);
    }

    @Test
    @DisplayName("authenticated user can reach support entry and create request")
    void userCanReachSupportEntryAndCreateRequest() throws Exception {
        mockMvc.perform(get("/app/support").with(user("requester@example.com").roles("USER")))
            .andExpect(status().isOk());

        mockMvc.perform(get("/app/support/new").with(user("requester@example.com").roles("USER")))
            .andExpect(status().isOk());

        mockMvc.perform(
                post("/app/support")
                    .with(user("requester@example.com").roles("USER"))
                    .with(csrf())
                    .param("requestType", "map")
                    .param("relatedFeature", "footprints")
                    .param("targetReference", "/app/footprints/501")
                    .param("replyPreference", "required")
                    .param("body", "Need help with a private footprint issue")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/app/support/*?submitted"));

        Long supportRequestId = jdbcTemplate.queryForObject(
            """
                SELECT id
                FROM support_requests
                WHERE user_id = ?
                ORDER BY id DESC
                FETCH FIRST 1 ROWS ONLY
                """,
            Long.class,
            100L
        );
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM support_requests WHERE id = ? AND user_id = ? AND deleted_at IS NULL",
            Integer.class,
            supportRequestId,
            100L
        );
        Integer historyCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM support_request_status_history WHERE support_request_id = ? AND status = ?",
            Integer.class,
            supportRequestId,
            "received"
        );

        Assertions.assertEquals(1, count);
        Assertions.assertEquals(1, historyCount);
    }

    @Test
    @DisplayName("request owner can view own support detail but others cannot")
    void ownerCanViewOwnSupportDetailButOthersCannot() throws Exception {
        mockMvc.perform(get("/app/support/801").with(user("requester@example.com").roles("USER")))
            .andExpect(status().isOk());

        mockMvc.perform(get("/app/support/801").with(user("other@example.com").roles("USER")))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/app/support/801").with(user("local@example.com").roles("LOCAL")))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("admin can review support queue and transition handled then closed")
    void adminCanReviewAndTransitionSupport() throws Exception {
        mockMvc.perform(get("/app/support/admin").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("account / requester")));

        mockMvc.perform(post("/app/support/801/handle")
                .with(user("admin@example.com").roles("ADMIN"))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/support/801?handled"));

        String handledStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM support_requests WHERE id = ?",
            String.class,
            801L
        );
        Long handledBy = jdbcTemplate.queryForObject(
            "SELECT handled_by_user_id FROM support_requests WHERE id = ?",
            Long.class,
            801L
        );

        Assertions.assertEquals("handled", handledStatus);
        Assertions.assertEquals(101L, handledBy);

        mockMvc.perform(post("/app/support/801/close")
                .with(user("admin@example.com").roles("ADMIN"))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/support/801?closed"));

        String closedStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM support_requests WHERE id = ?",
            String.class,
            801L
        );
        Integer historyCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM support_request_status_history WHERE support_request_id = ?",
            Integer.class,
            801L
        );

        Assertions.assertEquals("closed", closedStatus);
        Assertions.assertEquals(3, historyCount);
    }

    @Test
    @DisplayName("non admin cannot access support admin queue or transition status")
    void nonAdminCannotUseAdminSupportFlow() throws Exception {
        mockMvc.perform(get("/app/support/admin").with(user("requester@example.com").roles("USER")))
            .andExpect(status().isNotFound());

        mockMvc.perform(post("/app/support/801/handle")
                .with(user("requester@example.com").roles("USER"))
                .with(csrf()))
            .andExpect(status().isNotFound());

        mockMvc.perform(post("/app/support/801/close")
                .with(user("other@example.com").roles("USER"))
                .with(csrf()))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("users do not see other users support summaries in own list")
    void usersOnlySeeOwnSupportSummaries() throws Exception {
        mockMvc.perform(get("/app/support").with(user("other@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("account"))))
            .andExpect(content().string(not(containsString("Need help with account"))));
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

    private void insertSupportRequest(
        long supportRequestId,
        long requesterUserId,
        String requestType,
        String relatedFeature,
        String targetReference,
        String body,
        String status,
        Long handledByUserId,
        Timestamp now
    ) {
        jdbcTemplate.update(
            """
                INSERT INTO support_requests (
                    id, user_id, request_type, related_feature, target_reference, body,
                    reply_preference, status, handled_by_user_id, created_at, updated_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            supportRequestId,
            requesterUserId,
            requestType,
            relatedFeature,
            targetReference,
            body,
            "optional",
            status,
            handledByUserId,
            now,
            now,
            null
        );
        jdbcTemplate.update(
            """
                INSERT INTO support_request_status_history (
                    support_request_id, status, note, changed_by_user_id, created_at
                ) VALUES (?, ?, ?, ?, ?)
                """,
            supportRequestId,
            status,
            "Seeded support request",
            requesterUserId,
            now
        );
    }
}
