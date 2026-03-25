package com.furutabi.relation;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RelatedUserService {

    private final JdbcTemplate jdbcTemplate;

    public RelatedUserService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isProposalOwner(long userId, long proposalId) {
        return exists(
            """
                SELECT COUNT(*)
                FROM proposals
                WHERE id = ? AND host_user_id = ? AND deleted_at IS NULL
                """,
            proposalId,
            userId
        );
    }

    public boolean isProposalBridge(long userId, long proposalId) {
        return exists(
            """
                SELECT COUNT(*)
                FROM proposals
                WHERE id = ? AND bridge_user_id = ? AND deleted_at IS NULL
                """,
            proposalId,
            userId
        );
    }

    public boolean isProposalApplicant(long userId, long proposalId) {
        return exists(
            """
                SELECT COUNT(*)
                FROM proposal_applications
                WHERE proposal_id = ? AND applicant_user_id = ? AND deleted_at IS NULL
                """,
            proposalId,
            userId
        );
    }

    public boolean isProposalParty(long userId, long proposalId) {
        return isProposalOwner(userId, proposalId)
            || isProposalBridge(userId, proposalId)
            || isProposalApplicant(userId, proposalId);
    }

    public boolean isProposalApplicationParty(long userId, long applicationId) {
        return exists(
            """
                SELECT COUNT(*)
                FROM proposal_applications pa
                JOIN proposals p ON p.id = pa.proposal_id
                WHERE pa.id = ?
                  AND pa.deleted_at IS NULL
                  AND p.deleted_at IS NULL
                  AND (
                    pa.applicant_user_id = ?
                    OR p.host_user_id = ?
                    OR p.bridge_user_id = ?
                  )
                """,
            applicationId,
            userId,
            userId,
            userId
        );
    }

    public boolean isChatParticipant(long userId, long threadId) {
        return exists(
            """
                SELECT COUNT(*)
                FROM chat_threads
                WHERE id = ?
                  AND deleted_at IS NULL
                  AND (user_id = ? OR counterpart_id = ?)
                """,
            threadId,
            userId,
            userId
        );
    }

    public boolean isRelatedUser(long userId, String entityType, long entityId) {
        if (entityType == null || entityType.isBlank()) {
            return false;
        }

        return switch (entityType.trim().toUpperCase()) {
            case "PROPOSAL" -> isProposalParty(userId, entityId);
            case "PROPOSAL_APPLICATION" -> isProposalApplicationParty(userId, entityId);
            case "CHAT_THREAD" -> isChatParticipant(userId, entityId);
            default -> false;
        };
    }

    public boolean hasPrivilegedRelationRole(long userId) {
        return exists(
            """
                SELECT COUNT(*)
                FROM user_roles
                WHERE user_id = ? AND role_name IN ('BRIDGE', 'ADMIN')
                """,
            userId
        );
    }

    private boolean exists(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }
}
