package com.sprintmate.config;

import com.sprintmate.model.User;
import com.sprintmate.repository.UserRepository;
import com.sprintmate.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WebSocket security configuration for chat access control.
 * Validates authentication and subscription authorization.
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
@Slf4j
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private final ChatService chatService;
    private final UserRepository userRepository;

    private static final Pattern MATCH_TOPIC_PATTERN = Pattern.compile("/topic/match/([a-f0-9-]+)");
    private static final String GITHUB_BASE_URL = "https://github.com/";

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor == null) {
                    return message;
                }

                StompCommand command = accessor.getCommand();
                if (command == null) {
                    return message;
                }

                // Get the authenticated user from session
                Authentication authentication = (Authentication) accessor.getUser();

                switch (command) {
                    case CONNECT:
                        // Reject connection if not properly authenticated with OAuth2
                        // This prevents infinite reconnect loops when session has expired
                        if (authentication == null
                                || !authentication.isAuthenticated()
                                || !(authentication.getPrincipal() instanceof OAuth2User)) {
                            log.warn("WebSocket CONNECT rejected: no valid OAuth2 session (session may have expired)");
                            throw new IllegalStateException("Valid OAuth2 authentication required for WebSocket connection");
                        }
                        log.info("WebSocket CONNECT accepted for user: {}", getGithubLogin(authentication));
                        break;

                    case SUBSCRIBE:
                        // Validate subscription authorization
                        String destination = accessor.getDestination();
                        if (destination != null && destination.startsWith("/topic/match/")) {
                            validateMatchSubscription(authentication, destination);
                        }
                        break;

                    default:
                        break;
                }

                return message;
            }
        });
    }

    /**
     * Validates that the user is a participant in the match they're subscribing to.
     */
    private void validateMatchSubscription(Authentication authentication, String destination) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Subscription rejected: not authenticated for {}", destination);
            throw new IllegalStateException("Authentication required to subscribe");
        }

        Matcher matcher = MATCH_TOPIC_PATTERN.matcher(destination);
        if (matcher.matches()) {
            String matchIdStr = matcher.group(1);
            try {
                UUID matchId = UUID.fromString(matchIdStr);
                UUID userId = getUserIdFromAuthentication(authentication);

                if (userId == null) {
                    log.warn("Subscription rejected: could not resolve user ID for {}", destination);
                    throw new IllegalStateException("Could not resolve user");
                }

                if (!chatService.canAccessChat(matchId, userId)) {
                    log.warn("Subscription rejected: user {} is not a participant in match {}",
                             userId, matchId);
                    throw new IllegalStateException("You are not a participant in this match");
                }

                log.info("Subscription authorized: user {} to match {}", userId, matchId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid match ID in subscription: {}", destination);
                throw new IllegalStateException("Invalid match ID");
            }
        }
    }

    /**
     * Extracts GitHub login from OAuth2 authentication.
     */
    private String getGithubLogin(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            return oauth2User.getAttribute("login");
        }
        return "unknown";
    }

    /**
     * Resolves the internal user UUID from OAuth2 authentication.
     */
    private UUID getUserIdFromAuthentication(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            String login = oauth2User.getAttribute("login");
            if (login != null) {
                String githubUrl = GITHUB_BASE_URL + login;
                return userRepository.findByGithubUrl(githubUrl)
                    .map(User::getId)
                    .orElse(null);
            }
        }
        return null;
    }
}
