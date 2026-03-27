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

import com.furutabi.relation.RelatedUserService;

@SpringBootTest
@AutoConfigureMockMvc
class ProposalApplicationFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RelatedUserService relatedUserService;

    @BeforeEach
    void setUpApplicationData() {
        Timestamp now = Timestamp.from(Instant.parse("2026-03-25T00:00:00Z"));

        jdbcTemplate.update("DELETE FROM chat_messages");
        jdbcTemplate.update("DELETE FROM chat_threads");
        jdbcTemplate.update("DELETE FROM notification_delivery_logs");
        jdbcTemplate.update("DELETE FROM notifications");
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

        insertUser(100L, "host@example.com", "host-user", now);
        insertUser(101L, "bridge@example.com", "bridge-user", now);
        insertUser(102L, "guest@example.com", "guest-user", now);
        insertUser(103L, "viewer@example.com", "viewer-user", now);

        insertRole(100L, "USER", now);
        insertRole(101L, "BRIDGE", now);
        insertRole(102L, "USER", now);
        insertRole(103L, "USER", now);

        insertProposal(701L, 100L, 101L, "Public gate", "public", "published", now);
    }

    @Test
    @DisplayName("authenticated viewer can open gate application page")
    void authenticatedViewerCanOpenApplicationPage() throws Exception {
        mockMvc.perform(get("/app/gate/701/apply").with(user("guest@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Public gate")))
            .andExpect(content().string(containsString("申請を作成する")));
    }

    @Test
    @DisplayName("authenticated viewer can create proposal application")
    void authenticatedViewerCanCreateProposalApplication() throws Exception {
        mockMvc.perform(
                post("/app/gate/701/apply")
                    .with(user("guest@example.com").roles("USER"))
                    .with(csrf())
                    .param("applicantMessage", "参加してみたいです")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/gate/701?applied"));

        Long applicationId = jdbcTemplate.queryForObject(
            "SELECT id FROM proposal_applications WHERE proposal_id = ? AND applicant_user_id = ? AND deleted_at IS NULL",
            Long.class,
            701L,
            102L
        );
        Assertions.assertNotNull(applicationId);

        String applicationStatus = jdbcTemplate.queryForObject(
            "SELECT application_status FROM proposal_applications WHERE id = ?",
            String.class,
            applicationId
        );
        Long relatedThreadId = jdbcTemplate.queryForObject(
            "SELECT related_thread_id FROM proposal_applications WHERE id = ?",
            Long.class,
            applicationId
        );
        Integer historyCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM proposal_application_status_history WHERE proposal_application_id = ? AND status = ?",
            Integer.class,
            applicationId,
            "pending"
        );

        Assertions.assertEquals("pending", applicationStatus);
        Assertions.assertNull(relatedThreadId);
        Assertions.assertEquals(1, historyCount);
        Assertions.assertTrue(relatedUserService.isProposalApplicant(102L, 701L));
        Assertions.assertTrue(relatedUserService.isProposalApplicationParty(100L, applicationId));
        Assertions.assertTrue(relatedUserService.isProposalApplicationParty(102L, applicationId));
    }

    @Test
    @DisplayName("duplicate application is blocked")
    void duplicateApplicationIsBlocked() throws Exception {
        insertApplication(801L, 701L, 102L, "pending", Timestamp.from(Instant.parse("2026-03-25T01:00:00Z")));

        mockMvc.perform(
                post("/app/gate/701/apply")
                    .with(user("guest@example.com").roles("USER"))
                    .with(csrf())
                    .param("applicantMessage", "もう一度送る")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/gate/701/apply?blocked"));

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM proposal_applications WHERE proposal_id = ? AND applicant_user_id = ? AND deleted_at IS NULL",
            Integer.class,
            701L,
            102L
        );
        Assertions.assertEquals(1, count);
    }

    @Test
    @DisplayName("host cannot apply to own gate")
    void hostCannotApplyToOwnGate() throws Exception {
        mockMvc.perform(get("/app/gate/701/apply").with(user("host@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("今は申請できません")))
            .andExpect(content().string(not(containsString("申請を作成する"))));

        mockMvc.perform(
                post("/app/gate/701/apply")
                    .with(user("host@example.com").roles("USER"))
                    .with(csrf())
                    .param("applicantMessage", "自分で申し込む")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/gate/701/apply?blocked"));
    }

    @Test
    @DisplayName("unauthenticated application route redirects to login")
    void unauthenticatedApplicationRouteRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/app/gate/701/apply"))
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
            "LOCAL_GUIDE",
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
            "Existing application",
            now,
            null,
            false,
            now,
            now,
            null
        );
    }
}
