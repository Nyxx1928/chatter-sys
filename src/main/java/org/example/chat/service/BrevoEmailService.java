package org.example.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Service for sending emails using Brevo (formerly Sendinblue) HTTP API.
 * Brevo offers 300 emails/day on free tier with no domain required.
 */
@Service
public class BrevoEmailService {

    private static final Logger logger = LoggerFactory.getLogger(BrevoEmailService.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final WebClient webClient;
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;
    private final boolean enabled;

    public BrevoEmailService(
            @Value("${brevo.api-key:}") String apiKey,
            @Value("${brevo.from-email:}") String fromEmail,
            @Value("${brevo.from-name:Real-Time Chat}") String fromName,
            @Value("${brevo.enabled:false}") boolean enabled,
            WebClient.Builder webClientBuilder) {
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.enabled = enabled;
        this.webClient = webClientBuilder
                .baseUrl(BREVO_API_URL)
                .defaultHeader("api-key", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();

        if (!enabled) {
            logger.warn("Brevo email service is DISABLED. Set brevo.enabled=true to enable.");
        } else if (apiKey == null || apiKey.isBlank()) {
            logger.error("Brevo API key is not configured! Set BREVO_API_KEY environment variable.");
        } else if (fromEmail == null || fromEmail.isBlank()) {
            logger.error("Brevo from email is not configured! Set BREVO_FROM_EMAIL environment variable.");
        } else {
            logger.info("Brevo email service initialized with from: {} <{}>", fromName, fromEmail);
        }
    }

    /**
     * Sends a verification email using Brevo HTTP API.
     *
     * @param to the recipient email address
     * @param verificationUrl the verification URL to include in the email
     * @return EmailResult containing success status and any error message
     */
    public EmailResult sendVerificationEmail(String to, String verificationUrl) {
        if (!enabled) {
            logger.warn("Email service disabled. Would have sent verification email to: {}", to);
            return new EmailResult(false, "Email service is disabled", null);
        }

        if (apiKey == null || apiKey.isBlank()) {
            logger.error("Cannot send email: Brevo API key not configured");
            return new EmailResult(false, "Email service not configured", null);
        }

        if (fromEmail == null || fromEmail.isBlank()) {
            logger.error("Cannot send email: From email not configured");
            return new EmailResult(false, "Email service not configured", null);
        }

        String subject = "Verify your email - Real-Time Chat";
        String htmlContent = buildVerificationEmailHtml(verificationUrl);
        String textContent = buildVerificationEmailText(verificationUrl);

        return sendEmail(to, subject, htmlContent, textContent);
    }

    public EmailResult sendPasswordResetEmail(String to, String resetUrl, String username) {
        if (!enabled) {
            logger.warn("Email service disabled. Would have sent password reset email to: {}", to);
            return new EmailResult(false, "Email service is disabled", null);
        }

        if (apiKey == null || apiKey.isBlank()) {
            logger.error("Cannot send email: Brevo API key not configured");
            return new EmailResult(false, "Email service not configured", null);
        }

        if (fromEmail == null || fromEmail.isBlank()) {
            logger.error("Cannot send email: From email not configured");
            return new EmailResult(false, "Email service not configured", null);
        }

        String subject = "Reset Your Password - Real-Time Chat";
        String htmlContent = buildPasswordResetEmailHtml(resetUrl, username);
        String textContent = buildPasswordResetEmailText(resetUrl, username);

        return sendEmail(to, subject, htmlContent, textContent);
    }

    /**
     * Sends an email using Brevo HTTP API.
     *
     * @param to the recipient email address
     * @param subject the email subject
     * @param htmlContent the HTML content
     * @param textContent the plain text content (fallback)
     * @return EmailResult containing success status and any error message
     */
    private EmailResult sendEmail(String to, String subject, String htmlContent, String textContent) {
        try {
            Map<String, Object> emailRequest = Map.of(
                    "sender", Map.of(
                            "name", fromName,
                            "email", fromEmail
                    ),
                    "to", List.of(Map.of("email", to)),
                    "subject", subject,
                    "htmlContent", htmlContent,
                    "textContent", textContent
            );

            logger.debug("Sending email to {} via Brevo API", to);

            String response = webClient.post()
                    .bodyValue(emailRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            logger.info("Successfully sent verification email to: {}", to);
            logger.debug("Brevo API response: {}", response);

            // Parse response to get message ID
            String messageId = parseMessageId(response);
            return new EmailResult(true, null, messageId);

        } catch (WebClientResponseException e) {
            logger.error("Brevo API error (status {}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            return new EmailResult(false, "Email API error: " + e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Failed to send email to {} via Brevo API", to, e);
            return new EmailResult(false, "Failed to send email: " + e.getMessage(), null);
        }
    }

    private String parseMessageId(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> responseMap = mapper.readValue(response, Map.class);
            return (String) responseMap.get("messageId");
        } catch (Exception e) {
            logger.warn("Could not parse message ID from response", e);
            return null;
        }
    }

    private String buildVerificationEmailHtml(String verificationUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background-color: #f4f4f4; padding: 20px; border-radius: 10px;">
                    <h1 style="color: #4a5568; margin-bottom: 20px;">Welcome to Real-Time Chat!</h1>
                    <p style="font-size: 16px; margin-bottom: 20px;">
                        Thank you for registering! Please verify your email address to complete your registration.
                    </p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" 
                           style="background-color: #4299e1; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block; font-weight: bold;">
                            Verify Email Address
                        </a>
                    </div>
                    <p style="font-size: 14px; color: #666; margin-top: 20px;">
                        Or copy and paste this link into your browser:
                    </p>
                    <p style="font-size: 12px; color: #4299e1; word-break: break-all; background-color: #fff; padding: 10px; border-radius: 5px;">
                        %s
                    </p>
                    <p style="font-size: 14px; color: #666; margin-top: 30px;">
                        This link will expire in 24 hours.
                    </p>
                    <p style="font-size: 14px; color: #666;">
                        If you did not register for this account, please ignore this email.
                    </p>
                </div>
            </body>
            </html>
            """.formatted(verificationUrl, verificationUrl);
    }

    private String buildVerificationEmailText(String verificationUrl) {
        return """
            Welcome to Real-Time Chat!
            
            Thank you for registering! Please verify your email address to complete your registration.
            
            Click the link below to verify your email:
            %s
            
            This link will expire in 24 hours.
            
            If you did not register for this account, please ignore this email.
            """.formatted(verificationUrl);
    }

    private String buildPasswordResetEmailHtml(String resetUrl, String username) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background-color: #f4f4f4; padding: 20px; border-radius: 10px;">
                    <h1 style="color: #4a5568; margin-bottom: 20px;">Reset Your Password</h1>
                    <p style="font-size: 16px; margin-bottom: 20px;">
                        Hello %s,
                    </p>
                    <p style="font-size: 16px; margin-bottom: 20px;">
                        We received a request to reset your password. Click the button below to set a new password.
                    </p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s"
                           style="background-color: #4299e1; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block; font-weight: bold;">
                            Reset Password
                        </a>
                    </div>
                    <p style="font-size: 14px; color: #666; margin-top: 20px;">
                        Or copy and paste this link into your browser:
                    </p>
                    <p style="font-size: 12px; color: #4299e1; word-break: break-all; background-color: #fff; padding: 10px; border-radius: 5px;">
                        %s
                    </p>
                    <p style="font-size: 14px; color: #666; margin-top: 30px;">
                        This link will expire in 15 minutes.
                    </p>
                    <p style="font-size: 14px; color: #666;">
                        If you did not request a password reset, please ignore this email.
                    </p>
                </div>
            </body>
            </html>
            """.formatted(username, resetUrl, resetUrl);
    }

    private String buildPasswordResetEmailText(String resetUrl, String username) {
        return """
            Hello %s,
            
            We received a request to reset your password. Click the link below to set a new password:
            %s
            
            This link will expire in 15 minutes.
            
            If you did not request a password reset, please ignore this email.
            """.formatted(username, resetUrl);
    }

    /**
     * Result of an email sending operation.
     *
     * @param success whether the email was sent successfully
     * @param errorMessage error message if failed, null if successful
     * @param messageId the Brevo message ID if successful, null if failed
     */
    public record EmailResult(boolean success, String errorMessage, String messageId) {}
}
