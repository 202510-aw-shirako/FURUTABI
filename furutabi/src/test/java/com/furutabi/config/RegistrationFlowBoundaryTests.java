package com.furutabi.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.furutabi.auth.RegisterSessionState;
import com.furutabi.auth.RegistrationController;

@SpringBootTest
@AutoConfigureMockMvc
class RegistrationFlowBoundaryTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanRegistrationTables() {
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
        jdbcTemplate.update("DELETE FROM contact_preferences");
        jdbcTemplate.update("DELETE FROM user_profiles");
        jdbcTemplate.update("DELETE FROM sms_verifications");
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    @DisplayName("GET /register returns the backend registration page")
    void registerPageIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/register"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("SMS 認証へ進む")));
    }

    @Test
    @DisplayName("POST /register creates a pending user and redirects to SMS verification")
    void registerCreatesPendingUserAndSmsVerification() throws Exception {
        MvcResult result = mockMvc.perform(post("/register")
                .with(csrf())
                .param("name", "Sample User")
                .param("nameKana", "サンプルユーザー")
                .param("birthday", "1990-01-01")
                .param("gender", "NO_ANSWER")
                .param("email", "register@example.com")
                .param("phoneNumber", "090-1111-2222")
                .param("address", "Tokyo")
                .param("nickname", "register-sample")
                .param("password", "password123")
                .param("passwordConfirm", "password123")
                .param("agreedToTerms", "true")
                .param("agreedToPrivacyPolicy", "true")
                .param("agreedToSmsNotice", "true"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/register/sms"))
            .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        RegisterSessionState state = (RegisterSessionState) session.getAttribute(RegistrationController.REGISTER_SESSION_KEY);

        Long userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Long.class, "register@example.com");
        Long roleCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_roles WHERE role_name = 'USER'", Long.class);
        Long smsCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sms_verifications", Long.class);
        Boolean smsVerified = jdbcTemplate.queryForObject("SELECT sms_verified FROM users WHERE id = ?", Boolean.class, state.getUserId());

        Assertions.assertNotNull(state);
        Assertions.assertNotNull(state.getLatestSmsCode());
        Assertions.assertEquals(1L, userCount);
        Assertions.assertEquals(1L, roleCount);
        Assertions.assertEquals(1L, smsCount);
        Assertions.assertEquals(Boolean.FALSE, smsVerified);
    }

    @Test
    @DisplayName("GET /register/profile without session returns to register start")
    void profilePageRequiresVerifiedRegistrationSession() throws Exception {
        mockMvc.perform(get("/register/profile"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/register"));
    }

    @Test
    @DisplayName("register to SMS to profile stores the minimum registration data")
    void completeRegistrationFlowStoresSmsAndProfileData() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/register")
                .with(csrf())
                .param("name", "Profile User")
                .param("nameKana", "プロフィールユーザー")
                .param("birthday", "1992-02-02")
                .param("gender", "NO_ANSWER")
                .param("email", "profile@example.com")
                .param("phoneNumber", "090-3333-4444")
                .param("address", "Kyoto")
                .param("nickname", "profile-user")
                .param("password", "password123")
                .param("passwordConfirm", "password123")
                .param("agreedToTerms", "true")
                .param("agreedToPrivacyPolicy", "true")
                .param("agreedToSmsNotice", "true"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/register/sms"))
            .andReturn();

        MockHttpSession session = (MockHttpSession) registerResult.getRequest().getSession(false);
        RegisterSessionState state = (RegisterSessionState) session.getAttribute(RegistrationController.REGISTER_SESSION_KEY);

        mockMvc.perform(post("/register/sms/verify")
                .with(csrf())
                .session(session)
                .param("code", state.getLatestSmsCode()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/register/profile"));

        mockMvc.perform(post("/register/profile")
                .with(csrf())
                .session(session)
                .param("bio", "Bio")
                .param("region", "Tokyo")
                .param("interestRegion", "Kamakura")
                .param("visitHistory", "Visited once")
                .param("careNote", "No smoking")
                .param("withChildren", "UNSPECIFIED")
                .param("foodNote", "No shellfish")
                .param("relationNote", "Prefer daytime")
                .param("receiveOperationNotice", "true")
                .param("receiveSecurityNotice", "true")
                .param("receiveBridgeContact", "true")
                .param("receiveEmailNotice", "true"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/register/verify"));

        mockMvc.perform(get("/register/verify").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("/login?returnTo=/app/home")));

        Boolean smsVerified = jdbcTemplate.queryForObject("SELECT sms_verified FROM users WHERE id = ?", Boolean.class, state.getUserId());
        Long profileCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_profiles WHERE user_id = ?", Long.class, state.getUserId());
        Long preferenceCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM contact_preferences WHERE user_id = ?", Long.class, state.getUserId());

        Assertions.assertEquals(Boolean.TRUE, smsVerified);
        Assertions.assertEquals(1L, profileCount);
        Assertions.assertEquals(1L, preferenceCount);
    }

    @Test
    @DisplayName("profile skip keeps registration moving and points login to mypage")
    void profileSkipRedirectsToVerifyWithMypageNextStep() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/register")
                .with(csrf())
                .param("name", "Skip User")
                .param("nameKana", "スキップユーザー")
                .param("birthday", "1994-04-04")
                .param("gender", "NO_ANSWER")
                .param("email", "skip@example.com")
                .param("phoneNumber", "090-7777-8888")
                .param("address", "Nagoya")
                .param("nickname", "skip-user")
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

        mockMvc.perform(post("/register/profile/skip")
                .with(csrf())
                .session(session))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/register/verify"));

        mockMvc.perform(get("/register/verify").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("/login?returnTo=/app/mypage")))
            .andExpect(content().string(containsString("プロフィールはあとで設定できます。")));

        Long profileCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_profiles WHERE user_id = ?", Long.class, state.getUserId());
        Long preferenceCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM contact_preferences WHERE user_id = ?", Long.class, state.getUserId());

        Assertions.assertEquals(0L, profileCount);
        Assertions.assertEquals(0L, preferenceCount);
    }

    @Test
    @DisplayName("invalid SMS code keeps the user on the SMS step")
    void invalidSmsCodeDoesNotAdvance() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/register")
                .with(csrf())
                .param("name", "SMS User")
                .param("nameKana", "エスエムエスユーザー")
                .param("birthday", "1993-03-03")
                .param("gender", "NO_ANSWER")
                .param("email", "sms@example.com")
                .param("phoneNumber", "090-5555-6666")
                .param("address", "Osaka")
                .param("nickname", "sms-user")
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
                .param("code", "999999"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("認証コードが正しくないか、有効期限切れです。")));

        Boolean smsVerified = jdbcTemplate.queryForObject("SELECT sms_verified FROM users WHERE id = ?", Boolean.class, state.getUserId());
        Assertions.assertEquals(Boolean.FALSE, smsVerified);
    }
}
