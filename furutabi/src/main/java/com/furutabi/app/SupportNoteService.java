package com.furutabi.app;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.furutabi.visibility.VisibilityAccessService;

@Service
public class SupportNoteService {

    private static final int DEFAULT_GRACE_DAYS = 7;
    private static final int CARE_POINTS_PREVIEW_LIMIT = 5;
    private static final String DEFAULT_NOTE_TYPE = "support_note";
    private static final String RELATED_CARD_TYPE_APPLICATION = "proposal_application";
    private static final String BODY_TEMPLATE = """
        興味・関心:

        好きな食べ物:

        好きな関わり方:

        次に求めていること:

        来訪時の同伴者:
        """;

    private final JdbcTemplate jdbcTemplate;
    private final VisibilityAccessService visibilityAccessService;

    public SupportNoteService(JdbcTemplate jdbcTemplate, VisibilityAccessService visibilityAccessService) {
        this.jdbcTemplate = jdbcTemplate;
        this.visibilityAccessService = visibilityAccessService;
    }

    public SupportNoteListPageData loadList(String viewerEmail, long targetUserId) {
        Viewer viewer = requireViewer(viewerEmail);
        TargetUser target = requireTargetUser(targetUserId);
        AccessScope access = requireReadAccess(viewer, target);

        List<SupportNoteCardItem> items = jdbcTemplate.query(
            """
                SELECT n.id,
                       n.note_status,
                       n.body,
                       n.care_points,
                       n.visit_companions,
                       n.is_hidden,
                       n.created_at,
                       n.updated_at,
                       n.related_card_type,
                       n.related_card_id,
                       creator.nickname AS creator_nickname,
                       creator.email AS creator_email,
                       updater.nickname AS updater_nickname,
                       updater.email AS updater_email,
                       (
                           SELECT COUNT(*)
                           FROM support_note_reports r
                           WHERE r.support_note_id = n.id
                       ) AS report_count
                FROM support_notes n
                JOIN users creator ON creator.id = n.created_by_user_id
                LEFT JOIN users updater ON updater.id = n.updated_by_user_id
                WHERE n.target_user_id = ?
                  AND (? OR n.is_hidden = FALSE)
                ORDER BY n.updated_at DESC, n.id DESC
                """,
            (rs, rowNum) -> {
                RelatedCardInfo relatedCard = loadRelatedCard(
                    rs.getLong("id"),
                    rs.getString("related_card_type"),
                    rs.getObject("related_card_id", Long.class),
                    viewer.userId()
                );
                return new SupportNoteCardItem(
                    rs.getLong("id"),
                    displayNoteStatus(rs.getString("note_status")),
                    mergeLegacyVisitCompanions(rs.getString("body"), rs.getString("visit_companions")),
                    blankToNull(rs.getString("care_points")),
                    rs.getBoolean("is_hidden"),
                    formatTimestamp(rs.getTimestamp("created_at")),
                    formatTimestamp(rs.getTimestamp("updated_at")),
                    displayName(rs.getString("creator_nickname"), rs.getString("creator_email")),
                    displayName(rs.getString("updater_nickname"), rs.getString("updater_email")),
                    rs.getInt("report_count"),
                    relatedCard,
                    "/app/support-notes/" + rs.getLong("id")
                );
            },
            targetUserId,
            access.admin()
        );

        List<CarePointSummaryItem> allCarePoints = loadCarePoints(targetUserId, access);
        List<CarePointSummaryItem> previewCarePoints = allCarePoints.size() <= CARE_POINTS_PREVIEW_LIMIT
            ? allCarePoints
            : allCarePoints.subList(0, CARE_POINTS_PREVIEW_LIMIT);

        return new SupportNoteListPageData(
            targetUserId,
            target.displayName(),
            target.regionId(),
            access.admin(),
            access.hostViewer(),
            access.partnerViewer(),
            items,
            previewCarePoints,
            allCarePoints.size(),
            "/app/support-notes/users/" + targetUserId + "/care-points",
            "/app/support-notes/users/" + targetUserId + "/new",
            "/app/admin/users/" + targetUserId
        );
    }

    public SupportNoteCarePointsPageData loadCarePointsPage(String viewerEmail, long targetUserId) {
        Viewer viewer = requireViewer(viewerEmail);
        TargetUser target = requireTargetUser(targetUserId);
        AccessScope access = requireReadAccess(viewer, target);
        return new SupportNoteCarePointsPageData(
            targetUserId,
            target.displayName(),
            loadCarePoints(targetUserId, access),
            "/app/support-notes/users/" + targetUserId,
            "/app/admin/users/" + targetUserId
        );
    }

