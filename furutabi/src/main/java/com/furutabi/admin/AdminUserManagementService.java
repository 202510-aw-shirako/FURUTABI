package com.furutabi.admin;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.furutabi.admin.AdminUserManagementRepository.AccessLogRow;
import com.furutabi.admin.AdminUserManagementRepository.CurrentRoleStateRow;
import com.furutabi.admin.AdminUserManagementRepository.PartnerCandidateRow;
import com.furutabi.admin.AdminUserManagementRepository.PartnerAssignmentRow;
import com.furutabi.admin.AdminUserManagementRepository.PermissionChangeLogRow;
import com.furutabi.admin.AdminUserManagementRepository.PermissionRuleRow;
import com.furutabi.admin.AdminUserManagementRepository.ProposalApplicationSummaryRow;
import com.furutabi.admin.AdminUserManagementRepository.RegionSettingSummaryRow;
import com.furutabi.admin.AdminUserManagementRepository.RolePolicyRow;
import com.furutabi.admin.AdminUserManagementRepository.TargetUserRow;
import com.furutabi.admin.AdminUserManagementRepository.UserRow;

@Service
public class AdminUserManagementService {

    private static final DateTimeFormatter PAGE_TIME = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final AdminUserManagementRepository repository;
    private final ObjectMapper objectMapper;

