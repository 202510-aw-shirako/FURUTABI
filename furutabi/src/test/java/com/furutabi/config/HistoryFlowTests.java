package com.furutabi.config;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;

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
class HistoryFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpHistoryData() {
        Timestamp now = Timestamp.from(Instant.parse("2026-03-27T00:00:00Z"));
        Timestamp later = Timestamp.from(Instant.parse("2026-03-27T02:00:00Z"));

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
        jdbcTemplate.update("DELETE FROM sms_verifications");
        jdbcTemplate.update("DELETE FROM contact_preferences");
        jdbcTemplate.update("DELETE FROM user_profiles");
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM users");

        insertUser(100L, "history@example.com", "history-user", now);
        insertUser(101L, "local@example.com", "local-user", now);
        insertUser(102L, "bridge@example.com", "bridge-user", now);
        insertUser(103L, "other@example.com", "other-user", now);

        insertRole(100L, "USER", now);
        insertRole(101L, "LOCAL", now);
        insertRole(102L, "BRIDGE", now);
        insertRole(103L, "USER", now);

        insertProposal(701L, "LOCAL_GUIDE", 100L, 102L, later, "Host proposal");
        insertProposal(702L, "OKATTE", 101L, 102L, now, "Applied okatte");

        insertApplication(801L, 702L, 100L, "accepted", 901L, later);
        insertChatThread(901L, 100L, 101L, 801L, later);
        insertChatMessage(1001L, 901L, 100L, "USER", "Hello host", later);

        insertMapRecord(1101L, 100L, "Personal map", "private", false, now, now, "Personal map body");
        insertMapRecord(1102L, 100L, "Visible footprint", "public", false, now, later, "Visible footprint body");
        insertMapRecord(1103L, 103L, "Other user map", "public", false, now, later, "Other map body");
    }

    @Test
    @DisplayName("history list aggregates only current user involvement")
    void historyListAggregatesOnlyCurrentUserInvolvement() throws Exception {
        mockMvc.perform(get("/app/history").with(user("history@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Host proposal")))
            .andExpect(content().string(containsString("Applied okatte")))
            .andExpect(content().string(containsString("Accepted chat thread")))
            .andExpect(content().string(containsString("Personal map")))
            .andExpect(content().string(containsString("Visible footprint")))
            .andExpect(content().string(not(containsString("Other user map"))));
    }

    @Test
    @DisplayName("other user sees only their own history")
    void otherUserSeesOnlyOwnHistory() throws Exception {
        mockMvc.perform(get("/app/history").with(user("other@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Other user map")))
            .andExpect(content().string(not(containsString("Host proposal"))))
            .andExpect(content().string(not(containsString("Applied okatte"))))
            .andExpect(content().string(not(containsString("Accepted chat thread"))));
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

    private void insertProposal(long proposalId, String proposalType, long hostUserId, long bridgeUserId, Timestamp now, String title) {
        jdbcTemplate.update(
            """
                INSERT INTO proposals (
                    id, proposal_type, bridge_user_id, host_user_id, title, summary, body,
                    duration_minutes, location_name, status, visibility_scope, cover_image_path,
                    created_at, updated_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            proposalId,
            proposalType,
            bridgeUserId,
            hostUserId,
            title,
            "summary",
            "body",
            45,
            "Kamakura",
            "published",
            "public",
            null,
            now,
            now,
            null
        );
    }

    private void insertApplication(
        long applicationId,
        long proposalId,
        long applicantUserId,
        String status,
        long relatedThreadId,
        Timestamp now
    ) {
        jdbcTemplate.update(
            """
                INSERT INTO proposal_applications (
                    id, proposal_id, applicant_user_id, application_status, latest_message_preview,
                    latest_message_at, related_thread_id, requires_additional_verification,
                    applied_at, updated_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            applicationId,
            proposalId,
            applicantUserId,
            status,
            "Hello host",
            now,
            relatedThreadId,
            false,
            now,
            now,
            null
        );
    }

    private void insertChatThread(long threadId, long userId, long counterpartId, long applicationId, Timestamp now) {
        jdbcTemplate.update(
            """
                INSERT INTO chat_threads (
                    id, user_id, counterpart_id, counterpart_role, related_entity_type, related_entity_id,
                    title, status, unread_count, requires_attention, related_url, latest_message_preview,
                    latest_message_at, created_at, updated_at, closed_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            threadId,
            userId,
            counterpartId,
            "LOCAL",
            "PROPOSAL_APPLICATION",
            applicationId,
            "Accepted chat thread",
            "open",
            0,
            false,
            "/app/host-applications/" + applicationId,
            "Hello host",
            now,
            now,
            now,
            null,
            null
        );
    }

    private void insertChatMessage(long messageId, long threadId, long senderId, String senderRole, String body, Timestamp now) {
        jdbcTemplate.update(
            """
                INSERT INTO chat_messages (
                    id, thread_id, sender_id, sender_role, body, is_system_message, read_at, created_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            messageId,
            threadId,
            senderId,
            senderRole,
            body,
            false,
            null,
            now,
            null
        );
    }

    private void insertMapRecord(
        long mapRecordId,
        long userId,
        String title,
        String visibility,
        boolean draft,
        Timestamp createdAt,
        Timestamp updatedAt,
        String body
    ) {
        jdbcTemplate.update(
            """
                INSERT INTO map_records (
                    id, user_id, title, body, visibility, location_name, latitude, longitude,
                    location_precision_level, is_draft, created_at, updated_at, visibility_updated_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            mapRecordId,
            userId,
            title,
            body,
            visibility,
            "Kamakura",
            null,
            null,
            "area",
            draft,
            createdAt,
            updatedAt,
            updatedAt,
            null
        );
    }
}
