package com.architectai.backend.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

/**
 * Configuração de Spring AI e ChatClient
 */
@Configuration
public class AIConfiguration {

    @Value("${spring.ai.openai.api-key:}")
    private String openaiApiKey;

    @Value("${spring.ai.openai.chat.options.model:gpt-4-turbo}")
    private String model;

    /**
     * Bean do ChatClient configurado
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
            .defaultSystem("Você é um especialista em análise de código e arquitetura de software. Responda sempre em JSON estruturado quando solicitado.")
            .build();
    }
}

