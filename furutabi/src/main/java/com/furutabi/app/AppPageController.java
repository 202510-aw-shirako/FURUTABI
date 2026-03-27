package com.furutabi.app;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/app")
public class AppPageController {

    private final AppSettingsService appSettingsService;
    private final MapRecordService mapRecordService;
    private final GateService gateService;
    private final OkatteService okatteService;
    private final ProposalApplicationService proposalApplicationService;
    private final HostApplicationReviewService hostApplicationReviewService;
    private final ChatThreadMessagingService chatThreadMessagingService;
    private final HistoryService historyService;

    public AppPageController(
        AppSettingsService appSettingsService,
        MapRecordService mapRecordService,
        GateService gateService,
        OkatteService okatteService,
        ProposalApplicationService proposalApplicationService,
        HostApplicationReviewService hostApplicationReviewService,
        ChatThreadMessagingService chatThreadMessagingService,
        HistoryService historyService
    ) {
        this.appSettingsService = appSettingsService;
        this.mapRecordService = mapRecordService;
        this.gateService = gateService;
        this.okatteService = okatteService;
        this.proposalApplicationService = proposalApplicationService;
        this.hostApplicationReviewService = hostApplicationReviewService;
        this.chatThreadMessagingService = chatThreadMessagingService;
        this.historyService = historyService;
    }

    @GetMapping({"/home", "/home.html"})
    public String home() {
        return "app/home";
    }

    @GetMapping({"/local-member-home", "/local-member-home.html"})
    public String localMemberHome() {
        return "app/local-member-home";
    }

    @GetMapping({"/host-applications", "/host-applications.html", "/bridge-applications", "/bridge-applications.html"})
    public String hostApplications(Authentication authentication, Model model) {
        model.addAttribute("pageData", hostApplicationReviewService.loadPendingApplications(authentication.getName()));
        return "app/host-application-list";
    }

    @GetMapping({"/host-applications/{id}", "/bridge-applications/{id}"})
    public String hostApplicationDetail(@PathVariable long id, Authentication authentication, Model model) {
        try {
            model.addAttribute("pageData", hostApplicationReviewService.loadApplicationDetail(authentication.getName(), id));
            return "app/host-application-detail";
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposal application not found", ex);
        }
    }

    @PostMapping({"/host-applications/{id}/accept", "/bridge-applications/{id}/accept"})
    public String acceptHostApplication(@PathVariable long id, Authentication authentication) {
        try {
            hostApplicationReviewService.acceptApplication(authentication.getName(), id);
            return "redirect:/app/host-applications/" + id + "?accepted";
        } catch (HostApplicationReviewService.HostApplicationReviewConflictException ex) {
            return "redirect:/app/host-applications/" + id + "?blocked";
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposal application not found", ex);
        }
    }

    @PostMapping({"/host-applications/{id}/reject", "/bridge-applications/{id}/reject"})
    public String rejectHostApplication(@PathVariable long id, Authentication authentication) {
        try {
            hostApplicationReviewService.rejectApplication(authentication.getName(), id);
            return "redirect:/app/host-applications/" + id + "?rejected";
        } catch (HostApplicationReviewService.HostApplicationReviewConflictException ex) {
            return "redirect:/app/host-applications/" + id + "?blocked";
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposal application not found", ex);
        }
    }

    @GetMapping({"/chat", "/chat.html"})
    public String chatThreads(Authentication authentication, Model model) {
        model.addAttribute("pageData", chatThreadMessagingService.loadThreadList(authentication.getName()));
        return "app/chat-thread-list";
    }

    @GetMapping("/chat/{id}")
    public String chatThreadDetail(@PathVariable long id, Authentication authentication, Model model) {
        try {
            model.addAttribute("pageData", chatThreadMessagingService.loadThreadDetail(authentication.getName(), id));
            model.addAttribute("chatMessageForm", new ChatMessageForm());
            return "app/chat-thread-detail";
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat thread not found", ex);
        }
    }

    @GetMapping({"/history", "/history.html"})
    public String history(Authentication authentication, Model model) {
        model.addAttribute("pageData", historyService.loadHistory(authentication.getName()));
        return "app/history-list";
    }

    @PostMapping("/chat/{id}/messages")
    public String sendChatMessage(
        @PathVariable long id,
        Authentication authentication,
        @ModelAttribute("chatMessageForm") ChatMessageForm chatMessageForm
    ) {
        try {
            chatThreadMessagingService.sendMessage(authentication.getName(), id, chatMessageForm);
            return "redirect:/app/chat/" + id + "?sent";
        } catch (ChatThreadMessagingService.ChatThreadMessagingConflictException ex) {
            return "redirect:/app/chat/" + id + "?blocked";
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat thread not found", ex);
        }
    }

