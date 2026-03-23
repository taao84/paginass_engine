package com.paginass.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class LocalFileStorageService {

    private final Path uploadDir;

    public LocalFileStorageService(@Value("${app.storage.upload-dir}") String uploadDir) throws IOException {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);
    }

    /**
     * Saves the file and returns its relative sub-path (e.g. "businesses/uuid/image.jpg").
     * The caller is responsible for building the public URL.
     */
    public String saveFile(String subPath, InputStream inputStream) throws IOException {
        Path target = uploadDir.resolve(subPath).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new IllegalArgumentException("Invalid file path");
        }
        Files.createDirectories(target.getParent());
        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        return subPath;
    }

    public void deleteFile(String subPath) throws IOException {
        Path target = uploadDir.resolve(subPath).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new IllegalArgumentException("Invalid file path");
        }
        Files.deleteIfExists(target);
    }
}
