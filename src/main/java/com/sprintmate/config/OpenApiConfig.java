package com.sprintmate.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) configuration for Sprint Mate API documentation.
 * 
 * Business Intent:
 * Provides interactive API documentation accessible at /swagger-ui.html.
 * Since we use session-based OAuth2 authentication, the browser automatically
 * shares cookies with Swagger UI - no special auth config needed.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Configures OpenAPI metadata for the Sprint Mate API.
     * This information is displayed in the Swagger UI header.
     *
     * @return OpenAPI configuration with API title, version, and description
     */
    @Bean
    public OpenAPI sprintMateOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Sprint Mate API")
                .version("v1")
                .description("API for Sprint Mate - A platform for matching frontend and backend developers")
                .contact(new Contact()
                    .name("Sprint Mate Team")
                )
            );
    }
}
