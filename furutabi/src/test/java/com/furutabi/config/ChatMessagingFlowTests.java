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
class ChatMessagingFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpChatMessagingData() {
        Timestamp now = Timestamp.from(Instant.parse("2026-03-27T00:00:00Z"));

        jdbcTemplate.update("DELETE FROM chat_messages");
        jdbcTemplate.update("DELETE FROM chat_threads");
        jdbcTemplate.update("DELETE FROM proposal_application_status_history");
        jdbcTemplate.update("DELETE FROM proposal_applications");
        jdbcTemplate.update("DELETE FROM proposal_tags");
        jdbcTemplate.update("DELETE FROM proposals");
        jdbcTemplate.update("DELETE FROM sms_verifications");
        jdbcTemplate.update("DELETE FROM contact_preferences");
        jdbcTemplate.update("DELETE FROM user_profiles");
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM users");

        insertUser(100L, "local@example.com", "local-user", now);
        insertUser(101L, "bridge@example.com", "bridge-user", now);
        insertUser(102L, "guest@example.com", "guest-user", now);
        insertUser(103L, "viewer@example.com", "viewer-user", now);

        insertRole(100L, "LOCAL", now);
        insertRole(101L, "BRIDGE", now);
        insertRole(102L, "USER", now);
        insertRole(103L, "USER", now);

        insertProposal(701L, 100L, 101L, now);
        insertApplication(801L, 701L, 102L, 901L, now);
        insertThread(901L, 102L, 100L, "LOCAL", 801L, "open", now);
        insertMessage(1001L, 901L, 102L, "USER", "First hello", now);
    }

    @Test
    @DisplayName("participants can view thread list and detail")
    void participantsCanViewThreadListAndDetail() throws Exception {
        mockMvc.perform(get("/app/chat").with(user("guest@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Accepted application chat")));

        mockMvc.perform(get("/app/chat").with(user("local@example.com").roles("LOCAL")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Accepted application chat")));

        mockMvc.perform(get("/app/chat/901").with(user("guest@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("First hello")));
    }

    @Test
    @DisplayName("non participants cannot view detail or send messages")
    void nonParticipantsCannotViewDetailOrSendMessages() throws Exception {
        mockMvc.perform(get("/app/chat").with(user("bridge@example.com").roles("BRIDGE")))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("Accepted application chat"))));

        mockMvc.perform(get("/app/chat/901").with(user("bridge@example.com").roles("BRIDGE")))
            .andExpect(status().isNotFound());

        mockMvc.perform(post("/app/chat/901/messages")
                .with(user("viewer@example.com").roles("USER"))
                .with(csrf())
                .param("body", "Should not pass"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("participant can send message on open thread")
    void participantCanSendMessageOnOpenThread() throws Exception {
        mockMvc.perform(post("/app/chat/901/messages")
                .with(user("local@example.com").roles("LOCAL"))
                .with(csrf())
                .param("body", "Reply from host"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/chat/901?sent"));

        Integer messageCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM chat_messages WHERE thread_id = ?",
            Integer.class,
            901L
        );
        String latestMessagePreview = jdbcTemplate.queryForObject(
            "SELECT latest_message_preview FROM chat_threads WHERE id = ?",
            String.class,
            901L
        );

        Assertions.assertEquals(2, messageCount);
        Assertions.assertEquals("Reply from host", latestMessagePreview);
    }

    @Test
    @DisplayName("closed thread cannot receive new message")
    void closedThreadCannotReceiveNewMessage() throws Exception {
        Timestamp now = Timestamp.from(Instant.parse("2026-03-27T01:00:00Z"));
        jdbcTemplate.update(
            "UPDATE chat_threads SET status = ?, closed_at = ?, updated_at = ? WHERE id = ?",
            "closed",
            now,
            now,
            901L
        );

        mockMvc.perform(post("/app/chat/901/messages")
                .with(user("guest@example.com").roles("USER"))
                .with(csrf())
                .param("body", "Late reply"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/chat/901?blocked"));

        Integer messageCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM chat_messages WHERE thread_id = ?",
            Integer.class,
            901L
        );
        Assertions.assertEquals(1, messageCount);
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

    private void insertProposal(long proposalId, long hostUserId, long bridgeUserId, Timestamp now) {
        jdbcTemplate.update(
            """
                INSERT INTO proposals (
                    id, proposal_type, bridge_user_id, host_user_id, title, summary, body,
                    duration_minutes, location_name, status, visibility_scope, cover_image_path,
                    created_at, updated_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            proposalId,
            "OKATTE",
            bridgeUserId,
            hostUserId,
            "Accepted application chat",
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

    private void insertApplication(long applicationId, long proposalId, long applicantUserId, long relatedThreadId, Timestamp now) {
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
            "accepted",
            "First hello",
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
            "/app/host-applications/" + applicationId,
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
}
