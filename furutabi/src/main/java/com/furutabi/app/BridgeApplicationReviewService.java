package com.furutabi.app;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.furutabi.relation.RelatedUserService;

@Service
public class BridgeApplicationReviewService {

    private final JdbcTemplate jdbcTemplate;
    private final RelatedUserService relatedUserService;

    public BridgeApplicationReviewService(JdbcTemplate jdbcTemplate, RelatedUserService relatedUserService) {
        this.jdbcTemplate = jdbcTemplate;
        this.relatedUserService = relatedUserService;
    }

    public BridgeApplicationListPageData loadPendingApplications(String email) {
        long reviewerUserId = requireUserIdByEmail(email);
        if (!hasBridgeRole(reviewerUserId)) {
            return new BridgeApplicationListPageData(List.of());
        }

        List<BridgeApplicationSummary> items = jdbcTemplate.query(
            """
                SELECT pa.id, pa.proposal_id, pa.application_status, pa.latest_message_preview, pa.applied_at,
                       p.title, p.location_name, p.duration_minutes,
                       applicant.nickname AS applicant_nickname,
                       host.nickname AS host_nickname
                FROM proposal_applications pa
                JOIN proposals p ON p.id = pa.proposal_id
                JOIN users applicant ON applicant.id = pa.applicant_user_id
                JOIN users host ON host.id = p.host_user_id
                WHERE pa.deleted_at IS NULL
                  AND p.deleted_at IS NULL
                  AND p.bridge_user_id = ?
                  AND p.proposal_type IN ('LOCAL_GUIDE', 'GATE')
                  AND LOWER(pa.application_status) = 'pending'
                ORDER BY pa.applied_at DESC, pa.id DESC
                """,
            (rs, rowNum) -> new BridgeApplicationSummary(
                rs.getLong("id"),
                rs.getLong("proposal_id"),
                rs.getString("title"),
                rs.getString("applicant_nickname"),
                rs.getString("host_nickname"),
                rs.getString("location_name"),
                rs.getObject("duration_minutes", Integer.class),
                rs.getString("application_status"),
                rs.getString("latest_message_preview"),
                rs.getTimestamp("applied_at") == null ? null : rs.getTimestamp("applied_at").toLocalDateTime()
            ),
            reviewerUserId
        );
        return new BridgeApplicationListPageData(items);
    }

