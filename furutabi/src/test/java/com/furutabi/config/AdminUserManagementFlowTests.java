package com.furutabi.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

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
        insertProfile(3L, "Tokyo", now);

        jdbcTemplate.update(
            """
                INSERT INTO proposals (
                    id, proposal_type, bridge_user_id, host_user_id, title, summary, body,
                    duration_minutes, location_name, status, visibility_scope, cover_image_path,
                    created_at, updated_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            20L,
            "LOCAL_GUIDE",
            3L,
            2L,
            "Admin target proposal",
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
            30L,
            20L,
            1L,
            "pending",
            "preview",
            now,
            null,
            false,
            now,
            now,
            null
        );
    }

    @Test
    @DisplayName("admin can open admin user detail and deep access is logged")
    void adminCanOpenDetailAndAccessLogIsRecorded() throws Exception {
        mockMvc.perform(get("/app/admin/users/1").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("admin 詳細管理ページ")))
            .andExpect(content().string(containsString("既存認証用 role")))
            .andExpect(content().string(containsString("catalog fallback default")))
            .andExpect(content().string(containsString("支援メモ導線 placeholder")))
            .andExpect(content().string(containsString("region-scoped settings 導線")));

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM access_logs WHERE viewer_context = 'admin' AND target_type = 'admin_user_detail' AND target_id = ?",
            Integer.class,
            1L
        );
        org.assertj.core.api.Assertions.assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("non admin cannot open admin user detail")
    void nonAdminCannotOpenDetail() throws Exception {
        mockMvc.perform(get("/app/admin/users/1").with(user("user@example.com").roles("USER")))
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
            "Tokyo",
            1L,
            "host_permission"
        );
        String afterValue = jdbcTemplate.queryForObject(
            "SELECT after_value FROM permission_change_logs WHERE changed_object_type = 'permission' ORDER BY id DESC LIMIT 1",
            String.class
        );
        org.assertj.core.api.Assertions.assertThat(state).isEqualTo("active");
        org.assertj.core.api.Assertions.assertThat(afterValue).contains("host_permission").contains("active");
    }

    @Test
    @DisplayName("admin can suspend resume and revoke host permission with reason and effective range")
    void adminCanSuspendResumeAndRevokeHostPermission() throws Exception {
        jdbcTemplate.update(
            """
                INSERT INTO permission_rules (
                    region_id, target_user_id, permission_name, rule_state,
                    effective_from, effective_to, granted_at, grant_reason, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            "Tokyo",
            1L,
            "host_permission",
            "active",
            Date.valueOf("2026-03-01"),
            Date.valueOf("2026-04-30"),
            Timestamp.from(Instant.parse("2026-03-01T00:00:00Z")),
            "seed",
            Timestamp.from(Instant.parse("2026-03-01T00:00:00Z")),
            Timestamp.from(Instant.parse("2026-03-01T00:00:00Z"))
        );

        mockMvc.perform(
                post("/app/admin/users/1/permissions/host_permission/suspend")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .with(csrf())
                    .param("regionId", "Tokyo")
                    .param("effectiveFrom", "2026-03-01")
                    .param("effectiveTo", "2026-04-30")
                    .param("reason", "Suspend host permission")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/app/admin/users/1?*"));

        mockMvc.perform(
                post("/app/admin/users/1/permissions/host_permission/resume")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .with(csrf())
                    .param("regionId", "Tokyo")
                    .param("effectiveFrom", "2026-03-10")
                    .param("effectiveTo", "2026-04-20")
                    .param("reason", "Resume host permission")
            )
            .andExpect(status().is3xxRedirection());

        mockMvc.perform(
                post("/app/admin/users/1/permissions/host_permission/revoke")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .with(csrf())
                    .param("regionId", "Tokyo")
                    .param("effectiveFrom", "2026-03-10")
                    .param("effectiveTo", "2026-04-20")
                    .param("reason", "Revoke host permission")
            )
            .andExpect(status().is3xxRedirection());

        String state = jdbcTemplate.queryForObject(
            "SELECT rule_state FROM permission_rules WHERE region_id = ? AND target_user_id = ? AND permission_name = ?",
            String.class,
            "Tokyo",
            1L,
            "host_permission"
        );
        String revokeReason = jdbcTemplate.queryForObject(
            "SELECT revoke_reason FROM permission_rules WHERE region_id = ? AND target_user_id = ? AND permission_name = ?",
            String.class,
            "Tokyo",
            1L,
            "host_permission"
        );
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM permission_change_logs WHERE changed_object_type = 'permission' AND changed_object_name = 'host_permission'",
            Integer.class
        );
        org.assertj.core.api.Assertions.assertThat(state).isEqualTo("revoked");
        org.assertj.core.api.Assertions.assertThat(revokeReason).isEqualTo("Revoke host permission");
        org.assertj.core.api.Assertions.assertThat(count).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("admin can grant suspend resume and revoke partner permission for user role target")
    void adminCanOperatePartnerPermissionForUserRole() throws Exception {
        mockMvc.perform(
                post("/app/admin/users/1/permissions/partner_permission/grant")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .with(csrf())
                    .param("regionId", "Tokyo")
                    .param("effectiveFrom", "2026-03-01")
                    .param("effectiveTo", "2026-04-01")
                    .param("reason", "Grant partner permission")
            )
            .andExpect(status().is3xxRedirection());

        mockMvc.perform(
                post("/app/admin/users/1/permissions/partner_permission/suspend")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .with(csrf())
                    .param("regionId", "Tokyo")
                    .param("effectiveFrom", "2026-03-01")
                    .param("effectiveTo", "2026-04-01")
                    .param("reason", "Suspend partner permission")
            )
            .andExpect(status().is3xxRedirection());

        mockMvc.perform(
                post("/app/admin/users/1/permissions/partner_permission/resume")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .with(csrf())
                    .param("regionId", "Tokyo")
                    .param("effectiveFrom", "2026-03-05")
                    .param("effectiveTo", "2026-04-05")
                    .param("reason", "Resume partner permission")
            )
            .andExpect(status().is3xxRedirection());

        mockMvc.perform(
                post("/app/admin/users/1/permissions/partner_permission/revoke")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .with(csrf())
                    .param("regionId", "Tokyo")
                    .param("effectiveFrom", "2026-03-05")
                    .param("effectiveTo", "2026-04-05")
                    .param("reason", "Revoke partner permission")
            )
            .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/app/admin/users/1").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("permission 個別操作")))
            .andExpect(content().string(containsString("partner_permission")))
            .andExpect(content().string(containsString("permission_rule")));
    }

    @Test
    @DisplayName("permission operation is blocked when role policy does not allow individual override")
    void permissionOperationIsBlockedWhenPolicyDisallowsOverride() throws Exception {
        mockMvc.perform(
                post("/app/admin/users/2/permissions/host_permission/grant")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .with(csrf())
                    .param("regionId", "Tokyo")
                    .param("reason", "Should be blocked for local host default")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/app/admin/users/2?blocked*"));

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM permission_rules WHERE target_user_id = ? AND permission_name = ?",
            Integer.class,
            2L,
            "host_permission"
        );
        org.assertj.core.api.Assertions.assertThat(count).isZero();
    }

    @Test
    @DisplayName("bridge partner permission operation is not shown and direct revoke is blocked")
    void bridgePartnerPermissionOperationIsBlocked() throws Exception {
        mockMvc.perform(get("/app/admin/users/3").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("この permission は、role policy または role 既定で管理されるため、この段階では個別操作できません。")))
            .andExpect(content().string(not(containsString("/permissions/partner_permission/revoke"))));

        mockMvc.perform(
                post("/app/admin/users/3/permissions/partner_permission/revoke")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .with(csrf())
                    .param("regionId", "Tokyo")
                    .param("reason", "Should stay managed by bridge role")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/app/admin/users/3?blocked*"));
    }

    @Test
    @DisplayName("permission operation is blocked when reason is missing or date range is invalid")
    void permissionOperationValidationIsApplied() throws Exception {
        mockMvc.perform(
                post("/app/admin/users/1/permissions/host_permission/grant")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .with(csrf())
                    .param("regionId", "Tokyo")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/app/admin/users/1?blocked*"));

        mockMvc.perform(
                post("/app/admin/users/1/permissions/host_permission/grant")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .with(csrf())
                    .param("regionId", "Tokyo")
                    .param("effectiveFrom", "2026-04-10")
                    .param("effectiveTo", "2026-04-01")
                    .param("reason", "Invalid range")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/app/admin/users/1?blocked*"));
    }

    @Test
    @DisplayName("admin can suspend role state and it is logged")
    void adminCanSuspendRoleState() throws Exception {
        mockMvc.perform(
                post("/app/admin/users/1/role-state/suspend")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .with(csrf())
                    .param("regionId", "Tokyo")
                    .param("reason", "Suspend from admin detail")
            )
            .andExpect(status().is3xxRedirection());

        String roleState = jdbcTemplate.queryForObject(
            "SELECT role_state FROM user_role_states WHERE user_id = ?",
            String.class,
            1L
        );
        Integer logCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM permission_change_logs WHERE changed_object_type = 'role' AND target_user_id = ?",
            Integer.class,
            1L
        );
        org.assertj.core.api.Assertions.assertThat(roleState).isEqualTo("suspended");
        org.assertj.core.api.Assertions.assertThat(logCount).isEqualTo(1);
    }

    @Test
    @DisplayName("admin can create partner assignment for bridge partner and assignment log is recorded")
    void adminCanCreatePartnerAssignment() throws Exception {
        mockMvc.perform(
                post("/app/admin/users/1/assignments")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .with(csrf())
                    .param("regionId", "Tokyo")
                    .param("partnerUserId", "3")
                    .param("reason", "Assign bridge partner")
            )
            .andExpect(status().is3xxRedirection());

        String assignmentStatus = jdbcTemplate.queryForObject(
            "SELECT assignment_status FROM partner_assignments WHERE user_id = ? AND partner_user_id = ?",
            String.class,
            1L,
            3L
        );
        Integer logCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM permission_change_logs WHERE changed_object_type = 'partner_assignment' AND target_user_id = ?",
            Integer.class,
            1L
        );
        org.assertj.core.api.Assertions.assertThat(assignmentStatus).isEqualTo("active");
        org.assertj.core.api.Assertions.assertThat(logCount).isEqualTo(1);
    }

    @Test
    @DisplayName("admin can open support notes and region settings placeholders from admin detail routes")
    void adminCanOpenPlaceholders() throws Exception {
        mockMvc.perform(get("/app/admin/users/1/support-notes").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("支援メモ placeholder")));

        mockMvc.perform(get("/app/admin/regions/Tokyo").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("region-scoped settings placeholder")));
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
            id,
            email,
            "{noop}unused",
            nickname,
            name,
            "テスト",
            Date.valueOf("1990-01-01"),
            "NO_ANSWER",
            "000-0000-0000",
            "Tokyo",
            Boolean.TRUE,
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
            "40s",
            now,
            now
        );
    }
}
