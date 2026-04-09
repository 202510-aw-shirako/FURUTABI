package com.furutabi.app;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.furutabi.visibility.VisibilityAccessService;
import com.furutabi.visibility.VisibilityScope;

@Service
public class OkatteService {

    public static final String OKATTE_KIND_ITO = "ITO_OKATTE";
    public static final String OKATTE_KIND_HASEGAWA_GRAPE = "HASEGAWA_GRAPE";
    public static final String OKATTE_KIND_HASEGAWA_CHRYSANTHEMUM = "HASEGAWA_CHRYSANTHEMUM";
    public static final String OKATTE_KIND_HOSYO_ANAGO = "HOSYO_ANAGO";
    public static final String OKATTE_KIND_HOSYO_SCENERY = "HOSYO_SCENERY";
    public static final String OKATTE_KIND_TODO = "TODO_OKATTE";
    public static final String OKATTE_KIND_RIKYU = "RIKYU_OKATTE";
    public static final String OKATTE_KIND_UNKNOWN = "UNKNOWN";

    private static final String[] PIN_CLASSES = {
        "okattePin--a",
        "okattePin--b",
        "okattePin--c",
        "okattePin--d",
        "okattePin--e"
    };

    private final JdbcTemplate jdbcTemplate;
    private final VisibilityAccessService visibilityAccessService;
    public OkatteService(JdbcTemplate jdbcTemplate, VisibilityAccessService visibilityAccessService) {
        this.jdbcTemplate = jdbcTemplate;
        this.visibilityAccessService = visibilityAccessService;
    }

    public OkatteListPageData loadVisibleOkatteList(String email) {
        Long viewerUserId = findUserIdByEmail(email);
        Map<String, OkatteSummary> uniqueItems = new LinkedHashMap<>();
        loadOkatteCandidates().stream()
            .filter(item -> visibilityAccessService.canViewProposal(viewerUserId, item.proposalId()))
            .map(item -> item.withOwner(viewerUserId != null && viewerUserId == item.hostUserId()))
            .forEach(item -> uniqueItems.putIfAbsent(item.title(), item));
        return new OkatteListPageData(assignPinClasses(new ArrayList<>(uniqueItems.values())));
    }

    public OkatteDetailPageData loadVisibleOkatteDetail(String email, long proposalId) {
        Long viewerUserId = findUserIdByEmail(email);
        OkatteDetailRow row = loadOkatteDetailRow(proposalId);
        if (!visibilityAccessService.canViewProposal(viewerUserId, proposalId)) {
            throw new IllegalStateException("Okatte proposal is not visible: " + proposalId);
        }

        List<OkatteSummary> relatedItems = new ArrayList<>(loadVisibleOkatteList(email).items());
        boolean currentIncluded = relatedItems.stream().anyMatch(item -> item.proposalId() == proposalId);
        if (!currentIncluded) {
            ProposalPresentationCatalog.ProgramContent currentProgram = ProposalPresentationCatalog.resolveProgramByProposalId(proposalId, row.title(), nullableText(row.summary()), nullableText(row.body()), row.durationMinutes(), nullableText(row.locationName()));
            ProposalPresentationCatalog.HostProfile currentHost = ProposalPresentationCatalog.resolveHostForProposal(row.hostUserId(), proposalId, row.title(), nullableText(row.hostNickname()));
            relatedItems.add(0, new OkatteSummary(
                proposalId,
                currentProgram.title(),
                currentProgram.cardSummary(),
                nullableText(row.locationName()),
                row.durationMinutes(),
                VisibilityScope.fromDbValue(row.visibilityScope()).name(),
                nullableText(row.hostNickname()),
                loadTags(proposalId),
                viewerUserId != null && viewerUserId == row.hostUserId(),
                PIN_CLASSES[0],
                1,
                currentProgram.templateKind(),
                currentProgram.cardImage(),
                currentHost.portraitImage(),
                currentHost.portraitAlt()
            ));
        }

        ProposalPresentationCatalog.ProgramContent program = ProposalPresentationCatalog.resolveProgramByProposalId(
            proposalId,
            row.title(),
            nullableText(row.summary()),
            nullableText(row.body()),
            row.durationMinutes(),
            nullableText(row.locationName())
        );
        ProposalPresentationCatalog.HostProfile host = ProposalPresentationCatalog.resolveHostForProposal(
            row.hostUserId(),
            proposalId,
            row.title(),
            nullableText(row.hostNickname())
        );

        return new OkatteDetailPageData(
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
            "/app/history",
            loadSupportNoteUrl(viewerUserId, row),
            "/app/okatte",
            relatedItems,
            program.templateKind(),
            program,
            host
        );
    }

