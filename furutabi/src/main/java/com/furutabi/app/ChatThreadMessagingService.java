package com.furutabi.app;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatThreadMessagingService {

    private final JdbcTemplate jdbcTemplate;
    private final NotificationCenterService notificationCenterService;
    private final ApplicationChatThreadService applicationChatThreadService;

    public ChatThreadMessagingService(
        JdbcTemplate jdbcTemplate,
        NotificationCenterService notificationCenterService,
        ApplicationChatThreadService applicationChatThreadService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.notificationCenterService = notificationCenterService;
        this.applicationChatThreadService = applicationChatThreadService;
    }

    public ChatThreadListPageData loadThreadList(String currentUserEmail) {
        long currentUserId = requireUserId(currentUserEmail);
        List<ChatThreadListItem> threads = jdbcTemplate.query(
            """
                SELECT ct.id,
                       ct.title,
                       ct.status,
                       ct.related_url,
                       ct.latest_message_preview,
                       ct.latest_message_at,
                       ct.updated_at,
                       other.nickname AS counterpart_nickname,
                       ct.counterpart_role
                FROM chat_threads ct
                JOIN users other
                  ON other.id = CASE
                      WHEN ct.user_id = ? THEN ct.counterpart_id
                      ELSE ct.user_id
                  END
                WHERE ct.deleted_at IS NULL
                  AND (ct.user_id = ? OR ct.counterpart_id = ?)
                ORDER BY COALESCE(ct.latest_message_at, ct.updated_at) DESC, ct.id DESC
                """,
            (rs, rowNum) -> new ChatThreadListItem(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("counterpart_nickname"),
                rs.getString("counterpart_role"),
                rs.getString("status"),
                rs.getString("latest_message_preview"),
                rs.getTimestamp("latest_message_at"),
                rs.getString("related_url")
            ),
            currentUserId,
            currentUserId,
            currentUserId
        );
        return new ChatThreadListPageData(threads);
    }

    public ChatThreadDetailPageData loadThreadDetail(String currentUserEmail, long threadId) {
        long currentUserId = requireUserId(currentUserEmail);
        ThreadAccessRow thread = requireAccessibleThread(currentUserId, threadId);
        PartnerAssistAction partnerAssistAction = loadPartnerAssistAction(currentUserId, thread);
        List<ChatMessageItem> messages = jdbcTemplate.query(
            """
                SELECT cm.id,
                       cm.sender_id,
                       cm.sender_role,
                       cm.body,
                       cm.is_system_message,
                       cm.created_at,
                       sender.nickname AS sender_nickname
                FROM chat_messages cm
                LEFT JOIN users sender ON sender.id = cm.sender_id
                WHERE cm.thread_id = ?
                  AND cm.deleted_at IS NULL
                ORDER BY cm.created_at ASC, cm.id ASC
                """,
            (rs, rowNum) -> new ChatMessageItem(
                rs.getLong("id"),
                rs.getLong("sender_id"),
                rs.getString("sender_role"),
                rs.getString("sender_nickname"),
                rs.getString("body"),
                rs.getBoolean("is_system_message"),
                rs.getTimestamp("created_at"),
                rs.getLong("sender_id") == currentUserId
            ),
            threadId
        );
        return new ChatThreadDetailPageData(
            thread.threadId(),
            thread.title(),
            thread.status(),
            thread.relatedUrl(),
            thread.counterpartNickname(),
            thread.counterpartRole(),
            "open".equalsIgnoreCase(thread.status()),
            partnerAssistAction != null,
            partnerAssistAction == null ? null : partnerAssistAction.label(),
            messages
        );
    }

    @Transactional
    public void notifyPartnerAttention(String currentUserEmail, long threadId) {
        long currentUserId = requireUserId(currentUserEmail);
        ThreadAccessRow thread = requireAccessibleThread(currentUserId, threadId);
        PartnerAssistAction partnerAssistAction = loadPartnerAssistAction(currentUserId, thread);
        if (partnerAssistAction == null) {
            throw new ChatThreadMessagingConflictException("Partner assist is not available");
        }
        notificationCenterService.notifyBridgePartnerAttentionRequested(
            partnerAssistAction.applicationId(),
            currentUserId,
            partnerAssistAction.relatedUrl()
        );
    }

    @Transactional
    public void sendMessage(String currentUserEmail, long threadId, ChatMessageForm chatMessageForm) {
        long currentUserId = requireUserId(currentUserEmail);
        ThreadAccessRow thread = requireAccessibleThread(currentUserId, threadId);
        if (!"open".equalsIgnoreCase(thread.status())) {
            throw new ChatThreadMessagingConflictException("Closed thread cannot receive messages");
        }

        String body = normalizeBody(chatMessageForm == null ? null : chatMessageForm.getBody());
        if (body == null) {
            throw new ChatThreadMessagingConflictException("Message body is required");
        }

        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbcTemplate.update(
            """
                INSERT INTO chat_messages (
                    thread_id, sender_id, sender_role, body, is_system_message, read_at, created_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
            threadId,
            currentUserId,
            requirePrimaryRole(currentUserId),
            body,
            false,
            null,
            now,
            null
        );
        jdbcTemplate.update(
            """
                UPDATE chat_threads
                SET latest_message_preview = ?, latest_message_at = ?, updated_at = ?
                WHERE id = ? AND deleted_at IS NULL
                """,
            abbreviate(body),
            now,
            now,
            threadId
        );
        notificationCenterService.notifyNewChatMessage(threadId, currentUserId, body);
    }

    private long requireUserId(String email) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?",
                Long.class,
                email
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("User not found: " + email, ex);
        }
    }

    private ThreadAccessRow requireAccessibleThread(long currentUserId, long threadId) {
        try {
            return jdbcTemplate.queryForObject(
                """
                        SELECT ct.id,
                               ct.title,
                               ct.status,
                               ct.related_url,
                               ct.related_entity_type,
                               ct.related_entity_id,
                               ct.counterpart_role,
                               other.nickname AS counterpart_nickname
                    FROM chat_threads ct
                    JOIN users other
                      ON other.id = CASE
                          WHEN ct.user_id = ? THEN ct.counterpart_id
                          ELSE ct.user_id
                      END
                    WHERE ct.id = ?
                      AND ct.deleted_at IS NULL
                      AND (ct.user_id = ? OR ct.counterpart_id = ?)
                    """,
                (rs, rowNum) -> new ThreadAccessRow(
                    rs.getLong("id"),
                    rs.getString("title"),
                    rs.getString("status"),
                    rs.getString("related_url"),
                    rs.getString("related_entity_type"),
                    rs.getObject("related_entity_id", Long.class),
                    rs.getString("counterpart_nickname"),
                    rs.getString("counterpart_role")
                ),
                currentUserId,
                threadId,
                currentUserId,
                currentUserId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Chat thread not found: " + threadId, ex);
        }
    }

    private PartnerAssistAction loadPartnerAssistAction(long currentUserId, ThreadAccessRow thread) {
        if (!"PROPOSAL_APPLICATION".equalsIgnoreCase(thread.relatedEntityType()) || thread.relatedEntityId() == null) {
            return null;
        }
        try {
            PartnerAssistRow row = jdbcTemplate.queryForObject(
                """
                    SELECT pa.id AS application_id,
                           pa.applicant_user_id,
                           p.host_user_id,
                           p.bridge_user_id
                    FROM proposal_applications pa
                    JOIN proposals p ON p.id = pa.proposal_id
                    WHERE pa.id = ?
                      AND pa.deleted_at IS NULL
                      AND p.deleted_at IS NULL
                      AND LOWER(pa.application_status) = 'accepted'
                    """,
                (rs, rowNum) -> new PartnerAssistRow(
                    rs.getLong("application_id"),
                    rs.getLong("applicant_user_id"),
                    rs.getLong("host_user_id"),
                    rs.getObject("bridge_user_id", Long.class)
                ),
                thread.relatedEntityId()
            );
            if (row == null || row.bridgeUserId() == null || row.bridgeUserId() == currentUserId) {
                return null;
            }
            if (currentUserId != row.applicantUserId() && currentUserId != row.hostUserId()) {
                return null;
            }
            Long coordinationThreadId = applicationChatThreadService.ensureHostPartnerCoordinationThread(row.applicationId());
            String relatedUrl = coordinationThreadId == null ? "/app/chat" : "/app/chat/" + coordinationThreadId;
            return new PartnerAssistAction(
                row.applicationId(),
                "架け橋さんに通知",
                relatedUrl
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private String requirePrimaryRole(long userId) {
        try {
            return jdbcTemplate.queryForObject(
                """
                    SELECT role_name
                    FROM user_roles
                    WHERE user_id = ?
                    ORDER BY CASE role_name
                        WHEN 'ADMIN' THEN 1
                        WHEN 'BRIDGE' THEN 2
                        WHEN 'LOCAL' THEN 3
                        WHEN 'USER' THEN 4
                        ELSE 5
                    END
                    FETCH FIRST 1 ROWS ONLY
                    """,
                String.class,
                userId
            );
        } catch (EmptyResultDataAccessException ex) {
            return "USER";
        }
    }

    private String normalizeBody(String body) {
        if (body == null) {
            return null;
        }
        String normalized = body.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > 4000) {
            return normalized.substring(0, 4000);
        }
        return normalized;
    }

    private String abbreviate(String body) {
        if (body.length() <= 500) {
            return body;
        }
        return body.substring(0, 500);
    }

    public record ChatThreadListPageData(List<ChatThreadListItem> threads) {
    }

    public record ChatThreadListItem(
        long threadId,
        String title,
        String counterpartNickname,
        String counterpartRole,
        String status,
        String latestMessagePreview,
        Timestamp latestMessageAt,
        String relatedUrl
    ) {
    }

    public record ChatThreadDetailPageData(
        long threadId,
        String title,
        String status,
        String relatedUrl,
        String counterpartNickname,
        String counterpartRole,
        boolean canSend,
        boolean canNotifyPartner,
        String partnerNotifyLabel,
        List<ChatMessageItem> messages
    ) {
    }

    public record ChatMessageItem(
        long messageId,
        long senderId,
        String senderRole,
        String senderNickname,
        String body,
        boolean systemMessage,
        Timestamp createdAt,
        boolean mine
    ) {
    }

    private record ThreadAccessRow(
        long threadId,
        String title,
        String status,
        String relatedUrl,
        String relatedEntityType,
        Long relatedEntityId,
        String counterpartNickname,
        String counterpartRole
    ) {
    }

    private record PartnerAssistRow(long applicationId, long applicantUserId, long hostUserId, Long bridgeUserId) {
    }

    private record PartnerAssistAction(long applicationId, String label, String relatedUrl) {
    }

    public static final class ChatThreadMessagingConflictException extends RuntimeException {
        public ChatThreadMessagingConflictException(String message) {
            super(message);
        }
    }
}