    @GetMapping({"/mypage", "/mypage.html"})
    public String mypage(Authentication authentication, Model model) {
        model.addAttribute("summary", appSettingsService.loadMypageSummary(authentication.getName()));
        return "app/mypage";
    }

    @GetMapping({"/account", "/account.html"})
    public String account(Authentication authentication, Model model) {
        AppSettingsService.AccountPageData pageData = appSettingsService.loadAccountPage(authentication.getName());
        model.addAttribute("accountForm", pageData.form());
        model.addAttribute("accountEmail", pageData.email());
        model.addAttribute("smsVerified", pageData.smsVerified());
        model.addAttribute("additionalVerificationStatus", pageData.additionalVerificationStatus());
        return "app/account";
    }

    @PostMapping("/account")
    public String saveAccount(Authentication authentication, @ModelAttribute("accountForm") AppAccountForm accountForm) {
        appSettingsService.saveAccount(authentication.getName(), accountForm);
        return "redirect:/app/account?saved";
    }

    @GetMapping({"/profile", "/profile.html"})
    public String profile(Authentication authentication, Model model) {
        model.addAttribute("profileForm", appSettingsService.loadProfileForm(authentication.getName()));
        return "app/profile";
    }

    @PostMapping("/profile")
    public String saveProfile(Authentication authentication, @ModelAttribute("profileForm") AppProfileForm profileForm) {
        appSettingsService.saveProfile(authentication.getName(), profileForm);
        return "redirect:/app/profile?saved";
    }

    @GetMapping({"/privacy-settings", "/privacy-settings.html"})
    public String privacySettings(Authentication authentication, Model model) {
        AppSettingsService.PrivacySettingsPageData pageData =
            appSettingsService.loadPrivacySettingsPage(authentication.getName());
        model.addAttribute("privacySettingsForm", pageData.form());
        model.addAttribute("smsVerified", pageData.smsVerified());
        model.addAttribute("additionalVerificationStatus", pageData.additionalVerificationStatus());
        return "app/privacy-settings";
    }

    @PostMapping("/privacy-settings")
    public String savePrivacySettings(
        Authentication authentication,
        @ModelAttribute("privacySettingsForm") AppPrivacySettingsForm privacySettingsForm
    ) {
        appSettingsService.savePrivacySettings(authentication.getName(), privacySettingsForm);
        return "redirect:/app/privacy-settings?saved";
    }

    @GetMapping({"/map-records", "/map-records.html"})
    public String mapRecords(Authentication authentication, Model model) {
        model.addAttribute("pageData", mapRecordService.loadVisibleMapRecordList(authentication.getName()));
        return "app/map-records";
    }

    @GetMapping({"/gate", "/gate.html"})
    public String gateList(Authentication authentication, Model model) {
        model.addAttribute("pageData", gateService.loadVisibleGateList(authentication.getName()));
        return "app/gate-list";
    }

    @GetMapping("/gate/{id}")
    public String gateDetail(@PathVariable long id, Authentication authentication, Model model) {
        try {
            model.addAttribute("pageData", gateService.loadVisibleGateDetail(authentication.getName(), id));
            return "app/gate-detail";
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Gate proposal not found", ex);
        }
    }

    @GetMapping({"/okatte", "/okatte.html"})
    public String okatteList(Authentication authentication, Model model) {
        model.addAttribute("pageData", okatteService.loadVisibleOkatteList(authentication.getName()));
        return "app/okatte-list";
    }

    @GetMapping("/okatte/{id}")
    public String okatteDetail(@PathVariable long id, Authentication authentication, Model model) {
        try {
            model.addAttribute("pageData", okatteService.loadVisibleOkatteDetail(authentication.getName(), id));
            return "app/okatte-detail";
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Okatte proposal not found", ex);
        }
    }

    @GetMapping("/gate/{id}/apply")
    public String gateApplicationPage(@PathVariable long id, Authentication authentication, Model model) {
        try {
            model.addAttribute("pageData", proposalApplicationService.loadApplicationPage(authentication.getName(), id));
            model.addAttribute("proposalApplicationForm", new ProposalApplicationForm());
            return "app/gate-application-form";
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Gate proposal not found", ex);
        }
    }

