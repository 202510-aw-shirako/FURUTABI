package com.furutabi.dev;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevUserSeed implements ApplicationRunner {

    // Development-only fixed credentials for manual verification.
    // These users are not part of the production specification and can be removed later.
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;

    public DevUserSeed(
        JdbcTemplate jdbcTemplate,
        PasswordEncoder passwordEncoder,
        @Value("${app.dev-seed.enabled:true}") boolean enabled
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        seedUser("user@example.com", "USER", "一般ユーザー", "イッパンユーザー");
        seedUser("local@example.com", "LOCAL", "地域ユーザー", "チイキユーザー");
        seedUser("bridge@example.com", "BRIDGE", "架け橋ユーザー", "カケハシユーザー");
        seedUser("admin@example.com", "ADMIN", "管理ユーザー", "カンリユーザー");
    }

    private void seedUser(String email, String roleName, String name, String nameKana) {
        List<Long> existingUserIds = jdbcTemplate.query(
            "SELECT id FROM users WHERE email = ?",
            (rs, rowNum) -> rs.getLong("id"),
            email
        );
        Long existingUserId = existingUserIds.isEmpty() ? null : existingUserIds.getFirst();

        long userId;
        if (existingUserId == null) {
            Timestamp now = Timestamp.from(Instant.now());
            jdbcTemplate.update(
                """
                    INSERT INTO users (
                        email, password_hash, nickname, name, name_kana, birthday, gender,
                        phone_number, address, sms_verified, additional_verification_status,
                        created_at, updated_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                email,
                passwordEncoder.encode("password123"),
                roleName.toLowerCase() + "-seed",
                name,
                nameKana,
                Date.valueOf("1990-01-01"),
                "NO_ANSWER",
                "090-0000-0000",
                "Dev Seed",
                Boolean.TRUE,
                "UNREQUESTED",
                now,
                now
            );

            userId = Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?",
                Long.class,
                email
                ),
                "Seed user ID was not found after insert: " + email
            );
        } else {
            userId = existingUserId;
        }

        if (!hasRole(userId, roleName)) {
            jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role_name, created_at) VALUES (?, ?, ?)",
                userId,
                roleName,
                Timestamp.from(Instant.now())
            );
        }
    }

    private boolean hasRole(long userId, String roleName) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role_name = ?",
            Long.class,
            userId,
            roleName
        );
        return count != null && count > 0;
    }
}
