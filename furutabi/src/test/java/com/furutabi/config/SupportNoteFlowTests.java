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
class SupportNoteFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        Timestamp now = Timestamp.from(Instant.parse("2026-03-30T00:00:00Z"));

        jdbcTemplate.update("DELETE FROM support_note_reports");
        jdbcTemplate.update("DELETE FROM support_notes");
        jdbcTemplate.update("DELETE FROM access_logs");
        jdbcTemplate.update("DELETE FROM permission_change_logs");
        jdbcTemplate.update("DELETE FROM partner_assignments");
        jdbcTemplate.update("DELETE FROM permission_rules");
        jdbcTemplate.update("DELETE FROM role_policies");
        jdbcTemplate.update("DELETE FROM user_role_states");
        jdbcTemplate.update("DELETE FROM region_scoped_settings");
        jdbcTemplate.update("DELETE FROM support_request_status_history");
        jdbcTemplate.update("DELETE FROM support_requests");
        jdbcTemplate.update("DELETE FROM chat_messages");
        jdbcTemplate.update("DELETE FROM chat_threads");
        jdbcTemplate.update("DELETE FROM proposal_application_status_history");
        jdbcTemplate.update("DELETE FROM proposal_applications");
        jdbcTemplate.update("DELETE FROM proposal_tags");
        jdbcTemplate.update("DELETE FROM proposals");
        jdbcTemplate.update("DELETE FROM notification_delivery_logs");
        jdbcTemplate.update("DELETE FROM notifications");
        jdbcTemplate.update("DELETE FROM map_record_comments");
        jdbcTemplate.update("DELETE FROM map_record_images");
        jdbcTemplate.update("DELETE FROM map_records");
        jdbcTemplate.update("DELETE FROM sms_verifications");
        jdbcTemplate.update("DELETE FROM contact_preferences");
        jdbcTemplate.update("DELETE FROM user_profiles");
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM users");

        insertUser(100L, "user@example.com", "user", now);
        insertUser(101L, "admin@example.com", "admin", now);
        insertUser(102L, "host@example.com", "host", now);
        insertUser(103L, "partner@example.com", "partner", now);
        insertUser(104L, "other@example.com", "other", now);
        insertUser(105L, "other-host@example.com", "other-host", now);

        insertRole(100L, "USER", now);
        insertRole(101L, "ADMIN", now);
        insertRole(102L, "LOCAL", now);
        insertRole(103L, "BRIDGE", now);
        insertRole(104L, "USER", now);
        insertRole(105L, "LOCAL", now);

        insertProfile(100L, "Tokyo", now);
        insertProfile(102L, "Tokyo", now);
        insertProfile(103L, "Tokyo", now);
        insertProfile(105L, "Tokyo", now);

        insertProposalAndApplication(now);
        insertSecondaryProposalAndApplication(now);
        insertPartnerAssignment(now);
    }

    @Test
    @DisplayName("admin can create support note without note type and admin detail points to the real notes route")
    void adminCanCreateSupportNoteAndAdminDetailLinksToRealRoute() throws Exception {
        mockMvc.perform(
                post("/app/support-notes/users/100")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .with(csrf())
                    .param("noteStatus", "active")
                    .param("relatedCardId", "300")
                    .param("body", "興味・関心:\n散歩\n\n来訪時の同伴者:\n姉")
                    .param("carePoints", "大きな音が続くと疲れやすい")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/app/support-notes/*?created"));

        mockMvc.perform(get("/app/admin/users/100").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("/app/support-notes/users/100")));

        mockMvc.perform(get("/app/support-notes/users/100").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("留意点の集約")))
            .andExpect(content().string(containsString("入り口のカード")))
            .andExpect(content().string(not(containsString("種別"))));
    }

    @Test
    @DisplayName("create and edit pages use body template and no longer show note type or standalone visit companions")
    void formUsesTemplateWithoutLegacyFields() throws Exception {
        mockMvc.perform(get("/app/support-notes/users/100/new").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("興味・関心")))
            .andExpect(content().string(containsString("来訪時の同伴者")))
            .andExpect(content().string(not(containsString("種別"))))
            .andExpect(content().string(containsString("その他:")))
            .andExpect(content().string(not(containsString("visitCompanions"))));

        mockMvc.perform(get("/app/support-notes/users/100/new?relatedCardId=300").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("option value=\"300\"")))
            .andExpect(content().string(containsString("selected=\"selected\"")));
    }

    @Test
    @DisplayName("target user cannot view own support notes")
    void targetUserCannotViewOwnSupportNotes() throws Exception {
        long noteId = insertSupportNote(100L, 101L, "active", false, null, null, 300L);

        mockMvc.perform(get("/app/support-notes/users/100").with(user("user@example.com").roles("USER")))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/app/support-notes/" + noteId).with(user("user@example.com").roles("USER")))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("active partner can view notes and hidden notes become admin only")
    void activePartnerCanViewAndHiddenIsAdminOnly() throws Exception {
        long noteId = insertSupportNote(100L, 101L, "active", false, null, null, 300L);

        mockMvc.perform(get("/app/support-notes/users/100").with(user("partner@example.com").roles("BRIDGE")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("支援メモ")));

        mockMvc.perform(
                post("/app/support-notes/" + noteId + "/hide")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .with(csrf())
                    .param("reason", "表示確認のため一時非表示")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/support-notes/" + noteId + "?hidden"));

        mockMvc.perform(get("/app/support-notes/" + noteId).with(user("partner@example.com").roles("BRIDGE")))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/app/support-notes/" + noteId).with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("非表示理由")));
    }

    @Test
    @DisplayName("partner readability stays limited to active assignment")
    void partnerNeedsActiveAssignmentToView() throws Exception {
        long noteId = insertSupportNote(100L, 101L, "active", false, null, null, 300L);
        jdbcTemplate.update("DELETE FROM partner_assignments WHERE user_id = ? AND partner_user_id = ?", 100L, 103L);

        mockMvc.perform(get("/app/support-notes/users/100").with(user("partner@example.com").roles("BRIDGE")))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/app/support-notes/" + noteId).with(user("partner@example.com").roles("BRIDGE")))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("host visibility uses completed close timestamp and region setting can shorten it")
    void hostVisibilityUsesCompletedCloseAndRegionSetting() throws Exception {
        long noteId = insertSupportNote(100L, 101L, "active", false, null, null, 300L);

        jdbcTemplate.update(
            "UPDATE proposal_applications SET application_status = ?, updated_at = ? WHERE id = ?",
            "completed",
            Timestamp.from(Instant.parse("2026-03-27T00:00:00Z")),
            300L
        );

        mockMvc.perform(get("/app/support-notes/" + noteId).with(user("host@example.com").roles("LOCAL")))
            .andExpect(status().isOk());

        jdbcTemplate.update(
            """
                INSERT INTO region_scoped_settings (region_id, setting_key, setting_value, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
                """,
            "Tokyo",
            "support_note_close_grace_days",
            "2",
            Timestamp.from(Instant.parse("2026-03-30T00:00:00Z")),
            Timestamp.from(Instant.parse("2026-03-30T00:00:00Z"))
        );

        mockMvc.perform(get("/app/support-notes/" + noteId).with(user("host@example.com").roles("LOCAL")))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("host visibility uses cancelled status history before chat close timing")
    void hostVisibilityUsesCancelledStatusHistory() throws Exception {
        long noteId = insertSupportNote(100L, 101L, "active", false, null, null, 300L);

        jdbcTemplate.update(
            "UPDATE chat_threads SET status = ?, closed_at = ? WHERE id = ?",
            "closed",
            Timestamp.from(Instant.parse("2026-03-10T00:00:00Z")),
            400L
        );
        jdbcTemplate.update(
            """
                INSERT INTO proposal_application_status_history (
                    proposal_application_id, status, note, changed_by_user_id, created_at
                ) VALUES (?, ?, ?, ?, ?)
                """,
            300L,
            "cancelled",
            "cancelled for support note visibility test",
            101L,
            Timestamp.from(Instant.parse("2026-03-28T00:00:00Z"))
        );

        mockMvc.perform(get("/app/support-notes/" + noteId).with(user("host@example.com").roles("LOCAL")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("chat close timing does not override an older completed close marker")
    void chatCloseTimingDoesNotOverrideExplicitCloseMarker() throws Exception {
        long noteId = insertSupportNote(100L, 101L, "active", false, null, null, 300L);

        jdbcTemplate.update(
            "UPDATE chat_threads SET status = ?, closed_at = ? WHERE id = ?",
            "closed",
            Timestamp.from(Instant.parse("2026-03-29T00:00:00Z")),
            400L
        );
        jdbcTemplate.update(
            "UPDATE proposal_applications SET application_status = ?, updated_at = ? WHERE id = ?",
            "completed",
            Timestamp.from(Instant.parse("2026-03-20T00:00:00Z")),
            300L
        );

        mockMvc.perform(get("/app/support-notes/" + noteId).with(user("host@example.com").roles("LOCAL")))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("care points list links back to notes and outsider cannot view support notes")
    void carePointsListLinksBackAndOutsiderCannotView() throws Exception {
        long noteId = insertSupportNote(100L, 101L, "active", false, null, null, 300L);

        mockMvc.perform(get("/app/support-notes/users/100/care-points").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("留意点一覧")))
            .andExpect(content().string(containsString("/app/support-notes/" + noteId)));

        mockMvc.perform(get("/app/support-notes/" + noteId).with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("関連記録を見る")));

        mockMvc.perform(get("/app/support-notes/users/100").with(user("other@example.com").roles("USER")))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("host can open assignment-based related context view without widening full application detail")
    void hostCanOpenLimitedRelatedContextView() throws Exception {
        long noteId = insertSupportNote(100L, 101L, "active", false, null, null, 301L);

        mockMvc.perform(get("/app/support-notes/" + noteId + "/related-card").with(user("host@example.com").roles("LOCAL")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("関連記録")))
            .andExpect(content().string(containsString("閲覧できる範囲のみ表示しています")))
            .andExpect(content().string(not(containsString("通常の proposal 詳細を見る"))));

        mockMvc.perform(get("/app/host-applications/301").with(user("host@example.com").roles("LOCAL")))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("host cannot keep viewing related context after current assignment condition ends")
    void hostCannotKeepViewingRelatedContextAfterAssignmentEnds() throws Exception {
        long noteId = insertSupportNote(100L, 101L, "active", false, null, null, 301L);

        jdbcTemplate.update(
            "UPDATE proposal_applications SET application_status = ?, updated_at = ? WHERE id = ?",
            "pending",
            Timestamp.from(Instant.parse("2026-03-30T00:00:00Z")),
            300L
        );
        jdbcTemplate.update(
            "UPDATE proposal_applications SET application_status = ?, updated_at = ? WHERE id = ?",
            "pending",
            Timestamp.from(Instant.parse("2026-03-30T00:00:00Z")),
            301L
        );
        jdbcTemplate.update(
            "UPDATE chat_threads SET status = ? WHERE id = ?",
            "open",
            400L
        );
        jdbcTemplate.update(
            "UPDATE chat_threads SET status = ? WHERE id = ?",
            "open",
            401L
        );

        mockMvc.perform(get("/app/support-notes/" + noteId + "/related-card").with(user("host@example.com").roles("LOCAL")))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("report can be submitted and legacy admin route redirects to the real list")
    void reportAndLegacyRedirectWork() throws Exception {
        long noteId = insertSupportNote(100L, 101L, "active", false, null, null, 300L);

        mockMvc.perform(
                post("/app/support-notes/" + noteId + "/reports")
                    .with(user("partner@example.com").roles("BRIDGE"))
                    .with(csrf())
                    .param("reportNote", "表現が強すぎるので見直したいです")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/support-notes/" + noteId + "?reported"));

        Integer reportCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM support_note_reports WHERE support_note_id = ?",
            Integer.class,
            noteId
        );
        Assertions.assertEquals(1, reportCount);

        mockMvc.perform(get("/app/admin/users/100/support-notes").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/support-notes/users/100"));
    }

    private void insertProposalAndApplication(Timestamp now) {
        jdbcTemplate.update(
            """
                INSERT INTO proposals (
                    id, proposal_type, bridge_user_id, host_user_id, title, summary, body,
                    duration_minutes, location_name, status, visibility_scope, cover_image_path,
                    created_at, updated_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            200L,
            "LOCAL_GUIDE",
            103L,
            102L,
            "支援メモ連携用の入口",
            "summary",
            "body",
            60,
            "Tokyo",
            "published",
            "public",
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
            300L,
            200L,
            100L,
            "accepted",
            "accepted",
            now,
            400L,
            false,
            now,
            now,
            null
        );
        jdbcTemplate.update(
            """
                INSERT INTO chat_threads (
                    id, user_id, counterpart_id, counterpart_role, related_entity_type, related_entity_id,
                    title, status, unread_count, requires_attention, related_url, latest_message_preview,
                    latest_message_at, created_at, updated_at, closed_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            400L,
            100L,
            102L,
            "LOCAL",
            "PROPOSAL_APPLICATION",
            300L,
            "Support note chat",
            "open",
            0,
            false,
            "/app/chat/400",
            "accepted",
            now,
            now,
            now,
            null,
            null
        );
    }

    private void insertSecondaryProposalAndApplication(Timestamp now) {
        jdbcTemplate.update(
            """
                INSERT INTO proposals (
                    id, proposal_type, bridge_user_id, host_user_id, title, summary, body,
                    duration_minutes, location_name, status, visibility_scope, cover_image_path,
                    created_at, updated_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            201L,
            "OKATTE",
            103L,
            105L,
            "別の受け入れ側のおかって",
            "secondary summary",
            "secondary body",
            90,
            "Yokohama",
            "published",
            "private",
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
            100L,
            "accepted",
            "accepted",
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
                    id, user_id, counterpart_id, counterpart_role, related_entity_type, related_entity_id,
                    title, status, unread_count, requires_attention, related_url, latest_message_preview,
                    latest_message_at, created_at, updated_at, closed_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            401L,
            100L,
            105L,
            "LOCAL",
            "PROPOSAL_APPLICATION",
            301L,
            "Secondary support note chat",
            "completed",
            0,
            false,
            "/app/chat/401",
            "accepted",
            now,
            now,
            now,
            now,
            null
        );
    }

    private void insertPartnerAssignment(Timestamp now) {
        jdbcTemplate.update(
            """
                INSERT INTO partner_assignments (
                    id, region_id, user_id, partner_user_id, assignment_status, assigned_at,
                    effective_from, effective_to, assigned_by_user_id, assignment_reason, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            500L,
            "Tokyo",
            100L,
            103L,
            "active",
            now,
            Date.valueOf("2026-03-01"),
            Date.valueOf("2026-04-30"),
            101L,
            "seed assignment",
            now,
            now
        );
    }

    private long insertSupportNote(long targetUserId, long createdByUserId, String noteStatus, boolean hidden, String hiddenReason, Timestamp hiddenAt, Long relatedCardId) {
        Timestamp now = Timestamp.from(Instant.parse("2026-03-30T01:00:00Z"));
        jdbcTemplate.update(
            """
                INSERT INTO support_notes (
                    id, target_user_id, region_id, note_type, note_status, body, care_points, visit_companions,
                    related_card_type, related_card_id,
                    is_hidden, hidden_reason, hidden_by_user_id, hidden_at,
                    created_by_user_id, updated_by_user_id, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            900L,
            targetUserId,
            "Tokyo",
            "support_note",
            noteStatus,
            "興味・関心:\n散歩が好き\n\n次に求めていること:\n静かな場所で休みたい",
            "大きな音が続くと疲れやすい",
            "姉",
            relatedCardId == null ? null : "proposal_application",
            relatedCardId,
            hidden,
            hiddenReason,
            hidden ? createdByUserId : null,
            hiddenAt,
            createdByUserId,
            createdByUserId,
            now,
            now
        );
        return 900L;
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
            "Tokyo",
            true,
            "UNREQUESTED",
            now,
            now
        );
    }

    private void insertRole(long userId, String roleName, Timestamp now) {
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_name, created_at) VALUES (?, ?, ?)", userId, roleName, now);
    }

    private void insertProfile(long userId, String region, Timestamp now) {
        jdbcTemplate.update(
            """
                INSERT INTO user_profiles (
                    user_id, bio, icon_path, region, interest_region, visit_history,
                    care_note, with_children, food_note, relation_note, age_range,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            userId,
            "bio",
            null,
            region,
            region,
            null,
            null,
            null,
            null,
            null,
            "30s",
            now,
            now
        );
    }
}
