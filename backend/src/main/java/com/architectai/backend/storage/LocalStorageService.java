package com.architectai.backend.storage;

import com.architectai.backend.config.RuntimeProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class LocalStorageService implements StorageService {

    private final RuntimeProperties runtimeProperties;

    @Autowired
    public LocalStorageService(RuntimeProperties runtimeProperties) {
        this.runtimeProperties = runtimeProperties;
    }

    @Override
    public String upload(String objectKey, byte[] content, String contentType) {
        try {
            Path target = resolvePath(objectKey);
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(target, content);
            return getDownloadUrl(objectKey);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store object: " + objectKey, e);
        }
    }

    @Override
    public byte[] download(String objectKey) {
        try {
            Path path = resolvePath(objectKey);
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to download object: " + objectKey, e);
        }
    }

    @Override
    public String getDownloadUrl(String objectKey) {
        return resolvePath(objectKey).toAbsolutePath().normalize().toString();
    }

    @Override
    public String getStorageType() {
        return "local";
    }

    private Path resolvePath(String objectKey) {
        Path candidate = Paths.get(objectKey);
        if (candidate.isAbsolute()) {
            return candidate;
        }
        return Paths.get(runtimeProperties.getReportsDir()).resolve(objectKey).normalize();
    }
}

