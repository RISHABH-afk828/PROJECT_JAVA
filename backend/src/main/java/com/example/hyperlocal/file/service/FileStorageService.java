package com.example.hyperlocal.file.service;

import com.example.hyperlocal.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.*;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public Map<String, Object> storeFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException("FILE_EMPTY", "Cannot upload an empty file", HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ApiException("FILE_TOO_LARGE", "File size exceeds 5MB limit", HttpStatus.BAD_REQUEST);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new ApiException("INVALID_FILE_TYPE", "Only JPEG, PNG, and WebP images are allowed", HttpStatus.BAD_REQUEST);
        }

        String extension = ".jpg";
        if (contentType.equalsIgnoreCase("image/png")) extension = ".png";
        else if (contentType.equalsIgnoreCase("image/webp")) extension = ".webp";

        String fileId = UUID.randomUUID().toString();
        String storedFileName = fileId + extension;

        try {
            Path targetLocation = this.fileStorageLocation.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/api/v1/files/" + storedFileName;

            return Map.of(
                    "fileId", fileId,
                    "fileName", storedFileName,
                    "url", fileUrl,
                    "contentType", contentType,
                    "size", file.getSize()
            );
        } catch (IOException ex) {
            throw new ApiException("FILE_STORE_ERROR", "Could not store file. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ApiException("FILE_NOT_FOUND", "File not found " + fileName, HttpStatus.NOT_FOUND);
            }
        } catch (MalformedURLException ex) {
            throw new ApiException("FILE_NOT_FOUND", "File not found " + fileName, HttpStatus.NOT_FOUND);
        }
    }
}
