package com.furutabi.app;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import com.furutabi.visibility.VisibilityAccessService;
import com.furutabi.visibility.VisibilityScope;

@Service
public class MapRecordService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final JdbcTemplate jdbcTemplate;
    private final VisibilityAccessService visibilityAccessService;
    private final UserPointService userPointService;

    public MapRecordService(JdbcTemplate jdbcTemplate, VisibilityAccessService visibilityAccessService, UserPointService userPointService) {
        this.jdbcTemplate = jdbcTemplate;
        this.visibilityAccessService = visibilityAccessService;
        this.userPointService = userPointService;
    }

    public MapRecordListPageData loadVisibleMapRecordList(String email) {
        UserRow currentUser = requireUser(email);
        ViewerRoleContext viewerRoleContext = loadViewerRoleContext(currentUser.id());
        List<MapRecordSummary> records = loadMapRecordSummaries(
            currentUser,
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
                WHERE mr.deleted_at IS NULL AND mr.is_draft = FALSE
                ORDER BY mr.created_at DESC, mr.id DESC
                """
        );

        return new MapRecordListPageData(
            displayName(currentUser.nickname(), currentUser.name(), currentUser.email()),
            records,
            viewerRoleContext.reactionType(),
            viewerRoleContext.reactionLabel(),
            new FootprintFilterState(false, null, null, null)
        );
    }

    public MapRecordListPageData loadVisibleFootprintList(String email, Long ownerUserIdFilter) {
        UserRow currentUser = requireUser(email);
        ViewerRoleContext viewerRoleContext = loadViewerRoleContext(currentUser.id());
        List<MapRecordSummary> records = loadMapRecordSummaries(
            currentUser,
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
                WHERE mr.deleted_at IS NULL
                  AND mr.is_draft = FALSE
                  AND mr.visibility IN ('public', 'limited')
                ORDER BY COALESCE(mr.visibility_updated_at, mr.updated_at, mr.created_at) DESC, mr.id DESC
                """
        );

        List<MapRecordSummary> filteredRecords = ownerUserIdFilter == null
            ? records
            : records.stream().filter(record -> record.ownerUserId() == ownerUserIdFilter.longValue()).toList();

        return new MapRecordListPageData(
            displayName(currentUser.nickname(), currentUser.name(), currentUser.email()),
            filteredRecords,
            viewerRoleContext.reactionType(),
            viewerRoleContext.reactionLabel(),
            buildFootprintFilterState(ownerUserIdFilter, filteredRecords)
        );
    }

    public MapRecordDetailPageData loadVisibleMapRecordDetail(String email, long mapRecordId) {
        UserRow currentUser = requireUser(email);
        ViewerRoleContext viewerRoleContext = loadViewerRoleContext(currentUser.id());

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
                    long ownerUserId = rs.getLong("user_id");
                    ViewerEngagement engagement = loadViewerEngagement(currentUser.id(), mapRecordId, viewerRoleContext);
                    String ownerDisplayName = displayName(rs.getString("nickname"), rs.getString("name"), rs.getString("email"));
                    return new MapRecordDetailPageData(
                        new MapRecordDetail(
                            rs.getLong("id"),
                            ownerUserId,
                            rs.getString("title"),
                            rs.getString("body"),
                            rs.getString("location_name"),
                            rs.getString("location_precision_level"),
                            ownerDisplayName,
                            formatTimestamp(rs.getTimestamp("created_at")),
                            formatTimestamp(rs.getTimestamp("updated_at")),
                            formatTimestamp(rs.getTimestamp("visibility_updated_at")),
                            scope.name(),
                            visibilityLabel(scope),
                            rs.getInt("image_count"),
                            rs.getInt("comment_count"),
                            engagement,
                            "/app/footprints?userId=" + ownerUserId
                        ),
                        currentUser.id() == ownerUserId,
                        scope != VisibilityScope.PRIVATE,
                        scope != VisibilityScope.PRIVATE ? "/app/footprints/" + rs.getLong("id") : null,
                        "/app/history"
                    );
                },
                mapRecordId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Map record not found: " + mapRecordId, ex);
        }
    }

    public MapRecordDetailPageData loadVisibleFootprintDetail(String email, long mapRecordId) {
        MapRecordDetailPageData pageData = loadVisibleMapRecordDetail(email, mapRecordId);
        if ("PRIVATE".equalsIgnoreCase(pageData.record().visibilityKey())) {
            throw new IllegalStateException("Footprint record is not visible in footprint view: " + mapRecordId);
        }
        return pageData;
    }

    public MapRecordEditorPageData loadCreatePage(String email) {
        UserRow currentUser = requireUser(email);
        MapRecordForm form = new MapRecordForm();
        form.setVisibility(loadDefaultMapVisibility(currentUser.id()).name());
        form.setLocationPrecisionLevel("area");
        form.setDraft(false);
        return new MapRecordEditorPageData(
            "わたしの地図の記録を追加",
            "新しい記録を残します。公開設定と下書きの扱いだけ最小で持たせています。",
            currentUserDisplay(currentUser),
            "/app/map-records",
            false,
            null,
            form
        );
    }

    public MapRecordEditorPageData loadEditPage(String email, long mapRecordId) {
        UserRow currentUser = requireUser(email);
        MapRecordRow record = requireOwnedRecord(currentUser.id(), mapRecordId);
        MapRecordForm form = new MapRecordForm();
        form.setTitle(record.title());
        form.setBody(record.body());
        form.setVisibility(VisibilityScope.fromDbValue(record.visibility()).name());
        form.setLocationName(record.locationName());
        form.setLocationPrecisionLevel(record.locationPrecisionLevel());
        form.setDraft(record.draft());
        return new MapRecordEditorPageData(
            "わたしの地図の記録を編集",
            "保存すると一覧と詳細にそのまま反映されます。画像やコメントは次段階です。",
            currentUserDisplay(currentUser),
            "/app/map-records/" + mapRecordId,
            true,
            mapRecordId,
            form
        );
    }

    public MapRecordSaveResult createRecord(String email, MapRecordForm form) {
        UserRow currentUser = requireUser(email);
        Instant now = Instant.now();
        Timestamp timestamp = Timestamp.from(now);
        VisibilityScope scope = normalizeVisibility(form.getVisibility());
        boolean draft = form.isDraft();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                """
                    INSERT INTO map_records (
                        user_id, title, body, visibility, location_name, latitude, longitude,
                        location_precision_level, is_draft, created_at, updated_at, visibility_updated_at, deleted_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                new String[] {"id"}
            );
            statement.setLong(1, currentUser.id());
            statement.setString(2, normalizeTitle(form.getTitle()));
            statement.setString(3, normalizeText(form.getBody()));
            statement.setString(4, toDbVisibility(scope));
            statement.setString(5, normalizeText(form.getLocationName()));
            statement.setNull(6, Types.DECIMAL);
            statement.setNull(7, Types.DECIMAL);
            statement.setString(8, normalizePrecision(form.getLocationPrecisionLevel()));
            statement.setBoolean(9, draft);
            statement.setTimestamp(10, timestamp);
            statement.setTimestamp(11, timestamp);
            statement.setTimestamp(12, timestamp);
            statement.setNull(13, Types.TIMESTAMP);
            return statement;
        }, keyHolder);
        Number generatedId = Objects.requireNonNull(keyHolder.getKey(), "Map record ID was not generated");
        long mapRecordId = generatedId.longValue();
        userPointService.awardMapRecordPoint(currentUser.id(), mapRecordId, scope, draft);
        return new MapRecordSaveResult(mapRecordId, draft);
    }

    public MapRecordSaveResult updateRecord(String email, long mapRecordId, MapRecordForm form) {
        UserRow currentUser = requireUser(email);
        MapRecordRow currentRecord = requireOwnedRecord(currentUser.id(), mapRecordId);
        Instant now = Instant.now();
        Timestamp timestamp = Timestamp.from(now);
        VisibilityScope scope = normalizeVisibility(form.getVisibility());
        Timestamp visibilityUpdatedAt = currentRecord.visibility().equalsIgnoreCase(toDbVisibility(scope))
            ? currentRecord.visibilityUpdatedAt()
            : timestamp;

        jdbcTemplate.update(
            """
                UPDATE map_records
                SET title = ?,
                    body = ?,
                    visibility = ?,
                    location_name = ?,
                    location_precision_level = ?,
                    is_draft = ?,
                    updated_at = ?,
                    visibility_updated_at = ?
                WHERE id = ? AND user_id = ? AND deleted_at IS NULL
                """,
            normalizeTitle(form.getTitle()),
            normalizeText(form.getBody()),
            toDbVisibility(scope),
            normalizeText(form.getLocationName()),
            normalizePrecision(form.getLocationPrecisionLevel()),
            form.isDraft(),
            timestamp,
            visibilityUpdatedAt,
            mapRecordId,
            currentUser.id()
        );
        return new MapRecordSaveResult(mapRecordId, form.isDraft());
    }

    public void deleteRecord(String email, long mapRecordId) {
        UserRow currentUser = requireUser(email);
        requireOwnedRecord(currentUser.id(), mapRecordId);
        Timestamp timestamp = Timestamp.from(Instant.now());
        jdbcTemplate.update(
            """
                UPDATE map_records
                SET deleted_at = ?, updated_at = ?
                WHERE id = ? AND user_id = ? AND deleted_at IS NULL
                """,
            timestamp,
            timestamp,
            mapRecordId,
            currentUser.id()
        );
    }

    public void toggleViewerReaction(String email, long mapRecordId) {
        UserRow currentUser = requireUser(email);
        ViewerRoleContext viewerRoleContext = loadViewerRoleContext(currentUser.id());
        if (viewerRoleContext.reactionType() == null) {
            throw new MapRecordEngagementConflictException("No allowed reaction type for current user");
        }
        requireVisibleRecordForViewer(currentUser.id(), mapRecordId);

        Integer existing = jdbcTemplate.query(
            """
                SELECT id
                FROM map_record_reactions
                WHERE map_record_id = ? AND user_id = ? AND reaction_type = ?
                """,
            rs -> rs.next() ? rs.getInt("id") : null,
            mapRecordId,
            currentUser.id(),
            viewerRoleContext.reactionType()
        );

        if (existing != null) {
            jdbcTemplate.update(
                """
                    DELETE FROM map_record_reactions
                    WHERE map_record_id = ? AND user_id = ? AND reaction_type = ?
                    """,
                mapRecordId,
                currentUser.id(),
                viewerRoleContext.reactionType()
            );
            return;
        }

        jdbcTemplate.update(
            """
                DELETE FROM map_record_reactions
                WHERE map_record_id = ? AND user_id = ? AND reaction_type IN ('like', 'thanks')
                """,
            mapRecordId,
            currentUser.id()
        );
        jdbcTemplate.update(
            """
                INSERT INTO map_record_reactions (map_record_id, user_id, reaction_type, created_at)
                VALUES (?, ?, ?, ?)
                """,
            mapRecordId,
            currentUser.id(),
            viewerRoleContext.reactionType(),
            Timestamp.from(Instant.now())
        );
    }

    public void toggleBookmark(String email, long mapRecordId) {
        UserRow currentUser = requireUser(email);
        requireVisibleRecordForViewer(currentUser.id(), mapRecordId);

        Integer existing = jdbcTemplate.query(
            """
                SELECT id
                FROM map_record_bookmarks
                WHERE map_record_id = ? AND user_id = ?
                """,
            rs -> rs.next() ? rs.getInt("id") : null,
            mapRecordId,
            currentUser.id()
        );

        if (existing != null) {
            jdbcTemplate.update(
                "DELETE FROM map_record_bookmarks WHERE map_record_id = ? AND user_id = ?",
                mapRecordId,
                currentUser.id()
            );
            return;
        }

        jdbcTemplate.update(
            """
                INSERT INTO map_record_bookmarks (map_record_id, user_id, created_at)
                VALUES (?, ?, ?)
                """,
            mapRecordId,
            currentUser.id(),
            Timestamp.from(Instant.now())
        );
    }

    private MapRecordRow requireOwnedRecord(long userId, long mapRecordId) {
        try {
            return jdbcTemplate.queryForObject(
                """
                    SELECT id, user_id, title, body, visibility, location_name, location_precision_level,
                           is_draft, visibility_updated_at
                    FROM map_records
                    WHERE id = ? AND user_id = ? AND deleted_at IS NULL
                    """,
                (rs, rowNum) -> new MapRecordRow(
                    rs.getLong("id"),
                    rs.getLong("user_id"),
                    rs.getString("title"),
                    rs.getString("body"),
                    rs.getString("visibility"),
                    rs.getString("location_name"),
                    rs.getString("location_precision_level"),
                    rs.getBoolean("is_draft"),
                    rs.getTimestamp("visibility_updated_at")
                ),
                mapRecordId,
                userId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Owned map record not found: " + mapRecordId, ex);
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

    private void requireVisibleRecordForViewer(long viewerUserId, long mapRecordId) {
        if (!visibilityAccessService.canViewMapRecord(viewerUserId, mapRecordId)) {
            throw new IllegalStateException("Map record is not visible for current user: " + mapRecordId);
        }
        try {
            jdbcTemplate.queryForObject(
                """
                    SELECT id
                    FROM map_records
                    WHERE id = ? AND deleted_at IS NULL AND is_draft = FALSE
                    """,
                Long.class,
                mapRecordId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Map record not found: " + mapRecordId, ex);
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

    private List<MapRecordSummary> loadMapRecordSummaries(UserRow currentUser, String sql) {
        List<MapRecordSummary> visibleRecords = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> {
                VisibilityScope scope = VisibilityScope.fromDbValue(rs.getString("visibility"));
                long ownerUserId = rs.getLong("user_id");
                return new MapRecordSummary(
                    rs.getLong("id"),
                    ownerUserId,
                    rs.getString("title"),
                    summarize(rs.getString("body")),
                    rs.getString("location_name"),
                    displayName(rs.getString("nickname"), rs.getString("name"), rs.getString("email")),
                    formatTimestamp(rs.getTimestamp("created_at")),
                    scope.name(),
                    visibilityLabel(scope),
                    rs.getInt("image_count"),
                    rs.getInt("comment_count"),
                    currentUser.id() == ownerUserId,
                    null,
                    null
                );
            }
        ).stream()
            .filter(record -> visibilityAccessService.canView(
                VisibilityScope.fromDbValue(record.visibilityKey()),
                currentUser.id(),
                record.ownerUserId(),
                false
            ))
            .toList();

        ViewerRoleContext viewerRoleContext = loadViewerRoleContext(currentUser.id());
        Map<Long, ViewerEngagement> engagementByRecordId = loadViewerEngagementByRecordIds(
            currentUser.id(),
            visibleRecords.stream().map(MapRecordSummary::id).toList(),
            viewerRoleContext
        );

        return visibleRecords.stream()
            .map(record -> record.withEngagement(
                engagementByRecordId.getOrDefault(
                    record.id(),
                    ViewerEngagement.empty(viewerRoleContext.reactionType(), viewerRoleContext.reactionLabel())
                ),
                "/app/footprints?userId=" + record.ownerUserId()
            ))
            .toList();
    }

    private Map<Long, ViewerEngagement> loadViewerEngagementByRecordIds(
        long currentUserId,
        List<Long> mapRecordIds,
        ViewerRoleContext viewerRoleContext
    ) {
        if (mapRecordIds.isEmpty()) {
            return Map.of();
        }

        String placeholders = String.join(",", mapRecordIds.stream().map(id -> "?").toList());
        List<Object> params = new ArrayList<>();
        params.add(currentUserId);
        params.add(currentUserId);
        params.add(currentUserId);
        params.addAll(mapRecordIds);

        Map<Long, ViewerEngagement> engagementByRecordId = new HashMap<>();
        jdbcTemplate.query(
            """
                SELECT mr.id,
                       (SELECT COUNT(*) FROM map_record_reactions rr WHERE rr.map_record_id = mr.id AND rr.reaction_type = 'like') AS like_count,
                       (SELECT COUNT(*) FROM map_record_reactions rr WHERE rr.map_record_id = mr.id AND rr.reaction_type = 'thanks') AS thanks_count,
                       EXISTS(SELECT 1 FROM map_record_reactions rr WHERE rr.map_record_id = mr.id AND rr.user_id = ? AND rr.reaction_type = 'like') AS liked_by_viewer,
                       EXISTS(SELECT 1 FROM map_record_reactions rr WHERE rr.map_record_id = mr.id AND rr.user_id = ? AND rr.reaction_type = 'thanks') AS thanked_by_viewer,
                       EXISTS(SELECT 1 FROM map_record_bookmarks mb WHERE mb.map_record_id = mr.id AND mb.user_id = ?) AS bookmarked_by_viewer
                FROM map_records mr
                WHERE mr.id IN (PLACEHOLDER_IDS)
                """.replace("PLACEHOLDER_IDS", placeholders),
            rs -> {
                engagementByRecordId.put(
                    rs.getLong("id"),
                    new ViewerEngagement(
                        rs.getInt("like_count"),
                        rs.getInt("thanks_count"),
                        rs.getBoolean("liked_by_viewer"),
                        rs.getBoolean("thanked_by_viewer"),
                        rs.getBoolean("bookmarked_by_viewer"),
                        viewerRoleContext.reactionType(),
                        viewerRoleContext.reactionLabel()
                    )
                );
            },
            params.toArray()
        );
        return engagementByRecordId;
    }

    private ViewerEngagement loadViewerEngagement(long currentUserId, long mapRecordId, ViewerRoleContext viewerRoleContext) {
        return loadViewerEngagementByRecordIds(currentUserId, List.of(mapRecordId), viewerRoleContext)
            .getOrDefault(mapRecordId, ViewerEngagement.empty(viewerRoleContext.reactionType(), viewerRoleContext.reactionLabel()));
    }

    private ViewerRoleContext loadViewerRoleContext(long currentUserId) {
        List<String> roles = jdbcTemplate.query(
            """
                SELECT role_name
                FROM user_roles
                WHERE user_id = ?
                """,
            (rs, rowNum) -> rs.getString("role_name"),
            currentUserId
        );

        if (roles.contains("USER")) {
            return new ViewerRoleContext("like", "いいね");
        }
        if (roles.contains("LOCAL") || roles.contains("BRIDGE")) {
            return new ViewerRoleContext("thanks", "ありがとう");
        }
        return new ViewerRoleContext(null, null);
    }

    private FootprintFilterState buildFootprintFilterState(Long ownerUserIdFilter, List<MapRecordSummary> records) {
        if (ownerUserIdFilter == null) {
            return new FootprintFilterState(false, null, null, null);
        }

        String ownerLabel = records.stream()
            .findFirst()
            .map(MapRecordSummary::ownerDisplayName)
            .orElseGet(() -> loadUserDisplayName(ownerUserIdFilter));

        return new FootprintFilterState(true, ownerUserIdFilter, ownerLabel, "/app/footprints");
    }

    private String loadUserDisplayName(long userId) {
        try {
            return jdbcTemplate.queryForObject(
                """
                    SELECT nickname, name, email
                    FROM users
                    WHERE id = ?
                    """,
                (rs, rowNum) -> displayName(rs.getString("nickname"), rs.getString("name"), rs.getString("email")),
                userId
            );
        } catch (EmptyResultDataAccessException ex) {
            return "このユーザー";
        }
    }

    private String currentUserDisplay(UserRow currentUser) {
        return displayName(currentUser.nickname(), currentUser.name(), currentUser.email());
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

    private VisibilityScope normalizeVisibility(String value) {
        return VisibilityScope.fromDbValue(value);
    }

    private VisibilityScope loadDefaultMapVisibility(long userId) {
        String value = jdbcTemplate.query(
            """
                SELECT map_default_visibility
                FROM contact_preferences
                WHERE user_id = ?
                """,
            rs -> rs.next() ? rs.getString("map_default_visibility") : null,
            userId
        );
        return VisibilityScope.fromDbValue(value);
    }

    private String toDbVisibility(VisibilityScope scope) {
        return scope.name().toLowerCase();
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return "新しい記録";
        }
        return title.trim();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizePrecision(String value) {
        if (value == null || value.isBlank()) {
            return "area";
        }
        String trimmed = value.trim().toLowerCase();
        return switch (trimmed) {
            case "point", "spot" -> "point";
            case "town" -> "town";
            default -> "area";
        };
    }

    public record MapRecordListPageData(
        String currentUserLabel,
        List<MapRecordSummary> records,
        String viewerReactionType,
        String viewerReactionLabel,
        FootprintFilterState filterState
    ) {
    }

    public record MapRecordSummary(
        long id,
        long ownerUserId,
        String title,
        String summary,
        String locationName,
        String ownerDisplayName,
        String createdAt,
        String visibilityKey,
        String visibilityLabel,
        int imageCount,
        int commentCount,
        boolean owner,
        ViewerEngagement engagement,
        String ownerFilterPath
    ) {
        public MapRecordSummary withEngagement(ViewerEngagement updatedEngagement, String updatedOwnerFilterPath) {
            return new MapRecordSummary(
                id,
                ownerUserId,
                title,
                summary,
                locationName,
                ownerDisplayName,
                createdAt,
                visibilityKey,
                visibilityLabel,
                imageCount,
                commentCount,
                owner,
                updatedEngagement,
                updatedOwnerFilterPath
            );
        }
    }

    public record MapRecordDetailPageData(
        MapRecordDetail record,
        boolean ownerViewing,
        boolean footprintAvailable,
        String footprintDetailPath,
        String historyPath
    ) {
    }

    public record MapRecordDetail(
        long id,
        long ownerUserId,
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
        int commentCount,
        ViewerEngagement engagement,
        String ownerFilterPath
    ) {
    }

    public record FootprintFilterState(
        boolean active,
        Long ownerUserId,
        String ownerDisplayName,
        String clearPath
    ) {
    }

    public record ViewerEngagement(
        int likeCount,
        int thanksCount,
        boolean likedByViewer,
        boolean thankedByViewer,
        boolean bookmarkedByViewer,
        String reactionType,
        String reactionLabel
    ) {
        public static ViewerEngagement empty(String reactionType, String reactionLabel) {
            return new ViewerEngagement(0, 0, false, false, false, reactionType, reactionLabel);
        }

        public boolean reactionAllowed() {
            return reactionType != null && reactionLabel != null;
        }

        public boolean reactionActive() {
            return switch (reactionType == null ? "" : reactionType) {
                case "like" -> likedByViewer;
                case "thanks" -> thankedByViewer;
                default -> false;
            };
        }
    }

    public record ViewerRoleContext(String reactionType, String reactionLabel) {
    }

    public record MapRecordEditorPageData(
        String pageTitle,
        String leadText,
        String currentUserLabel,
        String submitPath,
        boolean editing,
        Long recordId,
        MapRecordForm form
    ) {
    }

    public record MapRecordSaveResult(long mapRecordId, boolean draft) {
    }

    public static class MapRecordEngagementConflictException extends RuntimeException {
        public MapRecordEngagementConflictException(String message) {
            super(message);
        }
    }

    private record UserRow(long id, String email, String nickname, String name) {
    }

    private record MapRecordRow(
        long id,
        long userId,
        String title,
        String body,
        String visibility,
        String locationName,
        String locationPrecisionLevel,
        boolean draft,
        Timestamp visibilityUpdatedAt
    ) {
    }
}