    private List<OkatteSummary> assignPinClasses(List<OkatteSummary> items) {
        List<OkatteSummary> assigned = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            OkatteSummary item = items.get(i);
            assigned.add(new OkatteSummary(
                item.proposalId(),
                item.title(),
                item.summary(),
                item.locationName(),
                item.durationMinutes(),
                item.visibilityLabel(),
                item.hostNickname(),
                item.tags(),
                item.owner(),
                PIN_CLASSES[i % PIN_CLASSES.length],
                i + 1,
                item.templateKind(),
                item.cardImagePath(),
                item.faceImagePath(),
                item.faceImageAlt()
            ));
        }
        return assigned;
    }

    private boolean canApply(Long viewerUserId, OkatteDetailRow row) {
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
    private List<OkatteSummaryRow> loadOkatteCandidates() {
        Map<Long, List<String>> tagMap = loadTagMap();
        return jdbcTemplate.query(
            """
                SELECT p.id, p.host_user_id, p.title, p.summary, p.location_name, p.duration_minutes,
                       p.visibility_scope, u.nickname AS host_nickname
                FROM proposals p
                JOIN users u ON u.id = p.host_user_id
                WHERE p.deleted_at IS NULL
                  AND LOWER(p.status) = 'published'
                  AND p.proposal_type = 'OKATTE'
                ORDER BY p.created_at DESC, p.id DESC
                """,
            (rs, rowNum) -> new OkatteSummaryRow(
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

    private OkatteDetailRow loadOkatteDetailRow(long proposalId) {
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
                      AND p.proposal_type = 'OKATTE'
                    """,
                (rs, rowNum) -> new OkatteDetailRow(
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
            throw new IllegalStateException("Okatte proposal not found: " + proposalId, ex);
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

    private String loadSupportNoteUrl(Long viewerUserId, OkatteDetailRow row) {
        if (viewerUserId == null) {
            return null;
        }
        if (viewerUserId.longValue() != row.hostUserId()
            && (row.bridgeUserId() == null || viewerUserId.longValue() != row.bridgeUserId().longValue())) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject(
                """
                    SELECT '/app/support-notes/users/' || pa.applicant_user_id || '/new?relatedCardId=' || pa.id
                    FROM proposal_applications pa
                    JOIN chat_threads ct ON ct.id = pa.related_thread_id AND ct.deleted_at IS NULL
                    WHERE pa.proposal_id = ?
                      AND pa.deleted_at IS NULL
                      AND LOWER(pa.application_status) = 'accepted'
                      AND LOWER(ct.status) IN ('closed', 'completed', 'cancelled')
                    ORDER BY ct.closed_at DESC, pa.id DESC
                    FETCH FIRST 1 ROWS ONLY
                    """,
                String.class,
                row.proposalId()
            );
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

    public record OkatteListPageData(List<OkatteSummary> items) {
    }

    public record OkatteSummary(
        long proposalId,
        String title,
        String summary,
        String locationName,
        Integer durationMinutes,
        String visibilityLabel,
        String hostNickname,
        List<String> tags,
        boolean owner,
        String pinClass,
        int displayNumber,
        String templateKind,
        String cardImagePath,
        String faceImagePath,
        String faceImageAlt
    ) {
    }

    public record OkatteDetailPageData(
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
        String historyPath,
        String supportNoteUrl,
        String listPath,
        List<OkatteSummary> relatedItems,
        String templateKind,
        ProposalPresentationCatalog.ProgramContent program,
        ProposalPresentationCatalog.HostProfile host
    ) {
    }

    private record OkatteSummaryRow(
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
        private OkatteSummary withOwner(boolean owner) {
            ProposalPresentationCatalog.ProgramContent program = ProposalPresentationCatalog.resolveProgramByProposalId(proposalId, title, titleIfBlank(summary), null, durationMinutes, locationName);
            ProposalPresentationCatalog.HostProfile host = ProposalPresentationCatalog.resolveHostForProposal(hostUserId, proposalId, title, hostNickname);
            return new OkatteSummary(
                proposalId,
                program.title(),
                program.cardSummary(),
                locationName,
                durationMinutes,
                VisibilityScope.fromDbValue(visibilityScope).name(),
                hostNickname,
                tags,
                owner,
                PIN_CLASSES[0],
                1,
                program.templateKind(),
                program.cardImage(),
                host.portraitImage(),
                host.portraitAlt()
            );
        }

        private String titleIfBlank(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            return value;
        }
    }

    private record OkatteDetailRow(
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
