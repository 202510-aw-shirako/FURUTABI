package com.furutabi.auth;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/register")
public class RegistrationController {

    public static final String REGISTER_SESSION_KEY = "registerSessionState";

    private final RegistrationService registrationService;
    private final boolean smsEnabled;

    public RegistrationController(
        RegistrationService registrationService,
        @Value("${app.sms.enabled:false}") boolean smsEnabled
    ) {
        this.registrationService = registrationService;
        this.smsEnabled = smsEnabled;
    }

    @GetMapping
    public String register(Model model) {
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterStartForm());
        }
        return "auth/register";
    }

    @PostMapping
    public String startRegistration(
        @Valid @ModelAttribute("registerForm") RegisterStartForm form,
        BindingResult bindingResult,
        HttpSession session
    ) {
        validateRegisterForm(form, bindingResult);
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            RegistrationService.RegistrationStartResult result = registrationService.startRegistration(form);
            session.setAttribute(
                REGISTER_SESSION_KEY,
                new RegisterSessionState(result.userId(), form.getEmail(), form.getPhoneNumber(), result.smsCode())
            );
            return "redirect:/register/sms";
        } catch (IllegalStateException ex) {
            bindingResult.rejectValue("email", "duplicate", "このメールアドレスは既に登録されています。");
            return "auth/register";
        }
    }

    @GetMapping("/sms")
    public String smsPage(Model model, HttpSession session) {
        RegisterSessionState state = sessionState(session);
        if (state == null) {
            return "redirect:/register";
        }

        if (!model.containsAttribute("registerSmsForm")) {
            model.addAttribute("registerSmsForm", new RegisterSmsForm());
        }
        populateSmsModel(model, state);
        return "auth/register-sms";
    }

    @PostMapping("/sms/send")
    public String resendSms(HttpSession session, RedirectAttributes redirectAttributes) {
        RegisterSessionState state = sessionState(session);
        if (state == null) {
            return "redirect:/register";
        }

        state.setLatestSmsCode(registrationService.issueSmsCode(state.getUserId(), state.getPhoneNumber()));
        redirectAttributes.addAttribute("resent", "1");
        return "redirect:/register/sms";
    }

    @PostMapping("/sms/verify")
    public String verifySms(
        @Valid @ModelAttribute("registerSmsForm") RegisterSmsForm form,
        BindingResult bindingResult,
        Model model,
        HttpSession session
    ) {
        RegisterSessionState state = sessionState(session);
        if (state == null) {
            return "redirect:/register";
        }

        if (bindingResult.hasErrors()) {
            populateSmsModel(model, state);
            return "auth/register-sms";
        }

        if (!registrationService.verifySmsCode(state.getUserId(), form.getCode())) {
            bindingResult.rejectValue("code", "invalid", "認証コードが正しくないか、有効期限切れです。");
            populateSmsModel(model, state);
            return "auth/register-sms";
        }

        state.setSmsVerified(true);
        state.setLatestSmsCode(null);
        return "redirect:/register/profile";
    }

    @GetMapping("/profile")
    public String profilePage(Model model, HttpSession session) {
        RegisterSessionState state = sessionState(session);
        if (state == null || !state.isSmsVerified()) {
            return "redirect:/register";
        }

        if (!model.containsAttribute("registerProfileForm")) {
            model.addAttribute("registerProfileForm", new RegisterProfileForm());
        }
        return "auth/register-profile";
    }

    @PostMapping("/profile")
    public String saveProfile(
        @ModelAttribute("registerProfileForm") RegisterProfileForm form,
        HttpSession session
    ) {
        RegisterSessionState state = sessionState(session);
        if (state == null || !state.isSmsVerified()) {
            return "redirect:/register";
        }

        registrationService.saveProfile(state.getUserId(), form);
        state.setProfileCompleted(true);
        return "redirect:/register/verify";
    }

    @GetMapping("/verify")
    public String verifyPage(Model model, HttpSession session) {
        RegisterSessionState state = sessionState(session);
        if (state == null || !state.isProfileCompleted()) {
            return "redirect:/register";
        }

        model.addAttribute("email", state.getEmail());
        return "auth/register-verify";
    }

    private void populateSmsModel(Model model, RegisterSessionState state) {
        model.addAttribute("phoneNumber", state.getPhoneNumber());
        model.addAttribute("smsEnabled", smsEnabled);
        model.addAttribute("developmentCode", state.getLatestSmsCode());
    }

    private RegisterSessionState sessionState(HttpSession session) {
        Object value = session.getAttribute(REGISTER_SESSION_KEY);
        return value instanceof RegisterSessionState state ? state : null;
    }

    private void validateRegisterForm(RegisterStartForm form, BindingResult bindingResult) {
        if (!form.isAgreedToTerms()) {
            bindingResult.rejectValue("agreedToTerms", "required", "利用規約への同意が必要です。");
        }
        if (!form.isAgreedToPrivacyPolicy()) {
            bindingResult.rejectValue("agreedToPrivacyPolicy", "required", "プライバシーポリシーへの同意が必要です。");
        }
        if (!form.isAgreedToSmsNotice()) {
            bindingResult.rejectValue("agreedToSmsNotice", "required", "SMS 認証の案内に同意してください。");
        }
        if (!bindingResult.hasFieldErrors("password") && !form.getPassword().equals(form.getPasswordConfirm())) {
            bindingResult.rejectValue("passwordConfirm", "mismatch", "パスワード確認が一致しません。");
        }
    }
}
