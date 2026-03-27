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
class NotificationFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpNotificationData() {
        Timestamp now = Timestamp.from(Instant.parse("2026-03-27T00:00:00Z"));

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

        insertUser(100L, "local@example.com", "local-user", now);
        insertUser(101L, "guest@example.com", "guest-user", now);
        insertUser(102L, "owner@example.com", "owner-user", now);
        insertUser(103L, "other@example.com", "other-user", now);
        insertUser(104L, "bridge@example.com", "bridge-user", now);

        insertRole(100L, "LOCAL", now);
        insertRole(101L, "USER", now);
        insertRole(102L, "USER", now);
        insertRole(103L, "USER", now);
        insertRole(104L, "BRIDGE", now);

        insertProposal(701L, "LOCAL_GUIDE", 100L, 104L, "Local host gate", now);
        insertProposal(702L, "OKATTE", 100L, 104L, "Accepted application chat", now);

        insertApplication(801L, 701L, 101L, "pending", null, now);
        insertApplication(802L, 702L, 101L, "accepted", 901L, now);
        insertThread(901L, 101L, 100L, "LOCAL", 802L, "open", now);
        insertMessage(1001L, 901L, 101L, "USER", "First hello", now);

        insertMapRecord(501L, 102L, "Public record", "public", now);
        insertNotification(990L, 101L, "chat_message", "Guest notification", "/app/chat/901", now);
    }

    @Test
    @DisplayName("host acceptance creates applicant notification that can be opened and marked read")
    void hostAcceptanceCreatesApplicantNotification() throws Exception {
        mockMvc.perform(post("/app/host-applications/801/accept")
                .with(user("local@example.com").roles("LOCAL"))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/host-applications/801?accepted"));

        Long notificationId = jdbcTemplate.queryForObject(
            """
                SELECT id
                FROM notifications
                WHERE user_id = ? AND type = ?
                ORDER BY created_at DESC, id DESC
                FETCH FIRST 1 ROWS ONLY
                """,
            Long.class,
            101L,
            "application_accepted"
        );
        String relatedUrl = jdbcTemplate.queryForObject(
            "SELECT related_url FROM notifications WHERE id = ?",
            String.class,
            notificationId
        );

        Assertions.assertNotNull(notificationId);
        Assertions.assertNotNull(relatedUrl);
        Assertions.assertTrue(relatedUrl.startsWith("/app/chat/"));

        mockMvc.perform(get("/app/notifications").with(user("guest@example.com").roles("USER")))
            .andExpect(status().isOk());

        mockMvc.perform(post("/app/notifications/" + notificationId + "/open")
                .with(user("guest@example.com").roles("USER"))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(relatedUrl));

        Boolean isRead = jdbcTemplate.queryForObject(
            "SELECT is_read FROM notifications WHERE id = ?",
            Boolean.class,
            notificationId
        );
        Timestamp readAt = jdbcTemplate.queryForObject(
            "SELECT read_at FROM notifications WHERE id = ?",
            Timestamp.class,
            notificationId
        );

        Assertions.assertEquals(Boolean.TRUE, isRead);
        Assertions.assertNotNull(readAt);
    }

    @Test
    @DisplayName("new chat message creates notification for counterpart")
    void newChatMessageCreatesNotification() throws Exception {
        mockMvc.perform(post("/app/chat/901/messages")
                .with(user("local@example.com").roles("LOCAL"))
                .with(csrf())
                .param("body", "Reply from host"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/chat/901?sent"));

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = ?",
            Integer.class,
            101L,
            "chat_message"
        );
        String latestUrl = jdbcTemplate.queryForObject(
            """
                SELECT related_url
                FROM notifications
                WHERE user_id = ? AND type = ?
                ORDER BY created_at DESC, id DESC
                FETCH FIRST 1 ROWS ONLY
                """,
            String.class,
            101L,
            "chat_message"
        );

        Assertions.assertEquals(2, count);
        Assertions.assertEquals("/app/chat/901", latestUrl);
    }

    @Test
    @DisplayName("new map comment creates notification for map owner")
    void newMapCommentCreatesNotification() throws Exception {
        mockMvc.perform(post("/app/map-records/501/comments")
                .with(user("guest@example.com").roles("USER"))
                .with(csrf())
                .param("body", "Nice map"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/map-records/501?commentSaved"));

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = ?",
            Integer.class,
            102L,
            "map_comment"
        );
        String relatedUrl = jdbcTemplate.queryForObject(
            """
                SELECT related_url
                FROM notifications
                WHERE user_id = ? AND type = ?
                ORDER BY created_at DESC, id DESC
                FETCH FIRST 1 ROWS ONLY
                """,
            String.class,
            102L,
            "map_comment"
        );

        Assertions.assertEquals(1, count);
        Assertions.assertEquals("/app/map-records/501", relatedUrl);
    }

    @Test
    @DisplayName("users cannot view or open notifications that belong to someone else")
    void usersCannotViewOrOpenOthersNotifications() throws Exception {
        mockMvc.perform(get("/app/notifications").with(user("other@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("Guest notification"))));

        mockMvc.perform(post("/app/notifications/990/open")
                .with(user("other@example.com").roles("USER"))
                .with(csrf()))
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

    private void insertApplication(long applicationId, long proposalId, long applicantUserId, String status, Long relatedThreadId, Timestamp now) {
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
            null,
            now,
            relatedThreadId,
            false,
            now,
            now,
            null
        );
    }

    private void insertThread(
        long threadId,
        long userId,
        long counterpartId,
        String counterpartRole,
        long applicationId,
        String status,
        Timestamp now
    ) {
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
            counterpartRole,
            "PROPOSAL_APPLICATION",
            applicationId,
            "Accepted application chat",
            status,
            0,
            false,
            "/app/chat/" + threadId,
            "First hello",
            now,
            now,
            now,
            null,
            null
        );
    }

    private void insertMessage(long messageId, long threadId, long senderId, String senderRole, String body, Timestamp now) {
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

    private void insertNotification(long notificationId, long recipientUserId, String type, String title, String relatedUrl, Timestamp now) {
        jdbcTemplate.update(
            """
                INSERT INTO notifications (
                    id, user_id, type, title, body, related_entity_type, related_entity_id,
                    related_url, sender_name, sender_role, preview_text, severity,
                    action_label, is_read, created_at, read_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            notificationId,
            recipientUserId,
            type,
            title,
            title,
            "CHAT_THREAD",
            901L,
            relatedUrl,
            "system",
            "USER",
            title,
            "normal",
            "Open",
            false,
            now,
            null,
            null
        );
    }
}
