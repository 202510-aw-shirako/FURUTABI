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
class IntegratedNavigationFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpIntegratedData() {
        Timestamp now = Timestamp.from(Instant.parse("2026-03-27T00:00:00Z"));

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
        jdbcTemplate.update("DELETE FROM contact_preferences");
        jdbcTemplate.update("DELETE FROM user_profiles");
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM users");

        insertUser(100L, "host@example.com", "host-user", now);
        insertUser(101L, "guest@example.com", "guest-user", now);
        insertUser(102L, "bridge@example.com", "bridge-user", now);

        insertRole(100L, "LOCAL", now);
        insertRole(101L, "USER", now);
        insertRole(102L, "BRIDGE", now);

        insertProposal(701L, "LOCAL_GUIDE", 100L, 102L, "Gate proposal", now);
        insertApplication(801L, 701L, 101L, "accepted", 901L, now);
        insertChatThread(901L, 101L, 100L, 801L, now);
        insertChatMessage(1001L, 901L, 101L, "USER", "Hello host", now);

        insertMapRecord(501L, 101L, "Public map", "public", now);
        insertMapRecord(502L, 101L, "Private map", "private", now);
    }

    @Test
    @DisplayName("proposal detail shows thin links to related chat and history")
    void proposalDetailShowsRelatedChatAndHistoryLinks() throws Exception {
        mockMvc.perform(get("/app/gate/701").with(user("guest@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("href=\"/app/chat/901\"")))
            .andExpect(content().string(containsString("href=\"/app/history\"")));
    }

    @Test
    @DisplayName("chat detail keeps links back to proposal, history, and notifications")
    void chatDetailShowsThinReturnLinks() throws Exception {
        mockMvc.perform(get("/app/chat/901").with(user("guest@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("href=\"/app/gate/701\"")))
            .andExpect(content().string(containsString("href=\"/app/history\"")))
            .andExpect(content().string(containsString("href=\"/app/notifications\"")));
    }

    @Test
    @DisplayName("map detail links to footprint only when the record is visible beyond private scope")
    void mapDetailShowsFootprintLinkOnlyForNonPrivateRecords() throws Exception {
        mockMvc.perform(get("/app/map-records/501").with(user("guest@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("href=\"/app/footprints/501\"")))
            .andExpect(content().string(containsString("href=\"/app/history\"")));

        mockMvc.perform(get("/app/map-records/502").with(user("guest@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("href=\"/app/footprints/502\""))));
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

    private void insertProposal(long proposalId, String proposalType, long hostUserId, long bridgeUserId, String title, Timestamp now) {
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
            60,
            "Kamakura",
            "published",
            "public",
            null,
            now,
            now,
            null
        );
    }

    private void insertApplication(long applicationId, long proposalId, long applicantUserId, String status, long relatedThreadId, Timestamp now) {
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
            "Accepted application chat",
            "open",
            0,
            false,
            "/app/gate/701",
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
