package com.sprintmate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Main application entry point for Sprint Mate.
 * 
 * @EnableRetry enables Spring Retry for automatic retry on transient failures
 * (e.g., 429 Too Many Requests from Gemini API).
 */
@SpringBootApplication
@EnableRetry
public class SprintMateApplication {

    public static void main(String[] args) {
        SpringApplication.run(SprintMateApplication.class, args);
    }
}
