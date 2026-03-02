package com.sprintmate.config;

import com.sprintmate.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;

/**
 * Security configuration for Sprint Mate application.
 * Configures GitHub OAuth2 authentication as the primary login mechanism.
 *
 * MVP Strategy: Secure by default - all endpoints require authentication
 * except explicitly whitelisted public paths (root, error pages, H2 console).
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    // Comma-separated list of allowed CORS origins.
    // In production, set FRONTEND_URL in Railway to the Vercel domain.
    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String corsAllowedOriginsConfig;

    @Value("${springdoc.swagger-ui.enabled:true}")
    private boolean swaggerEnabled;

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Enable CORS for cross-origin requests from frontend
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Configure URL-based authorization rules
            .authorizeHttpRequests(authorize -> {
                // Public endpoints - accessible without authentication
                authorize.requestMatchers("/", "/error").permitAll();
                // H2 console - only in dev when explicitly enabled
                if (h2ConsoleEnabled) {
                    authorize.requestMatchers("/h2-console/**").permitAll();
                } else {
                    authorize.requestMatchers("/h2-console/**").denyAll();
                }
                // Swagger UI - only when springdoc is enabled (disabled in prod)
                if (swaggerEnabled) {
                    authorize.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll();
                } else {
                    authorize.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").denyAll();
                }
                // WebSocket endpoints - permitAll for HTTP handshake, auth handled at STOMP level
                authorize.requestMatchers("/ws/**", "/ws-sockjs/**").permitAll();
                // Actuator health endpoint - accessible for load balancer health checks
                authorize.requestMatchers("/actuator/health", "/actuator/info").permitAll();
                // All other endpoints require authentication
                authorize.anyRequest().authenticated();
            })
            // Configure OAuth2 login with GitHub
            .oauth2Login(oauth2 -> oauth2
                // Use custom service to sync user data on successful login
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                // Redirect to frontend after successful login.
                // Uses a success handler (evaluated at request time, not at bean init)
                // so the app starts even before FRONTEND_URL env var is configured.
                .successHandler((HttpServletRequest req, HttpServletResponse res, Authentication auth)
                    -> res.sendRedirect(frontendUrl + "/role-select"))
            )
            // Configure logout for API-based session management
            // Returns HTTP 200 OK instead of redirecting - frontend handles routing
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.OK))
            )
            // CSRF protection with cookie-based token for SPA compatibility.
            // Frontend reads XSRF-TOKEN cookie and sends it as X-XSRF-TOKEN header.
            // H2 console and WebSocket handshake are exempt (STOMP handles WS auth).
            .csrf(csrf -> {
                CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
                requestHandler.setCsrfRequestAttributeName(null); // Opt out of deferred loading
                csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(requestHandler)
                    .ignoringRequestMatchers("/h2-console/**", "/ws/**", "/ws-sockjs/**");
            })
            // Security headers for production hardening
            .headers(headers -> {
                headers.frameOptions(frame -> frame.sameOrigin());
                headers.contentTypeOptions(withDefaults());
                headers.httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000));
                headers.referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                headers.permissionsPolicy(permissions -> permissions
                    .policy("camera=(), microphone=(), geolocation=()"));
                headers.contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; " +
                        "connect-src 'self' wss:; " +
                        "script-src 'self'; " +
                        "style-src 'self' 'unsafe-inline'; " +
                        "img-src 'self' data: https://avatars.githubusercontent.com; " +
                        "frame-ancestors 'none'"));
            });

        return http.build();
    }

    /**
     * CORS configuration for frontend access.
     * Allows one or more frontend origins (comma-separated) to make cross-origin
     * requests to the backend. Supports both local dev and Vercel production URLs.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.stream(corsAllowedOriginsConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true); // Required for JSESSIONID session cookies

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
