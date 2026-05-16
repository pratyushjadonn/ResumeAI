package com.example.auth_service.config;

import com.example.auth_service.security.CustomOAuth2UserService;
import com.example.auth_service.security.JwtAuthenticationFilter;
import com.example.auth_service.security.OAuth2AuthenticationFailureHandler;
import com.example.auth_service.security.OAuth2AuthenticationSuccessHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private CustomOAuth2UserService customOAuth2UserService;

    @Mock
    private OAuth2AuthenticationSuccessHandler successHandler;

    @Mock
    private OAuth2AuthenticationFailureHandler failureHandler;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(
                jwtAuthenticationFilter,
                userDetailsService,
                customOAuth2UserService,
                successHandler,
                failureHandler
        );
    }

    @Test
    void buildsCorsConfigurationFromAllowedOrigins() {
        ReflectionTestUtils.setField(securityConfig, "allowedOrigins", "http://localhost:3000,http://127.0.0.1:3000");

        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        CorsConfiguration configuration = source.getCorsConfiguration(new MockHttpServletRequest());

        assertEquals(2, configuration.getAllowedOrigins().size());
        assertTrue(configuration.getAllowedHeaders().contains("X-CSRF-TOKEN"));
        assertTrue(configuration.getAllowCredentials());
    }

    @Test
    void rejectsWildcardOriginsWhenCredentialsAreEnabled() {
        ReflectionTestUtils.setField(securityConfig, "allowedOrigins", "*");

        assertThrows(IllegalStateException.class, securityConfig::corsConfigurationSource);
    }

    @Test
    void createsAuthenticationInfrastructure() {
        AuthenticationProvider provider = securityConfig.authenticationProvider();

        assertInstanceOf(DaoAuthenticationProvider.class, provider);
        assertTrue(securityConfig.passwordEncoder().matches("secret", securityConfig.passwordEncoder().encode("secret")));
    }
}
