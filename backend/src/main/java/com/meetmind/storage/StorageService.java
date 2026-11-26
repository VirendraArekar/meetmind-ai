package com.meetmind.storage;

import com.meetmind.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * S3-compatible object storage service with automatic local filesystem fallback
 * for test environments or when S3 is disabled.
 */
@Slf4j
@Service
public class StorageService {

    private final boolean s3Enabled;
    private final String bucket;
    private final Path localBaseDir;
    private final S3Client s3Client;

    public StorageService(
            @Value("${app.storage.s3-enabled:false}") boolean s3Enabled,
            @Value("${app.storage.endpoint:http://localhost:9000}") String endpoint,
            @Value("${app.storage.region:us-east-1}") String region,
            @Value("${app.storage.bucket:meetmind-recordings}") String bucket,
            @Value("${app.storage.access-key:minioadmin}") String accessKey,
            @Value("${app.storage.secret-key:minioadmin_change_me}") String secretKey,
            @Value("${app.storage.local-dir:./uploads}") String localDir
    ) {
        this.s3Enabled = s3Enabled;
        this.bucket = bucket;
        this.localBaseDir = Paths.get(localDir).toAbsolutePath().normalize();

        if (s3Enabled) {
            this.s3Client = S3Client.builder()
                    .endpointOverride(URI.create(endpoint))
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                    .build();
        } else {
            this.s3Client = null;
        }
    }

    public record StoredObject(String storageKey, String provider) {}

    public StoredObject store(UUID organizationId, UUID meetingId, String filename, String contentType, byte[] data) {
        String safeName = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String key = organizationId + "/" + meetingId + "/" + UUID.randomUUID() + "-" + safeName;

        if (s3Enabled && s3Client != null) {
            try {
                ensureBucketExists();
                s3Client.putObject(
                        PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .contentType(contentType)
                                .build(),
                        RequestBody.fromBytes(data)
                );
                return new StoredObject(key, "S3");
            } catch (Exception ex) {
                log.warn("S3 upload failed ({}), falling back to local storage", ex.getMessage());
            }
        }

        try {
            Path target = localBaseDir.resolve(key).normalize();
            if (!target.startsWith(localBaseDir)) {
                throw new BadRequestException("Invalid storage path");
            }
            Files.createDirectories(target.getParent());
            Files.write(target, data);
            return new StoredObject(key, "LOCAL");
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store recording file", ex);
        }
    }

    public byte[] load(String storageKey, String provider) {
        if ("S3".equalsIgnoreCase(provider) && s3Client != null) {
            return s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .build()).asByteArray();
        }

        try {
            Path target = localBaseDir.resolve(storageKey).normalize();
            if (!target.startsWith(localBaseDir) || !Files.exists(target)) {
                throw new BadRequestException("Stored audio file not found");
            }
            return Files.readAllBytes(target);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read stored recording", ex);
        }
    }

    private void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (Exception ignored) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        }
    }
}