    public AdminUserManagementService(AdminUserManagementRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void requireAdminViewer(String viewerEmail) {
        UserRow viewer = repository.requireUserByEmail(viewerEmail);
        requireAdmin(viewer.userId());
    }

    @Transactional
    public AdminUserDetailPageData loadDetailPage(String viewerEmail, long targetUserId) {
        UserRow viewer = repository.requireUserByEmail(viewerEmail);
        requireAdmin(viewer.userId());

        TargetUserRow target = repository.requireTargetUser(targetUserId);
        String regionId = normalizeRegionId(target.region(), target.interestRegion());
        CurrentRoleStateRow currentRoleState = repository.findCurrentRoleState(targetUserId, regionId);
        String effectiveRegionId = normalizeRegionId(currentRoleState.regionId(), regionId);
        RolePolicyRow rolePolicy = repository.findRolePolicy(effectiveRegionId, currentRoleState.roleName());
        String legacyAuthRoleName = AdminRoleCatalog.canonicalRoleName(repository.findHighestLegacyRoleName(targetUserId));
        List<PermissionItem> permissions = List.of(
            buildPermissionItem(targetUserId, rolePolicy, effectiveRegionId, currentRoleState.roleName(), AdminRoleCatalog.HOST_PERMISSION),
            buildPermissionItem(targetUserId, rolePolicy, effectiveRegionId, currentRoleState.roleName(), AdminRoleCatalog.PARTNER_PERMISSION)
        );
        List<AssignmentItem> assignments = repository.listPartnerAssignments(targetUserId).stream().map(this::toAssignmentItem).toList();
        List<PartnerCandidateItem> partnerCandidates = repository.listPartnerCandidates(targetUserId).stream()
            .map(candidate -> toPartnerCandidateItem(candidate, effectiveRegionId))
            .filter(PartnerCandidateItem::partnerPermissionActive)
            .toList();
        ProposalApplicationSummaryRow proposalSummary = repository.loadProposalApplicationSummary(targetUserId);
        List<PermissionChangeLogItem> permissionChangeLogs = repository.listPermissionChangeLogs(targetUserId).stream().map(this::toPermissionChangeLogItem).toList();
        repository.insertAccessLog(
            viewer.userId(),
            AdminActionContext.ADMIN.name().toLowerCase(),
            "admin_user_detail",
            targetUserId,
            "Open admin detail page",
            Timestamp.from(Instant.now())
        );
        List<AccessLogItem> accessLogs = repository.listAccessLogs(targetUserId).stream().map(this::toAccessLogItem).toList();
        RegionSettingSummaryRow regionSettingSummary = repository.loadRegionSettingSummary(effectiveRegionId);

        return new AdminUserDetailPageData(
            new BasicInfoSection(
                target.userId(),
                displayName(target.nickname(), target.name(), target.email()),
                target.email(),
                target.ageRange(),
                currentRoleState.roleName(),
                AdminRoleCatalog.displayRoleLabel(currentRoleState.roleName()),
                currentRoleState.roleState(),
                effectiveRegionId,
                legacyAuthRoleName,
                !legacyAuthRoleName.equals(currentRoleState.roleName())
            ),
            new RolePolicySection(rolePolicy.regionId(), rolePolicy.roleName(), rolePolicy.configured(), rolePolicy.roleAssignable(), rolePolicy.defaultHostPermission(), rolePolicy.defaultPartnerPermission(), rolePolicy.individualHostPermissionGrantAllowed(), rolePolicy.individualPartnerPermissionGrantAllowed(), rolePolicy.adminApprovalRequiredForPause(), rolePolicy.adminApprovalRequiredForWithdrawal(), rolePolicy.adminApprovalRequiredForRoleRestore()),
            permissions,
            new RoleStateSection(currentRoleState.roleName(), currentRoleState.roleState(), formatTimestamp(currentRoleState.updatedAt()), currentRoleState.reason()),
            assignments,
            partnerCandidates,
            new ProposalApplicationSection(
                proposalSummary.hostProposalCount(),
                proposalSummary.bridgeProposalCount(),
                proposalSummary.okatteCandidateCount(),
                proposalSummary.applicationCount(),
                detailUrl("/app/gate/", proposalSummary.latestHostProposalId()),
                detailUrl("/app/gate/", proposalSummary.latestBridgeProposalId()),
                detailUrl("/app/okatte/", proposalSummary.latestOkatteCandidateId()),
                proposalSummary.latestApplicationId() == null ? null : "application#" + proposalSummary.latestApplicationId()
            ),
            permissionChangeLogs,
            accessLogs,
            new RelatedLinksSection(
                null,
                null,
                null,
                "/app/support/admin",
                "/app/history",
                "/app/notifications",
                "/app/admin/users/" + targetUserId + "/support-notes",
                detailUrl("/app/gate/", proposalSummary.latestHostProposalId()),
                detailUrl("/app/okatte/", proposalSummary.latestOkatteCandidateId())
            ),
            new RegionSettingsSection(regionSettingSummary.regionId(), regionSettingSummary.settingCount(), "/app/admin/regions/" + regionSettingSummary.regionId())
        );
    }

    @Transactional
    public void grantPermission(String viewerEmail, long targetUserId, String permissionName, AdminPermissionRuleOperationForm form) {
        changePermissionRule(viewerEmail, targetUserId, permissionName, form, "active", "grant");
    }

    @Transactional
    public void suspendPermission(String viewerEmail, long targetUserId, String permissionName, AdminPermissionRuleOperationForm form) {
        changePermissionRule(viewerEmail, targetUserId, permissionName, form, "suspended", "suspend");
    }

    @Transactional
    public void resumePermission(String viewerEmail, long targetUserId, String permissionName, AdminPermissionRuleOperationForm form) {
        changePermissionRule(viewerEmail, targetUserId, permissionName, form, "active", "resume");
    }

    @Transactional
    public void revokePermission(String viewerEmail, long targetUserId, String permissionName, AdminPermissionRuleOperationForm form) {
        changePermissionRule(viewerEmail, targetUserId, permissionName, form, "revoked", "revoke");
    }

    @Transactional
    public void createPartnerAssignment(String viewerEmail, long targetUserId, AdminPartnerAssignmentForm form) {
        UserRow viewer = repository.requireUserByEmail(viewerEmail);
        requireAdmin(viewer.userId());
        TargetUserRow target = repository.requireTargetUser(targetUserId);
        String regionId = normalizeRegionId(form == null ? null : form.getRegionId(), target.region(), target.interestRegion());
        Long partnerUserId = form == null ? null : form.getPartnerUserId();
        if (partnerUserId == null) {
            throw new AdminUserManagementConflictException("Partner user is required");
        }
        requirePartnerPermission(regionId, partnerUserId);

        Timestamp now = Timestamp.from(Instant.now());
        Date effectiveFrom = parseDate(form == null ? null : form.getEffectiveFrom());
        Date effectiveTo = parseDate(form == null ? null : form.getEffectiveTo());
        validateEffectiveRange(effectiveFrom, effectiveTo);
        String reason = normalizeRequiredReason(form == null ? null : form.getReason());
        long assignmentId = repository.createPartnerAssignment(regionId, targetUserId, partnerUserId, effectiveFrom, effectiveTo, viewer.userId(), reason, now);
        repository.insertPermissionChangeLog(regionId, targetUserId, "partner_assignment", "partner_assignment", "assign", toJson(Map.of("state", "none")), toJson(Map.of("assignment_id", assignmentId, "partner_user_id", partnerUserId, "state", "active")), viewer.userId(), reason, now);
    }

    @Transactional
    public void pausePartnerAssignment(String viewerEmail, long targetUserId, long assignmentId, AdminPartnerAssignmentForm form) {
        changeAssignmentState(viewerEmail, targetUserId, assignmentId, "paused", "suspend", form);
    }

    @Transactional
    public void resumePartnerAssignment(String viewerEmail, long targetUserId, long assignmentId, AdminPartnerAssignmentForm form) {
        changeAssignmentState(viewerEmail, targetUserId, assignmentId, "active", "resume", form);
    }

    @Transactional
    public void endPartnerAssignment(String viewerEmail, long targetUserId, long assignmentId, AdminPartnerAssignmentForm form) {
        changeAssignmentState(viewerEmail, targetUserId, assignmentId, "ended", "end", form);
    }

    @Transactional
    public void suspendRoleState(String viewerEmail, long targetUserId, AdminRoleStateOperationForm form) {
        changeRoleState(viewerEmail, targetUserId, form, "suspended", "suspend");
    }

    @Transactional
    public void restoreRoleState(String viewerEmail, long targetUserId, AdminRoleStateOperationForm form) {
        changeRoleState(viewerEmail, targetUserId, form, "active", "restore");
    }

    @Transactional
    public void revokeRoleState(String viewerEmail, long targetUserId, AdminRoleStateOperationForm form) {
        changeRoleState(viewerEmail, targetUserId, form, "revoked", "revoke");
    }

    private void changePermissionRule(String viewerEmail, long targetUserId, String permissionName, AdminPermissionRuleOperationForm form, String nextState, String actionType) {
        UserRow viewer = repository.requireUserByEmail(viewerEmail);
        requireAdmin(viewer.userId());
        TargetUserRow target = repository.requireTargetUser(targetUserId);
        String normalizedPermission = AdminRoleCatalog.normalizePermission(permissionName);
        if (!AdminRoleCatalog.isManagedPermission(normalizedPermission)) {
            throw new AdminUserManagementConflictException("Managed permission is required");
        }
        CurrentRoleStateRow currentRoleState = repository.findCurrentRoleState(targetUserId, normalizeRegionId(target.region(), target.interestRegion()));
        String regionId = normalizeRegionId(form == null ? null : form.getRegionId(), currentRoleState.regionId(), target.region(), target.interestRegion());
        RolePolicyRow rolePolicy = repository.findRolePolicy(regionId, currentRoleState.roleName());
        if (!isPermissionManageable(rolePolicy, currentRoleState.roleName(), normalizedPermission)) {
            throw new AdminUserManagementConflictException("This permission is managed by role policy or role defaults in the current step");
        }
        String normalizedReason = normalizeRequiredReason(form == null ? null : form.getReason());
        Date effectiveFrom = parseDate(form == null ? null : form.getEffectiveFrom());
        Date effectiveTo = parseDate(form == null ? null : form.getEffectiveTo());
        validateEffectiveRange(effectiveFrom, effectiveTo);
        PermissionRuleRow beforeRule = repository.findPermissionRule(regionId, targetUserId, normalizedPermission);
        Timestamp now = Timestamp.from(Instant.now());
        repository.upsertPermissionRule(regionId, targetUserId, normalizedPermission, nextState, effectiveFrom, effectiveTo, viewer.userId(), normalizedReason, now);
        PermissionRuleRow afterRule = repository.findPermissionRule(regionId, targetUserId, normalizedPermission);
        repository.insertPermissionChangeLog(regionId, targetUserId, "permission", normalizedPermission, actionType, toJson(permissionRuleMap(beforeRule)), toJson(permissionRuleMap(afterRule)), viewer.userId(), normalizedReason, now);
    }

    private void changeAssignmentState(String viewerEmail, long targetUserId, long assignmentId, String nextState, String actionType, AdminPartnerAssignmentForm form) {
        UserRow viewer = repository.requireUserByEmail(viewerEmail);
        requireAdmin(viewer.userId());
        repository.requireTargetUser(targetUserId);
        PartnerAssignmentRow beforeAssignment = repository.requirePartnerAssignment(assignmentId);
        if (beforeAssignment.targetUserId() != targetUserId) {
            throw new AdminUserManagementConflictException("Assignment target mismatch");
        }
        validateAssignmentTransition(beforeAssignment.assignmentStatus(), nextState);
        Timestamp now = Timestamp.from(Instant.now());
        Date effectiveFrom = parseDate(form == null ? null : form.getEffectiveFrom());
        Date effectiveTo = parseDate(form == null ? null : form.getEffectiveTo());
        validateEffectiveRange(effectiveFrom, effectiveTo);
        String normalizedReason = normalizeRequiredReason(form == null ? null : form.getReason());
        repository.updatePartnerAssignmentStatus(assignmentId, nextState, effectiveFrom, effectiveTo, viewer.userId(), normalizedReason, now);
        PartnerAssignmentRow afterAssignment = repository.requirePartnerAssignment(assignmentId);
        repository.insertPermissionChangeLog(beforeAssignment.regionId(), targetUserId, "partner_assignment", "partner_assignment", actionType, toJson(assignmentMap(beforeAssignment)), toJson(assignmentMap(afterAssignment)), viewer.userId(), normalizedReason, now);
    }

    private void changeRoleState(String viewerEmail, long targetUserId, AdminRoleStateOperationForm form, String nextState, String actionType) {
        UserRow viewer = repository.requireUserByEmail(viewerEmail);
        requireAdmin(viewer.userId());
        TargetUserRow target = repository.requireTargetUser(targetUserId);
        CurrentRoleStateRow beforeRoleState = repository.findCurrentRoleState(targetUserId, normalizeRegionId(target.region(), target.interestRegion()));
        String regionId = normalizeRegionId(form == null ? null : form.getRegionId(), beforeRoleState.regionId(), target.region(), target.interestRegion());
        Timestamp now = Timestamp.from(Instant.now());
        String normalizedReason = normalizeReason(form == null ? null : form.getReason());
        repository.upsertRoleState(targetUserId, beforeRoleState.roleName(), nextState, regionId, normalizedReason, viewer.userId(), now);
        CurrentRoleStateRow afterRoleState = repository.findCurrentRoleState(targetUserId, regionId);
        repository.insertPermissionChangeLog(regionId, targetUserId, "role", beforeRoleState.roleName(), actionType, toJson(roleStateMap(beforeRoleState)), toJson(roleStateMap(afterRoleState)), viewer.userId(), normalizedReason, now);
    }

    private PermissionItem buildPermissionItem(long targetUserId, RolePolicyRow rolePolicy, String regionId, String roleName, String permissionName) {
        PermissionRuleRow rule = repository.findPermissionRule(regionId, targetUserId, permissionName);
        boolean defaultGranted = AdminRoleCatalog.HOST_PERMISSION.equals(permissionName) ? rolePolicy.defaultHostPermission() : rolePolicy.defaultPartnerPermission();
        String ruleState = rule == null ? null : normalizeRuleState(rule.ruleState(), rule.effectiveTo());
        boolean active = switch (ruleState == null ? "" : ruleState) {
            case "suspended", "revoked", "expired" -> false;
            case "active" -> true;
            default -> defaultGranted;
        };
        boolean manageable = isPermissionManageable(rolePolicy, roleName, permissionName);
        String effectiveState = ruleState == null ? (defaultGranted ? "active" : "inactive") : ruleState;
        String source = ruleState == null ? "role_policy_default" : "permission_rule";
        return new PermissionItem(permissionName, active, defaultGranted, ruleState, effectiveState, source, manageable, regionId, formatDate(rule == null ? null : rule.effectiveFrom()), formatDate(rule == null ? null : rule.effectiveTo()));
    }

    private boolean isPermissionManageable(RolePolicyRow rolePolicy, String roleName, String permissionName) {
        if (AdminRoleCatalog.BRIDGE_ROLE.equals(roleName) && AdminRoleCatalog.PARTNER_PERMISSION.equals(permissionName)) {
            return false;
        }
        return switch (permissionName) {
            case AdminRoleCatalog.HOST_PERMISSION -> rolePolicy.individualHostPermissionGrantAllowed() || !rolePolicy.defaultHostPermission();
            case AdminRoleCatalog.PARTNER_PERMISSION -> rolePolicy.individualPartnerPermissionGrantAllowed() || !rolePolicy.defaultPartnerPermission();
            default -> false;
        };
    }

    private void requireAdmin(long viewerUserId) {
        if (!repository.isLegacyAdmin(viewerUserId)) {
            throw new AdminUserManagementAccessDeniedException("Admin access is required");
        }
    }

    private void requirePartnerPermission(String regionId, long partnerUserId) {
        TargetUserRow partner = repository.requireTargetUser(partnerUserId);
        CurrentRoleStateRow roleState = repository.findCurrentRoleState(partnerUserId, normalizeRegionId(partner.region(), partner.interestRegion()));
        RolePolicyRow rolePolicy = repository.findRolePolicy(regionId, roleState.roleName());
        PermissionItem permission = buildPermissionItem(partnerUserId, rolePolicy, regionId, roleState.roleName(), AdminRoleCatalog.PARTNER_PERMISSION);
        if (!permission.active()) {
            throw new AdminUserManagementConflictException("Partner permission is required");
        }
    }

    private String normalizeRegionId(String... candidates) {
        if (candidates == null) {
            return "default";
        }
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate.trim();
            }
        }
        return "default";
    }

    private String normalizeReason(String reason) {
        if (reason == null) {
            return null;
        }
        String normalized = reason.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.length() > 500 ? normalized.substring(0, 500) : normalized;
    }

    private String normalizeRequiredReason(String reason) {
        String normalized = normalizeReason(reason);
        if (normalized == null) {
            throw new AdminUserManagementConflictException("Reason is required");
        }
        return normalized;
    }

    private Date parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Date.valueOf(LocalDate.parse(raw.trim()));
    }

    private void validateEffectiveRange(Date effectiveFrom, Date effectiveTo) {
        if (effectiveFrom != null && effectiveTo != null && effectiveTo.before(effectiveFrom)) {
            throw new AdminUserManagementConflictException("effective_to must be on or after effective_from");
        }
    }

    private String normalizeRuleState(String ruleState, Date effectiveTo) {
        if (ruleState == null || ruleState.isBlank()) {
            return null;
        }
        if ("active".equalsIgnoreCase(ruleState) && effectiveTo != null && effectiveTo.toLocalDate().isBefore(LocalDate.now())) {
            return "expired";
        }
        return ruleState.toLowerCase();
    }

    private PermissionChangeLogItem toPermissionChangeLogItem(PermissionChangeLogRow row) {
        return new PermissionChangeLogItem(row.changedObjectType(), row.changedObjectName(), row.actionType(), row.beforeValue(), row.afterValue(), row.reason(), row.changedByLabel(), formatTimestamp(row.changedAt()));
    }

    private AccessLogItem toAccessLogItem(AccessLogRow row) {
        return new AccessLogItem(row.viewerContext(), row.targetType(), row.targetId(), row.viewReason(), row.viewerLabel(), formatTimestamp(row.viewedAt()));
    }

    private AssignmentItem toAssignmentItem(PartnerAssignmentRow row) {
        return new AssignmentItem(row.assignmentId(), row.regionId(), row.partnerUserId(), row.partnerLabel(), row.assignmentStatus(), formatDate(row.effectiveFrom()), formatDate(row.effectiveTo()), formatTimestamp(row.assignedAt()), formatTimestamp(row.endedAt()));
    }

    private PartnerCandidateItem toPartnerCandidateItem(PartnerCandidateRow row, String regionId) {
        CurrentRoleStateRow roleState = repository.findCurrentRoleState(row.userId(), normalizeRegionId(row.region(), row.interestRegion(), regionId));
        RolePolicyRow rolePolicy = repository.findRolePolicy(regionId, roleState.roleName());
        PermissionItem permission = buildPermissionItem(row.userId(), rolePolicy, regionId, roleState.roleName(), AdminRoleCatalog.PARTNER_PERMISSION);
        return new PartnerCandidateItem(row.userId(), row.displayName(), roleState.roleName(), permission.active());
    }

    private Map<String, Object> permissionRuleMap(PermissionRuleRow row) {
        if (row == null) {
            return Map.of("state", "none");
        }
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("permission_name", row.permissionName());
        values.put("rule_state", row.ruleState());
        values.put("effective_from", formatDate(row.effectiveFrom()));
        values.put("effective_to", formatDate(row.effectiveTo()));
        return values;
    }

    private Map<String, Object> assignmentMap(PartnerAssignmentRow row) {
        return Map.of("assignment_id", row.assignmentId(), "partner_user_id", row.partnerUserId(), "assignment_status", row.assignmentStatus(), "region_id", row.regionId());
    }

    private void validateAssignmentTransition(String currentState, String nextState) {
        if ("paused".equals(nextState) && !"active".equals(currentState)) {
            throw new AdminUserManagementConflictException("Only active assignments can be paused");
        }
        if ("active".equals(nextState) && !"paused".equals(currentState)) {
            throw new AdminUserManagementConflictException("Only paused assignments can be resumed");
        }
        if ("ended".equals(nextState) && !"active".equals(currentState) && !"paused".equals(currentState)) {
            throw new AdminUserManagementConflictException("Only active or paused assignments can be ended");
        }
    }

    private Map<String, Object> roleStateMap(CurrentRoleStateRow row) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("role_name", row.roleName());
        values.put("role_state", row.roleState());
        values.put("region_id", row.regionId());
        return values;
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize admin change log JSON", ex);
        }
    }

    private String formatTimestamp(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime().format(PAGE_TIME);
    }

    private String formatDate(Date date) {
        return date == null ? null : date.toLocalDate().toString();
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

    private String detailUrl(String prefix, Long id) {
        if (id == null) {
            return null;
        }
        return prefix + id;
    }

    public record AdminUserDetailPageData(BasicInfoSection basicInfo, RolePolicySection rolePolicy, List<PermissionItem> permissions, RoleStateSection roleState, List<AssignmentItem> assignments, List<PartnerCandidateItem> partnerCandidates, ProposalApplicationSection proposalApplication, List<PermissionChangeLogItem> permissionChangeLogs, List<AccessLogItem> accessLogs, RelatedLinksSection relatedLinks, RegionSettingsSection regionSettings) {}
    public record BasicInfoSection(long targetUserId, String displayName, String email, String ageRange, String roleName, String roleLabel, String roleState, String regionId, String legacyAuthRoleName, boolean roleSourceDiffers) {}
    public record RolePolicySection(String regionId, String roleName, boolean configured, boolean roleAssignable, boolean defaultHostPermission, boolean defaultPartnerPermission, boolean individualHostPermissionGrantAllowed, boolean individualPartnerPermissionGrantAllowed, boolean adminApprovalRequiredForPause, boolean adminApprovalRequiredForWithdrawal, boolean adminApprovalRequiredForRoleRestore) {}
    public record PermissionItem(String permissionName, boolean active, boolean defaultGranted, String ruleState, String effectiveState, String source, boolean manageable, String regionId, String effectiveFrom, String effectiveTo) {}
    public record RoleStateSection(String roleName, String roleState, String updatedAt, String reason) {}
    public record AssignmentItem(long assignmentId, String regionId, long partnerUserId, String partnerLabel, String assignmentStatus, String effectiveFrom, String effectiveTo, String assignedAt, String endedAt) {}
    public record PartnerCandidateItem(long partnerUserId, String partnerLabel, String roleName, boolean partnerPermissionActive) {}
    public record ProposalApplicationSection(int hostProposalCount, int bridgeProposalCount, int okatteCandidateCount, int applicationCount, String latestHostProposalUrl, String latestBridgeProposalUrl, String latestOkatteCandidateUrl, String latestApplicationReference) {}
    public record PermissionChangeLogItem(String changedObjectType, String changedObjectName, String actionType, String beforeValue, String afterValue, String reason, String changedByLabel, String changedAt) {}
    public record AccessLogItem(String viewerContext, String targetType, long targetId, String viewReason, String viewerLabel, String viewedAt) {}
    public record RelatedLinksSection(String accountUrl, String profileUrl, String privacySettingsUrl, String supportAdminUrl, String historyUrl, String notificationsUrl, String supportNotesUrl, String proposalUrl, String okatteUrl) {}
    public record RegionSettingsSection(String regionId, int settingCount, String settingsUrl) {}

    public static class AdminUserManagementAccessDeniedException extends RuntimeException {
        public AdminUserManagementAccessDeniedException(String message) { super(message); }
    }

    public static class AdminUserManagementConflictException extends RuntimeException {
        public AdminUserManagementConflictException(String message) { super(message); }
    }
}
