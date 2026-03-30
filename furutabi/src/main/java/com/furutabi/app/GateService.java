package com.furutabi.app;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.furutabi.visibility.VisibilityAccessService;
import com.furutabi.visibility.VisibilityScope;

@Service
public class GateService {

    private final JdbcTemplate jdbcTemplate;
    private final VisibilityAccessService visibilityAccessService;

    public GateService(JdbcTemplate jdbcTemplate, VisibilityAccessService visibilityAccessService) {
        this.jdbcTemplate = jdbcTemplate;
        this.visibilityAccessService = visibilityAccessService;
    }

    public GateListPageData loadVisibleGateList(String email) {
        Long viewerUserId = findUserIdByEmail(email);
        List<GateSummary> visibleItems = loadGateCandidates().stream()
            .filter(item -> visibilityAccessService.canViewProposal(viewerUserId, item.proposalId()))
            .map(item -> item.withOwner(viewerUserId != null && viewerUserId == item.hostUserId()))
            .toList();
        return new GateListPageData(visibleItems);
    }

    public GateDetailPageData loadVisibleGateDetail(String email, long proposalId) {
        Long viewerUserId = findUserIdByEmail(email);
        GateDetailRow row = loadGateDetailRow(proposalId);
        if (!visibilityAccessService.canViewProposal(viewerUserId, proposalId)) {
            throw new IllegalStateException("Gate proposal is not visible: " + proposalId);
        }

        return new GateDetailPageData(
            proposalId,
            row.title(),
            nullableText(row.summary()),
            nullableText(row.body()),
            nullableText(row.locationName()),
            row.durationMinutes(),
            VisibilityScope.fromDbValue(row.visibilityScope()).name(),
            nullableText(row.hostNickname()),
            nullableText(row.bridgeNickname()),
            loadTags(proposalId),
            row.createdAt() == null ? null : row.createdAt().toLocalDateTime(),
            viewerUserId != null && viewerUserId == row.hostUserId(),
            canApply(viewerUserId, row),
            loadApplicationStatus(viewerUserId, proposalId),
            loadRelatedChatPath(viewerUserId, proposalId),
            "/app/history"
        );
    }

    private boolean canApply(Long viewerUserId, GateDetailRow row) {
        if (viewerUserId == null) {
            return false;
        }
        if (viewerUserId == row.hostUserId()) {
            return false;
        }
        if (row.bridgeUserId() != null && viewerUserId == row.bridgeUserId()) {
            return false;
        }
        return loadApplicationStatus(viewerUserId, row.proposalId()) == null;
    }

    private List<GateSummaryRow> loadGateCandidates() {
        Map<Long, List<String>> tagMap = loadTagMap();
        return jdbcTemplate.query(
            """
                SELECT p.id, p.host_user_id, p.title, p.summary, p.location_name, p.duration_minutes,
                       p.visibility_scope, u.nickname AS host_nickname
                FROM proposals p
                JOIN users u ON u.id = p.host_user_id
                WHERE p.deleted_at IS NULL
                  AND LOWER(p.status) = 'published'
                  AND p.proposal_type IN ('LOCAL_GUIDE', 'GATE')
                ORDER BY p.created_at DESC, p.id DESC
                """,
            (rs, rowNum) -> new GateSummaryRow(
                rs.getLong("id"),
                rs.getLong("host_user_id"),
                rs.getString("title"),
                rs.getString("summary"),
                rs.getString("location_name"),
                rs.getObject("duration_minutes", Integer.class),
                rs.getString("visibility_scope"),
                rs.getString("host_nickname"),
                tagMap.getOrDefault(rs.getLong("id"), List.of())
            )
        );
    }

