package com.furutabi.app;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationCenterService {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final JdbcTemplate jdbcTemplate;

    public NotificationCenterService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public NotificationListPageData loadNotifications(String email) {
        long currentUserId = requireUserId(email);
        List<NotificationListItem> items = jdbcTemplate.query(
            """
                SELECT id, type, title, body, related_url, sender_name, sender_role,
                       preview_text, action_label, is_read, created_at
                FROM notifications
                WHERE user_id = ?
                  AND deleted_at IS NULL
                ORDER BY CASE WHEN is_read THEN 1 ELSE 0 END, created_at DESC, id DESC
                """,
            (rs, rowNum) -> new NotificationListItem(
                rs.getLong("id"),
                rs.getString("type"),
                kindLabel(rs.getString("type")),
                rs.getString("title"),
                rs.getString("body"),
                rs.getString("related_url"),
                rs.getString("sender_name"),
                rs.getString("sender_role"),
                rs.getString("preview_text"),
                rs.getString("action_label"),
                !rs.getBoolean("is_read"),
                formatTimestamp(rs.getTimestamp("created_at"))
            ),
            currentUserId
        );
        long unreadCount = items.stream().filter(NotificationListItem::unread).count();
        return new NotificationListPageData(items, unreadCount);
    }

    @Transactional
    public String openNotification(String email, long notificationId) {
        long currentUserId = requireUserId(email);
        NotificationRow row = requireNotification(currentUserId, notificationId);
        if (!row.read()) {
            Timestamp now = Timestamp.from(Instant.now());
            jdbcTemplate.update(
                """
                    UPDATE notifications
                    SET is_read = TRUE,
                        read_at = ?
                    WHERE id = ?
                      AND user_id = ?
                      AND deleted_at IS NULL
                    """,
                now,
                notificationId,
                currentUserId
            );
        }
        if (row.relatedUrl() == null || row.relatedUrl().isBlank()) {
            return "/app/notifications";
        }
        return row.relatedUrl();
    }

    public void notifyApplicationDecision(long applicationId, String nextStatus) {
        ApplicationDecisionNotificationRow row = requireApplicationDecisionRow(applicationId);
        boolean accepted = "accepted".equalsIgnoreCase(nextStatus);
        String title = accepted ? "申請が承認されました" : "申請は見合わせになりました";
        String body = accepted
            ? row.hostDisplayName() + " さんが申請を承認しました。連絡を確認できます。"
            : row.hostDisplayName() + " さんから見合わせの返答があります。";
        String relatedUrl = accepted && row.relatedThreadId() != null
            ? "/app/chat/" + row.relatedThreadId()
            : proposalDetailPath(row.proposalType(), row.proposalId());

        createNotification(
            row.applicantUserId(),
            accepted ? "application_accepted" : "application_postponed",
            title,
            body,
            "PROPOSAL_APPLICATION",
            applicationId,
            relatedUrl,
            row.hostDisplayName(),
            row.hostRole(),
            body,
            accepted ? "連絡を見る" : "提案を見る"
        );
        if (accepted && row.bridgeUserId() != null && row.bridgeUserId() != row.applicantUserId()) {
            createNotification(
                row.bridgeUserId(),
                "application_partner_accepted",
                "申請が承認されました",
                row.hostDisplayName() + " さんが申請を承認しました。提案先との状況を確認してください。",
                "PROPOSAL_APPLICATION",
                applicationId,
                "/app/bridge-applications/" + applicationId,
                row.hostDisplayName(),
                row.hostRole(),
                row.hostDisplayName() + " さんが承認しました。",
                "申請の状況を見る"
            );
        }
    }

    public void notifyNewChatMessage(long threadId, long senderUserId, String body) {
        ChatMessageNotificationRow row = requireChatNotificationRow(threadId, senderUserId);
        createNotification(
            row.recipientUserId(),
            "chat_message",
            "新しい連絡があります",
            row.senderDisplayName() + " さんから新しい message が届きました。",
            "CHAT_THREAD",
            threadId,
            "/app/chat/" + threadId,
            row.senderDisplayName(),
            row.senderRole(),
            abbreviate(body, 255),
            "連絡を見る"
        );
    }

    public void notifyMapComment(long mapRecordId, long commentAuthorUserId, String body) {
        MapCommentNotificationRow row = requireMapCommentNotificationRow(mapRecordId, commentAuthorUserId);
        if (row.ownerUserId() == commentAuthorUserId) {
            return;
        }
        createNotification(
            row.ownerUserId(),
            "map_comment",
            "地図に新しいコメントがあります",
            row.authorDisplayName() + " さんが「" + row.mapRecordTitle() + "」にコメントしました。",
            "MAP_RECORD",
            mapRecordId,
            "/app/map-records/" + mapRecordId,
            row.authorDisplayName(),
            row.authorRole(),
            abbreviate(body, 255),
            "地図を見る"
        );
    }

    public void notifyBridgePartnerAttentionRequested(long applicationId, long senderUserId, String relatedUrl) {
        BridgePartnerAttentionRequestRow row = requireBridgePartnerAttentionRequestRow(applicationId, senderUserId);
        if (row == null || row.bridgeUserId() == null || row.bridgeUserId() == senderUserId) {
            return;
        }
        String sender = displayName(row.senderNickname(), row.senderName(), row.senderEmail());
        createNotification(
            row.bridgeUserId(),
            "partner_attention_requested",
            "架け橋さんに入ってほしい連絡があります",
            sender + " さんから、今回の関わりに入ってほしい連絡が届いています。",
            "PROPOSAL_APPLICATION",
            applicationId,
            relatedUrl,
            sender,
            row.senderRole(),
            row.targetUserLabel() + " さんとの関わりです。",
            "連携チャットを見る"
        );
    }

    public void notifyBridgePartnerProjectClosed(long applicationId) {
        BridgePartnerClosureRow row = requireBridgePartnerClosureRow(applicationId);
        if (row == null || row.bridgeUserId() == null) {
            return;
        }
        String host = displayName(row.hostNickname(), row.hostName(), row.hostEmail());
        String title = "終了後の支援メモを書いてください";
        String body = row.targetUserLabel() + " さんとの関わりが一段落しました。引き継ぎのための支援メモを残してください。";
        createNotification(
            row.bridgeUserId(),
            "support_note_follow_up",
            title,
            body,
            "PROPOSAL_APPLICATION",
            applicationId,
            "/app/support-notes/users/" + row.targetUserId() + "/new?relatedCardId=" + applicationId,
            host,
            row.hostRole(),
            body,
            "支援メモを書く"
        );
    }

    private void createNotification(
        long recipientUserId,
        String type,
        String title,
        String body,
        String relatedEntityType,
        long relatedEntityId,
        String relatedUrl,
        String senderName,
        String senderRole,
        String previewText,
        String actionLabel
    ) {
        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.update(
            """
                INSERT INTO notifications (
                    user_id, type, title, body, related_entity_type, related_entity_id,
                    related_url, sender_name, sender_role, preview_text, severity,
                    action_label, is_read, created_at, read_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            recipientUserId,
            type,
            limit(title, 200),
            limit(body, 500),
            relatedEntityType,
            relatedEntityId,
            limit(relatedUrl, 255),
            limit(senderName, 100),
            limit(senderRole, 30),
            limit(previewText, 255),
            "normal",
            limit(actionLabel, 50),
            false,
            now,
            null,
            null
        );
    }

    private long requireUserId(String email) {
        try {
            return Objects.requireNonNull(
                jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email),
                "Notification user ID is required"
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("User not found for notifications: " + email, ex);
        }
    }

    private NotificationRow requireNotification(long currentUserId, long notificationId) {
        try {
            return Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                    """
                        SELECT id, related_url, is_read
                        FROM notifications
                        WHERE id = ?
                          AND user_id = ?
                          AND deleted_at IS NULL
                        """,
                    (rs, rowNum) -> new NotificationRow(
                        rs.getLong("id"),
                        rs.getString("related_url"),
                        rs.getBoolean("is_read")
                    ),
                    notificationId,
                    currentUserId
                ),
                "Notification row is required"
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Notification not found: " + notificationId, ex);
        }
    }

    private ApplicationDecisionNotificationRow requireApplicationDecisionRow(long applicationId) {
        try {
            return Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                    """
                        SELECT pa.id,
                               pa.applicant_user_id,
                               pa.related_thread_id,
                               p.bridge_user_id,
                               p.id AS proposal_id,
                               p.proposal_type,
                               host.email AS host_email,
                               host.nickname AS host_nickname,
                               host.name AS host_name,
                               COALESCE((
                                   SELECT ur.role_name
                                   FROM user_roles ur
                                   WHERE ur.user_id = p.host_user_id
                                   ORDER BY CASE ur.role_name
                                       WHEN 'ADMIN' THEN 1
                                       WHEN 'BRIDGE' THEN 2
                                       WHEN 'LOCAL' THEN 3
                                       WHEN 'USER' THEN 4
                                       ELSE 5
                                   END
                                   FETCH FIRST 1 ROWS ONLY
                               ), 'USER') AS host_role
                        FROM proposal_applications pa
                        JOIN proposals p ON p.id = pa.proposal_id
                        JOIN users host ON host.id = p.host_user_id
                        WHERE pa.id = ?
                          AND pa.deleted_at IS NULL
                          AND p.deleted_at IS NULL
                        """,
                    (rs, rowNum) -> new ApplicationDecisionNotificationRow(
                        rs.getLong("applicant_user_id"),
                        rs.getObject("bridge_user_id", Long.class),
                        rs.getLong("proposal_id"),
                        rs.getString("proposal_type"),
                        rs.getObject("related_thread_id", Long.class),
                        displayName(rs.getString("host_nickname"), rs.getString("host_name"), rs.getString("host_email")),
                        rs.getString("host_role")
                    ),
                    applicationId
                ),
                "Application decision notification row is required"
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Application not found for notifications: " + applicationId, ex);
        }
    }

    private ChatMessageNotificationRow requireChatNotificationRow(long threadId, long senderUserId) {
        try {
            return Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                    """
                        SELECT ct.user_id,
                               ct.counterpart_id,
                               sender.email AS sender_email,
                               sender.nickname AS sender_nickname,
                               sender.name AS sender_name,
                               COALESCE((
                                   SELECT ur.role_name
                                   FROM user_roles ur
                                   WHERE ur.user_id = ?
                                   ORDER BY CASE ur.role_name
                                       WHEN 'ADMIN' THEN 1
                                       WHEN 'BRIDGE' THEN 2
                                       WHEN 'LOCAL' THEN 3
                                       WHEN 'USER' THEN 4
                                       ELSE 5
                                   END
                                   FETCH FIRST 1 ROWS ONLY
                               ), 'USER') AS sender_role
                        FROM chat_threads ct
                        JOIN users sender ON sender.id = ?
                        WHERE ct.id = ?
                          AND ct.deleted_at IS NULL
                          AND (ct.user_id = ? OR ct.counterpart_id = ?)
                        """,
                    (rs, rowNum) -> {
                        long userId = rs.getLong("user_id");
                        long counterpartId = rs.getLong("counterpart_id");
                        long recipientUserId = userId == senderUserId ? counterpartId : userId;
                        return new ChatMessageNotificationRow(
                            recipientUserId,
                            displayName(rs.getString("sender_nickname"), rs.getString("sender_name"), rs.getString("sender_email")),
                            rs.getString("sender_role")
                        );
                    },
                    senderUserId,
                    senderUserId,
                    threadId,
                    senderUserId,
                    senderUserId
                ),
                "Chat message notification row is required"
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Chat thread not found for notifications: " + threadId, ex);
        }
    }

    private MapCommentNotificationRow requireMapCommentNotificationRow(long mapRecordId, long commentAuthorUserId) {
        try {
            return Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                    """
                        SELECT mr.user_id,
                               mr.title,
                               author.email AS author_email,
                               author.nickname AS author_nickname,
                               author.name AS author_name,
                               COALESCE((
                                   SELECT ur.role_name
                                   FROM user_roles ur
                                   WHERE ur.user_id = ?
                                   ORDER BY CASE ur.role_name
                                       WHEN 'ADMIN' THEN 1
                                       WHEN 'BRIDGE' THEN 2
                                       WHEN 'LOCAL' THEN 3
                                       WHEN 'USER' THEN 4
                                       ELSE 5
                                   END
                                   FETCH FIRST 1 ROWS ONLY
                               ), 'USER') AS author_role
                        FROM map_records mr
                        JOIN users author ON author.id = ?
                        WHERE mr.id = ?
                          AND mr.deleted_at IS NULL
                          AND mr.is_draft = FALSE
                        """,
                    (rs, rowNum) -> new MapCommentNotificationRow(
                        rs.getLong("user_id"),
                        rs.getString("title"),
                        displayName(rs.getString("author_nickname"), rs.getString("author_name"), rs.getString("author_email")),
                        rs.getString("author_role")
                    ),
                    commentAuthorUserId,
                    commentAuthorUserId,
                    mapRecordId
                ),
                "Map comment notification row is required"
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Map record not found for comment notifications: " + mapRecordId, ex);
        }
    }

    private BridgePartnerAttentionRequestRow requireBridgePartnerAttentionRequestRow(long applicationId, long senderUserId) {
        try {
            return jdbcTemplate.queryForObject(
                """
                    SELECT pa.id AS application_id,
                           p.bridge_user_id,
                           target.email AS target_email,
                           target.nickname AS target_nickname,
                           target.name AS target_name,
                           sender.email AS sender_email,
                           sender.nickname AS sender_nickname,
                           sender.name AS sender_name,
                           COALESCE((
                               SELECT ur.role_name
                               FROM user_roles ur
                               WHERE ur.user_id = ?
                               ORDER BY CASE ur.role_name
                                   WHEN 'ADMIN' THEN 1
                                   WHEN 'BRIDGE' THEN 2
                                   WHEN 'LOCAL' THEN 3
                                   WHEN 'USER' THEN 4
                                   ELSE 5
                               END
                               FETCH FIRST 1 ROWS ONLY
                           ), 'USER') AS sender_role
                    FROM proposal_applications pa
                    JOIN proposals p ON p.id = pa.proposal_id
                    JOIN users target ON target.id = pa.applicant_user_id
                    JOIN users sender ON sender.id = ?
                    WHERE pa.id = ?
                      AND pa.deleted_at IS NULL
                      AND p.deleted_at IS NULL
                      AND LOWER(pa.application_status) = 'accepted'
                    """,
                (rs, rowNum) -> new BridgePartnerAttentionRequestRow(
                    rs.getLong("application_id"),
                    rs.getObject("bridge_user_id", Long.class),
                    displayName(rs.getString("target_nickname"), rs.getString("target_name"), rs.getString("target_email")),
                    rs.getString("sender_email"),
                    rs.getString("sender_nickname"),
                    rs.getString("sender_name"),
                    rs.getString("sender_role")
                ),
                senderUserId,
                senderUserId,
                applicationId
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private BridgePartnerClosureRow requireBridgePartnerClosureRow(long applicationId) {
        try {
            return jdbcTemplate.queryForObject(
                """
                    SELECT pa.id AS application_id,
                           pa.applicant_user_id,
                           applicant.email AS applicant_email,
                           applicant.nickname AS applicant_nickname,
                           applicant.name AS applicant_name,
                           p.bridge_user_id,
                           host.email AS host_email,
                           host.nickname AS host_nickname,
                           host.name AS host_name,
                           COALESCE((
                               SELECT ur.role_name
                               FROM user_roles ur
                               WHERE ur.user_id = p.host_user_id
                               ORDER BY CASE ur.role_name
                                   WHEN 'ADMIN' THEN 1
                                   WHEN 'BRIDGE' THEN 2
                                   WHEN 'LOCAL' THEN 3
                                   WHEN 'USER' THEN 4
                                   ELSE 5
                               END
                               FETCH FIRST 1 ROWS ONLY
                           ), 'USER') AS host_role
                    FROM proposal_applications pa
                    JOIN proposals p ON p.id = pa.proposal_id
                    JOIN users applicant ON applicant.id = pa.applicant_user_id
                    JOIN users host ON host.id = p.host_user_id
                    WHERE pa.id = ?
                      AND pa.deleted_at IS NULL
                      AND p.deleted_at IS NULL
                      AND LOWER(pa.application_status) = 'accepted'
                    """,
                (rs, rowNum) -> new BridgePartnerClosureRow(
                    rs.getLong("application_id"),
                    rs.getLong("applicant_user_id"),
                    displayName(rs.getString("applicant_nickname"), rs.getString("applicant_name"), rs.getString("applicant_email")),
                    rs.getObject("bridge_user_id", Long.class),
                    rs.getString("host_email"),
                    rs.getString("host_nickname"),
                    rs.getString("host_name"),
                    rs.getString("host_role")
                ),
                applicationId
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private String kindLabel(String type) {
        return switch (type) {
            case "application_accepted", "application_postponed", "application_partner_accepted" -> "申請";
            case "chat_message" -> "連絡";
            case "map_comment" -> "コメント";
            case "partner_attention_requested" -> "連携";
            case "support_note_follow_up" -> "支援メモ";
            default -> "通知";
        };
    }

    private String proposalDetailPath(String proposalType, long proposalId) {
        if ("OKATTE".equalsIgnoreCase(proposalType)) {
            return "/app/okatte/" + proposalId;
        }
        return "/app/gate/" + proposalId;
    }

    private String displayName(String nickname, String name, String fallback) {
        if (nickname != null && !nickname.isBlank()) {
            return nickname;
        }
        if (name != null && !name.isBlank()) {
            return name;
        }
        return fallback;
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime().format(DATE_TIME_FORMAT);
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
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

    public record NotificationListPageData(List<NotificationListItem> items, long unreadCount) {
    }

    public record NotificationListItem(
        long notificationId,
        String type,
        String kindLabel,
        String title,
        String body,
        String relatedUrl,
        String senderName,
        String senderRole,
        String previewText,
        String actionLabel,
        boolean unread,
        String createdAt
    ) {
    }

    private record NotificationRow(long notificationId, String relatedUrl, boolean read) {
    }

    private record ApplicationDecisionNotificationRow(
        long applicantUserId,
        Long bridgeUserId,
        long proposalId,
        String proposalType,
        Long relatedThreadId,
        String hostDisplayName,
        String hostRole
    ) {
    }

    private record ChatMessageNotificationRow(long recipientUserId, String senderDisplayName, String senderRole) {
    }

    private record MapCommentNotificationRow(
        long ownerUserId,
        String mapRecordTitle,
        String authorDisplayName,
        String authorRole
    ) {
    }

    private record BridgePartnerAttentionRequestRow(
        long applicationId,
        Long bridgeUserId,
        String targetUserLabel,
        String senderEmail,
        String senderNickname,
        String senderName,
        String senderRole
    ) {
    }

    private record BridgePartnerClosureRow(
        long applicationId,
        long targetUserId,
        String targetUserLabel,
        Long bridgeUserId,
        String hostEmail,
        String hostNickname,
        String hostName,
        String hostRole
    ) {
    }
}
