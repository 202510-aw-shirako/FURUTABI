package com.furutabi.app;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupportRequestService {

    private static final DateTimeFormatter PAGE_TIME = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final JdbcTemplate jdbcTemplate;
    private final NotificationCenterService notificationCenterService;

    public SupportRequestService(JdbcTemplate jdbcTemplate, NotificationCenterService notificationCenterService) {
        this.jdbcTemplate = jdbcTemplate;
        this.notificationCenterService = notificationCenterService;
    }

    public SupportListPageData loadOwnRequests(String email) {
        UserRow currentUser = requireUser(email);
        List<SupportRequestListItem> items = jdbcTemplate.query(
            """
                SELECT sr.id, sr.request_type, sr.related_feature, sr.status, sr.created_at, sr.updated_at
                FROM support_requests sr
                WHERE sr.user_id = ?
                  AND sr.deleted_at IS NULL
                ORDER BY sr.created_at DESC, sr.id DESC
                """,
            (rs, rowNum) -> new SupportRequestListItem(
                rs.getLong("id"),
                labelForType(rs.getString("request_type")),
                rs.getString("related_feature"),
                labelForStatus(rs.getString("status")),
                formatTimestamp(rs.getTimestamp("created_at")),
                "/app/support/" + rs.getLong("id")
            ),
            currentUser.id()
        );
        return new SupportListPageData(displayName(currentUser), items, false);
    }

    public SupportCreatePageData loadCreatePage(String email) {
        UserRow currentUser = requireUser(email);
        return new SupportCreatePageData(displayName(currentUser));
    }

    public SupportListPageData loadAdminQueue(String email) {
        UserRow currentUser = requireUser(email);
        requireAdmin(currentUser.id());
        List<SupportRequestListItem> items = jdbcTemplate.query(
            """
                SELECT sr.id, sr.request_type, sr.related_feature, sr.status, sr.created_at,
                       requester.nickname AS requester_nickname,
                       requester.name AS requester_name,
                       requester.email AS requester_email
                FROM support_requests sr
                LEFT JOIN users requester ON requester.id = sr.user_id
                WHERE sr.deleted_at IS NULL
                ORDER BY CASE sr.status
                    WHEN 'received' THEN 0
                    WHEN 'open' THEN 1
                    WHEN 'handled' THEN 2
                    WHEN 'closed' THEN 3
                    ELSE 4
                END, sr.created_at DESC, sr.id DESC
                """,
            (rs, rowNum) -> new SupportRequestListItem(
                rs.getLong("id"),
                labelForType(rs.getString("request_type")),
                adminSummary(rs.getString("related_feature"), rs.getString("requester_nickname"), rs.getString("requester_name"), rs.getString("requester_email")),
                labelForStatus(rs.getString("status")),
                formatTimestamp(rs.getTimestamp("created_at")),
                "/app/support/" + rs.getLong("id")
            )
        );
        return new SupportListPageData(displayName(currentUser), items, true);
    }

    public SupportRequestDetailPageData loadDetail(String email, long supportRequestId) {
        UserRow currentUser = requireUser(email);
        boolean admin = isAdmin(currentUser.id());
        try {
            return Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                    """
                        SELECT sr.id, sr.request_type, sr.related_feature, sr.target_reference, sr.body,
                               sr.reply_preference, sr.status, sr.created_at, sr.updated_at,
                               sr.user_id, sr.handled_by_user_id,
                               requester.nickname AS requester_nickname,
                               requester.name AS requester_name,
                               requester.email AS requester_email,
                               handler.nickname AS handler_nickname,
                               handler.name AS handler_name,
                               handler.email AS handler_email
                        FROM support_requests sr
                        LEFT JOIN users requester ON requester.id = sr.user_id
                        LEFT JOIN users handler ON handler.id = sr.handled_by_user_id
                        WHERE sr.id = ?
                          AND sr.deleted_at IS NULL
                        """,
                    (rs, rowNum) -> {
                        Long requesterUserId = rs.getObject("user_id", Long.class);
                        if (!admin && (requesterUserId == null || requesterUserId.longValue() != currentUser.id())) {
                            throw new IllegalStateException("Support request not visible: " + supportRequestId);
                        }
                        return new SupportRequestDetailPageData(
                            rs.getLong("id"),
                            labelForType(rs.getString("request_type")),
                            rs.getString("related_feature"),
                            rs.getString("target_reference"),
                            rs.getString("body"),
                            labelForReplyPreference(rs.getString("reply_preference")),
                            labelForStatus(rs.getString("status")),
                            formatTimestamp(rs.getTimestamp("created_at")),
                            formatTimestamp(rs.getTimestamp("updated_at")),
                            displayName(
                                rs.getString("requester_nickname"),
                                rs.getString("requester_name"),
                                rs.getString("requester_email")
                            ),
                            displayName(
                                rs.getString("handler_nickname"),
                                rs.getString("handler_name"),
                                rs.getString("handler_email")
                            ),
                            admin ? buildAdminChatReviewEntryUrl(rs.getLong("id"), rs.getString("target_reference")) : null,
                            admin,
                            admin && canMoveToHandled(rs.getString("status")),
                            admin && canMoveToClosed(rs.getString("status"))
                        );
                    },
                    supportRequestId
                ),
                "Support request detail is required"
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Support request not found: " + supportRequestId, ex);
        }
    }

    @Transactional
    public long createRequest(String email, SupportRequestForm form) {
        UserRow currentUser = requireUser(email);
        String requestType = normalizeOption(form == null ? null : form.getRequestType(), "general");
        String relatedFeature = normalizeOptional(form == null ? null : form.getRelatedFeature(), 50);
        String targetReference = normalizeOptional(form == null ? null : form.getTargetReference(), 255);
        String body = normalizeBody(form == null ? null : form.getBody());
        String replyPreference = normalizeOption(form == null ? null : form.getReplyPreference(), "optional");
        if (body == null) {
            throw new SupportRequestConflictException("Support body is required");
        }

        Timestamp now = Timestamp.from(Instant.now());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                """
                    INSERT INTO support_requests (
                        user_id, request_type, related_feature, target_reference, body,
                        reply_preference, status, handled_by_user_id, created_at, updated_at, deleted_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                new String[] {"id"}
            );
            statement.setLong(1, currentUser.id());
            statement.setString(2, limit(requestType, 30));
            statement.setString(3, relatedFeature);
            statement.setString(4, targetReference);
            statement.setString(5, body);
            statement.setString(6, limit(replyPreference, 20));
            statement.setString(7, "received");
            statement.setObject(8, null);
            statement.setTimestamp(9, now);
            statement.setTimestamp(10, now);
            statement.setObject(11, null);
            return statement;
        }, keyHolder);
        Long supportRequestId = keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
        if (supportRequestId == null) {
            throw new IllegalStateException("Failed to create support request");
        }
        appendStatusHistory(supportRequestId, "received", "Support request created", currentUser.id(), now);
        return supportRequestId;
    }

    @Transactional
    public void markHandled(String email, long supportRequestId) {
        transitionStatus(email, supportRequestId, "handled", "Marked as handled");
    }

    @Transactional
    public void closeRequest(String email, long supportRequestId) {
        transitionStatus(email, supportRequestId, "closed", "Closed by operator");
    }

    private void transitionStatus(String email, long supportRequestId, String nextStatus, String note) {
        UserRow currentUser = requireUser(email);
        requireAdmin(currentUser.id());
        String currentStatus = requireCurrentStatus(supportRequestId);
        if (nextStatus.equalsIgnoreCase(currentStatus)) {
            return;
        }
        Timestamp now = Timestamp.from(Instant.now());
        int updated = jdbcTemplate.update(
            """
                UPDATE support_requests
                SET status = ?, handled_by_user_id = ?, updated_at = ?
                WHERE id = ?
                  AND deleted_at IS NULL
                """,
            nextStatus,
            currentUser.id(),
            now,
            supportRequestId
        );
        if (updated == 0) {
            throw new IllegalStateException("Support request not found: " + supportRequestId);
        }
        appendStatusHistory(supportRequestId, nextStatus, note, currentUser.id(), now);
    }

    private void appendStatusHistory(long supportRequestId, String status, String note, Long changedByUserId, Timestamp createdAt) {
        jdbcTemplate.update(
            """
                INSERT INTO support_request_status_history (
                    support_request_id, status, note, changed_by_user_id, created_at
                ) VALUES (?, ?, ?, ?, ?)
                """,
            supportRequestId,
            limit(status, 30),
            limit(note, 500),
            changedByUserId,
            createdAt
        );
    }

    private String requireCurrentStatus(long supportRequestId) {
        try {
            return Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                    "SELECT status FROM support_requests WHERE id = ? AND deleted_at IS NULL",
                    String.class,
                    supportRequestId
                ),
                "Support request status is required"
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Support request not found: " + supportRequestId, ex);
        }
    }

    private UserRow requireUser(String email) {
        try {
            return Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                    """
                        SELECT id, email, nickname, name
                        FROM users
                        WHERE email = ?
                        """,
                    (rs, rowNum) -> new UserRow(
                        rs.getLong("id"),
                        rs.getString("email"),
                        rs.getString("nickname"),
                        rs.getString("name")
                    ),
                    email
                ),
                "Support user is required"
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("User not found for support: " + email, ex);
        }
    }

    private void requireAdmin(long userId) {
        if (!isAdmin(userId)) {
            throw new IllegalStateException("Support admin access is required");
        }
    }

    private boolean isAdmin(long userId) {
        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM user_roles
                WHERE user_id = ? AND role_name = 'ADMIN'
                """,
            Integer.class,
            userId
        );
        return count != null && count > 0;
    }

    private boolean canMoveToHandled(String status) {
        return status != null && !"handled".equalsIgnoreCase(status) && !"closed".equalsIgnoreCase(status);
    }

    private boolean canMoveToClosed(String status) {
        return status != null && !"closed".equalsIgnoreCase(status);
    }

    private String normalizeBody(String body) {
        if (body == null) {
            return null;
        }
        String normalized = body.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return limit(normalized, 4000);
    }

    private String normalizeOptional(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return limit(normalized, maxLength);
    }

    private String normalizeOption(String value, String fallback) {
        String normalized = normalizeOptional(value, 30);
        return normalized == null ? fallback : normalized.toLowerCase();
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String displayName(UserRow user) {
        return displayName(user.nickname(), user.name(), user.email());
    }

    private String displayName(String nickname, String name, String email) {
        if (nickname != null && !nickname.isBlank()) {
            return nickname;
        }
        if (name != null && !name.isBlank()) {
            return name;
        }
        return email == null ? "unknown" : email;
    }

    private String adminSummary(String relatedFeature, String requesterNickname, String requesterName, String requesterEmail) {
        String requester = displayName(requesterNickname, requesterName, requesterEmail);
        if (relatedFeature == null || relatedFeature.isBlank()) {
            return requester;
        }
        return relatedFeature + " / " + requester;
    }

    private String buildAdminChatReviewEntryUrl(long supportRequestId, String targetReference) {
        Long chatThreadId = extractChatThreadId(targetReference);
        if (chatThreadId == null) {
            return null;
        }
        return "/app/admin/chat-threads/" + chatThreadId + "/review?supportRequestId=" + supportRequestId;
    }

    private Long extractChatThreadId(String targetReference) {
        if (targetReference == null) {
            return null;
        }
        String normalized = targetReference.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        String prefix = "/app/chat/";
        if (!normalized.startsWith(prefix)) {
            return null;
        }
        String suffix = normalized.substring(prefix.length());
        int nextSlash = suffix.indexOf('/');
        int query = suffix.indexOf('?');
        int cutIndex = suffix.length();
        if (nextSlash >= 0) {
            cutIndex = Math.min(cutIndex, nextSlash);
        }
        if (query >= 0) {
            cutIndex = Math.min(cutIndex, query);
        }
        String idToken = suffix.substring(0, cutIndex).trim();
        if (idToken.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(idToken);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String labelForType(String requestType) {
        if (requestType == null) {
            return "相談";
        }
        return switch (requestType.toLowerCase()) {
            case "account" -> "アカウント相談";
            case "map" -> "地図の相談";
            case "chat" -> "連絡の相談";
            case "proposal" -> "提案の相談";
            default -> "サポート";
        };
    }

    private String labelForStatus(String status) {
        if (status == null) {
            return "受付";
        }
        return switch (status.toLowerCase()) {
            case "open", "received" -> "受付";
            case "handled" -> "対応中";
            case "closed" -> "完了";
            default -> status;
        };
    }

    private String labelForReplyPreference(String replyPreference) {
        if (replyPreference == null) {
            return "任意";
        }
        return switch (replyPreference.toLowerCase()) {
            case "required" -> "返信希望";
            case "none" -> "返信不要";
            default -> "任意";
        };
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime().format(PAGE_TIME);
    }

    public record SupportListPageData(String currentUserLabel, List<SupportRequestListItem> items, boolean adminView) {
    }

    public record SupportRequestListItem(
        long supportRequestId,
        String typeLabel,
        String summary,
        String statusLabel,
        String createdAt,
        String detailUrl
    ) {
    }

    public record SupportCreatePageData(String currentUserLabel) {
    }

    public record SupportRequestDetailPageData(
        long supportRequestId,
        String typeLabel,
        String relatedFeature,
        String targetReference,
        String body,
        String replyPreferenceLabel,
        String statusLabel,
        String createdAt,
        String updatedAt,
        String requesterLabel,
        String handlerLabel,
        String adminChatReviewEntryUrl,
        boolean adminView,
        boolean canMarkHandled,
        boolean canClose
    ) {
    }

    private record UserRow(long id, String email, String nickname, String name) {
    }

    public static class SupportRequestConflictException extends RuntimeException {
        public SupportRequestConflictException(String message) {
            super(message);
        }
    }
}
