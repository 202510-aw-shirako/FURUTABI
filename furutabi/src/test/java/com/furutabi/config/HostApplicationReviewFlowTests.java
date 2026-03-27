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
class HostApplicationReviewFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpReviewData() {
        Timestamp now = Timestamp.from(Instant.parse("2026-03-25T00:00:00Z"));

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
        insertUser(102L, "other-bridge@example.com", "other-bridge", now);
        insertUser(103L, "guest@example.com", "guest-user", now);
        insertUser(104L, "viewer@example.com", "viewer-user", now);

        insertRole(100L, "LOCAL", now);
        insertRole(101L, "BRIDGE", now);
        insertRole(102L, "BRIDGE", now);
        insertRole(103L, "USER", now);
        insertRole(104L, "USER", now);

        insertProposal(701L, "LOCAL_GUIDE", 100L, 101L, "Local host gate", "public", "published", now);
        insertProposal(702L, "OKATTE", 101L, 101L, "Bridge host okatte", "public", "published", now);

        insertApplication(801L, 701L, 103L, "pending", Timestamp.from(Instant.parse("2026-03-25T01:00:00Z")));
        insertApplication(802L, 702L, 103L, "pending", Timestamp.from(Instant.parse("2026-03-25T01:10:00Z")));
    }

    @Test
    @DisplayName("proposal host can view pending application list and detail")
    void proposalHostCanViewPendingApplicationListAndDetail() throws Exception {
        mockMvc.perform(get("/app/host-applications").with(user("local@example.com").roles("LOCAL")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Local host gate")))
            .andExpect(content().string(not(containsString("Bridge host okatte"))));

        mockMvc.perform(get("/app/host-applications/801").with(user("local@example.com").roles("LOCAL")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("承認する")))
            .andExpect(content().string(containsString("拒否する")));

        mockMvc.perform(get("/app/host-applications").with(user("bridge@example.com").roles("BRIDGE")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Bridge host okatte")))
            .andExpect(content().string(not(containsString("Local host gate"))));

        mockMvc.perform(get("/app/host-applications/802").with(user("bridge@example.com").roles("BRIDGE")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("承認する")))
            .andExpect(content().string(containsString("拒否する")));
    }

    @Test
    @DisplayName("non-host users cannot review host applications")
    void nonHostUsersCannotReviewHostApplications() throws Exception {
        mockMvc.perform(get("/app/host-applications").with(user("other-bridge@example.com").roles("BRIDGE")))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("Bridge host okatte"))))
            .andExpect(content().string(not(containsString("Local host gate"))));

        mockMvc.perform(get("/app/host-applications/801").with(user("bridge@example.com").roles("BRIDGE")))
            .andExpect(status().isNotFound());

        mockMvc.perform(post("/app/host-applications/801/accept").with(user("viewer@example.com").roles("USER")).with(csrf()))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("local host can accept pending application")
    void localHostCanAcceptPendingApplication() throws Exception {
        mockMvc.perform(post("/app/host-applications/801/accept").with(user("local@example.com").roles("LOCAL")).with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/host-applications/801?accepted"));

        String applicationStatus = jdbcTemplate.queryForObject(
            "SELECT application_status FROM proposal_applications WHERE id = ?",
            String.class,
            801L
        );
        Integer acceptedHistoryCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM proposal_application_status_history WHERE proposal_application_id = ? AND status = ?",
            Integer.class,
            801L,
            "accepted"
        );
        Long relatedThreadId = jdbcTemplate.queryForObject(
            "SELECT related_thread_id FROM proposal_applications WHERE id = ?",
            Long.class,
            801L
        );
        String threadStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM chat_threads WHERE id = ?",
            String.class,
            relatedThreadId
        );

        Assertions.assertEquals("accepted", applicationStatus);
        Assertions.assertEquals(1, acceptedHistoryCount);
        Assertions.assertNotNull(relatedThreadId);
        Assertions.assertEquals("open", threadStatus);
    }

    @Test
    @DisplayName("bridge host can reject pending application")
    void bridgeHostCanRejectPendingApplication() throws Exception {
        mockMvc.perform(post("/app/host-applications/802/reject").with(user("bridge@example.com").roles("BRIDGE")).with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/host-applications/802?rejected"));

        String applicationStatus = jdbcTemplate.queryForObject(
            "SELECT application_status FROM proposal_applications WHERE id = ?",
            String.class,
            802L
        );
        Integer rejectedHistoryCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM proposal_application_status_history WHERE proposal_application_id = ? AND status = ?",
            Integer.class,
            802L,
            "rejected"
        );

        Assertions.assertEquals("rejected", applicationStatus);
        Assertions.assertEquals(1, rejectedHistoryCount);
    }

    @Test
    @DisplayName("reject closes already linked chat thread when one exists")
    void rejectClosesLinkedChatThread() throws Exception {
        Timestamp now = Timestamp.from(Instant.parse("2026-03-25T02:30:00Z"));
        jdbcTemplate.update(
            """
                INSERT INTO chat_threads (
                    id, user_id, counterpart_id, counterpart_role, related_entity_type, related_entity_id,
                    title, status, unread_count, requires_attention, related_url, latest_message_preview,
                    latest_message_at, created_at, updated_at, closed_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            901L,
            103L,
            101L,
            "BRIDGE",
            "PROPOSAL_APPLICATION",
            802L,
            "Bridge host okatte",
            "open",
            0,
            false,
            "/app/host-applications/802",
            null,
            null,
            now,
            now,
            null,
            null
        );
        jdbcTemplate.update(
            "UPDATE proposal_applications SET related_thread_id = ?, updated_at = ? WHERE id = ?",
            901L,
            now,
            802L
        );

        mockMvc.perform(post("/app/host-applications/802/reject").with(user("bridge@example.com").roles("BRIDGE")).with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/host-applications/802?rejected"));

        String threadStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM chat_threads WHERE id = ?",
            String.class,
            901L
        );
        Assertions.assertEquals("closed", threadStatus);
    }

    @Test
    @DisplayName("review cannot be repeated once application is no longer pending")
    void reviewCannotBeRepeated() throws Exception {
        jdbcTemplate.update(
            "UPDATE proposal_applications SET application_status = ?, updated_at = ? WHERE id = ?",
            "accepted",
            Timestamp.from(Instant.parse("2026-03-25T02:00:00Z")),
            801L
        );

        mockMvc.perform(post("/app/host-applications/801/reject").with(user("local@example.com").roles("LOCAL")).with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/host-applications/801?blocked"));
    }

    @Test
    @DisplayName("unauthenticated host review route redirects to login")
    void unauthenticatedHostReviewRouteRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/app/host-applications"))
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

    private void insertProposal(
        long proposalId,
        String proposalType,
        long hostUserId,
        long bridgeUserId,
        String title,
        String visibility,
        String status,
        Timestamp now
    ) {
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
            45,
            "Kamakura",
            status,
            visibility,
            null,
            now,
            now,
            null
        );
    }

    private void insertApplication(long applicationId, long proposalId, long applicantUserId, String status, Timestamp now) {
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
            null,
            false,
            now,
            now,
            null
        );
    }
}
