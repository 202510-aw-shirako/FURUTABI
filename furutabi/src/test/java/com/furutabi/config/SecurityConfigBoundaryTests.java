package com.furutabi.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

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
class SecurityConfigBoundaryTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpCurrentUserRow() {
        Timestamp now = Timestamp.from(Instant.parse("2026-03-25T00:00:00Z"));

        jdbcTemplate.update("DELETE FROM chat_messages");
        jdbcTemplate.update("DELETE FROM chat_threads");
        jdbcTemplate.update("DELETE FROM notification_delivery_logs");
        jdbcTemplate.update("DELETE FROM notifications");
        jdbcTemplate.update("DELETE FROM access_logs");
        jdbcTemplate.update("DELETE FROM permission_change_logs");
        jdbcTemplate.update("DELETE FROM support_note_reports");
        jdbcTemplate.update("DELETE FROM support_notes");
        jdbcTemplate.update("DELETE FROM partner_assignments");
        jdbcTemplate.update("DELETE FROM permission_rules");
        jdbcTemplate.update("DELETE FROM role_policies");
        jdbcTemplate.update("DELETE FROM user_role_states");
        jdbcTemplate.update("DELETE FROM region_scoped_settings");
        jdbcTemplate.update("DELETE FROM support_request_status_history");
        jdbcTemplate.update("DELETE FROM support_requests");
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

