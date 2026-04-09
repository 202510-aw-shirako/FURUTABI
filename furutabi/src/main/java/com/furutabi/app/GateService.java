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
public class GateService {

    public static final String GATE_KIND_ITO_WALK = "ITO_WALK";
    public static final String GATE_KIND_ITO_FLOWER = "ITO_FLOWER";
    public static final String GATE_KIND_HASEGAWA = "HASEGAWA";
    public static final String GATE_KIND_HOSYO = "HOSYO";
    public static final String GATE_KIND_TODO_WALK = "TODO_WALK";
    public static final String GATE_KIND_TODO_LEATHER = "TODO_LEATHER";
    public static final String GATE_KIND_UNKNOWN = "UNKNOWN";

    private static final String[] PIN_CLASSES = {
        "gateEntryPin--a",
        "gateEntryPin--b",
        "gateEntryPin--c",
        "gateEntryPin--d",
        "gateEntryPin--e"
    };

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
        return new GateListPageData(assignPinClasses(selectPrimaryGateItems(visibleItems)));
    }

    public GateDetailPageData loadVisibleGateDetail(String email, long proposalId) {
        Long viewerUserId = findUserIdByEmail(email);
        GateDetailRow row = loadGateDetailRow(proposalId);
        if (!visibilityAccessService.canViewProposal(viewerUserId, proposalId)) {
            throw new IllegalStateException("Gate proposal is not visible: " + proposalId);
        }

        List<GateSummary> relatedItems = loadVisibleGateList(email).items();

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
            "/app/history",
            loadSupportNoteUrl(viewerUserId, row),
            "/app/gate",
            relatedItems,
            program.templateKind(),
            program,
            host
        );
    }

    private List<GateSummary> assignPinClasses(List<GateSummary> items) {
        List<GateSummary> assigned = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            GateSummary item = items.get(i);
            assigned.add(new GateSummary(
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
                item.templateKind(),
                item.cardImagePath(),
                item.faceImagePath(),
                item.faceImageAlt()
            ));
        }
        return assigned;
    }

    private List<GateSummary> selectPrimaryGateItems(List<GateSummary> items) {
        List<GateSummary> selected = new ArrayList<>();
        List<Long> selectedIds = new ArrayList<>();
        for (String kind : List.of(
            GATE_KIND_ITO_WALK,
            GATE_KIND_ITO_FLOWER,
            GATE_KIND_HASEGAWA,
            GATE_KIND_HOSYO,
            GATE_KIND_TODO_WALK,
            GATE_KIND_TODO_LEATHER
        )) {
            items.stream()
                .filter(item -> kind.equals(item.templateKind()))
                .findFirst()
                .ifPresent(item -> {
                    selected.add(item);
                    selectedIds.add(item.proposalId());
                });
        }
        items.stream()
            .filter(item -> !selectedIds.contains(item.proposalId()))
            .forEach(selected::add);
        return selected;
    }

    private boolean canApply(Long viewerUserId, GateDetailRow row) {
        if (viewerUserId == null) {
            return false;
        }
        if (viewerUserId == row.hostUserId()) {
            return false;
        }
        if (row.bridgeUserId() != null && viewerUserId.longValue() == row.bridgeUserId().longValue()) {
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

    private String loadSupportNoteUrl(Long viewerUserId, GateDetailRow row) {
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
        boolean owner,
        String pinClass,
        String templateKind,
        String cardImagePath,
        String faceImagePath,
        String faceImageAlt
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
        String historyPath,
        String supportNoteUrl,
        String listPath,
        List<GateSummary> relatedItems,
        String templateKind,
        ProposalPresentationCatalog.ProgramContent program,
        ProposalPresentationCatalog.HostProfile host
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
            ProposalPresentationCatalog.ProgramContent program = ProposalPresentationCatalog.resolveProgramByProposalId(proposalId, title, titleIfBlank(summary), null, durationMinutes, locationName);
            ProposalPresentationCatalog.HostProfile host = ProposalPresentationCatalog.resolveHostForProposal(hostUserId, proposalId, title, hostNickname);
            return new GateSummary(
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