    public SupportNoteDetailPageData loadDetail(String viewerEmail, long noteId) {
        Viewer viewer = requireViewer(viewerEmail);
        SupportNoteRow row = requireSupportNote(noteId);
        TargetUser target = requireTargetUser(row.targetUserId());
        AccessScope access = requireReadAccess(viewer, target);
        if (row.hidden() && !access.admin()) {
            throw new IllegalStateException("Support note not available");
        }

        List<SupportNoteReportItem> reports = access.admin()
            ? jdbcTemplate.query(
                """
                    SELECT r.id, r.report_note, r.created_at, u.nickname, u.email
                    FROM support_note_reports r
                    JOIN users u ON u.id = r.reported_by_user_id
                    WHERE r.support_note_id = ?
                    ORDER BY r.created_at DESC, r.id DESC
                    """,
                (rs, rowNum) -> new SupportNoteReportItem(
                    rs.getLong("id"),
                    rs.getString("report_note"),
                    formatTimestamp(rs.getTimestamp("created_at")),
                    displayName(rs.getString("nickname"), rs.getString("email"))
                ),
                noteId
            )
            : List.of();

        boolean canEdit = access.admin() || row.createdByUserId() == viewer.userId();
        RelatedCardInfo relatedCard = loadRelatedCard(row.noteId(), row.relatedCardType(), row.relatedCardId(), viewer.userId());

        return new SupportNoteDetailPageData(
            row.noteId(),
            row.targetUserId(),
            target.displayName(),
            target.regionId(),
            displayNoteStatus(row.noteStatus()),
            row.noteStatus(),
            mergeLegacyVisitCompanions(row.body(), row.visitCompanions()),
            blankToDisplay(row.carePoints()),
            row.hidden(),
            blankToDisplay(row.hiddenReason()),
            formatTimestamp(row.hiddenAt()),
            row.createdByLabel(),
            formatTimestamp(row.createdAt()),
            row.updatedByLabel(),
            formatTimestamp(row.updatedAt()),
            canEdit,
            access.admin(),
            true,
            relatedCard,
            reports,
            "/app/support-notes/" + noteId + "/edit",
            "/app/support-notes/users/" + row.targetUserId(),
            "/app/admin/users/" + row.targetUserId()
        );
    }

    public SupportNoteEditorPageData loadCreatePage(String viewerEmail, long targetUserId, Long preferredRelatedCardId) {
        Viewer viewer = requireViewer(viewerEmail);
        TargetUser target = requireTargetUser(targetUserId);
        requireReadAccess(viewer, target);

        SupportNoteForm form = new SupportNoteForm();
        form.setNoteStatus("active");
        form.setBody(BODY_TEMPLATE);
        form.setBody(form.getBody() + "\nその他:\n");
        if (preferredRelatedCardId != null) {
            validateRelatedCard(targetUserId, preferredRelatedCardId);
            form.setRelatedCardId(preferredRelatedCardId);
        }

        return new SupportNoteEditorPageData(
            "支援メモを作成",
            targetUserId,
            target.displayName(),
            form,
            loadRelatedCardCandidates(targetUserId),
            "/app/support-notes/users/" + targetUserId,
            "/app/support-notes/users/" + targetUserId
        );
    }

    public SupportNoteEditorPageData loadEditPage(String viewerEmail, long noteId) {
        Viewer viewer = requireViewer(viewerEmail);
        SupportNoteRow row = requireSupportNote(noteId);
        TargetUser target = requireTargetUser(row.targetUserId());
        AccessScope access = requireReadAccess(viewer, target);
        if (row.hidden() && !access.admin()) {
            throw new IllegalStateException("Support note not available");
        }
        if (!access.admin() && row.createdByUserId() != viewer.userId()) {
            throw new IllegalStateException("Support note not editable");
        }

        SupportNoteForm form = new SupportNoteForm();
        form.setNoteStatus(row.noteStatus());
        form.setRelatedCardId(RELATED_CARD_TYPE_APPLICATION.equals(row.relatedCardType()) ? row.relatedCardId() : null);
        form.setBody(mergeLegacyVisitCompanions(row.body(), row.visitCompanions()));
        form.setCarePoints(row.carePoints());

        return new SupportNoteEditorPageData(
            "支援メモを編集",
            row.targetUserId(),
            target.displayName(),
            form,
            loadRelatedCardCandidates(row.targetUserId()),
            "/app/support-notes/" + noteId,
            "/app/support-notes/" + noteId
        );
    }

    public SupportNoteRelatedCardPageData loadRelatedCardPage(String viewerEmail, long noteId) {
        Viewer viewer = requireViewer(viewerEmail);
        SupportNoteRow row = requireSupportNote(noteId);
        TargetUser target = requireTargetUser(row.targetUserId());
        AccessScope access = requireReadAccess(viewer, target);
        if (row.hidden() && !access.admin()) {
            throw new IllegalStateException("Support note not available");
        }
        if (!RELATED_CARD_TYPE_APPLICATION.equals(row.relatedCardType()) || row.relatedCardId() == null) {
            throw new IllegalStateException("Related card not available");
        }
        return loadRelatedCardContext(noteId, row.relatedCardId(), viewer.userId(), target, access);
    }

