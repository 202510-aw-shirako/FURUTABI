package com.furutabi.auth;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    private final LoginRedirectHelper loginRedirectHelper;

    public LoginController(LoginRedirectHelper loginRedirectHelper) {
        this.loginRedirectHelper = loginRedirectHelper;
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String returnTo, Model model) {
        loginRedirectHelper.sanitize(returnTo).ifPresent(safeReturnTo -> model.addAttribute("returnTo", safeReturnTo));
        return "auth/login";
    }
}
