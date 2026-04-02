package com.furutabi.admin;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/app/admin")
public class AdminUserManagementController {

    private final AdminUserManagementService adminUserManagementService;

    public AdminUserManagementController(AdminUserManagementService adminUserManagementService) {
        this.adminUserManagementService = adminUserManagementService;
    }

    @GetMapping
    public String adminHome(Authentication authentication, Model model) {
        try {
            model.addAttribute("pageData", adminUserManagementService.loadAdminHomePage(authentication.getName()));
            return "app/admin-home";
        } catch (AdminUserManagementService.AdminUserManagementAccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access is required", ex);
        }
    }

    @GetMapping("/users")
    public String adminUserList(
        @RequestParam(name = "q", required = false) String query,
        @RequestParam(name = "page", required = false) Integer page,
        Authentication authentication,
        Model model
    ) {
        try {
            model.addAttribute("pageData", adminUserManagementService.loadUserListPage(authentication.getName(), query, page));
            return "app/admin-user-list";
        } catch (AdminUserManagementService.AdminUserManagementAccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access is required", ex);
        }
    }

    @GetMapping("/users/{id}")
    public String adminUserDetail(@PathVariable long id, Authentication authentication, Model model) {
        try {
            model.addAttribute("pageData", adminUserManagementService.loadDetailPage(authentication.getName(), id));
            model.addAttribute("permissionRuleForm", new AdminPermissionRuleOperationForm());
            model.addAttribute("partnerAssignmentForm", new AdminPartnerAssignmentForm());
            model.addAttribute("roleStateForm", new AdminRoleStateOperationForm());
            model.addAttribute("pointGrantForm", new AdminPointGrantForm());
            return "app/admin-user-detail";
        } catch (AdminUserManagementService.AdminUserManagementAccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access is required", ex);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user detail not found", ex);
        }
    }

    @GetMapping("/users/{id}/support-notes")
    public String adminSupportNotesRedirect(@PathVariable long id, Authentication authentication) {
        try {
            adminUserManagementService.loadDetailPage(authentication.getName(), id);
            return "redirect:/app/support-notes/users/" + id;
        } catch (AdminUserManagementService.AdminUserManagementAccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access is required", ex);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user detail not found", ex);
        }
    }

    @GetMapping("/regions/{regionId}")
    public String adminRegionSettingsPlaceholder(
        @PathVariable String regionId,
        @RequestParam(name = "fromUserId", required = false) Long fromUserId,
        Authentication authentication,
        Model model
    ) {
        try {
            adminUserManagementService.requireAdminViewer(authentication.getName());
            model.addAttribute("regionId", regionId);
            model.addAttribute("fromUserId", fromUserId);
            return "app/admin-region-settings-placeholder";
        } catch (AdminUserManagementService.AdminUserManagementAccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access is required", ex);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin region settings not found", ex);
        }
    }

    @PostMapping("/users/{id}/permissions/{permissionName}/grant")
    public String grantPermission(@PathVariable long id, @PathVariable String permissionName, Authentication authentication, @ModelAttribute("permissionRuleForm") AdminPermissionRuleOperationForm permissionRuleForm) {
        return change("granted", id, () -> adminUserManagementService.grantPermission(authentication.getName(), id, permissionName, permissionRuleForm));
    }

    @PostMapping("/users/{id}/permissions/{permissionName}/suspend")
    public String suspendPermission(@PathVariable long id, @PathVariable String permissionName, Authentication authentication, @ModelAttribute("permissionRuleForm") AdminPermissionRuleOperationForm permissionRuleForm) {
        return change("suspended", id, () -> adminUserManagementService.suspendPermission(authentication.getName(), id, permissionName, permissionRuleForm));
    }

    @PostMapping("/users/{id}/permissions/{permissionName}/resume")
    public String resumePermission(@PathVariable long id, @PathVariable String permissionName, Authentication authentication, @ModelAttribute("permissionRuleForm") AdminPermissionRuleOperationForm permissionRuleForm) {
        return change("resumed", id, () -> adminUserManagementService.resumePermission(authentication.getName(), id, permissionName, permissionRuleForm));
    }

    @PostMapping("/users/{id}/permissions/{permissionName}/revoke")
    public String revokePermission(@PathVariable long id, @PathVariable String permissionName, Authentication authentication, @ModelAttribute("permissionRuleForm") AdminPermissionRuleOperationForm permissionRuleForm) {
        return change("revoked", id, () -> adminUserManagementService.revokePermission(authentication.getName(), id, permissionName, permissionRuleForm));
    }

    @PostMapping("/users/{id}/role-state/suspend")
    public String suspendRoleState(@PathVariable long id, Authentication authentication, @ModelAttribute("roleStateForm") AdminRoleStateOperationForm roleStateForm) {
        return change("roleSuspended", id, () -> adminUserManagementService.suspendRoleState(authentication.getName(), id, roleStateForm));
    }

    @PostMapping("/users/{id}/role-state/restore")
    public String restoreRoleState(@PathVariable long id, Authentication authentication, @ModelAttribute("roleStateForm") AdminRoleStateOperationForm roleStateForm) {
        return change("roleRestored", id, () -> adminUserManagementService.restoreRoleState(authentication.getName(), id, roleStateForm));
    }

    @PostMapping("/users/{id}/role-state/revoke")
    public String revokeRoleState(@PathVariable long id, Authentication authentication, @ModelAttribute("roleStateForm") AdminRoleStateOperationForm roleStateForm) {
        return change("roleRevoked", id, () -> adminUserManagementService.revokeRoleState(authentication.getName(), id, roleStateForm));
    }

    @PostMapping("/users/{id}/assignments")
    public String createAssignment(@PathVariable long id, Authentication authentication, @ModelAttribute("partnerAssignmentForm") AdminPartnerAssignmentForm partnerAssignmentForm) {
        return change("assignmentCreated", id, () -> adminUserManagementService.createPartnerAssignment(authentication.getName(), id, partnerAssignmentForm));
    }

    @PostMapping("/users/{id}/assignments/{assignmentId}/pause")
    public String pauseAssignment(@PathVariable long id, @PathVariable long assignmentId, Authentication authentication, @ModelAttribute("partnerAssignmentForm") AdminPartnerAssignmentForm partnerAssignmentForm) {
        return change("assignmentPaused", id, () -> adminUserManagementService.pausePartnerAssignment(authentication.getName(), id, assignmentId, partnerAssignmentForm));
    }

    @PostMapping("/users/{id}/assignments/{assignmentId}/resume")
    public String resumeAssignment(@PathVariable long id, @PathVariable long assignmentId, Authentication authentication, @ModelAttribute("partnerAssignmentForm") AdminPartnerAssignmentForm partnerAssignmentForm) {
        return change("assignmentResumed", id, () -> adminUserManagementService.resumePartnerAssignment(authentication.getName(), id, assignmentId, partnerAssignmentForm));
    }

    @PostMapping("/users/{id}/assignments/{assignmentId}/end")
    public String endAssignment(@PathVariable long id, @PathVariable long assignmentId, Authentication authentication, @ModelAttribute("partnerAssignmentForm") AdminPartnerAssignmentForm partnerAssignmentForm) {
        return change("assignmentEnded", id, () -> adminUserManagementService.endPartnerAssignment(authentication.getName(), id, assignmentId, partnerAssignmentForm));
    }

    @PostMapping("/users/{id}/points/grant")
    public String grantPoints(@PathVariable long id, Authentication authentication, @ModelAttribute("pointGrantForm") AdminPointGrantForm pointGrantForm) {
        return change("pointsGranted", id, () -> adminUserManagementService.grantPoints(authentication.getName(), id, pointGrantForm));
    }

    private String change(String flag, long targetUserId, Runnable action) {
        try {
            action.run();
            return "redirect:/app/admin/users/" + targetUserId + "?" + flag;
        } catch (AdminUserManagementService.AdminUserManagementAccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access is required", ex);
        } catch (AdminUserManagementService.AdminUserManagementConflictException ex) {
            return "redirect:/app/admin/users/" + targetUserId + "?blocked";
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user detail not found", ex);
        }
    }

}
