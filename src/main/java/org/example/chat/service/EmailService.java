package org.example.chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String to, String token) {
        String subject = "Verify your email - Real-Time Chat";
        String verificationUrl = baseUrl + "/api/auth/verify-email?token=" + token;
        String message = """
            Thank you for registering!
            
            Please verify your email address by clicking the link below:
            %s
            
            This link will expire in 24 hours.
            
            If you did not register for this account, please ignore this email.
            """.formatted(verificationUrl);

        sendEmail(to, subject, message);
    }

    private void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(to);
            mailMessage.setSubject(subject);
            mailMessage.setText(text);
            if (fromEmail != null && !fromEmail.isBlank()) {
                mailMessage.setFrom(fromEmail);
            }
            mailSender.send(mailMessage);
            logger.info("Verification email sent to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
