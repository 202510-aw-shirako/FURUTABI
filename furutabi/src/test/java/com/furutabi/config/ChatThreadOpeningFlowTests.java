package com.furutabi.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class ChatThreadOpeningFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpChatOpeningData() {
        Timestamp now = Timestamp.from(Instant.parse("2026-03-26T00:00:00Z"));

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

        insertRole(100L, "LOCAL", now);
        insertRole(101L, "BRIDGE", now);
        insertRole(102L, "USER", now);

        insertProposal(701L, "LOCAL_GUIDE", 100L, 101L, "Local host gate", now);
        insertProposal(702L, "OKATTE", 101L, 101L, "Bridge host okatte", now);

        insertApplication(801L, 701L, 102L, "pending", null, now);
        insertApplication(802L, 702L, 102L, "pending", 901L, now);

        jdbcTemplate.update(
            """
                INSERT INTO chat_threads (
                    id, user_id, counterpart_id, counterpart_role, related_entity_type, related_entity_id,
                    title, status, unread_count, requires_attention, related_url, latest_message_preview,
                    latest_message_at, created_at, updated_at, closed_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            901L,
            102L,
            101L,
            "BRIDGE",
            "PROPOSAL_APPLICATION",
            802L,
            "Bridge host okatte",
            "closed",
            0,
            false,
            "/app/host-applications/802",
            null,
            null,
            now,
            now,
            now,
            null
        );
    }

    @Test
    @DisplayName("accept creates chat thread for local-hosted application")
    void acceptCreatesChatThreadForLocalHostedApplication() throws Exception {
        mockMvc.perform(post("/app/host-applications/801/accept").with(user("local@example.com").roles("LOCAL")).with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/host-applications/801?accepted"));

        Long relatedThreadId = jdbcTemplate.queryForObject(
            "SELECT related_thread_id FROM proposal_applications WHERE id = ?",
            Long.class,
            801L
        );

        Assertions.assertNotNull(relatedThreadId);
        Assertions.assertEquals(
            1,
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM chat_threads WHERE id = ? AND user_id = ? AND counterpart_id = ? AND status = ?",
                Integer.class,
                relatedThreadId,
                102L,
                100L,
                "open"
            )
        );
    }

    @Test
    @DisplayName("accept reopens existing chat thread for bridge-hosted application")
    void acceptReopensExistingChatThreadForBridgeHostedApplication() throws Exception {
        mockMvc.perform(post("/app/host-applications/802/accept").with(user("bridge@example.com").roles("BRIDGE")).with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/host-applications/802?accepted"));

        Long relatedThreadId = jdbcTemplate.queryForObject(
            "SELECT related_thread_id FROM proposal_applications WHERE id = ?",
            Long.class,
            802L
        );
        String threadStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM chat_threads WHERE id = ?",
            String.class,
            901L
        );
        Timestamp closedAt = jdbcTemplate.queryForObject(
            "SELECT closed_at FROM chat_threads WHERE id = ?",
            Timestamp.class,
            901L
        );

        Assertions.assertEquals(901L, relatedThreadId);
        Assertions.assertEquals("open", threadStatus);
        Assertions.assertNull(closedAt);
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
            title + " summary",
            title + " body",
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
            "Pending application",
            now,
            relatedThreadId,
            false,
            now,
            now,
            null
        );
    }
}
