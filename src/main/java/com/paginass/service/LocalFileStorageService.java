package com.paginass.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@Profile("dev")
public class LocalFileStorageService implements StorageService {

    private final Path uploadDir;
    private final String publicBaseUrl;

    public LocalFileStorageService(
            @Value("${app.storage.upload-dir}") String uploadDir,
            @Value("${app.storage.public-base-url}") String publicBaseUrl) throws IOException {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl;
        Files.createDirectories(this.uploadDir);
    }

    @Override
    public String saveFile(String subPath, InputStream inputStream, String contentType) throws IOException {
        Path target = uploadDir.resolve(subPath).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new IllegalArgumentException("Invalid file path");
        }
        Files.createDirectories(target.getParent());
        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        return subPath;
    }

    @Override
    public void deleteFile(String subPath) throws IOException {
        Path target = uploadDir.resolve(subPath).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new IllegalArgumentException("Invalid file path");
        }
        Files.deleteIfExists(target);
    }

    @Override
    public String toPublicUrl(String subPath) {
        return publicBaseUrl + "/uploads/" + subPath;
    }
}
