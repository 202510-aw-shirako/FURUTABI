package com.furutabi.app;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.furutabi.visibility.VisibilityScope;

@Service
public class AppSettingsService {

    private final JdbcTemplate jdbcTemplate;
    private final UserPointService userPointService;

    public AppSettingsService(JdbcTemplate jdbcTemplate, UserPointService userPointService) {
        this.jdbcTemplate = jdbcTemplate;
        this.userPointService = userPointService;
    }

    public MypageSummary loadMypageSummary(String email) {
        UserRow user = requireUser(email);
        UserProfileRow profile = loadProfileRow(user.id());
        UserPointService.UserPointSummary pointSummary = userPointService.loadUserPointSummary(user.id());
        return new MypageSummary(
            user.email(),
            user.nickname(),
            user.name(),
            user.smsVerified(),
            user.additionalVerificationStatus(),
            profile == null ? null : profile.region(),
            profile == null ? null : profile.interestRegion(),
            profile != null && hasProfileContent(profile),
            hasRole(user.id(), "ADMIN"),
            pointSummary.currentPoints(),
            pointSummary.enabledForRegion(),
            pointSummary.recentHistory()
        );
    }

    public AccountPageData loadAccountPage(String email) {
        UserRow user = requireUser(email);

        AppAccountForm form = new AppAccountForm();
        form.setNickname(user.nickname());
        form.setName(user.name());
        form.setNameKana(user.nameKana());
        form.setBirthday(user.birthday().toString());
        form.setGender(user.gender());
        form.setPhoneNumber(user.phoneNumber());
        form.setAddress(user.address());

        return new AccountPageData(user.email(), user.smsVerified(), user.additionalVerificationStatus(), form);
    }

    public AppProfileForm loadProfileForm(String email) {
        UserRow user = requireUser(email);
        UserProfileRow profile = loadProfileRow(user.id());

        AppProfileForm form = new AppProfileForm();
        if (profile != null) {
            form.setBio(profile.bio());
            form.setAgeRange(profile.ageRange());
            form.setRegion(profile.region());
            form.setInterestRegion(profile.interestRegion());
            form.setVisitHistory(profile.visitHistory());
            form.setCareNote(profile.careNote());
            form.setWithChildren(profile.withChildren());
            form.setFoodNote(profile.foodNote());
            form.setRelationNote(profile.relationNote());
        }
        return form;
    }

    public PrivacySettingsPageData loadPrivacySettingsPage(String email) {
        UserRow user = requireUser(email);
        ContactPreferenceRow preference = loadPreferenceRow(user.id());

        AppPrivacySettingsForm form = new AppPrivacySettingsForm();
        if (preference != null) {
            form.setProfileVisibility(preference.profileVisibility().name());
            form.setMapDefaultVisibility(preference.mapDefaultVisibility().name());
            form.setReceiveOperationNotice(preference.receiveOperationNotice());
            form.setReceiveSecurityNotice(preference.receiveSecurityNotice());
            form.setReceiveBridgeContact(preference.receiveBridgeContact());
            form.setReceiveLocalContact(preference.receiveLocalContact());
            form.setReceiveEmailNotice(preference.receiveEmailNotice());
            form.setReceiveSmsNotice(preference.receiveSmsNotice());
        }

        return new PrivacySettingsPageData(user.smsVerified(), user.additionalVerificationStatus(), form);
    }

    @Transactional
    public void saveAccount(String email, AppAccountForm form) {
        UserRow user = requireUser(email);
        Timestamp now = Timestamp.from(Instant.now());

        jdbcTemplate.update(
            """
                UPDATE users
                SET nickname = ?, name = ?, name_kana = ?, birthday = ?, gender = ?,
                    phone_number = ?, address = ?, updated_at = ?
                WHERE id = ?
                """,
            trim(form.getNickname()),
            trim(form.getName()),
            trim(form.getNameKana()),
            Date.valueOf(form.getBirthday()),
            trim(form.getGender()),
            trim(form.getPhoneNumber()),
            trim(form.getAddress()),
            now,
            user.id()
        );
    }

