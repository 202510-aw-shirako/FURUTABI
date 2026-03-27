package com.furutabi.config;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
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
class GateFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpGateData() {
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
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM users");

        insertUser(100L, "owner@example.com", "owner-user", now);
        insertUser(101L, "bridge@example.com", "bridge-user", now);
        insertUser(102L, "viewer@example.com", "viewer-user", now);

        insertRole(100L, "USER", now);
        insertRole(101L, "BRIDGE", now);
        insertRole(102L, "USER", now);

        insertProposal(701L, "LOCAL_GUIDE", 100L, 101L, "Public gate", "public", "published", now);
        insertProposal(702L, "LOCAL_GUIDE", 100L, 101L, "Private gate", "private", "published", now);
        insertProposal(703L, "LOCAL_GUIDE", 100L, 101L, "Limited gate", "limited", "published", now);
        insertProposal(704L, "LOCAL_GUIDE", 100L, 101L, "Deleted gate", "public", "published", now);
        insertProposal(705L, "LOCAL_GUIDE", 100L, 101L, "Draft gate", "public", "draft", now);
        insertProposal(706L, "OKATTE", 100L, 101L, "Okatte proposal", "public", "published", now);

        jdbcTemplate.update("UPDATE proposals SET deleted_at = ?, updated_at = ? WHERE id = ?", now, now, 704L);
        jdbcTemplate.update(
            "INSERT INTO proposal_tags (proposal_id, tag_name, sort_order, created_at) VALUES (?, ?, ?, ?)",
            701L,
            "海辺",
            0,
            now
        );
    }

    @Test
    @DisplayName("owner sees public, private, and limited gates in list")
    void ownerSeesOwnVisibleGatesInList() throws Exception {
        mockMvc.perform(get("/app/gate").with(user("owner@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Public gate")))
            .andExpect(content().string(containsString("Private gate")))
            .andExpect(content().string(containsString("Limited gate")))
            .andExpect(content().string(not(containsString("Deleted gate"))))
            .andExpect(content().string(not(containsString("Draft gate"))))
            .andExpect(content().string(not(containsString("Okatte proposal"))));
    }

    @Test
    @DisplayName("generic viewer sees only public gates in list")
    void viewerSeesOnlyPublicGatesInList() throws Exception {
        mockMvc.perform(get("/app/gate").with(user("viewer@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Public gate")))
            .andExpect(content().string(not(containsString("Private gate"))))
            .andExpect(content().string(not(containsString("Limited gate"))));
    }

    @Test
    @DisplayName("bridge user sees limited gate in list because relation already exists")
    void bridgeSeesLimitedGateInList() throws Exception {
        mockMvc.perform(get("/app/gate").with(user("bridge@example.com").roles("BRIDGE")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Public gate")))
            .andExpect(content().string(containsString("Limited gate")))
            .andExpect(content().string(not(containsString("Private gate"))));
    }

    @Test
    @DisplayName("public gate detail is visible to generic viewer")
    void publicGateDetailIsVisibleToViewer() throws Exception {
        mockMvc.perform(get("/app/gate/701").with(user("viewer@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Public gate")))
            .andExpect(content().string(containsString("owner-user")))
            .andExpect(content().string(containsString("海辺")));
    }

    @Test
    @DisplayName("private gate detail is hidden from generic viewer")
    void privateGateDetailIsHiddenFromViewer() throws Exception {
        mockMvc.perform(get("/app/gate/702").with(user("viewer@example.com").roles("USER")))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("limited gate detail is visible to bridge but not generic viewer")
    void limitedGateDetailUsesExistingRelationRule() throws Exception {
        mockMvc.perform(get("/app/gate/703").with(user("bridge@example.com").roles("BRIDGE")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Limited gate")));

        mockMvc.perform(get("/app/gate/703").with(user("viewer@example.com").roles("USER")))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("unauthenticated gate list still redirects to login")
    void unauthenticatedGateListRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/app/gate"))
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
}
