package com.furutabi.auth;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private static final Duration SMS_EXPIRY = Duration.ofMinutes(5);

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegistrationStartResult startRegistration(RegisterStartForm form) {
        if (emailExists(form.getEmail())) {
            throw new IllegalStateException("Email is already registered.");
        }

        Timestamp now = Timestamp.from(Instant.now());
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                """
                    INSERT INTO users (
                        email, password_hash, nickname, name, name_kana, birthday, gender,
                        phone_number, address, sms_verified, additional_verification_status,
                        created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, normalize(form.getEmail()));
            statement.setString(2, passwordEncoder.encode(form.getPassword()));
            statement.setString(3, trim(form.getNickname()));
            statement.setString(4, trim(form.getName()));
            statement.setString(5, trim(form.getNameKana()));
            statement.setDate(6, java.sql.Date.valueOf(form.getBirthday()));
            statement.setString(7, trim(form.getGender()));
            statement.setString(8, trim(form.getPhoneNumber()));
            statement.setString(9, trim(form.getAddress()));
            statement.setBoolean(10, false);
            statement.setString(11, "UNREQUESTED");
            statement.setTimestamp(12, now);
            statement.setTimestamp(13, now);
            return statement;
        }, keyHolder);

        Number userId = keyHolder.getKey();
        if (userId == null) {
            throw new IllegalStateException("Failed to create user.");
        }

        jdbcTemplate.update(
            "INSERT INTO user_roles (user_id, role_name, created_at) VALUES (?, ?, ?)",
            userId.longValue(),
            "USER",
            now
        );

        String smsCode = issueSmsCode(userId.longValue(), form.getPhoneNumber());
        return new RegistrationStartResult(userId.longValue(), smsCode);
    }

    @Transactional
    public String issueSmsCode(long userId, String phoneNumber) {
        String rawCode = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        Timestamp now = Timestamp.from(Instant.now());
        Timestamp expiresAt = Timestamp.from(now.toInstant().plus(SMS_EXPIRY));

        jdbcTemplate.update(
            """
                INSERT INTO sms_verifications (
                    user_id, phone_number, verification_code_hash, expires_at, used_at, retry_count, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
            userId,
            trim(phoneNumber),
            passwordEncoder.encode(rawCode),
            expiresAt,
            null,
            0,
            now,
            now
        );

        return rawCode;
    }

    @Transactional
    public boolean verifySmsCode(long userId, String code) {
        SmsVerificationRow row = latestVerification(userId);
        if (row == null || row.usedAt() != null || row.expiresAt().before(Timestamp.from(Instant.now()))) {
            return false;
        }

        if (!passwordEncoder.matches(code, row.codeHash())) {
            return false;
        }

        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.update(
            "UPDATE sms_verifications SET used_at = ?, updated_at = ? WHERE id = ?",
            now,
            now,
            row.id()
        );
        jdbcTemplate.update(
            "UPDATE users SET sms_verified = ?, updated_at = ? WHERE id = ?",
            true,
            now,
            userId
        );
        return true;
    }

    @Transactional
    public void saveProfile(long userId, RegisterProfileForm form) {
        Timestamp now = Timestamp.from(Instant.now());

        if (exists("SELECT COUNT(*) FROM user_profiles WHERE user_id = ?", userId)) {
            jdbcTemplate.update(
                """
                    UPDATE user_profiles
                    SET bio = ?, region = ?, interest_region = ?, visit_history = ?, care_note = ?,
                        with_children = ?, food_note = ?, relation_note = ?, updated_at = ?
                    WHERE user_id = ?
                    """,
                nullable(form.getBio()),
                nullable(form.getRegion()),
                nullable(form.getInterestRegion()),
                nullable(form.getVisitHistory()),
                nullable(form.getCareNote()),
                nullable(form.getWithChildren()),
                nullable(form.getFoodNote()),
                nullable(form.getRelationNote()),
                now,
                userId
            );
        } else {
            jdbcTemplate.update(
                """
                    INSERT INTO user_profiles (
                        user_id, bio, icon_path, region, interest_region, visit_history,
                        care_note, with_children, food_note, relation_note, age_range, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                userId,
                nullable(form.getBio()),
                null,
                nullable(form.getRegion()),
                nullable(form.getInterestRegion()),
                nullable(form.getVisitHistory()),
                nullable(form.getCareNote()),
                nullable(form.getWithChildren()),
                nullable(form.getFoodNote()),
                nullable(form.getRelationNote()),
                null,
                now,
                now
            );
        }

        if (exists("SELECT COUNT(*) FROM contact_preferences WHERE user_id = ?", userId)) {
            jdbcTemplate.update(
                """
                    UPDATE contact_preferences
                    SET receive_operation_notice = ?, receive_security_notice = ?, receive_bridge_contact = ?,
                        receive_local_contact = ?, receive_email_notice = ?, receive_sms_notice = ?, updated_at = ?
                    WHERE user_id = ?
                    """,
                form.isReceiveOperationNotice(),
                form.isReceiveSecurityNotice(),
                form.isReceiveBridgeContact(),
                form.isReceiveLocalContact(),
                form.isReceiveEmailNotice(),
                form.isReceiveSmsNotice(),
                now,
                userId
            );
        } else {
            jdbcTemplate.update(
                """
                    INSERT INTO contact_preferences (
                        user_id, receive_operation_notice, receive_security_notice, receive_bridge_contact,
                        receive_local_contact, receive_email_notice, receive_sms_notice, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                userId,
                form.isReceiveOperationNotice(),
                form.isReceiveSecurityNotice(),
                form.isReceiveBridgeContact(),
                form.isReceiveLocalContact(),
                form.isReceiveEmailNotice(),
                form.isReceiveSmsNotice(),
                now,
                now
            );
        }

        jdbcTemplate.update("UPDATE users SET updated_at = ? WHERE id = ?", now, userId);
    }

    private boolean emailExists(String email) {
        return exists("SELECT COUNT(*) FROM users WHERE email = ?", normalize(email));
    }

    private boolean exists(String sql, Object value) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, value);
        return count != null && count > 0;
    }

    private SmsVerificationRow latestVerification(long userId) {
        try {
            return jdbcTemplate.queryForObject(
                """
                    SELECT id, verification_code_hash, expires_at, used_at
                    FROM sms_verifications
                    WHERE user_id = ?
                    ORDER BY created_at DESC, id DESC
                    LIMIT 1
                    """,
                (rs, rowNum) -> new SmsVerificationRow(
                    rs.getLong("id"),
                    rs.getString("verification_code_hash"),
                    rs.getTimestamp("expires_at"),
                    rs.getTimestamp("used_at")
                ),
                userId
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private String normalize(String value) {
        return trim(value).toLowerCase();
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String nullable(String value) {
        String trimmed = value == null ? null : value.trim();
        return (trimmed == null || trimmed.isEmpty()) ? null : trimmed;
    }

    public record RegistrationStartResult(long userId, String smsCode) {
    }

    private record SmsVerificationRow(long id, String codeHash, Timestamp expiresAt, Timestamp usedAt) {
    }
}
