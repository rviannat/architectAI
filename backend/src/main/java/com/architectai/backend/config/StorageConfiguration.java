package com.architectai.backend.config;

import com.architectai.backend.storage.LocalStorageService;
import com.architectai.backend.storage.MinIOStorageService;
import com.architectai.backend.storage.StorageService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfiguration {

    @Bean
    public StorageService storageService(StorageProperties storageProperties,
                                         LocalStorageService localStorageService,
                                         MinIOStorageService minIOStorageService) {
        if ("minio".equalsIgnoreCase(storageProperties.getType())) {
            return minIOStorageService;
        }
        return localStorageService;
    }
}
