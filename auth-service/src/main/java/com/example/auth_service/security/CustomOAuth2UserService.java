package com.example.auth_service.security;

import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final String GOOGLE = "google";
    private static final String EMAIL = "email";
    private static final String NAME = "name";

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        if (!GOOGLE.equalsIgnoreCase(registrationId)) {
            throw new InternalAuthenticationServiceException("Unsupported oauth2 provider: " + registrationId);
        }

        Map<String, Object> attributes = new LinkedHashMap<>(oAuth2User.getAttributes());
        String email = asText(attributes.get(EMAIL));
        if (!StringUtils.hasText(email)) {
            throw new InternalAuthenticationServiceException("Email not found from Google account");
        }

        String fullName = asText(attributes.get(NAME));
        if (!StringUtils.hasText(fullName)) {
            fullName = email;
        }

        attributes.put(EMAIL, email.toLowerCase(Locale.ROOT).trim());
        attributes.put(NAME, fullName.trim());

        Set<GrantedAuthority> authorities = new HashSet<>(oAuth2User.getAuthorities());
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        return new DefaultOAuth2User(authorities, attributes, EMAIL);
    }

    private String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
