
package com.arul.authforge.handler;

import com.arul.authforge.dto.LoginResponse;
import com.arul.authforge.service.AuthService;
import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GoogleOAuth2SuccessHandler
        implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public GoogleOAuth2SuccessHandler(
            AuthService authService,
            ObjectMapper objectMapper) {
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

        String googleSubject = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String fullName = oidcUser.getFullName();

        Boolean emailVerified =
                oidcUser.getClaimAsBoolean("email_verified");

        if (!Boolean.TRUE.equals(emailVerified)) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Google email is not verified"
            );
            return;
        }

        try {
            LoginResponse loginResponse =
                    authService.loginWithGoogle(
                            googleSubject,
                            email,
                            fullName,
                            true
                    );

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(
                    MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            objectMapper.writeValue(
                    response.getOutputStream(),
                    loginResponse
            );

        } catch (IllegalArgumentException ex) {
            response.sendError(
                    HttpServletResponse.SC_CONFLICT,
                    ex.getMessage()
            );

        } catch (IllegalStateException ex) {
            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    ex.getMessage()
            );
        }
    }
}
