package com.furutabi.app;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.furutabi.visibility.VisibilityAccessService;
import com.furutabi.visibility.VisibilityScope;

@Service
public class MapRecordService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final JdbcTemplate jdbcTemplate;
    private final VisibilityAccessService visibilityAccessService;

    public MapRecordService(JdbcTemplate jdbcTemplate, VisibilityAccessService visibilityAccessService) {
        this.jdbcTemplate = jdbcTemplate;
        this.visibilityAccessService = visibilityAccessService;
    }

    public MapRecordListPageData loadOwnMapRecordList(String email) {
        UserRow currentUser = requireUser(email);
        List<MapRecordSummary> records = jdbcTemplate.query(
            """
                SELECT mr.id, mr.user_id, mr.title, mr.body, mr.visibility, mr.location_name, mr.created_at,
                       u.email, u.nickname, u.name,
                       (SELECT COUNT(*) FROM map_record_images mi WHERE mi.map_record_id = mr.id) AS image_count,
                       (
                           SELECT COUNT(*)
                           FROM map_record_comments mc
                           WHERE mc.map_record_id = mr.id AND mc.deleted_at IS NULL AND mc.is_hidden = FALSE
                       ) AS comment_count
                FROM map_records mr
                JOIN users u ON u.id = mr.user_id
                WHERE mr.user_id = ? AND mr.deleted_at IS NULL AND mr.is_draft = FALSE
                ORDER BY mr.created_at DESC, mr.id DESC
                """,
            (rs, rowNum) -> {
                VisibilityScope scope = VisibilityScope.fromDbValue(rs.getString("visibility"));
                return new MapRecordSummary(
                    rs.getLong("id"),
                    rs.getString("title"),
                    summarize(rs.getString("body")),
                    rs.getString("location_name"),
                    displayName(rs.getString("nickname"), rs.getString("name"), rs.getString("email")),
                    formatTimestamp(rs.getTimestamp("created_at")),
                    scope.name(),
                    visibilityLabel(scope),
                    rs.getInt("image_count"),
                    rs.getInt("comment_count"),
                    true
                );
            },
            currentUser.id()
        );

        return new MapRecordListPageData(displayName(currentUser.nickname(), currentUser.name(), currentUser.email()), records);
    }

    public MapRecordDetailPageData loadVisibleMapRecordDetail(String email, long mapRecordId) {
        UserRow currentUser = requireUser(email);

        if (!visibilityAccessService.canViewMapRecord(currentUser.id(), mapRecordId)) {
            throw new IllegalStateException("Map record is not visible for current user: " + mapRecordId);
        }

        try {
            return jdbcTemplate.queryForObject(
                """
                    SELECT mr.id, mr.user_id, mr.title, mr.body, mr.visibility, mr.location_name,
                           mr.location_precision_level, mr.created_at, mr.updated_at, mr.visibility_updated_at,
                           u.email, u.nickname, u.name,
                           (SELECT COUNT(*) FROM map_record_images mi WHERE mi.map_record_id = mr.id) AS image_count,
                           (
                               SELECT COUNT(*)
                               FROM map_record_comments mc
                               WHERE mc.map_record_id = mr.id AND mc.deleted_at IS NULL AND mc.is_hidden = FALSE
                           ) AS comment_count
                    FROM map_records mr
                    JOIN users u ON u.id = mr.user_id
                    WHERE mr.id = ? AND mr.deleted_at IS NULL AND mr.is_draft = FALSE
                    """,
                (rs, rowNum) -> {
                    VisibilityScope scope = VisibilityScope.fromDbValue(rs.getString("visibility"));
                    return new MapRecordDetailPageData(
                        new MapRecordDetail(
                            rs.getLong("id"),
                            rs.getString("title"),
                            rs.getString("body"),
                            rs.getString("location_name"),
                            rs.getString("location_precision_level"),
                            displayName(rs.getString("nickname"), rs.getString("name"), rs.getString("email")),
                            formatTimestamp(rs.getTimestamp("created_at")),
                            formatTimestamp(rs.getTimestamp("updated_at")),
                            formatTimestamp(rs.getTimestamp("visibility_updated_at")),
                            scope.name(),
                            visibilityLabel(scope),
                            rs.getInt("image_count"),
                            rs.getInt("comment_count")
                        ),
                        currentUser.id() == rs.getLong("user_id")
                    );
                },
                mapRecordId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Map record not found: " + mapRecordId, ex);
        }
    }

    private UserRow requireUser(String email) {
        try {
            return jdbcTemplate.queryForObject(
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
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("User not found for map records: " + email, ex);
        }
    }

    private String summarize(String body) {
        if (body == null) {
            return "まだ本文はありません。";
        }

        String trimmed = body.trim();
        if (trimmed.isEmpty()) {
            return "まだ本文はありません。";
        }
        if (trimmed.length() <= 96) {
            return trimmed;
        }
        return trimmed.substring(0, 96) + "...";
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
        return timestamp.toLocalDateTime().format(DATE_FORMAT);
    }

    private String visibilityLabel(VisibilityScope scope) {
        return switch (scope) {
            case PUBLIC -> "一般公開";
            case PRIVATE -> "本人のみ";
            case LIMITED -> "関係者まで";
        };
    }

    public record MapRecordListPageData(String currentUserLabel, List<MapRecordSummary> records) {
    }

    public record MapRecordSummary(
        long id,
        String title,
        String summary,
        String locationName,
        String ownerDisplayName,
        String createdAt,
        String visibilityKey,
        String visibilityLabel,
        int imageCount,
        int commentCount,
        boolean owner
    ) {
    }

    public record MapRecordDetailPageData(MapRecordDetail record, boolean ownerViewing) {
    }

    public record MapRecordDetail(
        long id,
        String title,
        String body,
        String locationName,
        String locationPrecisionLevel,
        String ownerDisplayName,
        String createdAt,
        String updatedAt,
        String visibilityUpdatedAt,
        String visibilityKey,
        String visibilityLabel,
        int imageCount,
        int commentCount
    ) {
    }

    private record UserRow(long id, String email, String nickname, String name) {
    }
}
