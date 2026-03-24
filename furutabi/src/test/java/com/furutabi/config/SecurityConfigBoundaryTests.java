package com.furutabi.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigBoundaryTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("未認証では app 配下へ直接入れずログインへリダイレクトされる")
    void unauthenticatedAppRequestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/app/home"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("認証済みなら app 配下の存在しない URL でもログインへは戻されない")
    void authenticatedAppRequestPassesSecurityBeforeMissingRoute() throws Exception {
        mockMvc.perform(get("/app/home").with(user("user@example.com").roles("USER")))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("現状コードでは public 配下も未認証のままでは公開されずログインへリダイレクトされる")
    void unauthenticatedPublicPathAlsoRedirectsToLoginInCurrentCode() throws Exception {
        mockMvc.perform(get("/public/index.html"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("認証済み POST は H2 console 以外では CSRF 保護により拒否される")
    void csrfIsStillRequiredOutsideH2Console() throws Exception {
        mockMvc.perform(post("/app/home").with(user("user@example.com").roles("USER")))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("H2 console 配下は未認証かつ CSRF トークンなしでも SecurityConfig 上は拒否されない")
    void h2ConsolePostIsNotBlockedByCsrfOrAuthentication() throws Exception {
        mockMvc.perform(post("/h2-console/login.do"))
            .andExpect(status().isNotFound());
    }
}
