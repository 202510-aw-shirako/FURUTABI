package com.furutabi.app;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.furutabi.visibility.VisibilityScope;

@Service
public class UserPointService {

    private static final String REGION_DEFAULT = "default";

    private static final String EVENT_REGISTRATION = "registration";
    private static final String EVENT_MAP_RECORD_POST = "map_record_post";
    private static final String EVENT_VISIT_PARTICIPATION = "visit_participation";
    private static final String EVENT_REPEAT_PARTICIPATION = "repeat_participation";
    private static final String EVENT_HOST_ROLE = "host_role";
    private static final String EVENT_ENTRY_ROLE = "entry_role";
    private static final String EVENT_PARTNER_ROLE = "partner_role";
    private static final String EVENT_TOUR_PARTICIPATION = "tour_participation";
    private static final String EVENT_CAMPAIGN = "campaign";
    private static final String EVENT_MANUAL = "manual";

    private static final String SOURCE_AUTO = "auto";
    private static final String SOURCE_CAMPAIGN = "campaign";
    private static final String SOURCE_MANUAL = "manual";

    private static final String TARGET_MAP_RECORD = "map_record";
    private static final String TARGET_PROPOSAL_APPLICATION = "proposal_application";
    private static final String TARGET_PARTNER_ASSIGNMENT = "partner_assignment";

    private static final String SETTING_POINTS_ENABLED = "points_program_enabled";
    private static final String SETTING_SIGNUP_POINTS = "points_signup_points";
    private static final String SETTING_MAP_RECORD_POINTS = "points_map_record_points";
    private static final String SETTING_MAP_RECORD_MONTHLY_CAP = "points_map_record_monthly_cap";
    private static final String SETTING_VISIT_PARTICIPATION_POINTS = "points_visit_participation_points";
    private static final String SETTING_REPEAT_PARTICIPATION_POINTS = "points_repeat_participation_points";
    private static final String SETTING_HOST_POINTS = "points_host_points";
    private static final String SETTING_PARTNER_POINTS = "points_partner_points";
    private static final String SETTING_ENTRY_FIRST_POINTS = "points_entry_first_points";
    private static final String SETTING_ENTRY_REPEAT_POINTS = "points_entry_repeat_points";
    private static final String SETTING_TOUR_POINTS = "points_tour_points";

    private static final int DEFAULT_SIGNUP_POINTS = 20;
    private static final int DEFAULT_MAP_RECORD_POINTS = 3;
    private static final int DEFAULT_MAP_RECORD_MONTHLY_CAP = 5;
    private static final int DEFAULT_VISIT_PARTICIPATION_POINTS = 10;
    private static final int DEFAULT_REPEAT_PARTICIPATION_POINTS = 12;
    private static final int DEFAULT_HOST_POINTS = 10;
    private static final int DEFAULT_PARTNER_POINTS = 10;
    private static final int DEFAULT_ENTRY_FIRST_POINTS = 12;
    private static final int DEFAULT_ENTRY_REPEAT_POINTS = 8;
    private static final int DEFAULT_TOUR_POINTS = 15;

    private static final DateTimeFormatter HISTORY_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");

    private final JdbcTemplate jdbcTemplate;

