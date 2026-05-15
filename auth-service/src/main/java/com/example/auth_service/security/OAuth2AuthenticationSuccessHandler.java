package com.example.auth_service.security;

import com.example.auth_service.dto.response.AuthResponse;
import com.example.auth_service.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectProvider<AuthService> authServiceProvider;

    @Value("${app.oauth2.authorized-redirect-uri}")
    private String authorizedRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = Objects.toString(oauth2User.getAttributes().get("email"), "").trim();
        String fullName = Objects.toString(oauth2User.getAttributes().get("name"), "").trim();
        if (!StringUtils.hasText(email)) {
            response.sendRedirect(authorizedRedirectUri + "#error=Email%20not%20provided%20by%20Google");
            return;
        }

        AuthResponse authResponse = authServiceProvider.getObject().oauth2Login(email, fullName);
        String redirectUrl = buildRedirectUrl(authResponse);
        response.sendRedirect(redirectUrl);
    }

    private String buildRedirectUrl(AuthResponse authResponse) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("accessToken", authResponse.accessToken());
        params.put("refreshToken", authResponse.refreshToken());
        params.put("tokenType", authResponse.tokenType());
        params.put("expiresIn", String.valueOf(authResponse.expiresIn()));

        String fragment = params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        return authorizedRedirectUri + "#" + fragment;
    }
}
