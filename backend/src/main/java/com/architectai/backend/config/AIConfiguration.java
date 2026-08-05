package com.architectai.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.client.RestTemplate;

/**
 * Configuracao dos componentes de IA para os agentes especialistas.
 */
@Configuration
@EnableConfigurationProperties({AgentAiProperties.class, RuntimeProperties.class})
public class AIConfiguration {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
