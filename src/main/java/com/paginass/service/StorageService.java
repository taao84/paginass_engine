package com.paginass.service;

import java.io.IOException;
import java.io.InputStream;

public interface StorageService {

    /**
     * Saves a file and returns its sub-path (e.g. "businesses/uuid/image.jpg").
     */
    String saveFile(String subPath, InputStream inputStream, String contentType) throws IOException;

    void deleteFile(String subPath) throws IOException;

    /**
     * Returns the full public URL for a stored sub-path.
     */
    String toPublicUrl(String subPath);
}
