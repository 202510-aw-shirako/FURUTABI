package com.furutabi.app;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

@Service
public class ApplicationChatThreadService {

    private final JdbcTemplate jdbcTemplate;

    public ApplicationChatThreadService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long ensureOpenThread(long applicationId) {
        ApplicationChatRow row = requireApplicationChatRow(applicationId);
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        Long existingThreadId = findReusableThreadId(row);
        if (existingThreadId != null) {
            jdbcTemplate.update(
                """
                    UPDATE chat_threads
                    SET status = ?, updated_at = ?, closed_at = ?, deleted_at = ?
                    WHERE id = ?
                    """,
                "open",
                now,
                null,
                null,
                existingThreadId
            );
            updateRelatedThreadId(applicationId, existingThreadId, now);
            return existingThreadId;
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                """
                    INSERT INTO chat_threads (
                        user_id, counterpart_id, counterpart_role, related_entity_type, related_entity_id,
                        title, status, unread_count, requires_attention, related_url, latest_message_preview,
                        latest_message_at, created_at, updated_at, closed_at, deleted_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                new String[] {"id"}
            );
            statement.setLong(1, row.applicantUserId());
            statement.setLong(2, row.hostUserId());
            statement.setString(3, row.hostRole());
            statement.setString(4, "PROPOSAL_APPLICATION");
            statement.setLong(5, applicationId);
            statement.setString(6, row.threadTitle());
            statement.setString(7, "open");
            statement.setInt(8, 0);
            statement.setBoolean(9, false);
            statement.setString(10, reviewDetailPath(applicationId));
            statement.setString(11, row.latestMessagePreview());
            statement.setTimestamp(12, row.latestMessageAt());
            statement.setTimestamp(13, now);
            statement.setTimestamp(14, now);
            statement.setTimestamp(15, null);
            statement.setTimestamp(16, null);
            return statement;
        }, keyHolder);

        long createdThreadId = Objects.requireNonNull(keyHolder.getKey(), "Chat thread ID was not generated").longValue();
        updateRelatedThreadId(applicationId, createdThreadId, now);
        return createdThreadId;
    }

    public void closeThread(long applicationId) {
        ApplicationChatRow row = requireApplicationChatRow(applicationId);
        Long threadId = findReusableThreadId(row);
        if (threadId == null) {
            return;
        }

        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbcTemplate.update(
            """
                UPDATE chat_threads
                SET status = ?, updated_at = ?, closed_at = ?
                WHERE id = ? AND deleted_at IS NULL
                """,
            "closed",
            now,
            now,
            threadId
        );
        updateRelatedThreadId(applicationId, threadId, now);
    }

    private ApplicationChatRow requireApplicationChatRow(long applicationId) {
        try {
            return jdbcTemplate.queryForObject(
                """
                    SELECT pa.id, pa.applicant_user_id, pa.related_thread_id, pa.latest_message_preview, pa.latest_message_at,
                           p.host_user_id, p.title,
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
                    WHERE pa.id = ?
                      AND pa.deleted_at IS NULL
                      AND p.deleted_at IS NULL
                    """,
                (rs, rowNum) -> new ApplicationChatRow(
                    rs.getLong("id"),
                    rs.getLong("applicant_user_id"),
                    rs.getLong("host_user_id"),
                    rs.getObject("related_thread_id", Long.class),
                    rs.getString("title"),
                    rs.getString("host_role"),
                    rs.getString("latest_message_preview"),
                    rs.getTimestamp("latest_message_at")
                ),
                applicationId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Proposal application not found for chat thread: " + applicationId, ex);
        }
    }

    private Long findReusableThreadId(ApplicationChatRow row) {
        if (row.relatedThreadId() != null) {
            Integer linked = jdbcTemplate.queryForObject(
                """
                    SELECT COUNT(*)
                    FROM chat_threads
                    WHERE id = ? AND deleted_at IS NULL
                    """,
                Integer.class,
                row.relatedThreadId()
            );
            if (linked != null && linked > 0) {
                return row.relatedThreadId();
            }
        }

        try {
            return jdbcTemplate.queryForObject(
                """
                    SELECT id
                    FROM chat_threads
                    WHERE related_entity_type = 'PROPOSAL_APPLICATION'
                      AND related_entity_id = ?
                      AND deleted_at IS NULL
                    ORDER BY updated_at DESC, id DESC
                    FETCH FIRST 1 ROWS ONLY
                    """,
                Long.class,
                row.applicationId()
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private void updateRelatedThreadId(long applicationId, long threadId, Timestamp now) {
        jdbcTemplate.update(
            """
                UPDATE proposal_applications
                SET related_thread_id = ?, updated_at = ?
                WHERE id = ? AND deleted_at IS NULL
                """,
            threadId,
            now,
            applicationId
        );
    }

    private String reviewDetailPath(long applicationId) {
        return "/app/host-applications/" + applicationId;
    }

    private record ApplicationChatRow(
        long applicationId,
        long applicantUserId,
        long hostUserId,
        Long relatedThreadId,
        String threadTitle,
        String hostRole,
        String latestMessagePreview,
        Timestamp latestMessageAt
    ) {
    }
}