    public BridgeApplicationDetailPageData loadApplicationDetail(String email, long applicationId) {
        long reviewerUserId = requireUserIdByEmail(email);
        if (!hasBridgeRole(reviewerUserId)) {
            throw new IllegalStateException("Bridge review is not available.");
        }

        BridgeApplicationDetailRow row;
        try {
            row = jdbcTemplate.queryForObject(
                """
                    SELECT pa.id, pa.proposal_id, pa.application_status, pa.latest_message_preview, pa.applied_at,
                           pa.related_thread_id, p.title, p.summary, p.location_name, p.duration_minutes,
                           applicant.nickname AS applicant_nickname,
                           host.nickname AS host_nickname
                    FROM proposal_applications pa
                    JOIN proposals p ON p.id = pa.proposal_id
                    JOIN users applicant ON applicant.id = pa.applicant_user_id
                    JOIN users host ON host.id = p.host_user_id
                    WHERE pa.id = ?
                      AND pa.deleted_at IS NULL
                      AND p.deleted_at IS NULL
                      AND p.proposal_type IN ('LOCAL_GUIDE', 'GATE')
                    """,
                (rs, rowNum) -> new BridgeApplicationDetailRow(
                    rs.getLong("id"),
                    rs.getLong("proposal_id"),
                    rs.getString("application_status"),
                    rs.getString("latest_message_preview"),
                    rs.getTimestamp("applied_at") == null ? null : rs.getTimestamp("applied_at").toLocalDateTime(),
                    rs.getObject("related_thread_id", Long.class),
                    rs.getString("title"),
                    rs.getString("summary"),
                    rs.getString("location_name"),
                    rs.getObject("duration_minutes", Integer.class),
                    rs.getString("applicant_nickname"),
                    rs.getString("host_nickname")
                ),
                applicationId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Proposal application not found: " + applicationId, ex);
        }

        if (!relatedUserService.isProposalBridge(reviewerUserId, row.proposalId())) {
            throw new IllegalStateException("Proposal application is not available to this bridge reviewer.");
        }

        return new BridgeApplicationDetailPageData(
            row.applicationId(),
            row.proposalId(),
            row.title(),
            nullableText(row.summary()),
            nullableText(row.locationName()),
            row.durationMinutes(),
            row.applicantNickname(),
            row.hostNickname(),
            row.applicationStatus(),
            nullableText(row.latestMessagePreview()),
            row.appliedAt(),
            "pending".equalsIgnoreCase(row.applicationStatus()),
            row.relatedThreadId() != null
        );
    }

    public void acceptApplication(String email, long applicationId) {
        decide(email, applicationId, "accepted", "Accepted by bridge reviewer.");
    }

    public void rejectApplication(String email, long applicationId) {
        decide(email, applicationId, "rejected", "Rejected by bridge reviewer.");
    }

    private void decide(String email, long applicationId, String nextStatus, String historyNote) {
        long reviewerUserId = requireUserIdByEmail(email);
        if (!hasBridgeRole(reviewerUserId)) {
            throw new IllegalStateException("Bridge review is not available.");
        }

        BridgeApplicationDecisionRow row;
        try {
            row = jdbcTemplate.queryForObject(
                """
                    SELECT pa.id, pa.proposal_id, pa.application_status
                    FROM proposal_applications pa
                    JOIN proposals p ON p.id = pa.proposal_id
                    WHERE pa.id = ?
                      AND pa.deleted_at IS NULL
                      AND p.deleted_at IS NULL
                      AND p.proposal_type IN ('LOCAL_GUIDE', 'GATE')
                    """,
                (rs, rowNum) -> new BridgeApplicationDecisionRow(
                    rs.getLong("id"),
                    rs.getLong("proposal_id"),
                    rs.getString("application_status")
                ),
                applicationId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Proposal application not found: " + applicationId, ex);
        }

        if (!relatedUserService.isProposalBridge(reviewerUserId, row.proposalId())) {
            throw new IllegalStateException("Proposal application is not available to this bridge reviewer.");
        }
        if (!"pending".equalsIgnoreCase(row.applicationStatus())) {
            throw new BridgeApplicationReviewConflictException("Only pending applications can be reviewed.");
        }

        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbcTemplate.update(
            """
                UPDATE proposal_applications
                SET application_status = ?, updated_at = ?
                WHERE id = ? AND deleted_at IS NULL
                """,
            nextStatus,
            now,
            applicationId
        );
        jdbcTemplate.update(
            """
                INSERT INTO proposal_application_status_history (
                    proposal_application_id, status, note, changed_by_user_id, created_at
                ) VALUES (?, ?, ?, ?, ?)
                """,
            applicationId,
            nextStatus,
            historyNote,
            reviewerUserId,
            now
        );
    }

    private long requireUserIdByEmail(String email) {
        try {
            Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?",
                Long.class,
                email
            );
            if (userId == null) {
                throw new IllegalStateException("User not found for email: " + email);
            }
            return userId;
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("User not found for email: " + email, ex);
        }
    }

    private boolean hasBridgeRole(long userId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role_name = 'BRIDGE'",
            Integer.class,
            userId
        );
        return count != null && count > 0;
    }

    private String nullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    public record BridgeApplicationListPageData(List<BridgeApplicationSummary> items) {
    }

    public record BridgeApplicationSummary(
        long applicationId,
        long proposalId,
        String title,
        String applicantNickname,
        String hostNickname,
        String locationName,
        Integer durationMinutes,
        String applicationStatus,
        String latestMessagePreview,
        LocalDateTime appliedAt
    ) {
    }

    public record BridgeApplicationDetailPageData(
        long applicationId,
        long proposalId,
        String title,
        String summary,
        String locationName,
        Integer durationMinutes,
        String applicantNickname,
        String hostNickname,
        String applicationStatus,
        String latestMessagePreview,
        LocalDateTime appliedAt,
        boolean canReview,
        boolean chatOpened
    ) {
    }

    private record BridgeApplicationDetailRow(
        long applicationId,
        long proposalId,
        String applicationStatus,
        String latestMessagePreview,
        LocalDateTime appliedAt,
        Long relatedThreadId,
        String title,
        String summary,
        String locationName,
        Integer durationMinutes,
        String applicantNickname,
        String hostNickname
    ) {
    }

    private record BridgeApplicationDecisionRow(long applicationId, long proposalId, String applicationStatus) {
    }

    public static class BridgeApplicationReviewConflictException extends RuntimeException {
        public BridgeApplicationReviewConflictException(String message) {
            super(message);
        }
    }
}
