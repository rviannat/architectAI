package com.architectai.backend.config;

import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI architectAiOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Architect AI Backend API")
                .description("API do backend do Architect AI para projetos, análises, agentes, webhooks e relatórios")
                .version("0.1.0-SNAPSHOT")
                .contact(new Contact()
                    .name("Architect AI")
                    .email("support@architectai.local"))
                .license(new License()
                    .name("Proprietary")));
    }
}

