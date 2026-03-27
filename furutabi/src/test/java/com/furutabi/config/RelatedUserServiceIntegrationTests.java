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

import com.furutabi.relation.RelatedUserService;

@SpringBootTest
class RelatedUserServiceIntegrationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RelatedUserService relatedUserService;

    @BeforeEach
    void setUpRelationData() {
        Timestamp now = Timestamp.from(Instant.parse("2026-03-25T00:00:00Z"));

        jdbcTemplate.update("DELETE FROM chat_messages");
        jdbcTemplate.update("DELETE FROM chat_threads");
        jdbcTemplate.update("DELETE FROM notification_delivery_logs");
        jdbcTemplate.update("DELETE FROM notifications");
        jdbcTemplate.update("DELETE FROM proposal_application_status_history");
        jdbcTemplate.update("DELETE FROM proposal_applications");
        jdbcTemplate.update("DELETE FROM proposal_tags");
        jdbcTemplate.update("DELETE FROM proposals");
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM users");

        insertUser(101L, "host@example.com", now);
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

        jdbcTemplate.update(
            """
                INSERT INTO proposals (
                    id, proposal_type, bridge_user_id, host_user_id, title, summary, body,
                    duration_minutes, location_name, status, visibility_scope, cover_image_path,
                    created_at, updated_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            201L,
            "LOCAL_GUIDE",
            102L,
            101L,
            "Morning walk",
            "A calm walk around town",
            "Let's walk quietly.",
            90,
            "Kamakura",
            "published",
            "limited",
            null,
            now,
            now,
            null
        );

        jdbcTemplate.update(
            """
                INSERT INTO proposal_applications (
                    id, proposal_id, applicant_user_id, application_status, latest_message_preview,
                    latest_message_at, related_thread_id, requires_additional_verification,
                    applied_at, updated_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            301L,
            201L,
            103L,
            "approved",
            "Looking forward to it",
            now,
            401L,
            false,
            now,
            now,
            null
        );

        jdbcTemplate.update(
            """
                INSERT INTO chat_threads (
                    id, user_id, counterpart_id, counterpart_role, related_entity_type,
                    related_entity_id, title, status, unread_count, requires_attention,
                    related_url, latest_message_preview, latest_message_at, created_at, updated_at,
                    closed_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            401L,
            101L,
            103L,
            "USER",
            "PROPOSAL_APPLICATION",
            301L,
            "Application chat",
            "open",
            0,
            false,
            "/app/chat/401",
            "Hello",
            now,
            now,
            now,
            null,
            null
        );
    }

    @Test
    @DisplayName("proposal owner, bridge, and applicant are relation parties")
    void proposalPartiesAreRecognized() {
        Assertions.assertTrue(relatedUserService.isProposalOwner(101L, 201L));
        Assertions.assertTrue(relatedUserService.isProposalBridge(102L, 201L));
        Assertions.assertTrue(relatedUserService.isProposalApplicant(103L, 201L));
        Assertions.assertTrue(relatedUserService.isProposalParty(101L, 201L));
        Assertions.assertTrue(relatedUserService.isProposalParty(102L, 201L));
        Assertions.assertTrue(relatedUserService.isProposalParty(103L, 201L));
        Assertions.assertFalse(relatedUserService.isProposalParty(104L, 201L));
    }

    @Test
    @DisplayName("proposal application parties are limited to applicant and proposal-side users")
    void proposalApplicationPartiesAreRecognized() {
        Assertions.assertTrue(relatedUserService.isProposalApplicationParty(101L, 301L));
        Assertions.assertTrue(relatedUserService.isProposalApplicationParty(102L, 301L));
        Assertions.assertTrue(relatedUserService.isProposalApplicationParty(103L, 301L));
        Assertions.assertFalse(relatedUserService.isProposalApplicationParty(104L, 301L));
    }

    @Test
    @DisplayName("chat participants are recognized without relying on role fallback")
    void chatParticipantsAreRecognized() {
        Assertions.assertTrue(relatedUserService.isChatParticipant(101L, 401L));
        Assertions.assertTrue(relatedUserService.isChatParticipant(103L, 401L));
        Assertions.assertFalse(relatedUserService.isChatParticipant(104L, 401L));
        Assertions.assertFalse(relatedUserService.isChatParticipant(106L, 401L));
    }

    @Test
    @DisplayName("generic related-user lookup supports known entity types only")
    void genericRelatedUserLookupSupportsKnownEntities() {
        Assertions.assertTrue(relatedUserService.isRelatedUser(103L, "proposal", 201L));
        Assertions.assertTrue(relatedUserService.isRelatedUser(102L, "PROPOSAL_APPLICATION", 301L));
        Assertions.assertTrue(relatedUserService.isRelatedUser(101L, "CHAT_THREAD", 401L));
        Assertions.assertFalse(relatedUserService.isRelatedUser(104L, "CHAT_THREAD", 401L));
        Assertions.assertFalse(relatedUserService.isRelatedUser(101L, "PROFILE", 999L));
    }

    @Test
    @DisplayName("privileged relation role is kept separate from relation party checks")
    void privilegedRoleIsSeparateFromRelationPartyChecks() {
        Assertions.assertTrue(relatedUserService.hasPrivilegedRelationRole(102L));
        Assertions.assertTrue(relatedUserService.hasPrivilegedRelationRole(105L));
        Assertions.assertFalse(relatedUserService.hasPrivilegedRelationRole(106L));
        Assertions.assertFalse(relatedUserService.isProposalParty(105L, 201L));
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
}
