package com.furutabi.auth;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.stereotype.Component;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final LoginRedirectHelper loginRedirectHelper;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    public LoginSuccessHandler(LoginRedirectHelper loginRedirectHelper) {
        this.loginRedirectHelper = loginRedirectHelper;
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {
        String targetUrl = loginRedirectHelper.resolveOrDefault(
            request.getParameter("returnTo"),
            authentication.getAuthorities()
        );
        redirectStrategy.sendRedirect(request, response, targetUrl);
    }
}
