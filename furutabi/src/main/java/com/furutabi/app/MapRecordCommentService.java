package com.furutabi.app;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.furutabi.visibility.VisibilityAccessService;

@Service
public class MapRecordCommentService {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final JdbcTemplate jdbcTemplate;
    private final VisibilityAccessService visibilityAccessService;
    private final NotificationCenterService notificationCenterService;

    public MapRecordCommentService(
        JdbcTemplate jdbcTemplate,
        VisibilityAccessService visibilityAccessService,
        NotificationCenterService notificationCenterService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.visibilityAccessService = visibilityAccessService;
        this.notificationCenterService = notificationCenterService;
    }

    public MapRecordCommentPageData loadVisibleComments(String email, long mapRecordId) {
        UserRow currentUser = findUser(email);
        Long viewerUserId = currentUser == null ? null : currentUser.id();

        if (!visibilityAccessService.canViewMapRecord(viewerUserId, mapRecordId)) {
            throw new IllegalStateException("Map record comments are not visible for current user: " + mapRecordId);
        }

        MapRecordRow mapRecord = requireMapRecord(mapRecordId);
        boolean canModerate = currentUser != null
            && (mapRecord.ownerUserId() == currentUser.id() || isAdmin(currentUser.id()));
        List<MapRecordCommentView> comments = jdbcTemplate.query(
            """
                SELECT mc.id, mc.user_id, mc.body, mc.created_at, u.email, u.nickname, u.name
                FROM map_record_comments mc
                JOIN users u ON u.id = mc.user_id
                WHERE mc.map_record_id = ?
                  AND mc.deleted_at IS NULL
                  AND mc.is_hidden = FALSE
                ORDER BY mc.created_at ASC, mc.id ASC
                """,
            (rs, rowNum) -> new MapRecordCommentView(
                rs.getLong("id"),
                rs.getLong("user_id"),
                displayName(rs.getString("nickname"), rs.getString("name"), rs.getString("email")),
                rs.getString("body"),
                formatTimestamp(rs.getTimestamp("created_at")),
                canModerate
            ),
            mapRecordId
        );

        return new MapRecordCommentPageData(
            mapRecordId,
            canModerate,
            currentUser != null && currentUser.id() == mapRecord.ownerUserId(),
            comments
        );
    }

    public void addComment(String email, long mapRecordId, MapRecordCommentForm form) {
        UserRow currentUser = requireUser(email);

        if (!visibilityAccessService.canViewMapRecord(currentUser.id(), mapRecordId)) {
            throw new IllegalStateException("Map record comments are not visible for current user: " + mapRecordId);
        }

        requireMapRecord(mapRecordId);
        String normalizedBody = normalizeBody(form.getBody());
        if (normalizedBody == null) {
            throw new MapRecordCommentConflictException("Comment body is empty");
        }

        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.update(
            """
                INSERT INTO map_record_comments (
                    map_record_id, user_id, body, is_hidden, created_at, updated_at, deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
            mapRecordId,
            currentUser.id(),
            normalizedBody,
            false,
            now,
            now,
            null
        );
        notificationCenterService.notifyMapComment(mapRecordId, currentUser.id(), normalizedBody);
    }

    public void hideComment(String email, long mapRecordId, long commentId) {
        UserRow currentUser = requireUser(email);
        MapRecordRow mapRecord = requireMapRecord(mapRecordId);

        if (mapRecord.ownerUserId() != currentUser.id() && !isAdmin(currentUser.id())) {
            throw new IllegalStateException("Map record comment moderation is not allowed: " + commentId);
        }

        int updated = jdbcTemplate.update(
            """
                UPDATE map_record_comments
                SET is_hidden = TRUE,
                    updated_at = ?
                WHERE id = ?
                  AND map_record_id = ?
                  AND deleted_at IS NULL
                  AND is_hidden = FALSE
                """,
            Timestamp.from(Instant.now()),
            commentId,
            mapRecordId
        );
        if (updated == 0) {
            throw new MapRecordCommentConflictException("Map record comment cannot be hidden: " + commentId);
        }
    }

    private MapRecordRow requireMapRecord(long mapRecordId) {
        try {
            return Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                    """
                        SELECT id, user_id
                        FROM map_records
                        WHERE id = ? AND deleted_at IS NULL AND is_draft = FALSE
                        """,
                    (rs, rowNum) -> new MapRecordRow(rs.getLong("id"), rs.getLong("user_id")),
                    mapRecordId
                ),
                "Map record row is required"
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Map record not found for comments: " + mapRecordId, ex);
        }
    }

    private UserRow findUser(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
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
                "Comment user row is required"
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("User not found for map record comments: " + email, ex);
        }
    }

    private UserRow requireUser(String email) {
        UserRow user = findUser(email);
        if (user == null) {
            throw new IllegalStateException("User not found for map record comments: " + email);
        }
        return user;
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

    private String normalizeBody(String body) {
        if (body == null) {
            return null;
        }
        String trimmed = body.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > 1000) {
            return trimmed.substring(0, 1000);
        }
        return trimmed;
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

    public record MapRecordCommentPageData(
        long mapRecordId,
        boolean moderationAvailable,
        boolean ownerViewing,
        List<MapRecordCommentView> comments
    ) {
    }

    public record MapRecordCommentView(
        long id,
        long authorUserId,
        String authorDisplayName,
        String body,
        String createdAt,
        boolean moderationAvailable
    ) {
    }

    private record UserRow(long id, String email, String nickname, String name) {
    }

    private record MapRecordRow(long id, long ownerUserId) {
    }

    public static class MapRecordCommentConflictException extends RuntimeException {
        public MapRecordCommentConflictException(String message) {
            super(message);
        }
    }
}
