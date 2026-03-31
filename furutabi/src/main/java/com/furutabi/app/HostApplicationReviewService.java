package com.furutabi.app;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.furutabi.relation.RelatedUserService;

@Service
public class HostApplicationReviewService {

    private final JdbcTemplate jdbcTemplate;
    private final RelatedUserService relatedUserService;
    private final ApplicationChatThreadService applicationChatThreadService;
    private final NotificationCenterService notificationCenterService;

    public HostApplicationReviewService(
        JdbcTemplate jdbcTemplate,
        RelatedUserService relatedUserService,
        ApplicationChatThreadService applicationChatThreadService,
        NotificationCenterService notificationCenterService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.relatedUserService = relatedUserService;
        this.applicationChatThreadService = applicationChatThreadService;
        this.notificationCenterService = notificationCenterService;
    }

    public BridgeApplicationListPageData loadPendingApplications(String email) {
        long reviewerUserId = requireUserIdByEmail(email);

        List<BridgeApplicationSummary> items = jdbcTemplate.query(
            """
                SELECT pa.id, pa.proposal_id, pa.application_status, pa.latest_message_preview, pa.applied_at,
                       p.proposal_type,
                       p.title, p.location_name, p.duration_minutes,
                       applicant.nickname AS applicant_nickname,
                       host.nickname AS host_nickname
                FROM proposal_applications pa
                JOIN proposals p ON p.id = pa.proposal_id
                JOIN users applicant ON applicant.id = pa.applicant_user_id
                JOIN users host ON host.id = p.host_user_id
                WHERE pa.deleted_at IS NULL
                  AND p.deleted_at IS NULL
                  AND p.host_user_id = ?
                  AND LOWER(pa.application_status) = 'pending'
                ORDER BY pa.applied_at DESC, pa.id DESC
                """,
            (rs, rowNum) -> new BridgeApplicationSummary(
                rs.getLong("id"),
                rs.getLong("proposal_id"),
                rs.getString("proposal_type"),
                rs.getString("title"),
                rs.getString("applicant_nickname"),
                rs.getString("host_nickname"),
                rs.getString("location_name"),
                rs.getObject("duration_minutes", Integer.class),
                rs.getString("application_status"),
                rs.getString("latest_message_preview"),
                rs.getTimestamp("applied_at") == null ? null : rs.getTimestamp("applied_at").toLocalDateTime(),
                proposalDetailPath(rs.getString("proposal_type"), rs.getLong("proposal_id"))
            ),
            reviewerUserId
        );
        return new BridgeApplicationListPageData(items);
    }

    public BridgeApplicationDetailPageData loadApplicationDetail(String email, long applicationId) {
        long reviewerUserId = requireUserIdByEmail(email);

        BridgeApplicationDetailRow row;
        try {
            row = jdbcTemplate.queryForObject(
                """
                    SELECT pa.id, pa.proposal_id, pa.application_status, pa.latest_message_preview, pa.applied_at,
                           pa.related_thread_id, pa.applicant_user_id,
                           p.title, p.summary, p.location_name, p.duration_minutes, p.bridge_user_id,
                           p.proposal_type,
                           ct.status AS thread_status,
                           applicant.nickname AS applicant_nickname,
                           host.nickname AS host_nickname
                    FROM proposal_applications pa
                    JOIN proposals p ON p.id = pa.proposal_id
                    JOIN users applicant ON applicant.id = pa.applicant_user_id
                    JOIN users host ON host.id = p.host_user_id
                    LEFT JOIN chat_threads ct ON ct.id = pa.related_thread_id AND ct.deleted_at IS NULL
                    WHERE pa.id = ?
                      AND pa.deleted_at IS NULL
                      AND p.deleted_at IS NULL
                    """,
                (rs, rowNum) -> new BridgeApplicationDetailRow(
                    rs.getLong("id"),
                    rs.getLong("proposal_id"),
                    rs.getString("application_status"),
                    rs.getString("latest_message_preview"),
                    rs.getTimestamp("applied_at") == null ? null : rs.getTimestamp("applied_at").toLocalDateTime(),
                    rs.getObject("related_thread_id", Long.class),
                    rs.getLong("applicant_user_id"),
                    rs.getString("title"),
                    rs.getString("summary"),
                    rs.getString("location_name"),
                    rs.getObject("duration_minutes", Integer.class),
                    rs.getString("proposal_type"),
                    rs.getObject("bridge_user_id", Long.class),
                    rs.getString("thread_status"),
                    rs.getString("applicant_nickname"),
                    rs.getString("host_nickname")
                ),
                applicationId
            );
            row = Objects.requireNonNull(row, "Proposal application not found: " + applicationId);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Proposal application not found: " + applicationId, ex);
        }

        if (!relatedUserService.isProposalOwner(reviewerUserId, row.proposalId())) {
            throw new IllegalStateException("Proposal application is not available to this proposal host.");
        }

        return new BridgeApplicationDetailPageData(
            row.applicationId(),
            row.proposalId(),
            row.proposalType(),
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
            row.relatedThreadId() != null,
            proposalDetailPath(row.proposalType(), row.proposalId()),
            buildSupportNoteUrl(row)
        );
    }

