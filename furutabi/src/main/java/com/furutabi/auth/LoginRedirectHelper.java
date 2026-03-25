package com.furutabi.auth;

import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class LoginRedirectHelper {

    private static final String DEFAULT_TARGET = "/preview/public/index.html";

    public Optional<String> sanitize(String returnTo) {
        if (returnTo == null || returnTo.isBlank()) {
            return Optional.empty();
        }

        if (!returnTo.startsWith("/") || returnTo.startsWith("//")) {
            return Optional.empty();
        }

        if (returnTo.startsWith("/login") || returnTo.startsWith("/logout")) {
            return Optional.empty();
        }

        if (returnTo.contains("\r") || returnTo.contains("\n")) {
            return Optional.empty();
        }

        return Optional.of(returnTo);
    }

    public String resolveOrDefault(String returnTo) {
        return sanitize(returnTo).orElse(DEFAULT_TARGET);
    }
}
