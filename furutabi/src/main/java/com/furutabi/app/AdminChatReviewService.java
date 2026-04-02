package com.furutabi.app;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.furutabi.admin.AdminUserManagementRepository;

@Service
public class AdminChatReviewService {

    private final JdbcTemplate jdbcTemplate;
    private final AdminUserManagementRepository adminUserManagementRepository;

    public AdminChatReviewService(
        JdbcTemplate jdbcTemplate,
        AdminUserManagementRepository adminUserManagementRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.adminUserManagementRepository = adminUserManagementRepository;
    }

    public AdminChatReviewPageData loadReviewGate(String email, long threadId, Long supportRequestId) {
        AdminViewer viewer = requireAdminViewer(email);
        ChatThreadSummary summary = requireThreadSummary(threadId);
        return new AdminChatReviewPageData(
            summary.threadId(),
            supportRequestId,
            summary.title(),
            summary.status(),
            summary.participantOneLabel(),
            summary.participantTwoLabel(),
            null,
            false,
            List.of()
        );
    }

    public AdminChatReviewPageData openReview(
        String email,
        long threadId,
        Long supportRequestId,
        AdminChatReviewForm form
    ) {
        AdminViewer viewer = requireAdminViewer(email);
        String reason = normalizeReason(form == null ? null : form.getReason());
        if (reason == null) {
            throw new AdminChatReviewConflictException("Admin review reason is required");
        }
        ChatThreadSummary summary = requireThreadSummary(threadId);
        List<ChatThreadMessagingService.ChatMessageItem> messages = loadMessages(threadId);
        adminUserManagementRepository.insertAccessLog(
            viewer.userId(),
            "admin",
            "chat_thread",
            threadId,
            reason,
            Timestamp.from(Instant.now())
        );
        return new AdminChatReviewPageData(
            summary.threadId(),
            supportRequestId,
            summary.title(),
            summary.status(),
            summary.participantOneLabel(),
            summary.participantTwoLabel(),
            reason,
            true,
            messages
        );
    }

    private AdminViewer requireAdminViewer(String email) {
        var viewer = adminUserManagementRepository.requireUserByEmail(email);
        if (!adminUserManagementRepository.isLegacyAdmin(viewer.userId())) {
            throw new IllegalStateException("Admin chat review access is required");
        }
        return new AdminViewer(viewer.userId(), displayName(viewer.nickname(), viewer.name(), viewer.email()));
    }

    private ChatThreadSummary requireThreadSummary(long threadId) {
        try {
            return Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                    """
                        SELECT ct.id,
                               ct.title,
                               ct.status,
                               owner.nickname AS owner_nickname,
                               owner.name AS owner_name,
                               owner.email AS owner_email,
                               counterpart.nickname AS counterpart_nickname,
                               counterpart.name AS counterpart_name,
                               counterpart.email AS counterpart_email
                        FROM chat_threads ct
                        LEFT JOIN users owner ON owner.id = ct.user_id
                        LEFT JOIN users counterpart ON counterpart.id = ct.counterpart_id
                        WHERE ct.id = ?
                          AND ct.deleted_at IS NULL
                        """,
                    (rs, rowNum) -> new ChatThreadSummary(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("status"),
                        displayName(
                            rs.getString("owner_nickname"),
                            rs.getString("owner_name"),
                            rs.getString("owner_email")
                        ),
                        displayName(
                            rs.getString("counterpart_nickname"),
                            rs.getString("counterpart_name"),
                            rs.getString("counterpart_email")
                        )
                    ),
                    threadId
                ),
                "Admin chat review thread is required"
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Chat thread not found: " + threadId, ex);
        }
    }

    private List<ChatThreadMessagingService.ChatMessageItem> loadMessages(long threadId) {
        return jdbcTemplate.query(
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
            (rs, rowNum) -> new ChatThreadMessagingService.ChatMessageItem(
                rs.getLong("id"),
                rs.getLong("sender_id"),
                rs.getString("sender_role"),
                rs.getString("sender_nickname"),
                rs.getString("body"),
                rs.getBoolean("is_system_message"),
                rs.getTimestamp("created_at"),
                false
            ),
            threadId
        );
    }

    private String normalizeReason(String reason) {
        if (reason == null) {
            return null;
        }
        String normalized = reason.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() <= 500) {
            return normalized;
        }
        return normalized.substring(0, 500);
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

    public record AdminChatReviewPageData(
        long threadId,
        Long supportRequestId,
        String title,
        String status,
        String participantOneLabel,
        String participantTwoLabel,
        String reason,
        boolean opened,
        List<ChatThreadMessagingService.ChatMessageItem> messages
    ) {
    }

    private record AdminViewer(long userId, String displayName) {
    }

    private record ChatThreadSummary(
        long threadId,
        String title,
        String status,
        String participantOneLabel,
        String participantTwoLabel
    ) {
    }

    public static class AdminChatReviewConflictException extends RuntimeException {
        public AdminChatReviewConflictException(String message) {
            super(message);
        }
    }
}