    @Transactional
    public void saveProfile(String email, AppProfileForm form) {
        UserRow user = requireUser(email);
        Timestamp now = Timestamp.from(Instant.now());

        if (exists("SELECT COUNT(*) FROM user_profiles WHERE user_id = ?", user.id())) {
            jdbcTemplate.update(
                """
                    UPDATE user_profiles
                    SET bio = ?, age_range = ?, region = ?, interest_region = ?, visit_history = ?, care_note = ?,
                        with_children = ?, food_note = ?, relation_note = ?, updated_at = ?
                    WHERE user_id = ?
                    """,
                nullable(form.getBio()),
                nullable(form.getAgeRange()),
                nullable(form.getRegion()),
                nullable(form.getInterestRegion()),
                nullable(form.getVisitHistory()),
                nullable(form.getCareNote()),
                nullable(form.getWithChildren()),
                nullable(form.getFoodNote()),
                nullable(form.getRelationNote()),
                now,
                user.id()
            );
        } else {
            jdbcTemplate.update(
                """
                    INSERT INTO user_profiles (
                        user_id, bio, icon_path, region, interest_region, visit_history,
                        care_note, with_children, food_note, relation_note, age_range, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                user.id(),
                nullable(form.getBio()),
                null,
                nullable(form.getRegion()),
                nullable(form.getInterestRegion()),
                nullable(form.getVisitHistory()),
                nullable(form.getCareNote()),
                nullable(form.getWithChildren()),
                nullable(form.getFoodNote()),
                nullable(form.getRelationNote()),
                nullable(form.getAgeRange()),
                now,
                now
            );
        }

        jdbcTemplate.update("UPDATE users SET updated_at = ? WHERE id = ?", now, user.id());
    }

    @Transactional
    public void savePrivacySettings(String email, AppPrivacySettingsForm form) {
        UserRow user = requireUser(email);
        Timestamp now = Timestamp.from(Instant.now());

        if (exists("SELECT COUNT(*) FROM contact_preferences WHERE user_id = ?", user.id())) {
            jdbcTemplate.update(
                """
                    UPDATE contact_preferences
                    SET profile_visibility = ?, map_default_visibility = ?,
                        receive_operation_notice = ?, receive_security_notice = ?, receive_bridge_contact = ?,
                        receive_local_contact = ?, receive_email_notice = ?, receive_sms_notice = ?, updated_at = ?
                    WHERE user_id = ?
                    """,
                toDbVisibility(form.getProfileVisibility()),
                toDbVisibility(form.getMapDefaultVisibility()),
                form.isReceiveOperationNotice(),
                form.isReceiveSecurityNotice(),
                form.isReceiveBridgeContact(),
                form.isReceiveLocalContact(),
                form.isReceiveEmailNotice(),
                form.isReceiveSmsNotice(),
                now,
                user.id()
            );
        } else {
            jdbcTemplate.update(
                """
                    INSERT INTO contact_preferences (
                        user_id, profile_visibility, map_default_visibility, receive_operation_notice,
                        receive_security_notice, receive_bridge_contact, receive_local_contact,
                        receive_email_notice, receive_sms_notice, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                user.id(),
                toDbVisibility(form.getProfileVisibility()),
                toDbVisibility(form.getMapDefaultVisibility()),
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

        jdbcTemplate.update("UPDATE users SET updated_at = ? WHERE id = ?", now, user.id());
    }

    private UserRow requireUser(String email) {
        try {
            return jdbcTemplate.queryForObject(
                """
                    SELECT id, email, nickname, name, name_kana, birthday, gender, phone_number, address,
                           sms_verified, additional_verification_status
                    FROM users
                    WHERE email = ?
                    """,
                (rs, rowNum) -> new UserRow(
                    rs.getLong("id"),
                    rs.getString("email"),
                    rs.getString("nickname"),
                    rs.getString("name"),
                    rs.getString("name_kana"),
                    rs.getDate("birthday"),
                    rs.getString("gender"),
                    rs.getString("phone_number"),
                    rs.getString("address"),
                    rs.getBoolean("sms_verified"),
                    rs.getString("additional_verification_status")
                ),
                email
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("User not found for settings: " + email, ex);
        }
    }

    private UserProfileRow loadProfileRow(long userId) {
        try {
            return jdbcTemplate.queryForObject(
                """
                    SELECT bio, age_range, region, interest_region, visit_history, care_note, with_children, food_note, relation_note
                    FROM user_profiles
                    WHERE user_id = ?
                    """,
                (rs, rowNum) -> new UserProfileRow(
                    rs.getString("bio"),
                    rs.getString("age_range"),
                    rs.getString("region"),
                    rs.getString("interest_region"),
                    rs.getString("visit_history"),
                    rs.getString("care_note"),
                    rs.getString("with_children"),
                    rs.getString("food_note"),
                    rs.getString("relation_note")
                ),
                userId
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private ContactPreferenceRow loadPreferenceRow(long userId) {
        try {
            return jdbcTemplate.queryForObject(
                """
                    SELECT profile_visibility, map_default_visibility, receive_operation_notice,
                           receive_security_notice, receive_bridge_contact, receive_local_contact,
                           receive_email_notice, receive_sms_notice
                    FROM contact_preferences
                    WHERE user_id = ?
                    """,
                (rs, rowNum) -> new ContactPreferenceRow(
                    VisibilityScope.fromDbValue(rs.getString("profile_visibility")),
                    VisibilityScope.fromDbValue(rs.getString("map_default_visibility")),
                    rs.getBoolean("receive_operation_notice"),
                    rs.getBoolean("receive_security_notice"),
                    rs.getBoolean("receive_bridge_contact"),
                    rs.getBoolean("receive_local_contact"),
                    rs.getBoolean("receive_email_notice"),
                    rs.getBoolean("receive_sms_notice")
                ),
                userId
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private boolean exists(String sql, long value) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, value);
        return count != null && count > 0;
    }

    private boolean hasRole(long userId, String roleName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role_name = ?",
            Integer.class,
            userId,
            roleName
        );
        return count != null && count > 0;
    }

    private boolean hasProfileContent(UserProfileRow row) {
        return row.bio() != null
            || row.ageRange() != null
            || row.region() != null
            || row.interestRegion() != null
            || row.visitHistory() != null
            || row.careNote() != null
            || row.withChildren() != null
            || row.foodNote() != null
            || row.relationNote() != null;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String nullable(String value) {
        String trimmed = value == null ? null : value.trim();
        return (trimmed == null || trimmed.isEmpty()) ? null : trimmed;
    }

    private String toDbVisibility(String value) {
        return VisibilityScope.fromDbValue(value).name().toLowerCase();
    }

    public record MypageSummary(
        String email,
        String nickname,
        String name,
        boolean smsVerified,
        String additionalVerificationStatus,
        String region,
        String interestRegion,
        boolean profileReady,
        boolean adminEntryAvailable,
        int currentPoints,
        boolean pointsEnabledForRegion,
        List<UserPointService.PointHistoryItem> pointHistory
    ) {
    }

    public record AccountPageData(
        String email,
        boolean smsVerified,
        String additionalVerificationStatus,
        AppAccountForm form
    ) {
    }

    public record PrivacySettingsPageData(
        boolean smsVerified,
        String additionalVerificationStatus,
        AppPrivacySettingsForm form
    ) {
    }

    private record UserRow(
        long id,
        String email,
        String nickname,
        String name,
        String nameKana,
        Date birthday,
        String gender,
        String phoneNumber,
        String address,
        boolean smsVerified,
        String additionalVerificationStatus
    ) {
    }

    private record UserProfileRow(
        String bio,
        String ageRange,
        String region,
        String interestRegion,
        String visitHistory,
        String careNote,
        String withChildren,
        String foodNote,
        String relationNote
    ) {
    }

    private record ContactPreferenceRow(
        VisibilityScope profileVisibility,
        VisibilityScope mapDefaultVisibility,
        boolean receiveOperationNotice,
        boolean receiveSecurityNotice,
        boolean receiveBridgeContact,
        boolean receiveLocalContact,
        boolean receiveEmailNotice,
        boolean receiveSmsNotice
    ) {
    }
}
