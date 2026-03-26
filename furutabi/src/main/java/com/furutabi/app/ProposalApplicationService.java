package com.furutabi.app;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import com.furutabi.visibility.VisibilityAccessService;
import com.furutabi.visibility.VisibilityScope;

@Service
public class ProposalApplicationService {

    private final JdbcTemplate jdbcTemplate;
    private final VisibilityAccessService visibilityAccessService;

    public ProposalApplicationService(JdbcTemplate jdbcTemplate, VisibilityAccessService visibilityAccessService) {
        this.jdbcTemplate = jdbcTemplate;
        this.visibilityAccessService = visibilityAccessService;
    }

    public ProposalApplicationPageData loadApplicationPage(String email, long proposalId) {
        long applicantUserId = requireUserIdByEmail(email);
        GateApplicationTarget target = requireVisibleGateTarget(applicantUserId, proposalId);
        ExistingApplication existingApplication = findExistingApplication(applicantUserId, proposalId);
        String blockingMessage = resolveBlockingMessage(applicantUserId, target, existingApplication);

        return new ProposalApplicationPageData(
            proposalId,
            target.title(),
            nullableText(target.summary()),
            nullableText(target.locationName()),
            target.durationMinutes(),
            nullableText(target.hostNickname()),
            nullableText(target.bridgeNickname()),
            VisibilityScope.fromDbValue(target.visibilityScope()).name(),
            existingApplication == null ? null : existingApplication.applicationStatus(),
            blockingMessage == null,
            blockingMessage
        );
    }

    public ProposalApplicationCreateResult createApplication(String email, long proposalId, ProposalApplicationForm form) {
        long applicantUserId = requireUserIdByEmail(email);
        GateApplicationTarget target = requireVisibleGateTarget(applicantUserId, proposalId);
        ExistingApplication existingApplication = findExistingApplication(applicantUserId, proposalId);
        String blockingMessage = resolveBlockingMessage(applicantUserId, target, existingApplication);
        if (blockingMessage != null) {
            throw new ProposalApplicationConflictException(blockingMessage);
        }

        String applicantMessage = normalizeApplicantMessage(form.getApplicantMessage());
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                """
                    INSERT INTO proposal_applications (
                        proposal_id, applicant_user_id, application_status, latest_message_preview,
                        latest_message_at, related_thread_id, requires_additional_verification,
                        applied_at, updated_at, deleted_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                new String[] {"id"}
            );
            statement.setLong(1, proposalId);
            statement.setLong(2, applicantUserId);
            statement.setString(3, "pending");
            statement.setString(4, applicantMessage);
            statement.setTimestamp(5, applicantMessage == null ? null : now);
            statement.setObject(6, null);
            statement.setBoolean(7, false);
            statement.setTimestamp(8, now);
            statement.setTimestamp(9, now);
            statement.setTimestamp(10, null);
            return statement;
        }, keyHolder);

        Number generatedKey = Objects.requireNonNull(
            keyHolder.getKey(),
            "Proposal application ID was not generated"
        );
        long applicationId = generatedKey.longValue();

        jdbcTemplate.update(
            """
                INSERT INTO proposal_application_status_history (
                    proposal_application_id, status, note, changed_by_user_id, created_at
                ) VALUES (?, ?, ?, ?, ?)
                """,
            applicationId,
            "pending",
            applicantMessage,
            applicantUserId,
            now
        );

        return new ProposalApplicationCreateResult(applicationId, proposalId);
    }

    private GateApplicationTarget requireVisibleGateTarget(long viewerUserId, long proposalId) {
        GateApplicationTarget target;
        try {
            target = jdbcTemplate.queryForObject(
                """
                    SELECT p.id, p.host_user_id, p.bridge_user_id, p.title, p.summary, p.location_name,
                           p.duration_minutes, p.visibility_scope,
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
                (rs, rowNum) -> new GateApplicationTarget(
                    rs.getLong("id"),
                    rs.getLong("host_user_id"),
                    rs.getObject("bridge_user_id", Long.class),
                    rs.getString("title"),
                    rs.getString("summary"),
                    rs.getString("location_name"),
                    rs.getObject("duration_minutes", Integer.class),
                    rs.getString("visibility_scope"),
                    rs.getString("host_nickname"),
                    rs.getString("bridge_nickname")
                ),
                proposalId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Gate proposal not found: " + proposalId, ex);
        }

        if (!visibilityAccessService.canViewProposal(viewerUserId, proposalId)) {
            throw new IllegalStateException("Gate proposal is not visible: " + proposalId);
        }

        return target;
    }

    private ExistingApplication findExistingApplication(long applicantUserId, long proposalId) {
        try {
            return jdbcTemplate.queryForObject(
                """
                    SELECT id, application_status
                    FROM proposal_applications
                    WHERE proposal_id = ?
                      AND applicant_user_id = ?
                      AND deleted_at IS NULL
                    ORDER BY applied_at DESC, id DESC
                    FETCH FIRST 1 ROWS ONLY
                    """,
                (rs, rowNum) -> new ExistingApplication(
                    rs.getLong("id"),
                    rs.getString("application_status")
                ),
                proposalId,
                applicantUserId
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private long requireUserIdByEmail(String email) {
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

    private String normalizeApplicantMessage(String applicantMessage) {
        if (applicantMessage == null || applicantMessage.isBlank()) {
            return null;
        }

        String normalized = applicantMessage.trim();
        if (normalized.length() > 500) {
            throw new ProposalApplicationConflictException("Application message must be 500 characters or fewer.");
        }
        return normalized;
    }

    private String nullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private String resolveBlockingMessage(
        long applicantUserId,
        GateApplicationTarget target,
        ExistingApplication existingApplication
    ) {
        if (applicantUserId == target.hostUserId()) {
            return "You cannot apply to your own gate proposal.";
        }
        if (target.bridgeUserId() != null && applicantUserId == target.bridgeUserId()) {
            return "Bridge-side users cannot apply to the same gate proposal.";
        }
        if (existingApplication != null) {
            return "You already have an application for this gate proposal.";
        }
        return null;
    }

    public record ProposalApplicationPageData(
        long proposalId,
        String title,
        String summary,
        String locationName,
        Integer durationMinutes,
        String hostNickname,
        String bridgeNickname,
        String visibilityLabel,
        String existingApplicationStatus,
        boolean canSubmit,
        String blockingMessage
    ) {
    }

    public record ProposalApplicationCreateResult(long applicationId, long proposalId) {
    }

    private record GateApplicationTarget(
        long proposalId,
        long hostUserId,
        Long bridgeUserId,
        String title,
        String summary,
        String locationName,
        Integer durationMinutes,
        String visibilityScope,
        String hostNickname,
        String bridgeNickname
    ) {
    }

    private record ExistingApplication(long applicationId, String applicationStatus) {
    }

    public static class ProposalApplicationConflictException extends RuntimeException {
        public ProposalApplicationConflictException(String message) {
            super(message);
        }
    }
}
