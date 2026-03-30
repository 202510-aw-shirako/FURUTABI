package com.furutabi.admin;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AdminUserManagementRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdminUserManagementRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserRow requireUserByEmail(String email) {
        try {
            return Objects.requireNonNull(
                jdbcTemplate.queryForObject(
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
                ),
                "Admin viewer is required"
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("User not found: " + email, ex);
        }
    }

    public TargetUserRow requireTargetUser(long targetUserId) {
        try {
            return Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                    """
                        SELECT u.id, u.email, u.nickname, u.name,
                               up.region, up.interest_region, up.age_range
                        FROM users u
                        LEFT JOIN user_profiles up ON up.user_id = u.id
                        WHERE u.id = ?
                        """,
                    (rs, rowNum) -> new TargetUserRow(
                        rs.getLong("id"),
                        rs.getString("email"),
                        rs.getString("nickname"),
                        rs.getString("name"),
                        rs.getString("region"),
                        rs.getString("interest_region"),
                        rs.getString("age_range")
                    ),
                    targetUserId
                ),
                "Admin target user is required"
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Target user not found: " + targetUserId, ex);
        }
    }

    public boolean isLegacyAdmin(long userId) {
        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM user_roles
                WHERE user_id = ? AND role_name = 'ADMIN'
                """,
            Integer.class,
            userId
        );
        return count != null && count > 0;
    }

    public CurrentRoleStateRow findCurrentRoleState(long targetUserId, String fallbackRegionId) {
        try {
            return Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                    """
                        SELECT user_id, role_name, role_state, region_id, reason, updated_at
                        FROM user_role_states
                        WHERE user_id = ?
                        """,
                    (rs, rowNum) -> new CurrentRoleStateRow(
                        rs.getLong("user_id"),
                        rs.getString("role_name"),
                        rs.getString("role_state"),
                        rs.getString("region_id"),
                        rs.getString("reason"),
                        rs.getTimestamp("updated_at")
                    ),
                    targetUserId
                ),
                "Current role state is required"
            );
        } catch (EmptyResultDataAccessException ex) {
            return new CurrentRoleStateRow(
                targetUserId,
                AdminRoleCatalog.canonicalRoleName(findHighestLegacyRoleName(targetUserId)),
                "active",
                fallbackRegionId,
                null,
                null
            );
        }
    }

    public RolePolicyRow findRolePolicy(String regionId, String roleName) {
        List<RolePolicyRow> rows = jdbcTemplate.query(
            """
                SELECT region_id, role_name, role_assignable, default_host_permission, default_partner_permission,
                       individual_host_permission_grant_allowed, individual_partner_permission_grant_allowed,
                       admin_approval_required_for_pause, admin_approval_required_for_withdrawal,
                       admin_approval_required_for_role_restore
                FROM role_policies
                WHERE role_name = ?
                  AND region_id IN (?, 'default')
                ORDER BY CASE WHEN region_id = ? THEN 0 ELSE 1 END
                """,
            (rs, rowNum) -> new RolePolicyRow(
                rs.getString("region_id"),
                rs.getString("role_name"),
                rs.getBoolean("role_assignable"),
                rs.getBoolean("default_host_permission"),
                rs.getBoolean("default_partner_permission"),
                rs.getBoolean("individual_host_permission_grant_allowed"),
                rs.getBoolean("individual_partner_permission_grant_allowed"),
                rs.getBoolean("admin_approval_required_for_pause"),
                rs.getBoolean("admin_approval_required_for_withdrawal"),
                rs.getBoolean("admin_approval_required_for_role_restore")
            ),
            roleName,
            regionId,
            regionId
        );
        if (!rows.isEmpty()) {
            return rows.get(0);
        }
        String normalizedRole = AdminRoleCatalog.normalize(roleName);
        return new RolePolicyRow(
            regionId,
            normalizedRole,
            true,
            AdminRoleCatalog.defaultHostPermission(normalizedRole),
            AdminRoleCatalog.defaultPartnerPermission(normalizedRole),
            AdminRoleCatalog.defaultIndividualHostGrantAllowed(normalizedRole),
            AdminRoleCatalog.defaultIndividualPartnerGrantAllowed(normalizedRole),
            false,
            false,
            false
        );
    }

    public PermissionRuleRow findPermissionRule(String regionId, long targetUserId, String permissionName) {
        try {
            return Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                    """
                        SELECT id, region_id, target_user_id, permission_name, rule_state,
                               effective_from, effective_to,
                               granted_at, granted_by_user_id, grant_reason,
                               suspended_at, suspended_by_user_id, suspension_reason,
                               resumed_at, resumed_by_user_id, resume_reason,
                               revoked_at, revoked_by_user_id, revoke_reason
                        FROM permission_rules
                        WHERE region_id = ?
                          AND target_user_id = ?
                          AND permission_name = ?
                        """,
                    (rs, rowNum) -> new PermissionRuleRow(
                        rs.getLong("id"),
                        rs.getString("region_id"),
                        rs.getLong("target_user_id"),
                        rs.getString("permission_name"),
                        rs.getString("rule_state"),
                        rs.getDate("effective_from"),
                        rs.getDate("effective_to"),
                        rs.getTimestamp("granted_at"),
                        getNullableLong(rs, "granted_by_user_id"),
                        rs.getString("grant_reason"),
                        rs.getTimestamp("suspended_at"),
                        getNullableLong(rs, "suspended_by_user_id"),
                        rs.getString("suspension_reason"),
                        rs.getTimestamp("resumed_at"),
                        getNullableLong(rs, "resumed_by_user_id"),
                        rs.getString("resume_reason"),
                        rs.getTimestamp("revoked_at"),
                        getNullableLong(rs, "revoked_by_user_id"),
                        rs.getString("revoke_reason")
                    ),
                    regionId,
                    targetUserId,
                    permissionName
                ),
                "Permission rule is required"
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public long upsertPermissionRule(
        String regionId,
        long targetUserId,
        String permissionName,
        String ruleState,
        Date effectiveFrom,
        Date effectiveTo,
        Long actorUserId,
        String reason,
        Timestamp changedAt
    ) {
        PermissionRuleRow existing = findPermissionRule(regionId, targetUserId, permissionName);
        if (existing == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                var statement = connection.prepareStatement(
                    """
                        INSERT INTO permission_rules (
                            region_id, target_user_id, permission_name, rule_state,
                            effective_from, effective_to,
                            granted_at, granted_by_user_id, grant_reason,
                            created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                    new String[] {"id"}
                );
                statement.setString(1, regionId);
                statement.setLong(2, targetUserId);
                statement.setString(3, permissionName);
                statement.setString(4, ruleState);
                statement.setDate(5, effectiveFrom);
                statement.setDate(6, effectiveTo);
                statement.setTimestamp(7, changedAt);
                setNullableLong(statement, 8, actorUserId);
                statement.setString(9, reason);
                statement.setTimestamp(10, changedAt);
                statement.setTimestamp(11, changedAt);
                return statement;
            }, keyHolder);
            return keyHolder.getKey() == null ? 0L : keyHolder.getKey().longValue();
        }

        jdbcTemplate.update(
            """
                UPDATE permission_rules
                SET rule_state = ?,
                    effective_from = ?,
                    effective_to = ?,
                    granted_at = CASE WHEN ? = 'active' THEN ? ELSE granted_at END,
                    granted_by_user_id = CASE WHEN ? = 'active' THEN ? ELSE granted_by_user_id END,
                    grant_reason = CASE WHEN ? = 'active' THEN ? ELSE grant_reason END,
                    suspended_at = CASE WHEN ? = 'suspended' THEN ? ELSE suspended_at END,
                    suspended_by_user_id = CASE WHEN ? = 'suspended' THEN ? ELSE suspended_by_user_id END,
                    suspension_reason = CASE WHEN ? = 'suspended' THEN ? ELSE suspension_reason END,
                    resumed_at = CASE WHEN ? = 'active' THEN ? ELSE resumed_at END,
                    resumed_by_user_id = CASE WHEN ? = 'active' THEN ? ELSE resumed_by_user_id END,
                    resume_reason = CASE WHEN ? = 'active' THEN ? ELSE resume_reason END,
                    revoked_at = CASE WHEN ? = 'revoked' THEN ? ELSE revoked_at END,
                    revoked_by_user_id = CASE WHEN ? = 'revoked' THEN ? ELSE revoked_by_user_id END,
                    revoke_reason = CASE WHEN ? = 'revoked' THEN ? ELSE revoke_reason END,
                    updated_at = ?
                WHERE id = ?
                """,
            ruleState,
            effectiveFrom,
            effectiveTo,
            ruleState, changedAt, ruleState, actorUserId, ruleState, reason,
            ruleState, changedAt, ruleState, actorUserId, ruleState, reason,
            ruleState, changedAt, ruleState, actorUserId, ruleState, reason,
            ruleState, changedAt, ruleState, actorUserId, ruleState, reason,
            changedAt,
            existing.permissionRuleId()
        );
        return existing.permissionRuleId();
    }

    public List<PartnerAssignmentRow> listPartnerAssignments(long targetUserId) {
        return jdbcTemplate.query(
            """
                SELECT pa.id, pa.region_id, pa.user_id, pa.partner_user_id, pa.assignment_status,
                       pa.effective_from, pa.effective_to, pa.assigned_at, pa.ended_at,
                       partner.nickname AS partner_nickname, partner.name AS partner_name, partner.email AS partner_email,
                       assigned_by.nickname AS assigned_by_nickname, assigned_by.name AS assigned_by_name, assigned_by.email AS assigned_by_email
                FROM partner_assignments pa
                LEFT JOIN users partner ON partner.id = pa.partner_user_id
                LEFT JOIN users assigned_by ON assigned_by.id = pa.assigned_by_user_id
                WHERE pa.user_id = ?
                ORDER BY CASE pa.assignment_status
                    WHEN 'active' THEN 0
                    WHEN 'paused' THEN 1
                    ELSE 2
                END, pa.assigned_at DESC, pa.id DESC
                """,
            (rs, rowNum) -> new PartnerAssignmentRow(
                rs.getLong("id"),
                rs.getString("region_id"),
                rs.getLong("user_id"),
                rs.getLong("partner_user_id"),
                rs.getString("assignment_status"),
                rs.getDate("effective_from"),
                rs.getDate("effective_to"),
                rs.getTimestamp("assigned_at"),
                rs.getTimestamp("ended_at"),
                displayName(rs.getString("partner_nickname"), rs.getString("partner_name"), rs.getString("partner_email")),
                displayName(rs.getString("assigned_by_nickname"), rs.getString("assigned_by_name"), rs.getString("assigned_by_email"))
            ),
            targetUserId
        );
    }

    public long createPartnerAssignment(
        String regionId,
        long targetUserId,
        long partnerUserId,
        Date effectiveFrom,
        Date effectiveTo,
        long actorUserId,
        String reason,
        Timestamp changedAt
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                """
                    INSERT INTO partner_assignments (
                        region_id, user_id, partner_user_id, assignment_status,
                        assigned_at, effective_from, effective_to,
                        assigned_by_user_id, assignment_reason, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                new String[] {"id"}
            );
            statement.setString(1, regionId);
            statement.setLong(2, targetUserId);
            statement.setLong(3, partnerUserId);
            statement.setString(4, "active");
            statement.setTimestamp(5, changedAt);
            statement.setDate(6, effectiveFrom);
            statement.setDate(7, effectiveTo);
            statement.setLong(8, actorUserId);
            statement.setString(9, reason);
            statement.setTimestamp(10, changedAt);
            statement.setTimestamp(11, changedAt);
            return statement;
        }, keyHolder);
        return keyHolder.getKey() == null ? 0L : keyHolder.getKey().longValue();
    }

    public PartnerAssignmentRow requirePartnerAssignment(long assignmentId) {
        try {
            return Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                    """
                        SELECT id, region_id, user_id, partner_user_id, assignment_status,
                               effective_from, effective_to, assigned_at, ended_at
                        FROM partner_assignments
                        WHERE id = ?
                        """,
                    (rs, rowNum) -> new PartnerAssignmentRow(
                        rs.getLong("id"),
                        rs.getString("region_id"),
                        rs.getLong("user_id"),
                        rs.getLong("partner_user_id"),
                        rs.getString("assignment_status"),
                        rs.getDate("effective_from"),
                        rs.getDate("effective_to"),
                        rs.getTimestamp("assigned_at"),
                        rs.getTimestamp("ended_at"),
                        null,
                        null
                    ),
                    assignmentId
                ),
                "Partner assignment is required"
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Partner assignment not found: " + assignmentId, ex);
        }
    }

    public void updatePartnerAssignmentStatus(long assignmentId, String nextStatus, long actorUserId, String reason, Timestamp changedAt) {
        jdbcTemplate.update(
            """
                UPDATE partner_assignments
                SET assignment_status = ?,
                    paused_at = CASE WHEN ? = 'paused' THEN ? ELSE paused_at END,
                    paused_by_user_id = CASE WHEN ? = 'paused' THEN ? ELSE paused_by_user_id END,
                    pause_reason = CASE WHEN ? = 'paused' THEN ? ELSE pause_reason END,
                    resumed_at = CASE WHEN ? = 'active' THEN ? ELSE resumed_at END,
                    resumed_by_user_id = CASE WHEN ? = 'active' THEN ? ELSE resumed_by_user_id END,
                    resume_reason = CASE WHEN ? = 'active' THEN ? ELSE resume_reason END,
                    ended_at = CASE WHEN ? = 'ended' THEN ? ELSE ended_at END,
                    ended_by_user_id = CASE WHEN ? = 'ended' THEN ? ELSE ended_by_user_id END,
                    end_reason = CASE WHEN ? = 'ended' THEN ? ELSE end_reason END,
                    updated_at = ?
                WHERE id = ?
                """,
            nextStatus,
            nextStatus, changedAt, nextStatus, actorUserId, nextStatus, reason,
            nextStatus, changedAt, nextStatus, actorUserId, nextStatus, reason,
            nextStatus, changedAt, nextStatus, actorUserId, nextStatus, reason,
            changedAt,
            assignmentId
        );
    }

    public void upsertRoleState(
        long targetUserId,
        String roleName,
        String roleState,
        String regionId,
        String reason,
        Long actorUserId,
        Timestamp changedAt
    ) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_role_states WHERE user_id = ?",
            Integer.class,
            targetUserId
        );
        if (count != null && count > 0) {
            jdbcTemplate.update(
                """
                    UPDATE user_role_states
                    SET role_name = ?, role_state = ?, region_id = ?, reason = ?,
                        changed_by_user_id = ?, changed_at = ?, updated_at = ?
                    WHERE user_id = ?
                    """,
                roleName,
                roleState,
                regionId,
                reason,
                actorUserId,
                changedAt,
                changedAt,
                targetUserId
            );
            return;
        }

        jdbcTemplate.update(
            """
                INSERT INTO user_role_states (
                    user_id, role_name, role_state, region_id, reason,
                    changed_by_user_id, changed_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            targetUserId,
            roleName,
            roleState,
            regionId,
            reason,
            actorUserId,
            changedAt,
            changedAt,
            changedAt
        );
    }

    public long insertPermissionChangeLog(
        String regionId,
        long targetUserId,
        String changedObjectType,
        String changedObjectName,
        String actionType,
        String beforeValue,
        String afterValue,
        long changedByUserId,
        String reason,
        Timestamp changedAt
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                """
                    INSERT INTO permission_change_logs (
                        region_id, target_user_id, changed_object_type, changed_object_name, action_type,
                        before_value, after_value, changed_by_user_id, reason, changed_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                new String[] {"id"}
            );
            statement.setString(1, regionId);
            statement.setLong(2, targetUserId);
            statement.setString(3, changedObjectType);
            statement.setString(4, changedObjectName);
            statement.setString(5, actionType);
            statement.setString(6, beforeValue);
            statement.setString(7, afterValue);
            statement.setLong(8, changedByUserId);
            statement.setString(9, reason);
            statement.setTimestamp(10, changedAt);
            return statement;
        }, keyHolder);
        return keyHolder.getKey() == null ? 0L : keyHolder.getKey().longValue();
    }

    public List<PermissionChangeLogRow> listPermissionChangeLogs(long targetUserId) {
        return jdbcTemplate.query(
            """
                SELECT pcl.id, pcl.region_id, pcl.changed_object_type, pcl.changed_object_name,
                       pcl.action_type, pcl.before_value, pcl.after_value, pcl.reason, pcl.changed_at,
                       actor.nickname AS actor_nickname, actor.name AS actor_name, actor.email AS actor_email
                FROM permission_change_logs pcl
                LEFT JOIN users actor ON actor.id = pcl.changed_by_user_id
                WHERE pcl.target_user_id = ?
                ORDER BY pcl.changed_at DESC, pcl.id DESC
                """,
            (rs, rowNum) -> new PermissionChangeLogRow(
                rs.getLong("id"),
                rs.getString("region_id"),
                rs.getString("changed_object_type"),
                rs.getString("changed_object_name"),
                rs.getString("action_type"),
                rs.getString("before_value"),
                rs.getString("after_value"),
                rs.getString("reason"),
                rs.getTimestamp("changed_at"),
                displayName(rs.getString("actor_nickname"), rs.getString("actor_name"), rs.getString("actor_email"))
            ),
            targetUserId
        );
    }

    public long insertAccessLog(long viewerUserId, String viewerContext, String targetType, long targetId, String viewReason, Timestamp viewedAt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                """
                    INSERT INTO access_logs (
                        viewer_user_id, viewer_context, target_type, target_id, view_reason, viewed_at
                    ) VALUES (?, ?, ?, ?, ?, ?)
                    """,
                new String[] {"id"}
            );
            statement.setLong(1, viewerUserId);
            statement.setString(2, viewerContext);
            statement.setString(3, targetType);
            statement.setLong(4, targetId);
            statement.setString(5, viewReason);
            statement.setTimestamp(6, viewedAt);
            return statement;
        }, keyHolder);
        return keyHolder.getKey() == null ? 0L : keyHolder.getKey().longValue();
    }

    public List<AccessLogRow> listAccessLogs(long targetUserId) {
        return jdbcTemplate.query(
            """
                SELECT al.id, al.viewer_context, al.target_type, al.target_id, al.view_reason, al.viewed_at,
                       viewer.nickname AS viewer_nickname, viewer.name AS viewer_name, viewer.email AS viewer_email
                FROM access_logs al
                LEFT JOIN users viewer ON viewer.id = al.viewer_user_id
                WHERE al.target_type = 'admin_user_detail'
                  AND al.target_id = ?
                ORDER BY al.viewed_at DESC, al.id DESC
                """,
            (rs, rowNum) -> new AccessLogRow(
                rs.getLong("id"),
                rs.getString("viewer_context"),
                rs.getString("target_type"),
                rs.getLong("target_id"),
                rs.getString("view_reason"),
                rs.getTimestamp("viewed_at"),
                displayName(rs.getString("viewer_nickname"), rs.getString("viewer_name"), rs.getString("viewer_email"))
            ),
            targetUserId
        );
    }

    public ProposalApplicationSummaryRow loadProposalApplicationSummary(long targetUserId) {
        Integer hostProposalCount = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM proposals
                WHERE host_user_id = ?
                  AND deleted_at IS NULL
                  AND proposal_type <> 'OKATTE'
                """,
            Integer.class,
            targetUserId
        );
        Integer bridgeProposalCount = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM proposals
                WHERE bridge_user_id = ?
                  AND deleted_at IS NULL
                  AND proposal_type <> 'OKATTE'
                """,
            Integer.class,
            targetUserId
        );
        Integer okatteCandidateCount = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM proposals
                WHERE bridge_user_id = ?
                  AND deleted_at IS NULL
                  AND proposal_type = 'OKATTE'
                """,
            Integer.class,
            targetUserId
        );
        Integer applicationCount = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM proposal_applications
                WHERE applicant_user_id = ?
                  AND deleted_at IS NULL
                """,
            Integer.class,
            targetUserId
        );
        return new ProposalApplicationSummaryRow(
            hostProposalCount == null ? 0 : hostProposalCount,
            bridgeProposalCount == null ? 0 : bridgeProposalCount,
            okatteCandidateCount == null ? 0 : okatteCandidateCount,
            applicationCount == null ? 0 : applicationCount
        );
    }

    public RegionSettingSummaryRow loadRegionSettingSummary(String regionId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM region_scoped_settings WHERE region_id = ?",
            Integer.class,
            regionId
        );
        return new RegionSettingSummaryRow(regionId, count == null ? 0 : count);
    }

    public String findHighestLegacyRoleName(long targetUserId) {
        List<String> roleNames = jdbcTemplate.query(
            """
                SELECT role_name
                FROM user_roles
                WHERE user_id = ?
                ORDER BY CASE role_name
                    WHEN 'ADMIN' THEN 0
                    WHEN 'BRIDGE' THEN 1
                    WHEN 'LOCAL' THEN 2
                    ELSE 3
                END, id ASC
                """,
            (rs, rowNum) -> rs.getString("role_name"),
            targetUserId
        );
        return roleNames.isEmpty() ? "USER" : roleNames.get(0);
    }

    private static void setNullableLong(java.sql.PreparedStatement statement, int index, Long value) throws java.sql.SQLException {
        if (value == null) {
            statement.setObject(index, null);
        } else {
            statement.setLong(index, value);
        }
    }

    private Long getNullableLong(java.sql.ResultSet resultSet, String columnName) throws java.sql.SQLException {
        long value = resultSet.getLong(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private String displayName(String nickname, String name, String email) {
        if (nickname != null && !nickname.isBlank()) {
            return nickname;
        }
        if (name != null && !name.isBlank()) {
            return name;
        }
        return email == null ? "unknown" : email;
    }

    public record UserRow(long userId, String email, String nickname, String name) {}
    public record TargetUserRow(long userId, String email, String nickname, String name, String region, String interestRegion, String ageRange) {}
    public record CurrentRoleStateRow(long userId, String roleName, String roleState, String regionId, String reason, Timestamp updatedAt) {}
    public record RolePolicyRow(String regionId, String roleName, boolean roleAssignable, boolean defaultHostPermission, boolean defaultPartnerPermission, boolean individualHostPermissionGrantAllowed, boolean individualPartnerPermissionGrantAllowed, boolean adminApprovalRequiredForPause, boolean adminApprovalRequiredForWithdrawal, boolean adminApprovalRequiredForRoleRestore) {}
    public record PermissionRuleRow(long permissionRuleId, String regionId, long targetUserId, String permissionName, String ruleState, Date effectiveFrom, Date effectiveTo, Timestamp grantedAt, Long grantedByUserId, String grantReason, Timestamp suspendedAt, Long suspendedByUserId, String suspensionReason, Timestamp resumedAt, Long resumedByUserId, String resumeReason, Timestamp revokedAt, Long revokedByUserId, String revokeReason) {}
    public record PartnerAssignmentRow(long assignmentId, String regionId, long targetUserId, long partnerUserId, String assignmentStatus, Date effectiveFrom, Date effectiveTo, Timestamp assignedAt, Timestamp endedAt, String partnerLabel, String assignedByLabel) {}
    public record PermissionChangeLogRow(long permissionChangeLogId, String regionId, String changedObjectType, String changedObjectName, String actionType, String beforeValue, String afterValue, String reason, Timestamp changedAt, String changedByLabel) {}
    public record AccessLogRow(long accessLogId, String viewerContext, String targetType, long targetId, String viewReason, Timestamp viewedAt, String viewerLabel) {}
    public record ProposalApplicationSummaryRow(int hostProposalCount, int bridgeProposalCount, int okatteCandidateCount, int applicationCount) {}
    public record RegionSettingSummaryRow(String regionId, int settingCount) {}
}
