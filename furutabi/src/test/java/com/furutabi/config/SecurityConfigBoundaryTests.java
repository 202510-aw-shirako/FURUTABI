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
    @DisplayName("Unauthenticated app route redirects to login")
    void unauthenticatedAppRequestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/app/home"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Authenticated app route passes security before missing route handling")
    void authenticatedAppRequestPassesSecurityBeforeMissingRoute() throws Exception {
        mockMvc.perform(get("/app/home").with(user("user@example.com").roles("USER")))
            .andExpect(status().isNotFound());
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