    @GetMapping("/okatte/{id}/apply")
    public String okatteApplicationPage(@PathVariable long id, Authentication authentication, Model model) {
        try {
            model.addAttribute("pageData", proposalApplicationService.loadOkatteApplicationPage(authentication.getName(), id));
            model.addAttribute("proposalApplicationForm", new ProposalApplicationForm());
            return "app/okatte-application-form";
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Okatte proposal not found", ex);
        }
    }

    @PostMapping("/okatte/{id}/apply")
    public String createOkatteApplication(
        @PathVariable long id,
        Authentication authentication,
        @ModelAttribute("proposalApplicationForm") ProposalApplicationForm proposalApplicationForm
    ) {
        try {
            proposalApplicationService.createOkatteApplication(authentication.getName(), id, proposalApplicationForm);
            return "redirect:/app/okatte/" + id + "?applied";
        } catch (ProposalApplicationService.ProposalApplicationConflictException ex) {
            return "redirect:/app/okatte/" + id + "/apply?blocked";
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Okatte proposal not found", ex);
        }
    }

    @PostMapping("/gate/{id}/apply")
    public String createGateApplication(
        @PathVariable long id,
        Authentication authentication,
        @ModelAttribute("proposalApplicationForm") ProposalApplicationForm proposalApplicationForm
    ) {
        try {
            proposalApplicationService.createApplication(authentication.getName(), id, proposalApplicationForm);
            return "redirect:/app/gate/" + id + "?applied";
        } catch (ProposalApplicationService.ProposalApplicationConflictException ex) {
            return "redirect:/app/gate/" + id + "/apply?blocked";
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Gate proposal not found", ex);
        }
    }

    @GetMapping("/map-records/new")
    public String createMapRecord(Authentication authentication, Model model) {
        MapRecordService.MapRecordEditorPageData pageData = mapRecordService.loadCreatePage(authentication.getName());
        model.addAttribute("pageData", pageData);
        model.addAttribute("mapRecordForm", pageData.form());
        model.addAttribute("visibilityOptions", visibilityOptions());
        return "app/map-record-form";
    }

    @PostMapping("/map-records")
    public String saveNewMapRecord(Authentication authentication, @ModelAttribute("mapRecordForm") MapRecordForm mapRecordForm) {
        MapRecordService.MapRecordSaveResult saveResult = mapRecordService.createRecord(authentication.getName(), mapRecordForm);
        if (saveResult.draft()) {
            return "redirect:/app/map-records/" + saveResult.mapRecordId() + "/edit?savedDraft";
        }
        return "redirect:/app/map-records/" + saveResult.mapRecordId() + "?saved";
    }

    @GetMapping("/map-records/{id}")
    public String mapRecordDetail(@PathVariable long id, Authentication authentication, Model model) {
        try {
            model.addAttribute("pageData", mapRecordService.loadVisibleMapRecordDetail(authentication.getName(), id));
            return "app/map-record-detail";
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Map record not found", ex);
        }
    }

    @GetMapping("/map-records/{id}/edit")
    public String editMapRecord(@PathVariable long id, Authentication authentication, Model model) {
        try {
            MapRecordService.MapRecordEditorPageData pageData = mapRecordService.loadEditPage(authentication.getName(), id);
            model.addAttribute("pageData", pageData);
            model.addAttribute("mapRecordForm", pageData.form());
            model.addAttribute("visibilityOptions", visibilityOptions());
            return "app/map-record-form";
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Map record not found", ex);
        }
    }

    @PostMapping("/map-records/{id}")
    public String updateMapRecord(
        @PathVariable long id,
        Authentication authentication,
        @ModelAttribute("mapRecordForm") MapRecordForm mapRecordForm
    ) {
        try {
            MapRecordService.MapRecordSaveResult saveResult = mapRecordService.updateRecord(
                authentication.getName(),
                id,
                mapRecordForm
            );
            if (saveResult.draft()) {
                return "redirect:/app/map-records/" + id + "/edit?savedDraft";
            }
            return "redirect:/app/map-records/" + id + "?saved";
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Map record not found", ex);
        }
    }

    @PostMapping("/map-records/{id}/delete")
    public String deleteMapRecord(@PathVariable long id, Authentication authentication) {
        try {
            mapRecordService.deleteRecord(authentication.getName(), id);
            return "redirect:/app/map-records?deleted";
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Map record not found", ex);
        }
    }

    private VisibilityOption[] visibilityOptions() {
        return new VisibilityOption[] {
            new VisibilityOption("PUBLIC", "一般公開"),
            new VisibilityOption("PRIVATE", "本人のみ"),
            new VisibilityOption("LIMITED", "関係者まで")
        };
    }

    public record VisibilityOption(String value, String label) {
    }
}
