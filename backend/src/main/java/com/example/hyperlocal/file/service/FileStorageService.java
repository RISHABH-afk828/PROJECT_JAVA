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

        validateMagicBytes(file, contentType);

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

    private void validateMagicBytes(MultipartFile file, String contentType) {
        try (var is = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = is.read(header);
            if (read < 4) {
                throw new ApiException("INVALID_FILE_CONTENT", "Invalid image file content", HttpStatus.BAD_REQUEST);
            }
            if ("image/jpeg".equalsIgnoreCase(contentType)) {
                if ((header[0] & 0xFF) != 0xFF || (header[1] & 0xFF) != 0xD8 || (header[2] & 0xFF) != 0xFF) {
                    throw new ApiException("INVALID_FILE_CONTENT", "File content does not match JPEG image format", HttpStatus.BAD_REQUEST);
                }
            } else if ("image/png".equalsIgnoreCase(contentType)) {
                if ((header[0] & 0xFF) != 0x89 || header[1] != 'P' || header[2] != 'N' || header[3] != 'G') {
                    throw new ApiException("INVALID_FILE_CONTENT", "File content does not match PNG image format", HttpStatus.BAD_REQUEST);
                }
            } else if ("image/webp".equalsIgnoreCase(contentType)) {
                if (header[0] != 'R' || header[1] != 'I' || header[2] != 'F' || header[3] != 'F' ||
                        read < 12 || header[8] != 'W' || header[9] != 'E' || header[10] != 'B' || header[11] != 'P') {
                    throw new ApiException("INVALID_FILE_CONTENT", "File content does not match WebP image format", HttpStatus.BAD_REQUEST);
                }
            }
        } catch (ApiException e) {
            throw e;
        } catch (IOException e) {
            throw new ApiException("FILE_READ_ERROR", "Could not read file header", HttpStatus.BAD_REQUEST);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        if (fileName == null || fileName.isBlank() || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new ApiException("ACCESS_DENIED", "Invalid file name", HttpStatus.BAD_REQUEST);
        }
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            if (!filePath.startsWith(this.fileStorageLocation)) {
                throw new ApiException("ACCESS_DENIED", "Access denied to requested file", HttpStatus.FORBIDDEN);
            }
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
