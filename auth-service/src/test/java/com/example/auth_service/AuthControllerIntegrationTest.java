package com.example.auth_service;

import com.example.auth_service.dto.request.LoginRequest;
import com.example.auth_service.dto.request.RefreshTokenRequest;
import com.example.auth_service.dto.request.RegisterRequest;
import com.example.auth_service.entity.OtpType;
import com.example.auth_service.entity.OtpVerification;
import com.example.auth_service.repository.OtpVerificationRepository;
import com.example.auth_service.service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OtpVerificationRepository otpVerificationRepository;

    @MockBean
    private EmailService emailService;

    @Test
    void authFlowShouldRegisterVerifyOtpLoginRefreshAndResetPassword() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("Test User", "test@example.com", "Password1");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.type").doesNotExist());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("test@example.com", "Password1"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Please verify your email before logging in."));

        OtpVerification verifyOtp = otpVerificationRepository
                .findTopByEmailIgnoreCaseAndTypeAndUsedFalseOrderByCreatedAtDesc("test@example.com", OtpType.VERIFY_EMAIL)
                .orElseThrow();

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "test@example.com",
                                "otp", verifyOtp.getOtp(),
                                "type", OtpType.VERIFY_EMAIL.name()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("test@example.com", "Password1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode loginJson = objectMapper.readTree(loginResponse);
        String accessToken = loginJson.get("accessToken").asText();
        String refreshToken = loginJson.get("refreshToken").asText();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Test User"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.verified").value(true));

        String refreshedTokens = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String latestRefreshToken = objectMapper.readTree(refreshedTokens).get("refreshToken").asText();
        assertThat(latestRefreshToken).isNotEqualTo(refreshToken);

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "test@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("RESET_PASSWORD"));

        OtpVerification resetOtp = otpVerificationRepository
                .findTopByEmailIgnoreCaseAndTypeAndUsedFalseOrderByCreatedAtDesc("test@example.com", OtpType.RESET_PASSWORD)
                .orElseThrow();

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "test@example.com",
                                "otp", resetOtp.getOtp(),
                                "type", OtpType.RESET_PASSWORD.name()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "test@example.com",
                                "otp", resetOtp.getOtp(),
                                "newPassword", "Password2"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("test@example.com", "Password1"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("test@example.com", "Password2"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }
}
