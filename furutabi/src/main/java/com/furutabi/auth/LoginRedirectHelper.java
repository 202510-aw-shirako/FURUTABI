package com.furutabi.auth;

import java.util.Collection;
import java.util.Optional;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class LoginRedirectHelper {

    private static final String DEFAULT_TARGET = "/preview/public/index.html";
    private static final String LOCAL_TARGET = "/preview/public/bridge.html";

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

    public String resolveOrDefault(String returnTo, Collection<? extends GrantedAuthority> authorities) {
        return sanitize(returnTo).orElseGet(() -> defaultTargetFor(authorities));
    }

    String defaultTargetFor(Collection<? extends GrantedAuthority> authorities) {
        if (hasRole(authorities, "ROLE_LOCAL") || hasRole(authorities, "ROLE_BRIDGE")) {
            return LOCAL_TARGET;
        }

        if (hasRole(authorities, "ROLE_ADMIN") || hasRole(authorities, "ROLE_USER")) {
            return DEFAULT_TARGET;
        }

        return DEFAULT_TARGET;
    }

    private boolean hasRole(Collection<? extends GrantedAuthority> authorities, String roleName) {
        return authorities.stream().anyMatch(authority -> roleName.equals(authority.getAuthority()));
    }
}
