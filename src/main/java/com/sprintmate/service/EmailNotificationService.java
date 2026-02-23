package com.sprintmate.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service for sending asynchronous email notifications.
 *
 * Business Intent:
 * Notifies both matched users immediately after a sprint match is created.
 * All email sending runs on a dedicated thread pool ("emailTaskExecutor") so
 * SMTP latency never delays the HTTP response to the matching request.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Sends a match notification email to one participant.
     * Runs asynchronously on the email thread pool — does NOT block the HTTP response.
     *
     * If the recipient has no email address on file (GitHub email was unavailable),
     * the notification is silently skipped and a warning is logged.
     *
     * @param toEmail      Recipient email address (skipped if null/blank)
     * @param userName     Recipient's display name
     * @param partnerName  Matched partner's display name
     * @param partnerRole  Partner's role (FRONTEND / BACKEND)
     * @param matchTopic   AI-generated project title
     */
    @Async("emailTaskExecutor")
    public void sendMatchNotification(String toEmail, String userName,
                                      String partnerName, String partnerRole, String matchTopic) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Skipping match notification for user '{}' — email address is unavailable", userName);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("🚀 Your Sprint Mate is Ready!");
            helper.setText(buildEmailBody(userName, partnerName, partnerRole, matchTopic), true);
            mailSender.send(message);
            log.info("Match notification sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send match notification to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    private String buildEmailBody(String userName, String partnerName,
                                  String partnerRole, String matchTopic) {
        return """
                <html>
                <body style="font-family: sans-serif; color: #333; max-width: 600px; margin: 0 auto;">
                  <h2 style="color: #4F46E5;">🚀 Your Sprint Mate is Ready!</h2>
                  <p>Hi <strong>%s</strong>,</p>
                  <p>Great news! We found a partner for you.</p>
                  <p>You are matched with <strong>%s</strong> (<em>%s Developer</em>)
                     for a new AI-generated project:</p>
                  <blockquote style="border-left: 4px solid #4F46E5; margin: 16px 0; padding: 8px 16px;
                                     background: #F5F3FF; border-radius: 4px;">
                    <strong>%s</strong>
                  </blockquote>
                  <p>
                    <a href="%s/dashboard"
                       style="display:inline-block; background:#4F46E5; color:white;
                              padding:12px 24px; border-radius:6px; text-decoration:none;
                              font-weight:bold;">
                      Jump into the Dashboard →
                    </a>
                  </p>
                  <p style="color:#888; font-size:12px; margin-top:32px;">
                    This email was sent by Sprint Mate.<br>
                    Your sprint window is 7 days — good luck and happy coding!
                  </p>
                </body>
                </html>
                """.formatted(userName, partnerName, partnerRole, matchTopic, frontendUrl);
    }
}
