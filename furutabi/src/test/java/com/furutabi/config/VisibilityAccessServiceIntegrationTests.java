package com.furutabi.config;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.furutabi.visibility.VisibilityAccessService;
import com.furutabi.visibility.VisibilityScope;

@SpringBootTest
class VisibilityAccessServiceIntegrationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private VisibilityAccessService visibilityAccessService;

    @BeforeEach
    void setUpVisibilityData() {
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

        insertUser(101L, "owner@example.com", now);
        insertUser(102L, "bridge@example.com", now);
        insertUser(103L, "guest@example.com", now);
        insertUser(104L, "outsider@example.com", now);
        insertUser(105L, "admin@example.com", now);
        insertUser(106L, "local@example.com", now);

        insertRole(101L, "USER", now);
        insertRole(102L, "BRIDGE", now);
        insertRole(103L, "USER", now);
        insertRole(104L, "USER", now);
        insertRole(105L, "ADMIN", now);
        insertRole(106L, "LOCAL", now);

        insertProposal(201L, 101L, 102L, "public", now);
        insertProposal(202L, 101L, 102L, "private", now);
        insertProposal(203L, 101L, 102L, "limited", now);

        jdbcTemplate.update(
            """
                INSERT INTO proposal_applications (
                    id, proposal_id, applicant_user_id, application_status, latest_message_preview,
                    latest_message_at, related_thread_id, requires_additional_verification,
                    applied_at, updated_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            301L,
            203L,
            103L,
            "pending",
            "Hello",
            now,
            null,
            false,
            now,
            now,
            null
        );

        insertMapRecord(401L, 101L, "public", now);
        insertMapRecord(402L, 101L, "private", now);
        insertMapRecord(403L, 101L, "limited", now);
    }

    @Test
    @DisplayName("PUBLIC visibility is viewable without authentication")
    void publicVisibilityIsVisibleWithoutAuthentication() {
        Assertions.assertTrue(visibilityAccessService.canViewProposal(null, 201L));
        Assertions.assertTrue(visibilityAccessService.canViewMapRecord(null, 401L));
    }

    @Test
    @DisplayName("PRIVATE visibility is limited to owner")
    void privateVisibilityIsLimitedToOwner() {
        Assertions.assertTrue(visibilityAccessService.canViewProposal(101L, 202L));
        Assertions.assertFalse(visibilityAccessService.canViewProposal(103L, 202L));
        Assertions.assertTrue(visibilityAccessService.canViewMapRecord(101L, 402L));
        Assertions.assertFalse(visibilityAccessService.canViewMapRecord(103L, 402L));
    }

    @Test
    @DisplayName("LIMITED proposal visibility allows relation users but not generic logged-in users")
    void limitedProposalVisibilityUsesRelationChecks() {
        Assertions.assertTrue(visibilityAccessService.canViewProposal(101L, 203L));
        Assertions.assertTrue(visibilityAccessService.canViewProposal(102L, 203L));
        Assertions.assertTrue(visibilityAccessService.canViewProposal(103L, 203L));
        Assertions.assertFalse(visibilityAccessService.canViewProposal(104L, 203L));
        Assertions.assertFalse(visibilityAccessService.canViewProposal(105L, 203L));
        Assertions.assertFalse(visibilityAccessService.canViewProposal(106L, 203L));
        Assertions.assertFalse(visibilityAccessService.canViewProposal(null, 203L));
    }

    @Test
    @DisplayName("LIMITED map visibility does not become logged-in-wide without relation basis")
    void limitedMapVisibilityDoesNotBecomeLoggedInWide() {
        Assertions.assertTrue(visibilityAccessService.canViewMapRecord(101L, 403L));
        Assertions.assertFalse(visibilityAccessService.canViewMapRecord(103L, 403L));
        Assertions.assertFalse(visibilityAccessService.canViewMapRecord(105L, 403L));
    }

    @Test
    @DisplayName("generic canView keeps role support separate from visibility core")
    void genericCanViewKeepsRoleSupportSeparate() {
        Assertions.assertTrue(visibilityAccessService.canView(VisibilityScope.PUBLIC, null, 101L, false));
        Assertions.assertTrue(visibilityAccessService.canView(VisibilityScope.PRIVATE, 101L, 101L, false));
        Assertions.assertFalse(visibilityAccessService.canView(VisibilityScope.PRIVATE, 103L, 101L, true));
        Assertions.assertTrue(visibilityAccessService.canView(VisibilityScope.LIMITED, 103L, 101L, true));
        Assertions.assertFalse(visibilityAccessService.canView(VisibilityScope.LIMITED, 103L, 101L, false));
    }

    private void insertUser(long id, String email, Timestamp now) {
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
            email,
            email,
            email,
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

    private void insertProposal(long proposalId, long hostUserId, long bridgeUserId, String visibilityScope, Timestamp now) {
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
            "Visibility proposal",
            "Visibility summary",
            "Visibility body",
            60,
            "Kamakura",
            "published",
            visibilityScope,
            null,
            now,
            now,
            null
        );
    }

    private void insertMapRecord(long mapRecordId, long ownerUserId, String visibility, Timestamp now) {
        jdbcTemplate.update(
            """
                INSERT INTO map_records (
                    id, user_id, title, body, visibility, location_name, latitude, longitude,
                    location_precision_level, is_draft, created_at, updated_at, visibility_updated_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            mapRecordId,
            ownerUserId,
            "Visibility map",
            "Visibility note",
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
