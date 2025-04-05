package com.taskmanager.service.impl;

import com.taskmanager.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.properties.mail.smtp.from}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            logger.info("Attempting to send password reset email from: {} to: {}", fromEmail, toEmail.trim());

            InternetAddress fromAddressObj = new InternetAddress(fromEmail);
            helper.setFrom(fromAddressObj);
            helper.setTo(toEmail.trim()); // Trim the recipient email for safety
            helper.setSubject("Password Reset Request");

            String resetLink = frontendUrl + "/reset-password?token=" + token;
            String emailBody = "<p>You have requested a password reset.</p>"
                    + "<p>Please click on the following link to reset your password:</p>"
                    + "<p><a href=\"" + resetLink + "\">" + resetLink + "</a></p>"
                    + "<p>This link will expire in 1 hour.</p>"
                    + "<p>If you did not request this, please ignore this email.</p>";

            helper.setText(emailBody, true); // Set to true for HTML

            logger.debug("Email content: {}", emailBody);

            mailSender.send(message);
            logger.info("Password reset email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            logger.error("Error sending password reset email to: {}", toEmail, e);
            // Consider throwing a custom exception here if you want to handle email sending failures differently
        }
    }
}