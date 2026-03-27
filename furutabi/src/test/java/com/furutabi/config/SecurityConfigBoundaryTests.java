package com.furutabi.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class SecurityConfigBoundaryTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpCurrentUserRow() {
        Timestamp now = Timestamp.from(Instant.parse("2026-03-25T00:00:00Z"));

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
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated local member route is available after passing security")
    void authenticatedLocalMemberRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/local-member-home").with(user("local@example.com").roles("LOCAL")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated mypage route is available after passing security")
    void authenticatedMypageRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/mypage").with(user("user@example.com").roles("USER")))
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
    @DisplayName("Authenticated map record detail route is available after passing security")
    void authenticatedMapRecordDetailRouteIsAvailable() throws Exception {
        mockMvc.perform(get("/app/map-records/10").with(user("user@example.com").roles("USER")))
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
