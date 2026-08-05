package com.architectai.backend.storage;

import com.architectai.backend.config.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

@Component
public class MinIOStorageService implements StorageService {
    private static final Logger log = LoggerFactory.getLogger(MinIOStorageService.class);

    private final StorageProperties storageProperties;
    private MinioClient minioClient;

    @Autowired
    public MinIOStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @PostConstruct
    public void init() {
        StorageProperties.Minio cfg = storageProperties.getMinio();
        minioClient = MinioClient.builder()
            .endpoint(cfg.getEndpoint())
            .credentials(cfg.getAccessKey(), cfg.getSecretKey())
            .build();
        ensureBucketExists();
    }

    private void ensureBucketExists() {
        String bucket = storageProperties.getMinio().getBucket();
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Bucket MinIO criado: {}", bucket);
            }
        } catch (Exception e) {
            log.warn("Não foi possível verificar/criar bucket MinIO: {}", e.getMessage());
        }
    }

    @Override
    public String upload(String objectKey, byte[] content, String contentType) {
        String bucket = storageProperties.getMinio().getBucket();
        try {
            minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .stream(new ByteArrayInputStream(content), content.length, -1)
                .contentType(contentType)
                .build());
            return getDownloadUrl(objectKey);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao fazer upload para MinIO: " + objectKey, e);
        }
    }

    @Override
    public byte[] download(String objectKey) {
        String bucket = storageProperties.getMinio().getBucket();
        try (var stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .build())) {
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao baixar do MinIO: " + objectKey, e);
        }
    }

    @Override
    public String getDownloadUrl(String objectKey) {
        String endpoint = storageProperties.getMinio().getEndpoint();
        String bucket = storageProperties.getMinio().getBucket();
        return endpoint + "/" + bucket + "/" + objectKey;
    }

    @Override
    public String getStorageType() {
        return "minio";
    }
}
