package com.furutabi.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PublicPageFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Public routes are accessible without authentication")
    void publicRoutesAreAccessibleWithoutAuthentication() throws Exception {
        String[] routes = {
            "/",
            "/bridge.html",
            "/index.html",
            "/about.html",
            "/faq.html",
            "/gate.html",
            "/gate-entry.html",
            "/local.html",
            "/story.html",
            "/notices.html",
            "/notice.html",
            "/okatte-entry.html",
            "/terms.html",
            "/privacy.html",
            "/contact.html",
            "/contact-complete.html"
        };

        for (String route : routes) {
            mockMvc.perform(get(route))
                .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Map exposure redirects to the public top map section")
    void mapExposureRedirectsToTopMapSection() throws Exception {
        mockMvc.perform(get("/map.html"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/index.html#top-footprints"));
    }

    @Test
    @DisplayName("Safety routes redirect to contact routes")
    void safetyRoutesRedirectToContactRoutes() throws Exception {
        mockMvc.perform(get("/safety.html"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/contact.html"));

        mockMvc.perform(get("/safety-complete.html"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/contact-complete.html"));
    }

    @Test
    @DisplayName("Tour route redirects to external placeholder")
    void tourRouteRedirectsToExternalPlaceholder() throws Exception {
        mockMvc.perform(get("/tour.html"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("https://example.com/furutabi-tour"));
    }
}
