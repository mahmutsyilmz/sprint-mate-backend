package com.sprintmate.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

/**
 * WebSocket configuration for real-time chat using STOMP protocol.
 * Enables WebSocket message broker for pub/sub messaging.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String FRONTEND_URL = "http://localhost:5173";

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
            .setAllowedOriginPatterns("*")
            .addInterceptors(httpSessionHandshakeInterceptor());

        // SockJS fallback endpoint
        registry.addEndpoint("/ws-sockjs")
            .setAllowedOriginPatterns("*")
            .addInterceptors(httpSessionHandshakeInterceptor())
            .withSockJS();
    }

    /**
     * Handshake interceptor to copy HTTP session to WebSocket session.
     * This ensures authentication information is available in WebSocket messages.
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
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                      WebSocketHandler wsHandler, Exception exception) {
                // No-op
            }
        };
    }
}
