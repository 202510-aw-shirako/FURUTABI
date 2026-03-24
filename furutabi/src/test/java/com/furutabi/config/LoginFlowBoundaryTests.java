package com.furutabi.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class LoginFlowBoundaryTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("未認証でもデフォルトのログイン画面には入れる")
    void loginPageIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("returnTo 付きの login URL は現状の Java 側では plain な login へ寄せられる")
    void loginPageWithReturnToRedirectsBackToPlainLogin() throws Exception {
        mockMvc.perform(get("/login").queryParam("returnTo", "/app/home"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("外部 URL の returnTo でも現状の Java 側では値を使わず plain な login へ戻す")
    void loginPageWithExternalReturnToRedirectsBackToPlainLogin() throws Exception {
        mockMvc.perform(get("/login").queryParam("returnTo", "https://example.com"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }
}