    public UserPointService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserPointSummary loadUserPointSummary(long userId) {
        String regionId = resolveRegionId(userId);
        boolean enabled = isPointsEnabled(regionId);
        Integer total = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(points), 0) FROM point_events WHERE user_id = ?",
            Integer.class,
            userId
        );
        List<PointHistoryItem> recentHistory = jdbcTemplate.query(
            """
                SELECT event_type, source_mode, points, event_reason, region_id, awarded_at
                FROM point_events
                WHERE user_id = ?
                ORDER BY awarded_at DESC, id DESC
                LIMIT 5
                """,
            (rs, rowNum) -> new PointHistoryItem(
                historyLabel(rs.getString("event_type"), rs.getString("source_mode"), rs.getString("event_reason")),
                rs.getInt("points"),
                HISTORY_TIME_FORMAT.format(rs.getTimestamp("awarded_at").toInstant().atZone(TOKYO).toLocalDate()),
                rs.getString("region_id")
            ),
            userId
        );
        return new UserPointSummary(enabled, total == null ? 0 : total, recentHistory);
    }

    @Transactional
    public void awardRegistrationPoint(long userId) {
        String regionId = resolveRegionId(userId);
        if (!isPointsEnabled(regionId) || hasSingleEvent(userId, EVENT_REGISTRATION)) {
            return;
        }
        int points = loadIntSetting(regionId, SETTING_SIGNUP_POINTS, DEFAULT_SIGNUP_POINTS);
        if (points <= 0) {
            return;
        }
        insertPointEvent(
            userId,
            regionId,
            EVENT_REGISTRATION,
            SOURCE_AUTO,
            points,
            "登録ポイント",
            null,
            null,
            null,
            Timestamp.from(Instant.now())
        );
    }

    @Transactional
    public void awardMapRecordPoint(long userId, long mapRecordId, VisibilityScope visibilityScope, boolean draft) {
        if (draft || visibilityScope == VisibilityScope.PRIVATE) {
            return;
        }
        String regionId = resolveRegionId(userId);
        if (!isPointsEnabled(regionId) || hasTargetEvent(userId, EVENT_MAP_RECORD_POST, TARGET_MAP_RECORD, mapRecordId)) {
            return;
        }
        int monthlyCap = loadIntSetting(regionId, SETTING_MAP_RECORD_MONTHLY_CAP, DEFAULT_MAP_RECORD_MONTHLY_CAP);
        if (monthlyCap >= 0 && countMapRecordAwardsThisMonth(userId, regionId) >= monthlyCap) {
            return;
        }
        int points = loadIntSetting(regionId, SETTING_MAP_RECORD_POINTS, DEFAULT_MAP_RECORD_POINTS);
        if (points <= 0) {
            return;
        }
        insertPointEvent(
            userId,
            regionId,
            EVENT_MAP_RECORD_POST,
            SOURCE_AUTO,
            points,
            "私の地図投稿ポイント",
            TARGET_MAP_RECORD,
            mapRecordId,
            null,
            Timestamp.from(Instant.now())
        );
    }

    @Transactional
    public void awardAcceptedApplicationPoints(long applicationId) {
        AcceptedApplicationPointContext context = jdbcTemplate.query(
            """
                SELECT pa.id,
                       pa.applicant_user_id,
                       pa.application_status,
                       p.proposal_type,
                       p.host_user_id,
                       p.bridge_user_id
                FROM proposal_applications pa
                JOIN proposals p ON p.id = pa.proposal_id
                WHERE pa.id = ?
                  AND pa.deleted_at IS NULL
                  AND p.deleted_at IS NULL
                """,
            rs -> rs.next()
                ? new AcceptedApplicationPointContext(
                    rs.getLong("id"),
                    rs.getLong("applicant_user_id"),
                    rs.getString("application_status"),
                    rs.getString("proposal_type"),
                    rs.getLong("host_user_id"),
                    rs.getObject("bridge_user_id", Long.class)
                )
                : null,
            applicationId
        );
        if (context == null || !"accepted".equalsIgnoreCase(context.applicationStatus())) {
            return;
        }

        awardParticipationPoint(context);
        awardHostPoint(context);
        awardEntryPoint(context);
    }

    @Transactional
    public void awardPartnerAssignmentPoint(long assignmentId) {
        PartnerAssignmentPointContext context = jdbcTemplate.query(
            """
                SELECT id, region_id, partner_user_id, assignment_status
                FROM partner_assignments
                WHERE id = ?
                """,
            rs -> rs.next()
                ? new PartnerAssignmentPointContext(
                    rs.getLong("id"),
                    rs.getString("region_id"),
                    rs.getLong("partner_user_id"),
                    rs.getString("assignment_status")
                )
                : null,
            assignmentId
        );
        if (context == null || !"active".equalsIgnoreCase(context.assignmentStatus())) {
            return;
        }
        String regionId = normalizeRegion(context.regionId());
        if (!isPointsEnabled(regionId) || hasTargetEvent(context.partnerUserId(), EVENT_PARTNER_ROLE, TARGET_PARTNER_ASSIGNMENT, assignmentId)) {
            return;
        }
        int points = loadIntSetting(regionId, SETTING_PARTNER_POINTS, DEFAULT_PARTNER_POINTS);
        if (points <= 0) {
            return;
        }
        insertPointEvent(
            context.partnerUserId(),
            regionId,
            EVENT_PARTNER_ROLE,
            SOURCE_AUTO,
            points,
            "パートナー担当ポイント",
            TARGET_PARTNER_ASSIGNMENT,
            assignmentId,
            null,
            Timestamp.from(Instant.now())
        );
    }

    @Transactional
    public void grantCampaignPoints(long userId, String regionId, int points, String reason, Long actorUserId) {
        String normalizedRegionId = normalizeRegion(regionId);
        if (!isPointsEnabled(normalizedRegionId) || points <= 0) {
            return;
        }
        insertPointEvent(
            userId,
            normalizedRegionId,
            EVENT_CAMPAIGN,
            SOURCE_CAMPAIGN,
            points,
            normalizeReason(reason, "キャンペーン付与"),
            null,
            null,
            actorUserId,
            Timestamp.from(Instant.now())
        );
    }

    @Transactional
    public void grantManualPoints(long userId, String regionId, int points, String reason, Long actorUserId) {
        String normalizedRegionId = normalizeRegion(regionId);
        if (!isPointsEnabled(normalizedRegionId) || points <= 0) {
            return;
        }
        insertPointEvent(
            userId,
            normalizedRegionId,
            EVENT_MANUAL,
            SOURCE_MANUAL,
            points,
            normalizeReason(reason, "手動付与"),
            null,
            null,
            actorUserId,
            Timestamp.from(Instant.now())
        );
    }

    public boolean isPointsEnabledForRegion(String regionId) {
        return isPointsEnabled(regionId);
    }

    public String normalizeRegionId(String regionId) {
        return normalizeRegion(regionId);
    }

    public int loadDefaultAwareSetting(String regionId, String settingKey, int defaultValue) {
        return loadIntSetting(regionId, settingKey, defaultValue);
    }

    private void awardParticipationPoint(AcceptedApplicationPointContext context) {
        boolean revisit = hasPriorAcceptedVisit(context.applicantUserId(), context.applicationId());
        String regionId = resolveRegionId(context.applicantUserId());
        if (!isPointsEnabled(regionId)) {
            return;
        }
        String eventType = revisit ? EVENT_REPEAT_PARTICIPATION : EVENT_VISIT_PARTICIPATION;
        if (hasTargetEvent(context.applicantUserId(), eventType, TARGET_PROPOSAL_APPLICATION, context.applicationId())) {
            return;
        }
        int points = revisit
            ? loadIntSetting(regionId, SETTING_REPEAT_PARTICIPATION_POINTS, DEFAULT_REPEAT_PARTICIPATION_POINTS)
            : loadIntSetting(regionId, SETTING_VISIT_PARTICIPATION_POINTS, DEFAULT_VISIT_PARTICIPATION_POINTS);
        if (points <= 0) {
            return;
        }
        insertPointEvent(
            context.applicantUserId(),
            regionId,
            eventType,
            SOURCE_AUTO,
            points,
            revisit ? "再参加ポイント" : "参加ポイント",
            TARGET_PROPOSAL_APPLICATION,
            context.applicationId(),
            null,
            Timestamp.from(Instant.now())
        );
    }

    private void awardHostPoint(AcceptedApplicationPointContext context) {
        String regionId = resolveRegionId(context.hostUserId());
        if (!isPointsEnabled(regionId) || hasTargetEvent(context.hostUserId(), EVENT_HOST_ROLE, TARGET_PROPOSAL_APPLICATION, context.applicationId())) {
            return;
        }
        int points = loadIntSetting(regionId, SETTING_HOST_POINTS, DEFAULT_HOST_POINTS);
        if (points <= 0) {
            return;
        }
        insertPointEvent(
            context.hostUserId(),
            regionId,
            EVENT_HOST_ROLE,
            SOURCE_AUTO,
            points,
            "ホスト担当ポイント",
            TARGET_PROPOSAL_APPLICATION,
            context.applicationId(),
            null,
            Timestamp.from(Instant.now())
        );
    }

    private void awardEntryPoint(AcceptedApplicationPointContext context) {
        if (context.bridgeUserId() == null || "OKATTE".equalsIgnoreCase(context.proposalType())) {
            return;
        }
        String regionId = resolveRegionId(context.bridgeUserId());
        if (!isPointsEnabled(regionId) || hasTargetEvent(context.bridgeUserId(), EVENT_ENTRY_ROLE, TARGET_PROPOSAL_APPLICATION, context.applicationId())) {
            return;
        }
        boolean firstEntry = !hasSingleEvent(context.bridgeUserId(), EVENT_ENTRY_ROLE);
        int points = firstEntry
            ? loadIntSetting(regionId, SETTING_ENTRY_FIRST_POINTS, DEFAULT_ENTRY_FIRST_POINTS)
            : loadIntSetting(regionId, SETTING_ENTRY_REPEAT_POINTS, DEFAULT_ENTRY_REPEAT_POINTS);
        if (points <= 0) {
            return;
        }
        insertPointEvent(
            context.bridgeUserId(),
            regionId,
            EVENT_ENTRY_ROLE,
            SOURCE_AUTO,
            points,
            firstEntry ? "入り口担当ポイント（初回）" : "入り口担当ポイント",
            TARGET_PROPOSAL_APPLICATION,
            context.applicationId(),
            null,
            Timestamp.from(Instant.now())
        );
    }

    private String resolveRegionId(long userId) {
        return jdbcTemplate.query(
            """
                SELECT COALESCE(NULLIF(up.region, ''), NULLIF(up.interest_region, ''), ?) AS region_id
                FROM users u
                LEFT JOIN user_profiles up ON up.user_id = u.id
                WHERE u.id = ?
                """,
            rs -> rs.next() ? normalizeRegion(rs.getString("region_id")) : REGION_DEFAULT,
            REGION_DEFAULT,
            userId
        );
    }

    private boolean isPointsEnabled(String regionId) {
        String value = loadSettingValue(regionId, SETTING_POINTS_ENABLED);
        if (value == null || value.isBlank()) {
            return false;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "on".equalsIgnoreCase(value);
    }

    private int loadIntSetting(String regionId, String settingKey, int defaultValue) {
        String value = loadSettingValue(regionId, settingKey);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private String loadSettingValue(String regionId, String settingKey) {
        return jdbcTemplate.query(
            """
                SELECT setting_value
                FROM region_scoped_settings
                WHERE region_id IN (?, ?)
                  AND setting_key = ?
                ORDER BY CASE WHEN region_id = ? THEN 0 ELSE 1 END
                LIMIT 1
                """,
            rs -> rs.next() ? rs.getString("setting_value") : null,
            normalizeRegion(regionId),
            REGION_DEFAULT,
            settingKey,
            normalizeRegion(regionId)
        );
    }

    private boolean hasSingleEvent(long userId, String eventType) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM point_events WHERE user_id = ? AND event_type = ?",
            Integer.class,
            userId,
            eventType
        );
        return count != null && count > 0;
    }

    private boolean hasTargetEvent(long userId, String eventType, String targetType, long targetId) {
        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM point_events
                WHERE user_id = ?
                  AND event_type = ?
                  AND related_target_type = ?
                  AND related_target_id = ?
                """,
            Integer.class,
            userId,
            eventType,
            targetType,
            targetId
        );
        return count != null && count > 0;
    }

    private boolean hasPriorAcceptedVisit(long userId, long currentApplicationId) {
        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM proposal_applications pa
                JOIN proposals p ON p.id = pa.proposal_id
                WHERE pa.applicant_user_id = ?
                  AND pa.id <> ?
                  AND pa.deleted_at IS NULL
                  AND p.deleted_at IS NULL
                  AND LOWER(pa.application_status) = 'accepted'
                  AND p.proposal_type IN ('LOCAL_GUIDE', 'GATE', 'OKATTE')
                """,
            Integer.class,
            userId,
            currentApplicationId
        );
        return count != null && count > 0;
    }

    private int countMapRecordAwardsThisMonth(long userId, String regionId) {
        YearMonth currentMonth = YearMonth.now(TOKYO);
        Timestamp from = Timestamp.valueOf(currentMonth.atDay(1).atStartOfDay());
        Timestamp to = Timestamp.valueOf(currentMonth.plusMonths(1).atDay(1).atStartOfDay());
        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM point_events
                WHERE user_id = ?
                  AND region_id = ?
                  AND event_type = ?
                  AND awarded_at >= ?
                  AND awarded_at < ?
                """,
            Integer.class,
            userId,
            normalizeRegion(regionId),
            EVENT_MAP_RECORD_POST,
            from,
            to
        );
        return count == null ? 0 : count;
    }

    private void insertPointEvent(
        long userId,
        String regionId,
        String eventType,
        String sourceMode,
        int points,
        String eventReason,
        String relatedTargetType,
        Long relatedTargetId,
        Long awardedByUserId,
        Timestamp awardedAt
    ) {
        jdbcTemplate.update(
            """
                INSERT INTO point_events (
                    user_id, region_id, event_type, source_mode, points, event_reason,
                    related_target_type, related_target_id, awarded_by_user_id, awarded_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            userId,
            normalizeRegion(regionId),
            eventType,
            sourceMode,
            points,
            eventReason,
            relatedTargetType,
            relatedTargetId,
            awardedByUserId,
            awardedAt,
            awardedAt
        );
    }

    private String normalizeReason(String reason, String fallback) {
        if (reason == null || reason.isBlank()) {
            return fallback;
        }
        return reason.trim();
    }

    private String normalizeRegion(String regionId) {
        if (regionId == null || regionId.isBlank()) {
            return REGION_DEFAULT;
        }
        return regionId.trim();
    }

    private String historyLabel(String eventType, String sourceMode, String eventReason) {
        return switch (eventType) {
            case EVENT_REGISTRATION -> "登録";
            case EVENT_MAP_RECORD_POST -> "私の地図投稿";
            case EVENT_VISIT_PARTICIPATION -> "参加";
            case EVENT_REPEAT_PARTICIPATION -> "再参加";
            case EVENT_HOST_ROLE -> "ホスト";
            case EVENT_ENTRY_ROLE -> "入り口";
            case EVENT_PARTNER_ROLE -> "パートナー";
            case EVENT_TOUR_PARTICIPATION -> "ごひいきさんツアー";
            case EVENT_CAMPAIGN -> "キャンペーン";
            case EVENT_MANUAL -> "手動付与";
            default -> sourceMode + ": " + eventReason;
        };
    }

    public record UserPointSummary(
        boolean enabledForRegion,
        int currentPoints,
        List<PointHistoryItem> recentHistory
    ) {}

    public record PointHistoryItem(
        String label,
        int points,
        String awardedAt,
        String regionId
    ) {}

    private record AcceptedApplicationPointContext(
        long applicationId,
        long applicantUserId,
        String applicationStatus,
        String proposalType,
        long hostUserId,
        Long bridgeUserId
    ) {}

    private record PartnerAssignmentPointContext(
        long assignmentId,
        String regionId,
        long partnerUserId,
        String assignmentStatus
    ) {}
}