    @Transactional
    public long createNote(String viewerEmail, long targetUserId, SupportNoteForm form) {
        Viewer viewer = requireViewer(viewerEmail);
        TargetUser target = requireTargetUser(targetUserId);
        requireReadAccess(viewer, target);

        SupportNotePayload payload = toPayload(form, targetUserId);
        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.update(
            """
                INSERT INTO support_notes (
                    target_user_id, region_id, note_type, note_status, body, care_points, visit_companions,
                    related_card_type, related_card_id,
                    is_hidden, hidden_reason, hidden_by_user_id, hidden_at,
                    created_by_user_id, updated_by_user_id, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, NULL, ?, ?, FALSE, NULL, NULL, NULL, ?, ?, ?, ?)
                """,
            targetUserId,
            target.regionId(),
            DEFAULT_NOTE_TYPE,
            payload.noteStatus(),
            payload.body(),
            payload.carePoints(),
            payload.relatedCardType(),
            payload.relatedCardId(),
            viewer.userId(),
            viewer.userId(),
            now,
            now
        );
        Long createdId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM support_notes", Long.class);
        if (createdId == null) {
            throw new IllegalStateException("Support note was not created");
        }
        return createdId;
    }

    @Transactional
    public void updateNote(String viewerEmail, long noteId, SupportNoteForm form) {
        Viewer viewer = requireViewer(viewerEmail);
        SupportNoteRow row = requireSupportNote(noteId);
        TargetUser target = requireTargetUser(row.targetUserId());
        AccessScope access = requireReadAccess(viewer, target);
        if (row.hidden() && !access.admin()) {
            throw new IllegalStateException("Support note not available");
        }
        if (!access.admin() && row.createdByUserId() != viewer.userId()) {
            throw new IllegalStateException("Support note not editable");
        }

        SupportNotePayload payload = toPayload(form, row.targetUserId());
        jdbcTemplate.update(
            """
                UPDATE support_notes
                SET note_status = ?, body = ?, care_points = ?, visit_companions = NULL,
                    related_card_type = ?, related_card_id = ?,
                    updated_by_user_id = ?, updated_at = ?
                WHERE id = ?
                """,
            payload.noteStatus(),
            payload.body(),
            payload.carePoints(),
            payload.relatedCardType(),
            payload.relatedCardId(),
            viewer.userId(),
            Timestamp.from(Instant.now()),
            noteId
        );
    }

    @Transactional
    public void hideNote(String viewerEmail, long noteId, SupportNoteModerationForm form) {
        Viewer viewer = requireViewer(viewerEmail);
        if (!viewer.admin()) {
            throw new IllegalStateException("Support note not available");
        }
        requireSupportNote(noteId);
        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.update(
            """
                UPDATE support_notes
                SET is_hidden = TRUE,
                    hidden_reason = ?,
                    hidden_by_user_id = ?,
                    hidden_at = ?,
                    updated_by_user_id = ?,
                    updated_at = ?
                WHERE id = ?
                """,
            normalizeRequiredText(form == null ? null : form.getReason(), 500, "reason"),
            viewer.userId(),
            now,
            viewer.userId(),
            now,
            noteId
        );
    }

    @Transactional
    public void unhideNote(String viewerEmail, long noteId, SupportNoteModerationForm form) {
        Viewer viewer = requireViewer(viewerEmail);
        if (!viewer.admin()) {
            throw new IllegalStateException("Support note not available");
        }
        requireSupportNote(noteId);
        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.update(
            """
                UPDATE support_notes
                SET is_hidden = FALSE,
                    hidden_reason = NULL,
                    hidden_by_user_id = NULL,
                    hidden_at = NULL,
                    updated_by_user_id = ?,
                    updated_at = ?
                WHERE id = ?
                """,
            viewer.userId(),
            now,
            noteId
        );
    }

    @Transactional
    public void reportNote(String viewerEmail, long noteId, SupportNoteReportForm form) {
        Viewer viewer = requireViewer(viewerEmail);
        SupportNoteRow row = requireSupportNote(noteId);
        TargetUser target = requireTargetUser(row.targetUserId());
        AccessScope access = requireReadAccess(viewer, target);
        if (row.hidden() && !access.admin()) {
            throw new IllegalStateException("Support note not available");
        }
        jdbcTemplate.update(
            """
                INSERT INTO support_note_reports (
                    support_note_id, reported_by_user_id, report_note, created_at
                ) VALUES (?, ?, ?, ?)
                """,
            noteId,
            viewer.userId(),
            normalizeRequiredText(form == null ? null : form.getReportNote(), 1000, "reportNote"),
            Timestamp.from(Instant.now())
        );
    }