    private String buildSupportNoteUrl(BridgeApplicationDetailRow row) {
        if (!"accepted".equalsIgnoreCase(row.applicationStatus())) {
            return null;
        }
        if (!isClosedThreadStatus(row.threadStatus())) {
            return null;
        }
        return "/app/support-notes/users/" + row.applicantUserId() + "/new?relatedCardId=" + row.applicationId();
    }

    private boolean isClosedThreadStatus(String threadStatus) {
        if (threadStatus == null) {
            return false;
        }
        return "closed".equalsIgnoreCase(threadStatus)
            || "completed".equalsIgnoreCase(threadStatus)
            || "cancelled".equalsIgnoreCase(threadStatus);
    }

    @Transactional
    public void acceptApplication(String email, long applicationId) {
        decide(email, applicationId, "accepted", "Accepted by proposal host.");
    }

    @Transactional
    public void rejectApplication(String email, long applicationId) {
        decide(email, applicationId, "rejected", "Rejected by proposal host.");
    }

    private void decide(String email, long applicationId, String nextStatus, String historyNote) {
        long reviewerUserId = requireUserIdByEmail(email);

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
                    """,
                (rs, rowNum) -> new BridgeApplicationDecisionRow(
                    rs.getLong("id"),
                    rs.getLong("proposal_id"),
                    rs.getString("application_status")
                ),
                applicationId
            );
            row = Objects.requireNonNull(row, "Proposal application not found: " + applicationId);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Proposal application not found: " + applicationId, ex);
        }

        if (!relatedUserService.isProposalOwner(reviewerUserId, row.proposalId())) {
            throw new IllegalStateException("Proposal application is not available to this proposal host.");
        }
        if (!"pending".equalsIgnoreCase(row.applicationStatus())) {
            throw new HostApplicationReviewConflictException("Only pending applications can be reviewed.");
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

        if ("accepted".equalsIgnoreCase(nextStatus)) {
            applicationChatThreadService.ensureOpenThread(applicationId);
        } else {
            applicationChatThreadService.closeThread(applicationId);
        }
        notificationCenterService.notifyApplicationDecision(applicationId, nextStatus);
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

    private String nullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private String proposalDetailPath(String proposalType, long proposalId) {
        if ("OKATTE".equalsIgnoreCase(proposalType)) {
            return "/app/okatte/" + proposalId;
        }
        return "/app/gate/" + proposalId;
    }

    public record BridgeApplicationListPageData(List<BridgeApplicationSummary> items) {
    }

    public record BridgeApplicationSummary(
        long applicationId,
        long proposalId,
        String proposalType,
        String title,
        String applicantNickname,
        String hostNickname,
        String locationName,
        Integer durationMinutes,
        String applicationStatus,
        String latestMessagePreview,
        LocalDateTime appliedAt,
        String proposalDetailPath
    ) {
    }

    public record BridgeApplicationDetailPageData(
        long applicationId,
        long proposalId,
        String proposalType,
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
        boolean chatOpened,
        String proposalDetailPath,
        String supportNoteUrl
    ) {
    }

    private record BridgeApplicationDetailRow(
        long applicationId,
        long proposalId,
        String applicationStatus,
        String latestMessagePreview,
        LocalDateTime appliedAt,
        Long relatedThreadId,
        long applicantUserId,
        String title,
        String summary,
        String locationName,
        Integer durationMinutes,
        String proposalType,
        Long bridgeUserId,
        String threadStatus,
        String applicantNickname,
        String hostNickname
    ) {
    }

    private record BridgeApplicationDecisionRow(long applicationId, long proposalId, String applicationStatus) {
    }

    public static class HostApplicationReviewConflictException extends RuntimeException {
        public HostApplicationReviewConflictException(String message) {
            super(message);
        }
    }
}
