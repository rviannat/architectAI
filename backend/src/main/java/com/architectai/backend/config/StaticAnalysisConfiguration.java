package com.architectai.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StaticAnalysisProperties.class)
public class StaticAnalysisConfiguration {
}
