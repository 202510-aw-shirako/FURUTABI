package com.furutabi.visibility;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.furutabi.relation.RelatedUserService;

@Service
public class VisibilityAccessService {

    private final JdbcTemplate jdbcTemplate;
    private final RelatedUserService relatedUserService;

    public VisibilityAccessService(JdbcTemplate jdbcTemplate, RelatedUserService relatedUserService) {
        this.jdbcTemplate = jdbcTemplate;
        this.relatedUserService = relatedUserService;
    }

    public boolean canViewProposal(Long viewerUserId, long proposalId) {
        ProposalVisibilityRow row = loadProposalVisibility(proposalId);
        return canView(row.scope(), viewerUserId, row.ownerUserId(), relatedUserService.isProposalParty(asPrimitive(viewerUserId), proposalId));
    }

    public boolean canViewMapRecord(Long viewerUserId, long mapRecordId) {
        MapRecordVisibilityRow row = loadMapRecordVisibility(mapRecordId);
        return canView(row.scope(), viewerUserId, row.ownerUserId(), false);
    }

    public boolean canView(VisibilityScope scope, Long viewerUserId, long ownerUserId, boolean isRelatedUser) {
        if (scope == VisibilityScope.PUBLIC) {
            return true;
        }

        if (viewerUserId == null) {
            return false;
        }

        boolean isOwner = viewerUserId == ownerUserId;
        if (isOwner) {
            return true;
        }

        if (scope == VisibilityScope.PRIVATE) {
            return false;
        }

        return isRelatedUser;
    }

    private ProposalVisibilityRow loadProposalVisibility(long proposalId) {
        try {
            return jdbcTemplate.queryForObject(
                """
                    SELECT host_user_id, visibility_scope
                    FROM proposals
                    WHERE id = ? AND deleted_at IS NULL
                    """,
                (rs, rowNum) -> new ProposalVisibilityRow(
                    rs.getLong("host_user_id"),
                    VisibilityScope.fromDbValue(rs.getString("visibility_scope"))
                ),
                proposalId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Proposal not found for visibility check: " + proposalId, ex);
        }
    }

    private MapRecordVisibilityRow loadMapRecordVisibility(long mapRecordId) {
        try {
            return jdbcTemplate.queryForObject(
                """
                    SELECT user_id, visibility
                    FROM map_records
                    WHERE id = ? AND deleted_at IS NULL
                    """,
                (rs, rowNum) -> new MapRecordVisibilityRow(
                    rs.getLong("user_id"),
                    VisibilityScope.fromDbValue(rs.getString("visibility"))
                ),
                mapRecordId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Map record not found for visibility check: " + mapRecordId, ex);
        }
    }

    private long asPrimitive(Long viewerUserId) {
        return viewerUserId == null ? -1L : viewerUserId;
    }

    private record ProposalVisibilityRow(long ownerUserId, VisibilityScope scope) {
    }

    private record MapRecordVisibilityRow(long ownerUserId, VisibilityScope scope) {
    }
}
