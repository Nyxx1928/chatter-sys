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

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.port:0}")
    private int mailPort;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.mail.from:}")
    private String configuredFrom;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean sendVerificationEmail(String to, String verificationUrl) {
        String subject = "Verify your email - Real-Time Chat";
        String message = """
            Thank you for registering!
            
            Please verify your email address by clicking the link below:
            %s
            
            This link will expire in 24 hours.
            
            If you did not register for this account, please ignore this email.
            """.formatted(verificationUrl);

        return sendEmail(to, subject, message);
    }

    public boolean sendPasswordResetEmail(String to, String resetUrl, String username) {
        String subject = "Reset Your Password - Real-Time Chat";
        String message = """
            Hello %s,
            
            We received a request to reset your password. Click the link below to set a new password:
            %s
            
            This link will expire in 15 minutes.
            
            If you did not request a password reset, please ignore this email.
            """.formatted(username, resetUrl);

        return sendEmail(to, subject, message);
    }

    private boolean sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(to);
            mailMessage.setSubject(subject);
            mailMessage.setText(text);
            String from = (configuredFrom != null && !configuredFrom.isBlank()) ? configuredFrom : fromEmail;
            if (from != null && !from.isBlank() && from.contains("@")) {
                mailMessage.setFrom(from);
            } else if (from != null && !from.isBlank()) {
                logger.warn("Configured mail From is not an email address; not setting From header: {}", from);
            }
            mailSender.send(mailMessage);
            logger.info("Verification email sent to: {}", to);
            return true;
        } catch (Exception e) {
            logger.error(
                    "Failed to send email to {} via SMTP {}:{} (check MAIL_* env vars).",
                    to, mailHost, mailPort, e);
            return false;
        }
    }
}
