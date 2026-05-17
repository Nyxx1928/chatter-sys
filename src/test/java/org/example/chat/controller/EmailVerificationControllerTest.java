package org.example.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.chat.config.WebMvcTestConfig;
import org.example.chat.dto.ResendVerificationRequest;
import org.example.chat.entity.User;
import org.example.chat.service.EmailVerificationService;
import org.example.chat.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EmailVerificationController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
})
@Import({WebMvcTestConfig.class, org.example.chat.exception.GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class EmailVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmailVerificationService emailVerificationService;

    @MockBean
    private RegistrationService registrationService;

    @Test
    void verifyEmail_ValidToken_RedirectsToFrontendSuccessPage() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        when(registrationService.completeRegistration("valid-token")).thenReturn(user);

        mockMvc.perform(get("/api/auth/verify-email").param("token", "valid-token"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        "http://localhost:3000/auth/verify-email?status=success&message=Email%20verified%20successfully!%20You%20can%20now%20log%20in."));
    }

    @Test
    void verifyEmail_InvalidToken_RedirectsToFrontendErrorPage() throws Exception {
        when(registrationService.completeRegistration("bad-token"))
                .thenThrow(new IllegalArgumentException("Invalid verification token"));
        doThrow(new IllegalArgumentException("Invalid verification token"))
                .when(emailVerificationService).verifyEmail("bad-token");

        mockMvc.perform(get("/api/auth/verify-email").param("token", "bad-token"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        "http://localhost:3000/auth/verify-email?status=error&message=Invalid%20verification%20token"));
    }

    @Test
    void resendVerification_ValidRequest_ReturnsOk() throws Exception {
        ResendVerificationRequest request = new ResendVerificationRequest("test@example.com");

        doNothing().when(emailVerificationService).resendVerification(anyString());

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
