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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.furutabi.app.MapRecordForm;
import com.furutabi.app.MapRecordService;
import com.furutabi.app.UserPointService;
import com.furutabi.auth.RegisterSessionState;
import com.furutabi.auth.RegistrationController;

@SpringBootTest
@AutoConfigureMockMvc
class PointSystemFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MapRecordService mapRecordService;

    @Autowired
    private UserPointService userPointService;

    @BeforeEach
    void setUp() {
        Timestamp now = Timestamp.from(Instant.parse("2026-04-02T00:00:00Z"));

        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.update("DELETE FROM access_logs");
        jdbcTemplate.update("DELETE FROM permission_change_logs");
        jdbcTemplate.update("DELETE FROM partner_assignments");
        jdbcTemplate.update("DELETE FROM permission_rules");
        jdbcTemplate.update("DELETE FROM role_policies");
        jdbcTemplate.update("DELETE FROM user_role_states");
        jdbcTemplate.update("DELETE FROM point_events");
        jdbcTemplate.update("DELETE FROM region_scoped_settings");
        jdbcTemplate.update("DELETE FROM proposal_application_status_history");
        jdbcTemplate.update("DELETE FROM proposal_applications");
        jdbcTemplate.update("DELETE FROM proposal_tags");
        jdbcTemplate.update("DELETE FROM proposals");
        jdbcTemplate.update("DELETE FROM support_request_status_history");
        jdbcTemplate.update("DELETE FROM support_requests");
        jdbcTemplate.update("DELETE FROM chat_messages");
        jdbcTemplate.update("DELETE FROM chat_threads");
        jdbcTemplate.update("DELETE FROM map_record_bookmarks");
        jdbcTemplate.update("DELETE FROM map_record_reactions");
        jdbcTemplate.update("DELETE FROM map_record_comments");
        jdbcTemplate.update("DELETE FROM map_record_images");
        jdbcTemplate.update("DELETE FROM map_records");
        jdbcTemplate.update("DELETE FROM contact_preferences");
        jdbcTemplate.update("DELETE FROM user_profiles");
        jdbcTemplate.update("DELETE FROM sms_verifications");
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        insertSetting("default", "points_program_enabled", "true", now);
        insertSetting("default", "points_signup_points", "20", now);
        insertSetting("Tokyo", "points_program_enabled", "true", now);
        insertSetting("Tokyo", "points_map_record_points", "3", now);
        insertSetting("Tokyo", "points_map_record_monthly_cap", "2", now);
        insertSetting("Tokyo", "points_visit_participation_points", "7", now);
        insertSetting("Tokyo", "points_repeat_participation_points", "11", now);
        insertSetting("Tokyo", "points_host_points", "13", now);
        insertSetting("Tokyo", "points_partner_points", "5", now);
        insertSetting("Tokyo", "points_entry_first_points", "17", now);
        insertSetting("Tokyo", "points_entry_repeat_points", "9", now);

        insertUser(100L, "user@example.com", "point-user", "Point User", now);
        insertUser(101L, "local@example.com", "local-user", "Local User", now);
        insertUser(102L, "bridge@example.com", "bridge-user", "Bridge User", now);
        insertUser(103L, "admin@example.com", "admin-user", "Admin User", now);

        insertRole(100L, "USER", now);
        insertRole(101L, "LOCAL", now);
        insertRole(102L, "BRIDGE", now);
        insertRole(103L, "ADMIN", now);

        insertProfile(100L, "Tokyo", "Kamakura", now);
        insertProfile(101L, "Tokyo", "Kamakura", now);
        insertProfile(102L, "Tokyo", "Kamakura", now);
        insertProfile(103L, "Tokyo", "Kamakura", now);
    }

    @Test
    @DisplayName("registration awards one signup point event")
    void registrationAwardsSignupPoint() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/register")
                .with(csrf())
                .param("name", "Point User")
                .param("nameKana", "POINT USER")
                .param("birthday", "1990-01-01")
                .param("gender", "NO_ANSWER")
                .param("email", "register-point@example.com")
                .param("phoneNumber", "090-0000-0001")
                .param("address", "Tokyo")
                .param("nickname", "register-point")
                .param("password", "password123")
                .param("passwordConfirm", "password123")
                .param("agreedToTerms", "true")
                .param("agreedToPrivacyPolicy", "true")
                .param("agreedToSmsNotice", "true"))
            .andExpect(status().is3xxRedirection())
            .andReturn();

        MockHttpSession session = (MockHttpSession) registerResult.getRequest().getSession(false);
        RegisterSessionState state = (RegisterSessionState) session.getAttribute(RegistrationController.REGISTER_SESSION_KEY);

        mockMvc.perform(post("/register/sms/verify")
                .with(csrf())
                .session(session)
                .param("code", state.getLatestSmsCode()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/register/profile"));

        Integer signupCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM point_events WHERE user_id = ? AND event_type = 'registration'",
            Integer.class,
            state.getUserId()
        );
        Integer points = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(points), 0) FROM point_events WHERE user_id = ?",
            Integer.class,
            state.getUserId()
        );

        Assertions.assertEquals(1, signupCount);
        Assertions.assertEquals(20, points);
    }

    @Test
    @DisplayName("map record posting respects private visibility and monthly cap")
    void mapRecordPostingRespectsVisibilityAndMonthlyCap() {
        MapRecordForm publicForm = new MapRecordForm();
        publicForm.setTitle("Public record");
        publicForm.setBody("Body");
        publicForm.setVisibility("PUBLIC");
        publicForm.setLocationName("Tokyo");
        publicForm.setLocationPrecisionLevel("area");
        publicForm.setDraft(false);

        mapRecordService.createRecord("user@example.com", publicForm);
        mapRecordService.createRecord("user@example.com", publicForm);
        mapRecordService.createRecord("user@example.com", publicForm);

        MapRecordForm privateForm = new MapRecordForm();
        privateForm.setTitle("Private record");
        privateForm.setBody("Body");
        privateForm.setVisibility("PRIVATE");
        privateForm.setLocationName("Tokyo");
        privateForm.setLocationPrecisionLevel("area");
        privateForm.setDraft(false);

        mapRecordService.createRecord("user@example.com", privateForm);

        Integer publicEventCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM point_events WHERE user_id = ? AND event_type = 'map_record_post'",
            Integer.class,
            100L
        );
        Integer totalPoints = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(points), 0) FROM point_events WHERE user_id = ?",
            Integer.class,
            100L
        );

        Assertions.assertEquals(2, publicEventCount);
        Assertions.assertEquals(6, totalPoints);
    }

    @Test
    @DisplayName("accepted gate applications award participation host and entry points with revisit handling")
    void acceptedApplicationsAwardParticipationHostAndEntryPoints() throws Exception {
        Timestamp now = Timestamp.from(Instant.parse("2026-04-02T01:00:00Z"));
        insertProposal(201L, "LOCAL_GUIDE", 102L, 101L, now);
        insertProposal(202L, "LOCAL_GUIDE", 102L, 101L, now);
        insertApplication(301L, 201L, 100L, "pending", now);
        insertApplication(302L, 202L, 100L, "pending", now);

        mockMvc.perform(post("/app/host-applications/301/accept").with(user("local@example.com").roles("LOCAL")).with(csrf()))
            .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/app/host-applications/302/accept").with(user("local@example.com").roles("LOCAL")).with(csrf()))
            .andExpect(status().is3xxRedirection());

        Integer firstParticipation = jdbcTemplate.queryForObject(
            "SELECT points FROM point_events WHERE user_id = ? AND event_type = 'visit_participation' AND related_target_id = ?",
            Integer.class,
            100L,
            301L
        );
        Integer repeatParticipation = jdbcTemplate.queryForObject(
            "SELECT points FROM point_events WHERE user_id = ? AND event_type = 'repeat_participation' AND related_target_id = ?",
            Integer.class,
            100L,
            302L
        );
        Integer hostCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM point_events WHERE user_id = ? AND event_type = 'host_role'",
            Integer.class,
            101L
        );
        Integer firstEntry = jdbcTemplate.queryForObject(
            "SELECT points FROM point_events WHERE user_id = ? AND event_type = 'entry_role' AND related_target_id = ?",
            Integer.class,
            102L,
            301L
        );
        Integer repeatEntry = jdbcTemplate.queryForObject(
            "SELECT points FROM point_events WHERE user_id = ? AND event_type = 'entry_role' AND related_target_id = ?",
            Integer.class,
            102L,
            302L
        );

        Assertions.assertEquals(7, firstParticipation);
        Assertions.assertEquals(11, repeatParticipation);
        Assertions.assertEquals(2, hostCount);
        Assertions.assertEquals(17, firstEntry);
        Assertions.assertEquals(9, repeatEntry);
    }

    @Test
    @DisplayName("partner assignment awards partner role points")
    void partnerAssignmentAwardsPartnerRolePoints() throws Exception {
        mockMvc.perform(
                post("/app/admin/users/100/assignments")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .with(csrf())
                    .param("regionId", "Tokyo")
                    .param("partnerUserId", "102")
                    .param("reason", "Assign bridge partner"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/app/admin/users/100?assignmentCreated*"));

        Integer partnerPoints = jdbcTemplate.queryForObject(
            "SELECT points FROM point_events WHERE user_id = ? AND event_type = 'partner_role'",
            Integer.class,
            102L
        );

        Assertions.assertEquals(5, partnerPoints);
    }

    @Test
    @DisplayName("admin can grant manual and campaign points from admin detail")
    void adminCanGrantManualAndCampaignPointsFromAdminDetail() throws Exception {
        mockMvc.perform(get("/app/admin/users/100").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("/app/admin/users/100/points/grant")));

        mockMvc.perform(
                post("/app/admin/users/100/points/grant")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .with(csrf())
                    .param("sourceMode", "campaign")
                    .param("regionId", "Tokyo")
                    .param("points", "12")
                    .param("reason", "Spring campaign"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/app/admin/users/100?pointsGranted*"));

        mockMvc.perform(
                post("/app/admin/users/100/points/grant")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .with(csrf())
                    .param("sourceMode", "manual")
                    .param("regionId", "Tokyo")
                    .param("points", "4")
                    .param("reason", "Admin adjustment"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/app/admin/users/100?pointsGranted*"));

        Integer campaignCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM point_events WHERE user_id = ? AND source_mode = 'campaign'",
            Integer.class,
            100L
        );
        Integer manualCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM point_events WHERE user_id = ? AND source_mode = 'manual'",
            Integer.class,
            100L
        );

        Assertions.assertEquals(1, campaignCount);
        Assertions.assertEquals(1, manualCount);
    }

    @Test
    @DisplayName("mypage shows current points and simple history")
    void mypageShowsPointsAndHistory() throws Exception {
        userPointService.grantCampaignPoints(100L, "Tokyo", 12, "Spring campaign", 900L);

        mockMvc.perform(get("/app/mypage").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Points")))
            .andExpect(content().string(containsString("Current points")))
            .andExpect(content().string(containsString("12")))
            .andExpect(content().string(containsString("キャンペーン")));
    }

    private void insertSetting(String regionId, String settingKey, String settingValue, Timestamp now) {
        jdbcTemplate.update(
            """
                INSERT INTO region_scoped_settings (
                    region_id, setting_key, setting_value, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?)
                """,
            regionId,
            settingKey,
            settingValue,
            now,
            now
        );
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
        jdbcTemplate.update(
            "INSERT INTO user_roles (user_id, role_name, created_at) VALUES (?, ?, ?)",
            userId,
            roleName,
            now
        );
    }

    private void insertProfile(long userId, String region, String interestRegion, Timestamp now) {
        jdbcTemplate.update(
            """
                INSERT INTO user_profiles (
                    user_id, bio, icon_path, region, interest_region, visit_history,
                    care_note, with_children, food_note, relation_note, age_range, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            userId,
            "Bio",
            null,
            region,
            interestRegion,
            null,
            null,
            null,
            null,
            null,
            null,
            now,
            now
        );
    }

    private void insertProposal(long id, String proposalType, Long bridgeUserId, long hostUserId, Timestamp now) {
        jdbcTemplate.update(
            """
                INSERT INTO proposals (
                    id, proposal_type, bridge_user_id, host_user_id, title, summary, body,
                    duration_minutes, location_name, status, visibility_scope, cover_image_path,
                    created_at, updated_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            id,
            proposalType,
            bridgeUserId,
            hostUserId,
            "Proposal " + id,
            "summary",
            "body",
            90,
            "Tokyo",
            "published",
            "public",
            null,
            now,
            now,
            null
        );
    }

    private void insertApplication(long id, long proposalId, long applicantUserId, String status, Timestamp now) {
        jdbcTemplate.update(
            """
                INSERT INTO proposal_applications (
                    id, proposal_id, applicant_user_id, application_status, latest_message_preview,
                    latest_message_at, related_thread_id, requires_additional_verification,
                    applied_at, updated_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            id,
            proposalId,
            applicantUserId,
            status,
            "hello",
            now,
            null,
            false,
            now,
            now,
            null
        );
        jdbcTemplate.update(
            """
                INSERT INTO proposal_application_status_history (
                    proposal_application_id, status, note, changed_by_user_id, created_at
                ) VALUES (?, ?, ?, ?, ?)
                """,
            id,
            status,
            status,
            applicantUserId,
            now
        );
    }
}
