package com.furutabi.config;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LoginFlowBoundaryTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUpUser() {
        Timestamp now = Timestamp.from(Instant.parse("2026-03-25T00:00:00Z"));

        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM users");

        jdbcTemplate.update(
            """
                INSERT INTO users (
                    id, email, password_hash, nickname, name, name_kana, birthday, gender,
                    phone_number, address, sms_verified, additional_verification_status,
                    created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            1L,
            "user@example.com",
            passwordEncoder.encode("password123"),
            "sample-user",
            "Sample User",
            "sample-user-kana",
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
    }

    @Test
    @DisplayName("GET /login returns the backend login page")
    void loginPageIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"email\"")));
    }

    @Test
    @DisplayName("GET /login with safe returnTo keeps it in a hidden field")
    void loginPageKeepsSafeReturnTo() throws Exception {
        mockMvc.perform(get("/login").queryParam("returnTo", "/app/home"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"returnTo\"")))
            .andExpect(content().string(containsString("value=\"/app/home\"")));
    }

    @Test
    @DisplayName("GET /login does not keep external returnTo")
    void loginPageDoesNotKeepExternalReturnTo() throws Exception {
        mockMvc.perform(get("/login").queryParam("returnTo", "https://example.com"))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("name=\"returnTo\""))));
    }

    @Test
    @DisplayName("POST /login authenticates with email and password_hash")
    void loginAuthenticatesAgainstUsersTable() throws Exception {
        mockMvc.perform(formLogin("/login").user("email", "user@example.com").password("password", "password123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/preview/public/index.html"))
            .andExpect(authenticated().withUsername("user@example.com"));
    }

    @Test
    @DisplayName("POST /login redirects to a safe internal returnTo")
    void loginUsesSafeInternalReturnTo() throws Exception {
        mockMvc.perform(
                post("/login")
                    .with(csrf())
                    .param("email", "user@example.com")
                    .param("password", "password123")
                    .param("returnTo", "/app/home")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/home"))
            .andExpect(authenticated().withUsername("user@example.com"));
    }

    @Test
    @DisplayName("POST /login ignores external returnTo and falls back")
    void loginIgnoresExternalReturnTo() throws Exception {
        mockMvc.perform(
                post("/login")
                    .with(csrf())
                    .param("email", "user@example.com")
                    .param("password", "password123")
                    .param("returnTo", "https://example.com")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/preview/public/index.html"))
            .andExpect(authenticated().withUsername("user@example.com"));
    }

    @Test
    @DisplayName("POST /login ignores protocol-relative returnTo and falls back")
    void loginIgnoresProtocolRelativeReturnTo() throws Exception {
        mockMvc.perform(
                post("/login")
                    .with(csrf())
                    .param("email", "user@example.com")
                    .param("password", "password123")
                    .param("returnTo", "//example.com")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/preview/public/index.html"))
            .andExpect(authenticated().withUsername("user@example.com"));
    }

    @Test
    @DisplayName("POST /login with invalid password returns to login with error")
    void loginFailureRedirectsBackToLogin() throws Exception {
        mockMvc.perform(formLogin("/login").user("email", "user@example.com").password("password", "wrong-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?error"))
            .andExpect(unauthenticated());
    }

    @Test
    @DisplayName("POST /logout invalidates the session and redirects to login")
    void logoutRedirectsBackToLogin() throws Exception {
        mockMvc.perform(logout("/logout"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?logout"))
            .andExpect(unauthenticated());
    }
}
