package com.furutabi.config;

import static org.hamcrest.Matchers.containsString;
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
class AdminUserManagementFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        Timestamp now = Timestamp.from(Instant.parse("2026-03-25T00:00:00Z"));

        jdbcTemplate.update("DELETE FROM access_logs");
        jdbcTemplate.update("DELETE FROM permission_change_logs");
        jdbcTemplate.update("DELETE FROM support_note_reports");
        jdbcTemplate.update("DELETE FROM support_notes");
        jdbcTemplate.update("DELETE FROM partner_assignments");
        jdbcTemplate.update("DELETE FROM permission_rules");
        jdbcTemplate.update("DELETE FROM role_policies");
        jdbcTemplate.update("DELETE FROM user_role_states");
        jdbcTemplate.update("DELETE FROM region_scoped_settings");
        jdbcTemplate.update("DELETE FROM proposal_application_status_history");
        jdbcTemplate.update("DELETE FROM proposal_applications");
        jdbcTemplate.update("DELETE FROM proposal_tags");
        jdbcTemplate.update("DELETE FROM proposals");
        jdbcTemplate.update("DELETE FROM support_request_status_history");
        jdbcTemplate.update("DELETE FROM support_requests");
        jdbcTemplate.update("DELETE FROM chat_messages");
        jdbcTemplate.update("DELETE FROM chat_threads");
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

        insertUser(1L, "user@example.com", "user", "User Example", now);
        insertUser(2L, "local@example.com", "local", "Local Example", now);
        insertUser(3L, "bridge@example.com", "bridge", "Bridge Example", now);
        insertUser(4L, "admin@example.com", "admin", "Admin Example", now);

        insertRole(1L, "USER", now);
        insertRole(2L, "LOCAL", now);
        insertRole(3L, "BRIDGE", now);
        insertRole(4L, "ADMIN", now);

        insertProfile(1L, "Tokyo", now);
        insertProfile(2L, "Tokyo", now);
        insertProfile(3L, "Tokyo", now);

        jdbcTemplate.update(
            """
                INSERT INTO proposals (
                    id, proposal_type, bridge_user_id, host_user_id, title, summary, body,
                    duration_minutes, location_name, status, visibility_scope, cover_image_path,
                    created_at, updated_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            21L, "LOCAL_GUIDE", null, 1L, "Target host proposal", "summary", "body", 90,
            "Tokyo", "published", "public", null, now, now, null
        );
        jdbcTemplate.update(
            """
                INSERT INTO proposals (
                    id, proposal_type, bridge_user_id, host_user_id, title, summary, body,
                    duration_minutes, location_name, status, visibility_scope, cover_image_path,
                    created_at, updated_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            22L, "LOCAL_GUIDE", 1L, 2L, "Target bridge proposal", "summary", "body", 75,
            "Tokyo", "published", "public", null, now, now, null
        );
        jdbcTemplate.update(
            """
                INSERT INTO support_requests (
                    id, user_id, request_type, related_feature, target_reference, body,
                    reply_preference, status, handled_by_user_id, created_at, updated_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            40L, 1L, "general", "admin-detail", "user#1", "Need support follow-up",
            "optional", "received", null, now, now, null
        );
    }

    @Test
    @DisplayName("admin can open admin user detail and access log is recorded")
    void adminCanOpenDetailAndAccessLogIsRecorded() throws Exception {
        mockMvc.perform(get("/app/admin/users/1").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("/app/admin")))
            .andExpect(content().string(containsString("/app/admin/users")))
            .andExpect(content().string(containsString("/app/support-notes/users/1")))
            .andExpect(content().string(containsString("/app/admin/regions/Tokyo?fromUserId=1")))
            .andExpect(content().string(containsString("/app/history")))
            .andExpect(content().string(containsString("/app/notifications")));

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM access_logs WHERE viewer_context = 'admin' AND target_type = 'admin_user_detail' AND target_id = ?",
            Integer.class,
            1L
        );
        org.assertj.core.api.Assertions.assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("admin can open admin home and user list entry")
    void adminCanOpenAdminHomeAndUserList() throws Exception {
        mockMvc.perform(get("/app/admin").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("admin入口")))
            .andExpect(content().string(containsString("/app/admin/users")))
            .andExpect(content().string(containsString("/app/admin/regions/default")));

        mockMvc.perform(get("/app/admin/users").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("adminユーザー一覧")))
            .andExpect(content().string(containsString("user#1")))
            .andExpect(content().string(containsString("/app/admin/users/1")))
            .andExpect(content().string(org.hamcrest.Matchers.not(containsString("/app/mypage"))));
    }

    @Test
    @DisplayName("admin user list search works for email and id")
    void adminUserListSearchWorks() throws Exception {
        mockMvc.perform(get("/app/admin/users").param("q", "local@example.com").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("local@example.com")))
            .andExpect(content().string(org.hamcrest.Matchers.not(containsString("bridge@example.com"))));

        mockMvc.perform(get("/app/admin/users").param("q", "3").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("user#3")))
            .andExpect(content().string(containsString("/app/admin/users/3")));
    }

    @Test
    @DisplayName("non admin cannot open admin user detail")
    void nonAdminCannotOpenDetail() throws Exception {
        mockMvc.perform(get("/app/admin/users/1").with(user("user@example.com").roles("USER")))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("non admin cannot open admin home or list")
    void nonAdminCannotOpenAdminHomeOrList() throws Exception {
        mockMvc.perform(get("/app/admin").with(user("user@example.com").roles("USER")))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/app/admin/users").with(user("user@example.com").roles("USER")))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("admin can grant permission rule and change log stores json before after")
    void adminCanGrantPermissionRule() throws Exception {
        mockMvc.perform(
                post("/app/admin/users/1/permissions/host_permission/grant")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .with(csrf())
                    .param("regionId", "Tokyo")
                    .param("reason", "Grant host permission for trial")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/app/admin/users/1?*"));

        String state = jdbcTemplate.queryForObject(
            "SELECT rule_state FROM permission_rules WHERE region_id = ? AND target_user_id = ? AND permission_name = ?",
            String.class,
            "Tokyo", 1L, "host_permission"
        );
        String afterValue = jdbcTemplate.queryForObject(
            "SELECT after_value FROM permission_change_logs WHERE changed_object_type = 'permission' ORDER BY id DESC LIMIT 1",
            String.class
        );
        org.assertj.core.api.Assertions.assertThat(state).isEqualTo("active");
        org.assertj.core.api.Assertions.assertThat(afterValue).contains("host_permission").contains("active");
    }

    @Test
    @DisplayName("admin detail shows permission change and access log summaries")
    void adminDetailShowsLogSummaries() throws Exception {
        jdbcTemplate.update(
            """
                INSERT INTO permission_change_logs (
                    region_id, target_user_id, changed_object_type, changed_object_name, action_type,
                    before_value, after_value, changed_by_user_id, reason, changed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            "Tokyo", 1L, "permission", "host_permission", "grant",
            "{\"state\":\"none\"}",
            "{\"permission_name\":\"host_permission\",\"rule_state\":\"active\"}",
            4L, "Grant for test", Timestamp.from(Instant.parse("2026-03-26T00:00:00Z"))
        );
        jdbcTemplate.update(
            """
                INSERT INTO access_logs (
                    viewer_user_id, viewer_context, target_type, target_id, view_reason, viewed_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """,
            4L, "admin", "admin_user_detail", 1L, "Audit review",
            Timestamp.from(Instant.parse("2026-03-26T01:00:00Z"))
        );

        mockMvc.perform(get("/app/admin/users/1").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("host_permission")))
            .andExpect(content().string(containsString("grant")))
            .andExpect(content().string(containsString("before: state=none")))
            .andExpect(content().string(containsString("rule_state = active")))
            .andExpect(content().string(containsString("user#4")))
            .andExpect(content().string(containsString("Audit review")));
    }

    @Test
    @DisplayName("bridge partner permission operation is not shown and direct revoke is blocked")
    void bridgePartnerPermissionOperationIsBlocked() throws Exception {
        mockMvc.perform(get("/app/admin/users/3").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(containsString("/app/admin/users/3/permissions/partner_permission/revoke"))));

        mockMvc.perform(
                post("/app/admin/users/3/permissions/partner_permission/revoke")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .with(csrf())
                    .param("regionId", "Tokyo")
                    .param("reason", "Bridge partner revoke must be blocked")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/app/admin/users/3?blocked*"));
    }

    @Test
    @DisplayName("admin can create assignment and ended assignment cannot resume")
    void adminCanCreateAssignmentAndEndedAssignmentCannotResume() throws Exception {
        mockMvc.perform(
                post("/app/admin/users/1/assignments")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .with(csrf())
                    .param("regionId", "Tokyo")
                    .param("partnerUserId", "3")
                    .param("effectiveFrom", "2026-03-01")
                    .param("effectiveTo", "2026-04-30")
                    .param("reason", "Assign bridge partner")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/app/admin/users/1?assignmentCreated*"));

        Long assignmentId = jdbcTemplate.queryForObject(
            "SELECT id FROM partner_assignments WHERE user_id = ? AND partner_user_id = ?",
            Long.class,
            1L, 3L
        );
        jdbcTemplate.update("UPDATE partner_assignments SET assignment_status = 'ended' WHERE id = ?", assignmentId);

        mockMvc.perform(
                post("/app/admin/users/1/assignments/" + assignmentId + "/resume")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .with(csrf())
                    .param("reason", "Resume ended assignment should fail")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/app/admin/users/1?blocked*"));
    }

    @Test
    @DisplayName("admin support notes route redirects to real list and region settings placeholder still opens")
    void adminSupportNotesRedirectAndRegionPlaceholderOpen() throws Exception {
        mockMvc.perform(get("/app/admin/users/1/support-notes").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/support-notes/users/1"));

        mockMvc.perform(get("/app/admin/regions/Tokyo").param("fromUserId", "1").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Tokyo")))
            .andExpect(content().string(containsString("/app/admin/users/1")));
    }

    private void insertUser(long id, String email, String nickname, String name, Timestamp now) {
        jdbcTemplate.update(
            """
                INSERT INTO users (
                    id, email, password_hash, nickname, name, name_kana, birthday, gender,
                    phone_number, address, sms_verified, additional_verification_status,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            id, email, "{noop}unused", nickname, name, "Test Kana", Date.valueOf("1990-01-01"),
            "NO_ANSWER", "000-0000-0000", "Tokyo", Boolean.TRUE, "UNREQUESTED", now, now
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
            userId, "bio", null, region, region, null, null, null, null, null, "40s", now, now
        );
    }
}
