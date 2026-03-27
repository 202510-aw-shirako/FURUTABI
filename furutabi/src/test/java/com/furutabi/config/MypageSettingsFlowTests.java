package com.furutabi.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
class MypageSettingsFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpUserData() {
        Timestamp now = Timestamp.from(Instant.parse("2026-03-25T00:00:00Z"));

        jdbcTemplate.update("DELETE FROM chat_messages");
        jdbcTemplate.update("DELETE FROM chat_threads");
        jdbcTemplate.update("DELETE FROM proposal_application_status_history");
        jdbcTemplate.update("DELETE FROM proposal_applications");
        jdbcTemplate.update("DELETE FROM proposal_tags");
        jdbcTemplate.update("DELETE FROM proposals");
        jdbcTemplate.update("DELETE FROM map_record_comments");
        jdbcTemplate.update("DELETE FROM map_record_images");
        jdbcTemplate.update("DELETE FROM map_records");
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
            100L,
            "user@example.com",
            "{noop}unused",
            "furutabi-user",
            "Furutabi Hanako",
            "FURUTABI HANAKO",
            Date.valueOf("1992-06-12"),
            "FEMALE",
            "090-1234-5678",
            "Tokyo Chiyoda 1-2-3",
            Boolean.TRUE,
            "UNREQUESTED",
            now,
            now
        );
        jdbcTemplate.update(
            "INSERT INTO user_roles (user_id, role_name, created_at) VALUES (?, ?, ?)",
            100L,
            "USER",
            now
        );
        jdbcTemplate.update(
            """
                INSERT INTO user_profiles (
                    user_id, bio, icon_path, region, interest_region, visit_history,
                    care_note, with_children, food_note, relation_note, age_range, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            100L,
            "I enjoy talking with local people while traveling.",
            null,
            "Tokyo",
            "Kamakura",
            "Visited a few times each year",
            "Crowded places are a little difficult.",
            "UNSPECIFIED",
            "Shellfish allergy",
            "Quiet places are easier for me.",
            null,
            now,
            now
        );
        jdbcTemplate.update(
            """
                INSERT INTO contact_preferences (
                    user_id, receive_operation_notice, receive_security_notice, receive_bridge_contact,
                    receive_local_contact, receive_email_notice, receive_sms_notice, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            100L,
            true,
            true,
            true,
            false,
            true,
            false,
            now,
            now
        );
    }

    @Test
    @DisplayName("GET /app/account shows saved account information")
    void accountPageShowsSavedData() throws Exception {
        mockMvc.perform(get("/app/account").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("furutabi-user")))
            .andExpect(content().string(containsString("user@example.com")));
    }

    @Test
    @DisplayName("POST /app/account updates users table")
    void accountPageUpdatesUsersTable() throws Exception {
        mockMvc.perform(post("/app/account")
                .with(user("user@example.com").roles("USER"))
                .with(csrf())
                .param("nickname", "furutabi-updated")
                .param("name", "Furutabi Hana")
                .param("nameKana", "FURUTABI HANA")
                .param("birthday", "1991-01-02")
                .param("gender", "NO_ANSWER")
                .param("phoneNumber", "080-1111-2222")
                .param("address", "Kamakura 1-2-3"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/account?saved"));

        String nickname = jdbcTemplate.queryForObject("SELECT nickname FROM users WHERE id = ?", String.class, 100L);
        String phoneNumber = jdbcTemplate.queryForObject("SELECT phone_number FROM users WHERE id = ?", String.class, 100L);

        Assertions.assertEquals("furutabi-updated", nickname);
        Assertions.assertEquals("080-1111-2222", phoneNumber);
    }

    @Test
    @DisplayName("GET /app/profile shows saved profile information")
    void profilePageShowsSavedData() throws Exception {
        mockMvc.perform(get("/app/profile").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("I enjoy talking with local people while traveling.")))
            .andExpect(content().string(containsString("Kamakura")));
    }

    @Test
    @DisplayName("POST /app/profile updates user_profiles table")
    void profilePageUpdatesUserProfilesTable() throws Exception {
        mockMvc.perform(post("/app/profile")
                .with(user("user@example.com").roles("USER"))
                .with(csrf())
                .param("bio", "I enjoy slow walks in seaside towns.")
                .param("region", "Shonan")
                .param("interestRegion", "Onomichi")
                .param("visitHistory", "Visited Kamakura three times")
                .param("careNote", "Fewer stairs would help.")
                .param("withChildren", "WITHOUT_CHILDREN")
                .param("foodNote", "Mild spice preferred")
                .param("relationNote", "Quiet lodging helps a lot."))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/profile?saved"));

        String region = jdbcTemplate.queryForObject("SELECT region FROM user_profiles WHERE user_id = ?", String.class, 100L);
        String withChildren = jdbcTemplate.queryForObject("SELECT with_children FROM user_profiles WHERE user_id = ?", String.class, 100L);

        Assertions.assertEquals("Shonan", region);
        Assertions.assertEquals("WITHOUT_CHILDREN", withChildren);
    }

    @Test
    @DisplayName("GET /app/privacy-settings shows saved contact preferences")
    void privacySettingsPageShowsSavedData() throws Exception {
        mockMvc.perform(get("/app/privacy-settings").with(user("user@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("/app/privacy-settings")))
            .andExpect(content().string(containsString("receiveBridgeContact")));
    }

    @Test
    @DisplayName("POST /app/privacy-settings updates contact_preferences table")
    void privacySettingsPageUpdatesContactPreferencesTable() throws Exception {
        mockMvc.perform(post("/app/privacy-settings")
                .with(user("user@example.com").roles("USER"))
                .with(csrf())
                .param("receiveOperationNotice", "true")
                .param("receiveSecurityNotice", "false")
                .param("receiveBridgeContact", "false")
                .param("receiveLocalContact", "true")
                .param("receiveEmailNotice", "false")
                .param("receiveSmsNotice", "true"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/privacy-settings?saved"));

        Boolean receiveLocalContact = jdbcTemplate.queryForObject(
            "SELECT receive_local_contact FROM contact_preferences WHERE user_id = ?",
            Boolean.class,
            100L
        );
        Boolean receiveSmsNotice = jdbcTemplate.queryForObject(
            "SELECT receive_sms_notice FROM contact_preferences WHERE user_id = ?",
            Boolean.class,
            100L
        );

        Assertions.assertEquals(Boolean.TRUE, receiveLocalContact);
        Assertions.assertEquals(Boolean.TRUE, receiveSmsNotice);
    }
}
