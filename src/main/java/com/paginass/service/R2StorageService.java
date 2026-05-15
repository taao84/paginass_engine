package com.paginass.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

@Service
@Profile("prod")
public class R2StorageService implements StorageService {

    private final S3Client s3Client;
    private final String bucket;
    private final String publicBaseUrl;

    public R2StorageService(
            @Value("${app.storage.r2.account-id}") String accountId,
            @Value("${app.storage.r2.access-key-id}") String accessKeyId,
            @Value("${app.storage.r2.secret-access-key}") String secretAccessKey,
            @Value("${app.storage.r2.bucket}") String bucket,
            @Value("${app.storage.public-base-url}") String publicBaseUrl) {

        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl;
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create("https://" + accountId + ".r2.cloudflarestorage.com"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .region(Region.of("auto"))
                .build();
    }

    @Override
    public String saveFile(String subPath, InputStream inputStream, String contentType) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(subPath)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(bytes));
        return subPath;
    }

    @Override
    public void deleteFile(String subPath) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(subPath)
                        .build());
    }

    @Override
    public String toPublicUrl(String subPath) {
        return publicBaseUrl + "/" + subPath;
    }
}
