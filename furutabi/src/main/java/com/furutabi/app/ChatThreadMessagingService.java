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

    public ChatThreadMessagingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
            messages
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
        String counterpartNickname,
        String counterpartRole
    ) {
    }

    public static final class ChatThreadMessagingConflictException extends RuntimeException {
        public ChatThreadMessagingConflictException(String message) {
            super(message);
        }
    }
}
