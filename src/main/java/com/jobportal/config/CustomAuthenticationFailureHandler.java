package com.jobportal.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Logs WHY a login attempt failed (bad credentials, user not found, or a
 * database/connectivity problem bubbling up as an AuthenticationServiceException)
 * instead of the app just silently redirecting back to /login?error.
 */
@Component
public class CustomAuthenticationFailureHandler
        extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger log =
            LoggerFactory.getLogger(CustomAuthenticationFailureHandler.class);

    public CustomAuthenticationFailureHandler() {
        super("/login?error");
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException, ServletException {

        String attemptedUser = request.getParameter("username");

        log.warn(
                "Login failed for user '{}': {} ({})",
                attemptedUser,
                exception.getMessage(),
                exception.getClass().getSimpleName()
        );

        super.onAuthenticationFailure(request, response, exception);
    }
}