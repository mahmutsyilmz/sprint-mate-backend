package com.sprintmate.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiting filter.
 * Limits requests per session to prevent API abuse.
 *
 * Limits:
 * - General API: 60 requests per minute
 * - Match creation (POST /api/matches/find): 10 requests per minute
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int GENERAL_LIMIT = 60;
    private static final int MATCH_LIMIT = 10;
    private static final int OTP_SEND_LIMIT = 3;
    private static final int OTP_VERIFY_LIMIT = 10;
    private static final long WINDOW_MS = 60_000; // 1 minute

    private final Map<String, RateWindow> generalWindows = new ConcurrentHashMap<>();
    private final Map<String, RateWindow> matchWindows = new ConcurrentHashMap<>();
    private final Map<String, RateWindow> otpSendWindows = new ConcurrentHashMap<>();
    private final Map<String, RateWindow> otpVerifyWindows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        String requestUri = request.getRequestURI();

        // Only rate-limit API endpoints
        if (!requestUri.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = resolveClientKey(request);

        // Check OTP send rate limit (3 per minute)
        if ("POST".equalsIgnoreCase(request.getMethod())
                && requestUri.equals("/api/users/me/email/send-verification")) {
            if (!isAllowed(otpSendWindows, clientKey, OTP_SEND_LIMIT)) {
                log.warn("OTP send rate limit exceeded for client {}", clientKey);
                sendTooManyRequests(response);
                return;
            }
        }

        // Check OTP verify rate limit (10 per minute)
        if ("POST".equalsIgnoreCase(request.getMethod())
                && requestUri.equals("/api/users/me/email/verify")) {
            if (!isAllowed(otpVerifyWindows, clientKey, OTP_VERIFY_LIMIT)) {
                log.warn("OTP verify rate limit exceeded for client {}", clientKey);
                sendTooManyRequests(response);
                return;
            }
        }

        // Check match-specific rate limit
        if ("POST".equalsIgnoreCase(request.getMethod())
                && requestUri.startsWith("/api/matches/find")) {
            if (!isAllowed(matchWindows, clientKey, MATCH_LIMIT)) {
                log.warn("Match rate limit exceeded for client {}", clientKey);
                sendTooManyRequests(response);
                return;
            }
        }

        // Check general rate limit
        if (!isAllowed(generalWindows, clientKey, GENERAL_LIMIT)) {
            log.warn("General rate limit exceeded for client {}", clientKey);
            sendTooManyRequests(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowed(Map<String, RateWindow> windows, String key, int limit) {
        long now = System.currentTimeMillis();
        RateWindow window = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.startTime > WINDOW_MS) {
                return new RateWindow(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });
        return window.count.get() <= limit;
    }

    private String resolveClientKey(HttpServletRequest request) {
        // Prefer authenticated user principal (prevents different users sharing a bucket)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "user:" + auth.getName();
        }
        // Fall back to session ID, then IP for unauthenticated endpoints
        String sessionId = request.getRequestedSessionId();
        if (sessionId != null) {
            return "session:" + sessionId;
        }
        return "ip:" + request.getRemoteAddr();
    }

    private void sendTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", "60");
        response.setContentType("application/json");
        response.getWriter().write(
            "{\"timestamp\":\"%s\",\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}"
                .formatted(java.time.LocalDateTime.now())
        );
    }

    /**
     * Evicts expired rate limit windows every 2 minutes to prevent memory leak.
     */
    @Scheduled(fixedDelay = 120_000)
    public void evictExpiredWindows() {
        long now = System.currentTimeMillis();
        generalWindows.entrySet().removeIf(e -> now - e.getValue().startTime() > WINDOW_MS);
        matchWindows.entrySet().removeIf(e -> now - e.getValue().startTime() > WINDOW_MS);
        otpSendWindows.entrySet().removeIf(e -> now - e.getValue().startTime() > WINDOW_MS);
        otpVerifyWindows.entrySet().removeIf(e -> now - e.getValue().startTime() > WINDOW_MS);
    }

    private record RateWindow(long startTime, AtomicInteger count) {}
}
