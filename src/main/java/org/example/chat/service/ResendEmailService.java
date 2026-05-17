package org.example.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Service for sending emails using Resend HTTP API.
 * More reliable than SMTP and provides better error messages.
 */
@Service
public class ResendEmailService {

    private static final Logger logger = LoggerFactory.getLogger(ResendEmailService.class);
    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final WebClient webClient;
    private final String apiKey;
    private final String fromEmail;
    private final boolean enabled;

    public ResendEmailService(
            @Value("${resend.api-key:}") String apiKey,
            @Value("${resend.from-email:}") String fromEmail,
            @Value("${resend.enabled:true}") boolean enabled,
            WebClient.Builder webClientBuilder) {
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.enabled = enabled;
        this.webClient = webClientBuilder
                .baseUrl(RESEND_API_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        if (!enabled) {
            logger.warn("Resend email service is DISABLED. Set resend.enabled=true to enable.");
        } else if (apiKey == null || apiKey.isBlank()) {
            logger.error("Resend API key is not configured! Set RESEND_API_KEY environment variable.");
        } else if (fromEmail == null || fromEmail.isBlank()) {
            logger.error("Resend from email is not configured! Set RESEND_FROM_EMAIL environment variable.");
        } else {
            logger.info("Resend email service initialized with from: {}", fromEmail);
        }
    }

    /**
     * Sends a verification email using Resend HTTP API.
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
            logger.error("Cannot send email: Resend API key not configured");
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

    /**
     * Sends an email using Resend HTTP API.
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
                    "from", fromEmail,
                    "to", new String[]{to},
                    "subject", subject,
                    "html", htmlContent,
                    "text", textContent
            );

            logger.debug("Sending email to {} via Resend API", to);

            String response = webClient.post()
                    .bodyValue(emailRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            logger.info("Successfully sent verification email to: {}", to);
            logger.debug("Resend API response: {}", response);

            // Parse response to get email ID
            String emailId = parseEmailId(response);
            return new EmailResult(true, null, emailId);

        } catch (WebClientResponseException e) {
            logger.error("Resend API error (status {}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            return new EmailResult(false, "Email API error: " + e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Failed to send email to {} via Resend API", to, e);
            return new EmailResult(false, "Failed to send email: " + e.getMessage(), null);
        }
    }

    private String parseEmailId(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> responseMap = mapper.readValue(response, Map.class);
            return (String) responseMap.get("id");
        } catch (Exception e) {
            logger.warn("Could not parse email ID from response", e);
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

    /**
     * Result of an email sending operation.
     *
     * @param success whether the email was sent successfully
     * @param errorMessage error message if failed, null if successful
     * @param emailId the Resend email ID if successful, null if failed
     */
    public record EmailResult(boolean success, String errorMessage, String emailId) {}
}
