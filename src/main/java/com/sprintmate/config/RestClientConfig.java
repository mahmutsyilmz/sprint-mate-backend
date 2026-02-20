package com.sprintmate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuration for RestClient beans.
 * Provides reusable RestClient.Builder for dependency injection.
 *
 * Business Intent:
 * - Enables testability by allowing RestClient mocking
 * - Centralizes RestClient configuration
 * - Follows Spring dependency injection best practices
 */
@Configuration
public class RestClientConfig {

    /**
     * Provides a RestClient.Builder bean for dependency injection.
     * Services can inject this to create configured RestClient instances.
     *
     * @return A new RestClient.Builder instance
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
