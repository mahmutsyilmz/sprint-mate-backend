package com.sprintmate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Service for sending asynchronous email notifications via Resend HTTP API.
 *
 * Business Intent:
 * Notifies both matched users immediately after a sprint match is created.
 * All email sending runs on a dedicated thread pool ("emailTaskExecutor") so
 * API latency never delays the HTTP response to the matching request.
 *
 * Uses Resend (https://resend.com) instead of SMTP to avoid port-blocking
 * issues on cloud platforms like Railway.
 */
@Service
@Slf4j
public class EmailNotificationService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final RestTemplate restTemplate;
    private final String frontendUrl;
    private final String resendApiKey;
    private final String fromEmail;

    public EmailNotificationService(
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${resend.api-key:}") String resendApiKey,
            @Value("${resend.from-email:Sprint Mate <onboarding@resend.dev>}") String fromEmail) {
        this.restTemplate = new RestTemplate();
        this.frontendUrl = frontendUrl;
        this.resendApiKey = resendApiKey;
        this.fromEmail = fromEmail;
    }

    /**
     * Sends a match notification email to one participant.
     * Runs asynchronously on the email thread pool — does NOT block the HTTP response.
     */
    @Async("emailTaskExecutor")
    public void sendMatchNotification(String toEmail, String userName,
                                      String partnerName, String partnerRole, String matchTopic) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Skipping match notification for user '{}' — email address is unavailable", userName);
            return;
        }
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("Skipping match notification — RESEND_API_KEY is not configured");
            return;
        }
        try {
            sendEmail(toEmail, "\uD83D\uDE80 Your Sprint Mate is Ready!",
                    buildEmailBody(userName, partnerName, partnerRole, matchTopic));
            log.info("Match notification sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send match notification to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    /**
     * Sends an email verification OTP to the user.
     * Runs asynchronously — does NOT block the HTTP response.
     */
    @Async("emailTaskExecutor")
    public void sendVerificationEmail(String toEmail, String userName, String otpCode) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("Skipping verification email — RESEND_API_KEY is not configured");
            return;
        }
        try {
            sendEmail(toEmail, "Your Sprint Mate Verification Code",
                    buildVerificationEmailBody(userName, otpCode));
            log.info("Verification email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    private void sendEmail(String to, String subject, String html) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(resendApiKey);

        Map<String, Object> body = Map.of(
                "from", fromEmail,
                "to", List.of(to),
                "subject", subject,
                "html", html
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(RESEND_API_URL, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Resend API returned " + response.getStatusCode() + ": " + response.getBody());
        }
    }

    private String buildVerificationEmailBody(String userName, String otpCode) {
        return """
                <html>
                <body style="font-family: sans-serif; color: #333; max-width: 600px; margin: 0 auto;">
                  <h2 style="color: #4F46E5;">🔐 Verify Your Email</h2>
                  <p>Hi <strong>%s</strong>,</p>
                  <p>Use the code below to verify your email address on Sprint Mate:</p>
                  <div style="text-align: center; margin: 32px 0;">
                    <span style="display: inline-block; background: #F5F3FF; border: 2px solid #4F46E5;
                                 border-radius: 8px; padding: 16px 32px; font-size: 36px;
                                 font-weight: bold; letter-spacing: 8px; color: #4F46E5;
                                 font-family: monospace;">
                      %s
                    </span>
                  </div>
                  <p>This code expires in <strong>10 minutes</strong>.</p>
                  <p>If you did not request this, you can safely ignore this email.</p>
                  <p style="color:#888; font-size:12px; margin-top:32px;">
                    This email was sent by Sprint Mate.
                  </p>
                </body>
                </html>
                """.formatted(userName, otpCode);
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
