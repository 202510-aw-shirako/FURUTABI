package com.furutabi.visibility;

public enum VisibilityScope {
    PUBLIC,
    PRIVATE,
    LIMITED;

    public static VisibilityScope fromDbValue(String value) {
        if (value == null || value.isBlank()) {
            return PRIVATE;
        }
        return switch (value.trim().toUpperCase()) {
            case "PUBLIC" -> PUBLIC;
            case "LIMITED" -> LIMITED;
            default -> PRIVATE;
        };
    }
}
