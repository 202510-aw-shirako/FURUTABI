package com.furutabi.admin;

import java.util.Locale;

public final class AdminRoleCatalog {

    public static final String USER_ROLE = "user_role";
    public static final String BRIDGE_ROLE = "bridge_role";
    public static final String LOCAL_ROLE = "local_role";
    public static final String ADMIN_ROLE = "admin_role";

    public static final String HOST_PERMISSION = "host_permission";
    public static final String PARTNER_PERMISSION = "partner_permission";

    private AdminRoleCatalog() {
    }

    public static String canonicalRoleName(String legacyRoleName) {
        if (legacyRoleName == null || legacyRoleName.isBlank()) {
            return USER_ROLE;
        }
        return switch (legacyRoleName.trim().toUpperCase(Locale.ROOT)) {
            case "ADMIN" -> ADMIN_ROLE;
            case "BRIDGE" -> BRIDGE_ROLE;
            case "LOCAL" -> LOCAL_ROLE;
            default -> USER_ROLE;
        };
    }

    public static String displayRoleLabel(String canonicalRoleName) {
        if (canonicalRoleName == null || canonicalRoleName.isBlank()) {
            return "ごひいきさん";
        }
        return switch (canonicalRoleName.trim().toLowerCase(Locale.ROOT)) {
            case BRIDGE_ROLE -> "架け橋さん";
            case LOCAL_ROLE -> "地域の方";
            case ADMIN_ROLE -> "管理用";
            default -> "ごひいきさん";
        };
    }

    public static boolean defaultHostPermission(String canonicalRoleName) {
        String normalized = normalize(canonicalRoleName);
        return BRIDGE_ROLE.equals(normalized) || LOCAL_ROLE.equals(normalized);
    }

    public static boolean defaultPartnerPermission(String canonicalRoleName) {
        return BRIDGE_ROLE.equals(normalize(canonicalRoleName));
    }

    public static boolean defaultIndividualHostGrantAllowed(String canonicalRoleName) {
        return USER_ROLE.equals(normalize(canonicalRoleName));
    }

    public static boolean defaultIndividualPartnerGrantAllowed(String canonicalRoleName) {
        return USER_ROLE.equals(normalize(canonicalRoleName));
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return USER_ROLE;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isManagedPermission(String permissionName) {
        String normalized = normalizePermission(permissionName);
        return HOST_PERMISSION.equals(normalized) || PARTNER_PERMISSION.equals(normalized);
    }

    public static String normalizePermission(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
