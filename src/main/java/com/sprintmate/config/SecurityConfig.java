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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.List;

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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Enable CORS for cross-origin requests from frontend
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Configure URL-based authorization rules
            .authorizeHttpRequests(authorize -> authorize
                // Public endpoints - accessible without authentication
                .requestMatchers("/", "/error", "/h2-console/**").permitAll()
                // Swagger UI endpoints - accessible for API documentation
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                // WebSocket endpoints - permitAll for HTTP handshake, auth handled at STOMP level
                .requestMatchers("/ws/**", "/ws-sockjs/**").permitAll()
                // Actuator health endpoint - accessible for load balancer health checks
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            // Configure OAuth2 login with GitHub
            .oauth2Login(oauth2 -> oauth2
                // Use custom service to sync user data on successful login
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                // Redirect to frontend after successful login
                .defaultSuccessUrl(frontendUrl + "/role-select", true)
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
            // Disable CSRF for H2 console, API endpoints, and WebSocket
            // API endpoints use session cookies for auth, but CSRF is disabled
            // because frontend and backend may run on different origins (CORS)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**", "/api/**", "/ws/**", "/ws-sockjs/**")
            )
            // Allow H2 console frames (development only)
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }

    /**
     * CORS configuration for frontend access.
     * Allows the frontend origin to make cross-origin requests to the backend.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendUrl));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true); // Important for session cookies
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