    private GateDetailRow loadGateDetailRow(long proposalId) {
        try {
            return jdbcTemplate.queryForObject(
                """
                    SELECT p.id, p.host_user_id, p.title, p.summary, p.body, p.location_name, p.duration_minutes,
                           p.visibility_scope, p.created_at, p.bridge_user_id,
                           host.nickname AS host_nickname,
                           bridge.nickname AS bridge_nickname
                    FROM proposals p
                    JOIN users host ON host.id = p.host_user_id
                    LEFT JOIN users bridge ON bridge.id = p.bridge_user_id
                    WHERE p.id = ?
                      AND p.deleted_at IS NULL
                      AND LOWER(p.status) = 'published'
                      AND p.proposal_type IN ('LOCAL_GUIDE', 'GATE')
                    """,
                (rs, rowNum) -> new GateDetailRow(
                    rs.getLong("id"),
                    rs.getLong("host_user_id"),
                    rs.getString("title"),
                    rs.getString("summary"),
                    rs.getString("body"),
                    rs.getString("location_name"),
                    rs.getObject("duration_minutes", Integer.class),
                    rs.getString("visibility_scope"),
                    rs.getString("host_nickname"),
                    rs.getString("bridge_nickname"),
                    rs.getTimestamp("created_at"),
                    rs.getObject("bridge_user_id", Long.class)
                ),
                proposalId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Gate proposal not found: " + proposalId, ex);
        }
    }

    private Map<Long, List<String>> loadTagMap() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
                SELECT proposal_id, tag_name
                FROM proposal_tags
                ORDER BY proposal_id, sort_order, id
                """
        );
        Map<Long, List<String>> tagsByProposalId = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Long proposalId = ((Number) row.get("proposal_id")).longValue();
            tagsByProposalId.computeIfAbsent(proposalId, ignored -> new ArrayList<>())
                .add((String) row.get("tag_name"));
        }
        return tagsByProposalId;
    }

    private List<String> loadTags(long proposalId) {
        return jdbcTemplate.query(
            """
                SELECT tag_name
                FROM proposal_tags
                WHERE proposal_id = ?
                ORDER BY sort_order, id
                """,
            (rs, rowNum) -> rs.getString("tag_name"),
            proposalId
        );
    }

    private Long findUserIdByEmail(String email) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?",
                Long.class,
                email
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("User not found for email: " + email, ex);
        }
    }

    private String loadApplicationStatus(Long viewerUserId, long proposalId) {
        if (viewerUserId == null) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject(
                """
                    SELECT application_status
                    FROM proposal_applications
                    WHERE proposal_id = ?
                      AND applicant_user_id = ?
                      AND deleted_at IS NULL
                    ORDER BY applied_at DESC, id DESC
                    FETCH FIRST 1 ROWS ONLY
                    """,
                String.class,
                proposalId,
                viewerUserId
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private String loadRelatedChatPath(Long viewerUserId, long proposalId) {
        if (viewerUserId == null) {
            return null;
        }
        try {
            Long threadId = jdbcTemplate.queryForObject(
                """
                    SELECT related_thread_id
                    FROM proposal_applications
                    WHERE proposal_id = ?
                      AND applicant_user_id = ?
                      AND deleted_at IS NULL
                      AND related_thread_id IS NOT NULL
                    ORDER BY applied_at DESC, id DESC
                    FETCH FIRST 1 ROWS ONLY
                    """,
                Long.class,
                proposalId,
                viewerUserId
            );
            return threadId == null ? null : "/app/chat/" + threadId;
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private String nullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    public record GateListPageData(List<GateSummary> items) {
    }

    public record GateSummary(
        long proposalId,
        String title,
        String summary,
        String locationName,
        Integer durationMinutes,
        String visibilityLabel,
        String hostNickname,
        List<String> tags,
        boolean owner
    ) {
    }

    public record GateDetailPageData(
        long proposalId,
        String title,
        String summary,
        String body,
        String locationName,
        Integer durationMinutes,
        String visibilityLabel,
        String hostNickname,
        String bridgeNickname,
        List<String> tags,
        LocalDateTime createdAt,
        boolean owner,
        boolean canApply,
        String applicationStatus,
        String relatedChatPath,
        String historyPath
    ) {
    }

    private record GateSummaryRow(
        long proposalId,
        long hostUserId,
        String title,
        String summary,
        String locationName,
        Integer durationMinutes,
        String visibilityScope,
        String hostNickname,
        List<String> tags
    ) {
        private GateSummary withOwner(boolean owner) {
            return new GateSummary(
                proposalId,
                title,
                titleIfBlank(summary),
                locationName,
                durationMinutes,
                VisibilityScope.fromDbValue(visibilityScope).name(),
                hostNickname,
                tags,
                owner
            );
        }

        private String titleIfBlank(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            return value;
        }
    }

    private record GateDetailRow(
        long proposalId,
        long hostUserId,
        String title,
        String summary,
        String body,
        String locationName,
        Integer durationMinutes,
        String visibilityScope,
        String hostNickname,
        String bridgeNickname,
        Timestamp createdAt,
        Long bridgeUserId
    ) {
    }
}