        jdbcTemplate.update(
            """
                INSERT INTO users (
                    id, email, password_hash, nickname, name, name_kana, birthday, gender,
                    phone_number, address, sms_verified, additional_verification_status,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            1L,
            "user@example.com",
            "{noop}unused",
            "user",
            "User Example",
            "ユーザー",
            Date.valueOf("1990-01-01"),
            "NO_ANSWER",
            "000-0000-0000",
            "Tokyo",
            Boolean.TRUE,
            "UNREQUESTED",
            now,
            now
        );
        jdbcTemplate.update(
            """
                INSERT INTO users (
                    id, email, password_hash, nickname, name, name_kana, birthday, gender,
                    phone_number, address, sms_verified, additional_verification_status,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            4L,
            "admin@example.com",
            "{noop}unused",
            "admin",
            "Admin Example",
            "アドミン",
            Date.valueOf("1990-01-01"),
            "NO_ANSWER",
            "000-0000-0000",
            "Tokyo",
            Boolean.TRUE,
            "UNREQUESTED",
            now,
            now
        );
        jdbcTemplate.update(
            """
                INSERT INTO users (
                    id, email, password_hash, nickname, name, name_kana, birthday, gender,
                    phone_number, address, sms_verified, additional_verification_status,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            2L,
            "local@example.com",
            "{noop}unused",
            "local",
            "Local Example",
            "ローカル",
            Date.valueOf("1990-01-01"),
            "NO_ANSWER",
            "000-0000-0000",
            "Tokyo",
            Boolean.TRUE,
            "UNREQUESTED",
            now,
            now
        );
        jdbcTemplate.update(
            """
                INSERT INTO users (
                    id, email, password_hash, nickname, name, name_kana, birthday, gender,
                    phone_number, address, sms_verified, additional_verification_status,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            3L,
            "bridge@example.com",
            "{noop}unused",
            "bridge",
            "Bridge Example",
            "ブリッジ",
            Date.valueOf("1990-01-01"),
            "NO_ANSWER",
            "000-0000-0000",
            "Tokyo",
            Boolean.TRUE,
            "UNREQUESTED",
            now,
            now
        );
        jdbcTemplate.update(
            "INSERT INTO user_roles (user_id, role_name, created_at) VALUES (?, ?, ?)",
            1L,
            "USER",
            now
        );
        jdbcTemplate.update(
            "INSERT INTO user_roles (user_id, role_name, created_at) VALUES (?, ?, ?)",
            2L,
            "LOCAL",
            now
        );
        jdbcTemplate.update(
            "INSERT INTO user_roles (user_id, role_name, created_at) VALUES (?, ?, ?)",
            3L,
            "BRIDGE",
            now
        );
        jdbcTemplate.update(
            "INSERT INTO user_roles (user_id, role_name, created_at) VALUES (?, ?, ?)",
            4L,
            "ADMIN",
            now
        );
        jdbcTemplate.update(
            """
                INSERT INTO map_records (
                    id, user_id, title, body, visibility, location_name, latitude, longitude,
                    location_precision_level, is_draft, created_at, updated_at, visibility_updated_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            10L,
            1L,
            "Boundary map",
            "Boundary body",
            "public",
            "Tokyo",
            null,
            null,
            "area",
            false,
            now,
            now,
            now,
            null
        );
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
            "Boundary gate",
            "Boundary gate summary",
            "Boundary gate body",
            45,
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
                INSERT INTO proposals (
                    id, proposal_type, bridge_user_id, host_user_id, title, summary, body,
                    duration_minutes, location_name, status, visibility_scope, cover_image_path,
                    created_at, updated_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            21L,
            "OKATTE",
            3L,
            2L,
            "Boundary okatte",
            "Boundary okatte summary",
            "Boundary okatte body",
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
            "Boundary application",
            now,
            null,
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
            40L,
            1L,
            2L,
            "LOCAL",
            "PROPOSAL_APPLICATION",
            30L,
            "Boundary chat",
            "open",
            0,
            false,
            "/app/host-applications/30",
            "Boundary application",
            now,
            now,
            now,
            null,
            null
        );
        jdbcTemplate.update(
            """
                INSERT INTO notifications (
                    id, user_id, type, title, body, related_entity_type, related_entity_id,
                    related_url, sender_name, sender_role, preview_text, severity,
                    action_label, is_read, created_at, read_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            50L,
            1L,
            "chat_message",
            "New chat message",
            "Local host sent a new chat message",
            "CHAT_THREAD",
            40L,
            "/app/chat/40",
            "local",
            "LOCAL",
            "Boundary application",
            "normal",
            "Open chat",
            false,
            now,
            null,
            null
        );
        jdbcTemplate.update(
            """
                INSERT INTO support_requests (
                    id, user_id, request_type, related_feature, target_reference, body,
                    reply_preference, status, handled_by_user_id, created_at, updated_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            60L,
            1L,
            "general",
            "account",
            "/app/account",
            "Need help with account settings",
            "optional",
            "received",
            null,
            now,
            now,
            null
        );
    }

    @Test
    @DisplayName("Unauthenticated app route redirects to login")
    void unauthenticatedAppRequestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/app/home"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Authenticated app route is available after passing security")
    void authenticatedAppRequestPassesSecurityBeforeMissingRoute() throws Exception {
        mockMvc.perform(get("/app/home").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("わたしの地図")))
            .andExpect(content().string(containsString("ごひいきさんの足あと")))
            .andExpect(content().string(containsString("data-map-tab=\"sub-footprints\"")))
            .andExpect(content().string(containsString("data-my-map-record-list")));
    }

    @Test
    @DisplayName("Authenticated local member route is available after passing security")
    void authenticatedLocalMemberRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/local-member-home").with(user("local@example.com").roles("LOCAL")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("地域登録ユーザー向けログイン後ホーム")))
            .andExpect(content().string(containsString("data-top-map-kind=\"footprints\"")))
            .andExpect(content().string(containsString("data-my-map")));
    }

    @Test
    @DisplayName("Authenticated mypage route is available after passing security")
    void authenticatedMypageRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/mypage").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated history route is available after passing security")
    void authenticatedHistoryRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/history").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated notifications route is available after passing security")
    void authenticatedNotificationsRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/notifications").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated support list route is available after passing security")
    void authenticatedSupportListRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/support").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated support create route is available after passing security")
    void authenticatedSupportCreateRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/support/new").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated support detail route is available for request owner")
    void authenticatedSupportDetailRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/support/60").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated support admin route is available for admin")
    void authenticatedSupportAdminRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/support/admin").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated admin detail route is available for admin")
    void authenticatedAdminDetailRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/admin/users/1").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated admin support notes redirect route is available for admin")
    void authenticatedAdminSupportNotesRedirectRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/admin/users/1/support-notes").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/support-notes/users/1"));
    }

    @Test
    @DisplayName("Authenticated support note list route is available after passing security")
    void authenticatedSupportNoteListRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/support-notes/users/1").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated admin region settings placeholder route is available for admin")
    void authenticatedAdminRegionSettingsPlaceholderRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/admin/regions/Tokyo").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated account route is available after passing security")
    void authenticatedAccountRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/account").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated profile route is available after passing security")
    void authenticatedProfileRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/profile").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated privacy settings route is available after passing security")
    void authenticatedPrivacySettingsRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/privacy-settings").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated map record list route is available after passing security")
    void authenticatedMapRecordListRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/map-records").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated footprint list route is available after passing security")
    void authenticatedFootprintListRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/footprints").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated map record detail route is available after passing security")
    void authenticatedMapRecordDetailRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/map-records/10").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated footprint detail route is available after passing security")
    void authenticatedFootprintDetailRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/footprints/10").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated map record create route is available after passing security")
    void authenticatedMapRecordCreateRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/map-records/new").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated map record edit route is available after passing security")
    void authenticatedMapRecordEditRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/map-records/10/edit").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated gate list route is available after passing security")
    void authenticatedGateListRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/gate").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated gate detail route is available after passing security")
    void authenticatedGateDetailRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/gate/20").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated gate application route is available after passing security")
    void authenticatedGateApplicationRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/gate/20/apply").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated host application list route is available after passing security")
    void authenticatedHostApplicationListRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/host-applications").with(user("local@example.com").roles("LOCAL")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated host application detail route is available after passing security")
    void authenticatedHostApplicationDetailRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/host-applications/30").with(user("local@example.com").roles("LOCAL")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated okatte list route is available after passing security")
    void authenticatedOkatteListRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/okatte").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated okatte detail route is available after passing security")
    void authenticatedOkatteDetailRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/okatte/21").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated okatte apply route is available after passing security")
    void authenticatedOkatteApplyRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/okatte/21/apply").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated chat list route is available after passing security")
    void authenticatedChatListRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/chat").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated chat detail route is available for participants after passing security")
    void authenticatedChatDetailRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/chat/40").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated admin chat review route is available for admin")
    void authenticatedAdminChatReviewRouteIsAvailable() throws Exception {
        jdbcTemplate.update(
            """
                UPDATE support_requests
                SET request_type = ?, related_feature = ?, target_reference = ?
                WHERE id = ?
                """,
            "chat",
            "chat",
            "/app/chat/40",
            60L
        );

        mockMvc.perform(get("/app/admin/chat-threads/40/review?supportRequestId=60")
                .with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated notification open route is available for notification owner")
    void authenticatedNotificationOpenRouteIsAvailable() throws Exception {
        mockMvc.perform(post("/app/notifications/50/open").with(user("user@example.com").roles("USER")).with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/chat/40"));
    }

    @Test
    @DisplayName("Preview public index is accessible without authentication")
    void unauthenticatedPreviewPublicIndexIsAccessible() throws Exception {
        mockMvc.perform(get("/preview/public/index.html"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Preview bridge is accessible without authentication")
    void unauthenticatedPreviewBridgeIsAccessible() throws Exception {
        mockMvc.perform(get("/preview/public/bridge.html"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Preview auth login page is accessible without authentication")
    void unauthenticatedPreviewAuthLoginIsAccessible() throws Exception {
        mockMvc.perform(get("/preview/auth/login.html"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Preview static JavaScript is accessible without authentication")
    void previewStaticJavaScriptIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/assets/scripts/proposals.js"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Non-preview public path still redirects to login in current code")
    void unauthenticatedPublicPathAlsoRedirectsToLoginInCurrentCode() throws Exception {
        mockMvc.perform(get("/public/index.html"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("CSRF is still required outside H2 console")
    void csrfIsStillRequiredOutsideH2Console() throws Exception {
        mockMvc.perform(post("/app/home").with(user("user@example.com").roles("USER")))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("H2 console is not blocked by authentication or CSRF in current security config")
    void h2ConsolePostIsNotBlockedByCsrfOrAuthentication() throws Exception {
        mockMvc.perform(post("/h2-console/login.do"))
            .andExpect(status().isNotFound());
    }
}
