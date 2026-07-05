package org.example.chat.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.chat.dto.ForgotPasswordRequest;
import org.example.chat.dto.LoginRequest;
import org.example.chat.dto.LoginResponse;
import org.example.chat.dto.RegisterRequest;
import org.example.chat.dto.ResendOtpRequest;
import org.example.chat.dto.ResetPasswordRequest;
import org.example.chat.dto.UserResponse;
import org.example.chat.dto.VerifyOtpRequest;
import org.example.chat.dto.VerifyOtpResponse;
import org.example.chat.entity.User;
import org.example.chat.service.AuthenticationService;
import org.example.chat.service.ForgotPasswordService;
import org.example.chat.service.RateLimiterService;
import org.example.chat.service.RegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication operations.
 * Handles user registration and login.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationService authenticationService;
    private final ForgotPasswordService forgotPasswordService;
    private final RateLimiterService rateLimiterService;
    private final RegistrationService registrationService;

    public AuthController(AuthenticationService authenticationService,
                          ForgotPasswordService forgotPasswordService,
                          RateLimiterService rateLimiterService,
                          RegistrationService registrationService) {
        this.authenticationService = authenticationService;
        this.forgotPasswordService = forgotPasswordService;
        this.rateLimiterService = rateLimiterService;
        this.registrationService = registrationService;
    }

    /**
     * Registers a new user.
     * Creates a pending registration and sends verification email.
     * User must verify email before account is created.
     *
     * @param request the registration request containing username, email, password, and display name
     * @return ResponseEntity with registration status
     */
    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        logger.info("Registration request received for username: {}", request.getUsername());

        // Rate limit: 3 registrations per 60 minutes per IP (JLabs3 pattern)
        rateLimiterService.checkRegistration(httpRequest.getRemoteAddr());

        try {
            AuthenticationService.RegistrationResult result = authenticationService.registerUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getDisplayName()
            );

            if (!result.verificationEmailSent()) {
                logger.warn("Registration email failed to send for username {}: {}",
                        request.getUsername(), result.errorMessage());
                RegistrationResponse response = new RegistrationResponse(
                        "Registration saved but verification email could not be sent. Please contact support.",
                        false,
                        result.errorMessage()
                );
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            }

            RegistrationResponse response = new RegistrationResponse(
                    "Registration initiated. Please check your email to verify your account.",
                    result.verificationEmailSent(),
                    result.errorMessage()
            );

            logger.info("Registration initiated for username: {}, email sent: {}",
                    request.getUsername(), result.verificationEmailSent());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Registration failed for username {}: {}", request.getUsername(), e.getMessage());
            throw e;
        }
    }

    /**
     * Response for registration initiation.
     */
    public record RegistrationResponse(
            String message,
            boolean emailSent,
            String errorMessage
    ) {}

    /**
     * Authenticates a user and returns a JWT token and CSRF token.
     *
     * @param request the login request containing username and password
     * @return ResponseEntity with LoginResponse containing token, CSRF token, and user information
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info("Login request received for username: {}", request.getUsername());

        // Rate limit: 5 attempts per 1 minute per username (JLabs3 pattern)
        rateLimiterService.checkLogin(request.getUsername());

        try {
            String token = authenticationService.authenticateUser(
                request.getUsername(),
                request.getPassword()
            );

            User user = authenticationService.getUserByUsername(request.getUsername());
            
            LoginResponse response = LoginResponse.from(token, user, null);

            logger.info("User logged in successfully: {}", request.getUsername());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Login failed for username {}: {}", request.getUsername(), e.getMessage());
            throw e;
        }
    }

    /**
     * Initiates a password reset by sending a reset link to the user's email.
     * Always returns 200 OK to prevent email enumeration.
     *
     * @param request the forgot password request containing the email
     * @return ResponseEntity with 200 OK
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        logger.info("Password reset requested for email: {}", request.getEmail());

        forgotPasswordService.initiateReset(request.getEmail());

        return ResponseEntity.ok().build();
    }

    /**
     * Resets the user's password using a valid reset token.
     *
     * @param request the reset password request containing token and new password
     * @return ResponseEntity with 200 OK if successful
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        logger.info("Password reset attempt received");

        forgotPasswordService.resetPassword(request.getToken(), request.getNewPassword());

        logger.info("Password reset completed successfully");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest) {
        logger.info("OTP verification request for email: {}", request.email());

        rateLimiterService.checkOtpVerification(httpRequest.getRemoteAddr());

        RegistrationService.OtpVerificationResult result =
                registrationService.verifyOtp(request.email(), request.otp());

        VerifyOtpResponse response = new VerifyOtpResponse(result.success(), result.message());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<VerifyOtpResponse> resendOtp(
            @Valid @RequestBody ResendOtpRequest request,
            HttpServletRequest httpRequest) {
        logger.info("Resend OTP request for email: {}", request.email());

        rateLimiterService.checkResendVerification(request.email());
        registrationService.resendOtp(request.email());

        VerifyOtpResponse response = new VerifyOtpResponse(true,
                "If the email is pending verification, a new code has been sent.");
        return ResponseEntity.ok(response);
    }
}
