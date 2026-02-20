package com.sprintmate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.util.Map;

/**
 * WebSocket configuration for real-time chat using STOMP protocol.
 * Enables WebSocket message broker for pub/sub messaging.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * Configures the message broker for STOMP messaging.
     * - /topic: for pub/sub destinations (chat messages)
     * - /app: prefix for messages bound to @MessageMapping methods
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory broker for /topic destinations
        config.enableSimpleBroker("/topic");
        // Prefix for messages from clients that need processing
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Registers STOMP endpoints for WebSocket connections.
     * - /ws: Native WebSocket endpoint (primary)
     * - /ws-sockjs: SockJS fallback endpoint
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Native WebSocket endpoint (no SockJS)
        // Use setAllowedOriginPatterns for credentials support
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns(frontendUrl)
            .addInterceptors(httpSessionHandshakeInterceptor())
            .setHandshakeHandler(authenticationHandshakeHandler());

        // SockJS fallback endpoint
        registry.addEndpoint("/ws-sockjs")
            .setAllowedOriginPatterns(frontendUrl)
            .addInterceptors(httpSessionHandshakeInterceptor())
            .setHandshakeHandler(authenticationHandshakeHandler())
            .withSockJS();
    }

    /**
     * Handshake interceptor that copies the HTTP session and Spring Security
     * Authentication into WebSocket session attributes.
     * The Authentication is stored so the STOMP interceptor can access it.
     */
    private HandshakeInterceptor httpSessionHandshakeInterceptor() {
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                          WebSocketHandler wsHandler, Map<String, Object> attributes) {
                if (request instanceof ServletServerHttpRequest servletRequest) {
                    HttpSession session = servletRequest.getServletRequest().getSession(false);
                    if (session != null) {
                        attributes.put("HTTP_SESSION_ID", session.getId());
                    }
                }
                // Store the current authentication for STOMP-level access
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated()) {
                    attributes.put("SPRING_SECURITY_AUTHENTICATION", auth);
                }
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                      WebSocketHandler wsHandler, Exception exception) {
                // No-op
            }
        };
    }

    /**
     * Custom handshake handler that exposes the Spring Security Authentication
     * as the WebSocket session's Principal, making it accessible via
     * StompHeaderAccessor.getUser() in STOMP interceptors.
     */
    private DefaultHandshakeHandler authenticationHandshakeHandler() {
        return new DefaultHandshakeHandler() {
            @Override
            protected Principal determineUser(ServerHttpRequest request,
                                             WebSocketHandler wsHandler,
                                             Map<String, Object> attributes) {
                Authentication auth = (Authentication) attributes.get("SPRING_SECURITY_AUTHENTICATION");
                if (auth != null) {
                    return auth;
                }
                return super.determineUser(request, wsHandler, attributes);
            }
        };
    }
}
