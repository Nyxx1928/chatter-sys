package org.example.chat.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.chat.dto.RegisterRequest;
import org.example.chat.dto.ResendOtpRequest;
import org.example.chat.dto.VerifyOtpRequest;
import org.example.chat.entity.PendingRegistration;
import org.example.chat.repository.PendingRegistrationRepository;
import org.example.chat.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.example.chat.service.RateLimiterService;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OtpVerificationE2EIT extends BaseIntegrationTest {

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RateLimiterService rateLimiterService;

    @AfterEach
    void tearDown() {
        pendingRegistrationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void otpVerificationFlow_RegisterVerifyLogin_Success() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("otpuser", "otp@example.com", "TestP@ss1", "OTP User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(registerRequest))
                .with(r -> { r.setRemoteAddr("10.1.1.1"); return r; }))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailSent").isBoolean());

        PendingRegistration pending = pendingRegistrationRepository.findByEmail("otp@example.com").orElse(null);
        assertNotNull(pending);
        assertEquals("otpuser", pending.getUsername());

        String knownOtp = "123456";
        pending.setOtpHash(passwordEncoder.encode(knownOtp));
        pending.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        pending.setAttemptCount(0);
        pendingRegistrationRepository.save(pending);

        VerifyOtpRequest verifyRequest = new VerifyOtpRequest("otp@example.com", knownOtp);
        mockMvc.perform(post("/api/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email verified successfully"));

        assertFalse(pendingRegistrationRepository.findByEmail("otp@example.com").isPresent());
        assertTrue(userRepository.findByUsername("otpuser").isPresent());
        assertTrue(userRepository.findByUsername("otpuser").get().getEmailVerified());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new org.example.chat.dto.LoginRequest("otpuser", "TestP@ss1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn();

        JsonNode loginResponse = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        assertNotNull(loginResponse.get("token").asText());
    }

    @Test
    void otpVerification_ExpiredOtp_ReturnsFailure() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("expireuser", "expire@example.com", "TestP@ss1", "Expire User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(registerRequest))
                .with(r -> { r.setRemoteAddr("10.1.1.2"); return r; }))
                .andExpect(status().isCreated());

        PendingRegistration pending = pendingRegistrationRepository.findByEmail("expire@example.com").orElse(null);
        assertNotNull(pending);

        String knownOtp = "123456";
        pending.setOtpHash(passwordEncoder.encode(knownOtp));
        pending.setOtpExpiry(LocalDateTime.now().minusMinutes(1));
        pending.setAttemptCount(0);
        pendingRegistrationRepository.save(pending);

        VerifyOtpRequest verifyRequest = new VerifyOtpRequest("expire@example.com", knownOtp);
        mockMvc.perform(post("/api/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Code expired"));

        assertFalse(pendingRegistrationRepository.findByEmail("expire@example.com").isPresent());
    }

    @Test
    void otpVerification_MaxAttempts_BlocksAndAllowsReRegister() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("maxattuser", "maxatt@example.com", "TestP@ss1", "Max Att User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(registerRequest))
                .with(r -> { r.setRemoteAddr("10.1.1.3"); return r; }))
                .andExpect(status().isCreated());

        PendingRegistration pending = pendingRegistrationRepository.findByEmail("maxatt@example.com").orElse(null);
        assertNotNull(pending);
        pending.setOtpHash(passwordEncoder.encode("123456"));
        pending.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        pending.setAttemptCount(0);
        pendingRegistrationRepository.save(pending);

        for (int i = 0; i < 3; i++) {
            String wrongOtp = String.format("%06d", 999999 - i);
            VerifyOtpRequest wrongRequest = new VerifyOtpRequest("maxatt@example.com", wrongOtp);

            int expectedRemaining = 2 - i;
            if (expectedRemaining > 0) {
                mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(wrongRequest)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.message").value("Invalid code. " + expectedRemaining + " attempts remaining."));
            } else {
                mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(wrongRequest)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.message").value("Too many attempts"));
            }
        }

        assertFalse(pendingRegistrationRepository.findByEmail("maxatt@example.com").isPresent());

        RegisterRequest reRegisterRequest = new RegisterRequest("maxattuser", "maxatt@example.com", "TestP@ss1", "Max Att User");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(reRegisterRequest))
                .with(r -> { r.setRemoteAddr("10.1.1.4"); return r; }))
                .andExpect(status().isCreated());

        assertTrue(pendingRegistrationRepository.findByEmail("maxatt@example.com").isPresent());
    }

    @Test
    void resendOtp_InvalidatesOldOtp() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("resenduser", "resend@example.com", "TestP@ss1", "Resend User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(registerRequest))
                .with(r -> { r.setRemoteAddr("10.1.1.5"); return r; }))
                .andExpect(status().isCreated());

        PendingRegistration pending = pendingRegistrationRepository.findByEmail("resend@example.com").orElse(null);
        assertNotNull(pending);
        String oldHash = pending.getOtpHash();

        ResendOtpRequest resendRequest = new ResendOtpRequest("resend@example.com");
        mockMvc.perform(post("/api/auth/resend-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(resendRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        PendingRegistration updated = pendingRegistrationRepository.findByEmail("resend@example.com").orElse(null);
        assertNotNull(updated);
        assertNotEquals(oldHash, updated.getOtpHash());
        assertEquals(0, updated.getAttemptCount());

        String knownOtp = "654321";
        updated.setOtpHash(passwordEncoder.encode(knownOtp));
        updated.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        updated.setAttemptCount(0);
        pendingRegistrationRepository.save(updated);

        VerifyOtpRequest verifyRequest = new VerifyOtpRequest("resend@example.com", knownOtp);
        mockMvc.perform(post("/api/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void rateLimiting_OtpVerification_EnforcesLimit() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("rateotpuser", "rateotp@example.com", "TestP@ss1", "Rate OTP User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(registerRequest))
                .with(r -> { r.setRemoteAddr("10.99.99.1"); return r; }))
                .andExpect(status().isCreated());

        ReflectionTestUtils.setField(rateLimiterService, "rateLimitEnabled", true);
        try {
            String ip = "10.99.99.99";
            for (int i = 0; i < 5; i++) {
                VerifyOtpRequest req = new VerifyOtpRequest("rateotp@example.com", "000000");
                mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req))
                        .with(r -> { r.setRemoteAddr(ip); return r; }))
                        .andExpect(status().isOk());
            }

            VerifyOtpRequest overflow = new VerifyOtpRequest("rateotp@example.com", "000000");
            mockMvc.perform(post("/api/auth/verify-otp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(overflow))
                    .with(r -> { r.setRemoteAddr(ip); return r; }))
                    .andExpect(status().isTooManyRequests());
        } finally {
            ReflectionTestUtils.setField(rateLimiterService, "rateLimitEnabled", false);
        }
    }
}
