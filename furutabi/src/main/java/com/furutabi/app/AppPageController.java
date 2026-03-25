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

    public AppPageController(AppSettingsService appSettingsService, MapRecordService mapRecordService) {
        this.appSettingsService = appSettingsService;
        this.mapRecordService = mapRecordService;
    }

    @GetMapping({"/home", "/home.html"})
    public String home() {
        return "app/home";
    }

    @GetMapping({"/local-member-home", "/local-member-home.html"})
    public String localMemberHome() {
        return "app/local-member-home";
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
        model.addAttribute("pageData", mapRecordService.loadOwnMapRecordList(authentication.getName()));
        return "app/map-records";
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
}
