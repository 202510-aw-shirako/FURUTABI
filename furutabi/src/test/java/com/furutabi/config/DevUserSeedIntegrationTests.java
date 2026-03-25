package com.furutabi.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class DevUserSeedIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("dev profile seeds minimum login users and roles")
    void devProfileSeedsMinimumUsers() throws Exception {
        assertSeededUser("user@example.com", "USER");
        assertSeededUser("local@example.com", "LOCAL");
        assertSeededUser("bridge@example.com", "BRIDGE");
        assertSeededUser("admin@example.com", "ADMIN");

        mockMvc.perform(formLogin("/login").user("email", "user@example.com").password("password", "password123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/home"))
            .andExpect(authenticated().withUsername("user@example.com"));
    }

    private void assertSeededUser(String email, String roleName) {
        Long userCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE email = ? AND sms_verified = TRUE",
            Long.class,
            email
        );
        Long roleCount = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM user_roles ur
                JOIN users u ON u.id = ur.user_id
                WHERE u.email = ? AND ur.role_name = ?
                """,
            Long.class,
            email,
            roleName
        );

        if (userCount == null || userCount != 1L || roleCount == null || roleCount != 1L) {
            throw new AssertionError("dev seed user was not created correctly for " + email + " / " + roleName);
        }
    }
}
