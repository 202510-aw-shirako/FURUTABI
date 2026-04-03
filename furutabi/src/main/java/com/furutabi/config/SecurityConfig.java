package com.furutabi.config;

import com.furutabi.auth.LoginSuccessHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, LoginSuccessHandler loginSuccessHandler) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Dev-only support for the embedded H2 console.
                .requestMatchers("/h2-console/**").permitAll()
                // Preview routes stay public until the real login / access design is implemented.
                .requestMatchers("/preview/**").permitAll()
                .requestMatchers(
                    "/",
                    "/bridge", "/bridge.html",
                    "/index", "/index.html",
                    "/about", "/about.html",
                    "/faq", "/faq.html",
                    "/gate", "/gate.html",
                    "/gate-entry", "/gate-entry.html",
                    "/local", "/local.html",
                    "/story", "/story.html",
                    "/notices", "/notices.html",
                    "/notice", "/notice.html",
                    "/okatte-entry", "/okatte-entry.html",
                    "/terms", "/terms.html",
                    "/privacy", "/privacy.html",
                    "/contact", "/contact.html",
                    "/contact-complete", "/contact-complete.html",
                    "/map", "/map.html",
                    "/safety", "/safety.html",
                    "/safety-complete", "/safety-complete.html",
                    "/tour", "/tour.html"
                ).permitAll()
                .requestMatchers("/login", "/register", "/register/**").permitAll()
                // Preview pages depend on these static assets being readable without authentication.
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/assets/**").permitAll()
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**")
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                // returnTo is limited to safe internal paths; role-based routing stays deferred.
                .successHandler(loginSuccessHandler)
                .failureUrl("/login?error")
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
            );

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