    private SupportNotePayload toPayload(SupportNoteForm form, long targetUserId) {
        Long relatedCardId = form == null ? null : form.getRelatedCardId();
        validateRelatedCard(targetUserId, relatedCardId);
        return new SupportNotePayload(
            normalizeNoteStatus(form == null ? null : form.getNoteStatus()),
            normalizeRequiredText(form == null ? null : form.getBody(), 4000, "body"),
            normalizeOptionalText(form == null ? null : form.getCarePoints(), 4000),
            relatedCardId == null ? null : RELATED_CARD_TYPE_APPLICATION,
            relatedCardId
        );
    }

    private void validateRelatedCard(long targetUserId, Long relatedCardId) {
        if (relatedCardId == null) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM proposal_applications
                WHERE id = ?
                  AND applicant_user_id = ?
                  AND deleted_at IS NULL
                """,
            Integer.class,
            relatedCardId,
            targetUserId
        );
        if (count == null || count == 0) {
            throw new SupportNoteConflictException("relatedCardId is invalid");
        }
    }

    private AccessScope requireReadAccess(Viewer viewer, TargetUser target) {
        if (viewer.userId() == target.userId()) {
            throw new IllegalStateException("Support note not available");
        }
        if (viewer.admin()) {
            return new AccessScope(true, false, false);
        }

        int graceDays = loadCloseGraceDays(target.regionId());
        Instant closeThreshold = Instant.now().minusSeconds(graceDays * 86400L);
        Timestamp closeThresholdTimestamp = Timestamp.from(closeThreshold);

        boolean hostViewer = isCurrentAssignedHostViewer(viewer.userId(), target.userId())
            || isWithinSupportNoteGracePeriod(viewer.userId(), target.userId(), closeThresholdTimestamp);
        // Keep current partner readability conservative: active assignment only.
        boolean partnerViewer = hasActivePartnerAssignment(viewer.userId(), target.userId());
        if (!hostViewer && !partnerViewer) {
            throw new IllegalStateException("Support note not available");
        }
        return new AccessScope(false, hostViewer, partnerViewer);
    }

    private boolean hasActivePartnerAssignment(long viewerUserId, long targetUserId) {
        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM partner_assignments
                WHERE user_id = ?
                  AND partner_user_id = ?
                  AND assignment_status = 'active'
                  AND (effective_from IS NULL OR effective_from <= ?)
                  AND (effective_to IS NULL OR effective_to >= ?)
                """,
            Integer.class,
            targetUserId,
            viewerUserId,
            LocalDate.now(),
            LocalDate.now()
        );
        return count != null && count > 0;
    }

    private boolean isCurrentAssignedHostViewer(long viewerUserId, long targetUserId) {
        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM proposal_applications pa
                JOIN proposals p ON p.id = pa.proposal_id
                LEFT JOIN chat_threads ct ON ct.id = pa.related_thread_id AND ct.deleted_at IS NULL
                WHERE pa.deleted_at IS NULL
                  AND p.deleted_at IS NULL
                  AND pa.applicant_user_id = ?
                  AND p.host_user_id = ?
                  AND LOWER(pa.application_status) = 'accepted'
                  AND (
                      pa.related_thread_id IS NULL
                      OR LOWER(COALESCE(ct.status, '')) = 'open'
                  )
                """,
            Integer.class,
            targetUserId,
            viewerUserId
        );
        return count != null && count > 0;
    }

    private boolean isWithinSupportNoteGracePeriod(long viewerUserId, long targetUserId, Timestamp closeThreshold) {
        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM proposal_applications pa
                JOIN proposals p ON p.id = pa.proposal_id
                LEFT JOIN chat_threads ct ON ct.id = pa.related_thread_id AND ct.deleted_at IS NULL
                LEFT JOIN (
                    SELECT proposal_application_id, MAX(created_at) AS close_recorded_at
                    FROM proposal_application_status_history
                    WHERE LOWER(status) IN ('completed', 'cancelled')
                    GROUP BY proposal_application_id
                ) history_close ON history_close.proposal_application_id = pa.id
                WHERE pa.deleted_at IS NULL
                  AND p.deleted_at IS NULL
                  AND pa.applicant_user_id = ?
                  AND p.host_user_id = ?
                  AND (
                      (
                          history_close.close_recorded_at IS NOT NULL
                          AND history_close.close_recorded_at >= ?
                      )
                      OR (
                          history_close.close_recorded_at IS NULL
                          AND LOWER(pa.application_status) IN ('completed', 'cancelled')
                          AND pa.updated_at >= ?
                      )
                      OR (
                          history_close.close_recorded_at IS NULL
                          AND LOWER(pa.application_status) = 'accepted'
                          AND LOWER(COALESCE(ct.status, '')) IN ('completed', 'cancelled', 'closed')
                          AND ct.closed_at IS NOT NULL
                          AND ct.closed_at >= ?
                      )
                  )
                """,
            Integer.class,
            targetUserId,
            viewerUserId,
            closeThreshold,
            closeThreshold,
            closeThreshold
        );
        return count != null && count > 0;
    }

    private int loadCloseGraceDays(String regionId) {
        try {
            String raw = jdbcTemplate.queryForObject(
                """
                    SELECT setting_value
                    FROM region_scoped_settings
                    WHERE region_id = ? AND setting_key = 'support_note_close_grace_days'
                    """,
                String.class,
                regionId
            );
            if (raw == null || raw.isBlank()) {
                return DEFAULT_GRACE_DAYS;
            }
            return Integer.parseInt(raw.trim());
        } catch (EmptyResultDataAccessException | NumberFormatException ex) {
            return DEFAULT_GRACE_DAYS;
        }
    }

    private List<CarePointSummaryItem> loadCarePoints(long targetUserId, AccessScope access) {
        return jdbcTemplate.query(
            """
                SELECT n.id,
                       n.care_points,
                       n.created_at,
                       creator.nickname AS creator_nickname,
                       creator.email AS creator_email
                FROM support_notes n
                JOIN users creator ON creator.id = n.created_by_user_id
                WHERE n.target_user_id = ?
                  AND n.care_points IS NOT NULL
                  AND TRIM(n.care_points) <> ''
                  AND (? OR n.is_hidden = FALSE)
                ORDER BY n.updated_at DESC, n.id DESC
                """,
            (rs, rowNum) -> new CarePointSummaryItem(
                rs.getLong("id"),
                summarizeCarePoints(rs.getString("care_points")),
                formatTimestamp(rs.getTimestamp("created_at")),
                displayName(rs.getString("creator_nickname"), rs.getString("creator_email")),
                "/app/support-notes/" + rs.getLong("id")
            ),
            targetUserId,
            access.admin()
        );
    }

    private List<RelatedCardCandidateItem> loadRelatedCardCandidates(long targetUserId) {
        return jdbcTemplate.query(
            """
                SELECT pa.id,
                       pa.applied_at,
                       p.proposal_type,
                       p.title,
                       p.location_name
                FROM proposal_applications pa
                JOIN proposals p ON p.id = pa.proposal_id
                WHERE pa.applicant_user_id = ?
                  AND pa.deleted_at IS NULL
                  AND p.deleted_at IS NULL
                ORDER BY pa.applied_at DESC, pa.id DESC
                FETCH FIRST 12 ROWS ONLY
                """,
            (rs, rowNum) -> new RelatedCardCandidateItem(
                rs.getLong("id"),
                buildRelatedCardLabel(
                    rs.getString("proposal_type"),
                    rs.getString("title"),
                    rs.getString("location_name"),
                    rs.getTimestamp("applied_at")
                ),
                formatDate(rs.getTimestamp("applied_at")),
                displayScene(rs.getString("proposal_type")),
                "/app/host-applications/" + rs.getLong("id")
            ),
            targetUserId
        );
    }

    private RelatedCardInfo loadRelatedCard(long noteId, String relatedCardType, Long relatedCardId, long viewerUserId) {
        if (!RELATED_CARD_TYPE_APPLICATION.equals(relatedCardType) || relatedCardId == null) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject(
                """
                    SELECT pa.id,
                           pa.proposal_id,
                           pa.applied_at,
                           p.proposal_type,
                           p.title,
                           p.location_name,
                           ct.closed_at
                    FROM proposal_applications pa
                    JOIN proposals p ON p.id = pa.proposal_id
                    LEFT JOIN chat_threads ct ON ct.id = pa.related_thread_id AND ct.deleted_at IS NULL
                    WHERE pa.id = ?
                      AND pa.deleted_at IS NULL
                      AND p.deleted_at IS NULL
                    """,
                (rs, rowNum) -> {
                    Timestamp visitTimestamp = rs.getTimestamp("closed_at");
                    if (visitTimestamp == null) {
                        visitTimestamp = rs.getTimestamp("applied_at");
                    }
                    return new RelatedCardInfo(
                        RELATED_CARD_TYPE_APPLICATION,
                        rs.getLong("id"),
                        buildRelatedCardLabel(
                            rs.getString("proposal_type"),
                            rs.getString("title"),
                            rs.getString("location_name"),
                            rs.getTimestamp("applied_at")
                        ),
                        formatDate(visitTimestamp),
                        displayScene(rs.getString("proposal_type")),
                        "/app/support-notes/" + noteId + "/related-card",
                        canViewProposalDetail(viewerUserId, rs.getLong("proposal_id"))
                            ? proposalDetailPath(rs.getString("proposal_type"), rs.getLong("proposal_id"))
                            : null
                    );
                },
                relatedCardId
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private SupportNoteRelatedCardPageData loadRelatedCardContext(
        long noteId,
        long relatedCardId,
        long viewerUserId,
        TargetUser target,
        AccessScope access
    ) {
        try {
            return jdbcTemplate.queryForObject(
                """
                    SELECT pa.id,
                           pa.proposal_id,
                           pa.application_status,
                           pa.applied_at,
                           p.proposal_type,
                           p.title,
                           p.summary,
                           p.location_name,
                           p.duration_minutes,
                           host.nickname AS host_nickname,
                           bridge.nickname AS bridge_nickname,
                           applicant.nickname AS applicant_nickname,
                           ct.status AS thread_status,
                           ct.closed_at
                    FROM proposal_applications pa
                    JOIN proposals p ON p.id = pa.proposal_id
                    JOIN users applicant ON applicant.id = pa.applicant_user_id
                    JOIN users host ON host.id = p.host_user_id
                    LEFT JOIN users bridge ON bridge.id = p.bridge_user_id
                    LEFT JOIN chat_threads ct ON ct.id = pa.related_thread_id AND ct.deleted_at IS NULL
                    WHERE pa.id = ?
                      AND pa.deleted_at IS NULL
                      AND p.deleted_at IS NULL
                """,
                (rs, rowNum) -> {
                    Timestamp visitTimestamp = rs.getTimestamp("closed_at");
                    if (visitTimestamp == null) {
                        visitTimestamp = rs.getTimestamp("applied_at");
                    }
                    String proposalType = rs.getString("proposal_type");
                    long proposalId = rs.getLong("proposal_id");
                    return new SupportNoteRelatedCardPageData(
                        noteId,
                        relatedCardId,
                        target.userId(),
                        target.displayName(),
                        buildRelatedCardLabel(
                            proposalType,
                            rs.getString("title"),
                            rs.getString("location_name"),
                            rs.getTimestamp("applied_at")
                        ),
                        displayScene(proposalType),
                        formatDate(visitTimestamp),
                        nullableText(rs.getString("title")),
                        nullableText(rs.getString("summary")),
                        nullableText(rs.getString("location_name")),
                        rs.getObject("duration_minutes", Integer.class),
                        displayName(rs.getString("applicant_nickname"), null),
                        displayName(rs.getString("host_nickname"), null),
                        displayName(rs.getString("bridge_nickname"), null),
                        access.admin() ? "管理者として閲覧できる範囲の関連記録です。"
                            : access.hostViewer() ? "現在担当中の利用者に関する関連記録です。閲覧できる範囲のみ表示しています。"
                            : "閲覧できる範囲のみ関連記録を表示しています。",
                        canViewProposalDetail(viewerUserId, proposalId) ? proposalDetailPath(proposalType, proposalId) : null,
                        "/app/support-notes/" + noteId,
                        "/app/admin/users/" + target.userId()
                    );
                },
                relatedCardId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Related card not found: " + relatedCardId, ex);
        }
    }

    private Viewer requireViewer(String email) {
        try {
            return jdbcTemplate.queryForObject(
                """
                    SELECT u.id,
                           EXISTS (
                               SELECT 1 FROM user_roles ur
                               WHERE ur.user_id = u.id AND ur.role_name = 'ADMIN'
                           ) AS admin_viewer
                    FROM users u
                    WHERE u.email = ?
                    """,
                (rs, rowNum) -> new Viewer(rs.getLong("id"), rs.getBoolean("admin_viewer")),
                email
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("User not found: " + email, ex);
        }
    }

    private TargetUser requireTargetUser(long targetUserId) {
        try {
            return jdbcTemplate.queryForObject(
                """
                    SELECT u.id,
                           u.nickname,
                           u.email,
                           COALESCE(NULLIF(up.region, ''), NULLIF(up.interest_region, ''), 'default') AS region_id
                    FROM users u
                    LEFT JOIN user_profiles up ON up.user_id = u.id
                    WHERE u.id = ?
                    """,
                (rs, rowNum) -> new TargetUser(
                    rs.getLong("id"),
                    displayName(rs.getString("nickname"), rs.getString("email")),
                    rs.getString("region_id")
                ),
                targetUserId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Target user not found: " + targetUserId, ex);
        }
    }

    private SupportNoteRow requireSupportNote(long noteId) {
        try {
            return jdbcTemplate.queryForObject(
                """
                    SELECT n.id,
                           n.target_user_id,
                           n.note_type,
                           n.note_status,
                           n.body,
                           n.care_points,
                           n.visit_companions,
                           n.related_card_type,
                           n.related_card_id,
                           n.is_hidden,
                           n.hidden_reason,
                           n.hidden_at,
                           n.created_by_user_id,
                           creator.nickname AS creator_nickname,
                           creator.email AS creator_email,
                           updater.nickname AS updater_nickname,
                           updater.email AS updater_email,
                           n.created_at,
                           n.updated_at
                    FROM support_notes n
                    JOIN users creator ON creator.id = n.created_by_user_id
                    LEFT JOIN users updater ON updater.id = n.updated_by_user_id
                    WHERE n.id = ?
                    """,
                (rs, rowNum) -> new SupportNoteRow(
                    rs.getLong("id"),
                    rs.getLong("target_user_id"),
                    rs.getString("note_type"),
                    rs.getString("note_status"),
                    rs.getString("body"),
                    rs.getString("care_points"),
                    rs.getString("visit_companions"),
                    rs.getString("related_card_type"),
                    rs.getObject("related_card_id", Long.class),
                    rs.getBoolean("is_hidden"),
                    rs.getString("hidden_reason"),
                    rs.getTimestamp("hidden_at"),
                    rs.getLong("created_by_user_id"),
                    displayName(rs.getString("creator_nickname"), rs.getString("creator_email")),
                    displayName(rs.getString("updater_nickname"), rs.getString("updater_email")),
                    rs.getTimestamp("created_at"),
                    rs.getTimestamp("updated_at")
                ),
                noteId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Support note not found: " + noteId, ex);
        }
    }

    private String normalizeNoteStatus(String raw) {
        String normalized = normalizeOptionalText(raw, 20);
        if (normalized == null) {
            return "active";
        }
        return "archived".equals(normalized) ? "archived" : "active";
    }

    private String normalizeRequiredText(String raw, int maxLength, String fieldName) {
        String normalized = normalizeOptionalText(raw, maxLength);
        if (normalized == null) {
            throw new SupportNoteConflictException(fieldName + " is required");
        }
        return normalized;
    }

    private String normalizeOptionalText(String raw, int maxLength) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private String mergeLegacyVisitCompanions(String body, String visitCompanions) {
        String normalizedBody = body == null ? "" : body.trim();
        String normalizedCompanions = blankToNull(visitCompanions);
        if (normalizedCompanions == null) {
            return normalizedBody;
        }
        if (normalizedBody.contains("来訪時の同伴者:")) {
            return normalizedBody;
        }
        StringBuilder builder = new StringBuilder(normalizedBody);
        if (!normalizedBody.isBlank()) {
            builder.append("\n\n");
        }
        builder.append("来訪時の同伴者:\n").append(normalizedCompanions);
        return builder.toString();
    }

    private String displayNoteStatus(String noteStatus) {
        return "archived".equals(noteStatus) ? "保管" : "有効";
    }

    private String displayName(String nickname, String email) {
        if (nickname != null && !nickname.isBlank()) {
            return nickname;
        }
        return email == null ? "unknown" : email;
    }

    private String summarizeCarePoints(String carePoints) {
        String normalized = blankToNull(carePoints);
        if (normalized == null) {
            return "留意点の記載はまだありません。";
        }
        String oneLine = normalized.replace("\r", " ").replace("\n", " ").trim();
        return oneLine.length() <= 80 ? oneLine : oneLine.substring(0, 80) + "...";
    }

    private String blankToDisplay(String value) {
        return value == null || value.isBlank() ? "未記入" : value;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String nullableText(String value) {
        return blankToNull(value);
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        LocalDateTime time = timestamp.toLocalDateTime();
        return String.format(
            Locale.ROOT,
            "%04d.%02d.%02d %02d:%02d",
            time.getYear(),
            time.getMonthValue(),
            time.getDayOfMonth(),
            time.getHour(),
            time.getMinute()
        );
    }

    private String formatDate(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        LocalDateTime time = timestamp.toLocalDateTime();
        return String.format(Locale.ROOT, "%04d.%02d.%02d", time.getYear(), time.getMonthValue(), time.getDayOfMonth());
    }

    private String displayScene(String proposalType) {
        return "OKATTE".equalsIgnoreCase(proposalType) ? "おかって" : "入り口";
    }

    private boolean canViewProposalDetail(long viewerUserId, long proposalId) {
        return visibilityAccessService.canViewProposal(viewerUserId, proposalId);
    }

    private String proposalDetailPath(String proposalType, long proposalId) {
        if ("OKATTE".equalsIgnoreCase(proposalType)) {
            return "/app/okatte/" + proposalId;
        }
        return "/app/gate/" + proposalId;
    }

    private String normalizeApplicationStatusLabel(String value) {
        if (value == null || value.isBlank()) {
            return "未記録";
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "accepted", "approved" -> "承認";
            case "pending", "submitted", "applied" -> "申請中";
            case "rejected" -> "見合わせ";
            default -> value;
        };
    }

    private String normalizeThreadStatusLabel(String value) {
        if (value == null || value.isBlank()) {
            return "未記録";
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "open" -> "進行中";
            case "completed", "closed" -> "終了";
            case "cancelled" -> "中止";
            default -> value;
        };
    }

    private String buildRelatedCardLabel(String proposalType, String title, String locationName, Timestamp appliedAt) {
        String scene = displayScene(proposalType);
        String date = formatDate(appliedAt);
        StringBuilder builder = new StringBuilder(scene).append("のカード");
        if (title != null && !title.isBlank()) {
            builder.append(" / ").append(title.trim());
        }
        if (locationName != null && !locationName.isBlank()) {
            builder.append(" / ").append(locationName.trim());
        }
        if (date != null) {
            builder.append(" / ").append(date);
        }
        return builder.toString();
    }

    public record SupportNoteListPageData(
        long targetUserId,
        String targetUserLabel,
        String regionId,
        boolean adminView,
        boolean hostView,
        boolean partnerView,
        List<SupportNoteCardItem> items,
        List<CarePointSummaryItem> carePointPreviewItems,
        int totalCarePointCount,
        String carePointsUrl,
        String createUrl,
        String adminDetailUrl
    ) {}

    public record SupportNoteCardItem(
        long noteId,
        String noteStatusLabel,
        String body,
        String carePoints,
        boolean hidden,
        String createdAt,
        String updatedAt,
        String createdByLabel,
        String updatedByLabel,
        int reportCount,
        RelatedCardInfo relatedCard,
        String detailUrl
    ) {}

    public record SupportNoteCarePointsPageData(
        long targetUserId,
        String targetUserLabel,
        List<CarePointSummaryItem> items,
        String listUrl,
        String adminDetailUrl
    ) {}

    public record CarePointSummaryItem(
        long noteId,
        String carePointsSummary,
        String createdAt,
        String createdByLabel,
        String detailUrl
    ) {}

    public record SupportNoteDetailPageData(
        long noteId,
        long targetUserId,
        String targetUserLabel,
        String regionId,
        String noteStatusLabel,
        String noteStatus,
        String body,
        String carePoints,
        boolean hidden,
        String hiddenReason,
        String hiddenAt,
        String createdByLabel,
        String createdAt,
        String updatedByLabel,
        String updatedAt,
        boolean canEdit,
        boolean canModerate,
        boolean canReport,
        RelatedCardInfo relatedCard,
        List<SupportNoteReportItem> reports,
        String editUrl,
        String listUrl,
        String adminDetailUrl
    ) {}

    public record RelatedCardInfo(
        String cardType,
        long cardId,
        String cardLabel,
        String visitDate,
        String relatedScene,
        String cardUrl,
        String proposalDetailUrl
    ) {}

    public record SupportNoteRelatedCardPageData(
        long noteId,
        long relatedCardId,
        long targetUserId,
        String targetUserLabel,
        String cardLabel,
        String relatedScene,
        String visitDate,
        String title,
        String summary,
        String locationName,
        Integer durationMinutes,
        String applicantLabel,
        String hostLabel,
        String bridgeLabel,
        String accessNote,
        String proposalDetailUrl,
        String noteDetailUrl,
        String adminDetailUrl
    ) {}

    public record RelatedCardCandidateItem(
        long cardId,
        String cardLabel,
        String visitDate,
        String relatedScene,
        String cardUrl
    ) {}

    public record SupportNoteReportItem(long reportId, String reportNote, String reportedAt, String reportedByLabel) {}

    public record SupportNoteEditorPageData(
        String pageTitle,
        long targetUserId,
        String targetUserLabel,
        SupportNoteForm form,
        List<RelatedCardCandidateItem> relatedCardCandidates,
        String submitUrl,
        String backUrl
    ) {}

    private record Viewer(long userId, boolean admin) {}
    private record TargetUser(long userId, String displayName, String regionId) {}
    private record AccessScope(boolean admin, boolean hostViewer, boolean partnerViewer) {}
    private record SupportNotePayload(String noteStatus, String body, String carePoints, String relatedCardType, Long relatedCardId) {}
    private record SupportNoteRow(
        long noteId,
        long targetUserId,
        String noteType,
        String noteStatus,
        String body,
        String carePoints,
        String visitCompanions,
        String relatedCardType,
        Long relatedCardId,
        boolean hidden,
        String hiddenReason,
        Timestamp hiddenAt,
        long createdByUserId,
        String createdByLabel,
        String updatedByLabel,
        Timestamp createdAt,
        Timestamp updatedAt
    ) {}

    public static final class SupportNoteConflictException extends RuntimeException {
        public SupportNoteConflictException(String message) {
            super(message);
        }
    }
}
