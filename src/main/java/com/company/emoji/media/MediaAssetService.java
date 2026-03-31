package com.company.emoji.media;

import com.company.emoji.common.api.ApiErrorCode;
import com.company.emoji.common.api.ApiException;
import com.company.emoji.media.dto.UploadPolicyRequest;
import com.company.emoji.media.dto.UploadPolicyResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MediaAssetService {
    private final UploadProperties uploadProperties;

    public MediaAssetService(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    public UploadPolicyResponse createUploadPolicy(UploadPolicyRequest request) {
        validateContentType(request.contentType());
        String sanitizedFileName = sanitizeFileName(request.fileName());
        String extension = extractExtension(sanitizedFileName);
        String objectKey = joinPath(
                uploadProperties.uploadPathPrefix(),
                uploadProperties.sourcePathPrefix(),
                LocalDate.now().toString(),
                UUID.randomUUID() + extension
        );

        return new UploadPolicyResponse(
                objectKey,
                toPublicUrl(objectKey),
                "PUT",
                Map.of("Content-Type", request.contentType()),
                uploadProperties.uploadExpiresInSeconds()
        );
    }

    public void assertValidSourceObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "inputObjectKey is required");
        }
        String expectedPrefix = joinPath(uploadProperties.uploadPathPrefix(), uploadProperties.sourcePathPrefix()) + "/";
        if (!objectKey.startsWith(expectedPrefix)) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "inputObjectKey is outside managed upload scope");
        }
    }

    public String buildPreviewObjectKey(String providerTaskId, int index) {
        return joinPath(uploadProperties.uploadPathPrefix(), uploadProperties.previewPathPrefix(), providerTaskId + "-" + index + ".png");
    }

    public String buildResultObjectKey(String providerTaskId, int index) {
        return joinPath(uploadProperties.uploadPathPrefix(), uploadProperties.resultPathPrefix(), providerTaskId + "-" + index + ".png");
    }

    public String toPublicUrl(String reference) {
        if (reference == null || reference.isBlank()) {
            return "";
        }
        if (reference.startsWith("http://") || reference.startsWith("https://")) {
            return reference;
        }
        return uploadProperties.publicUploadBaseUrl().replaceAll("/$", "") + "/" + reference;
    }

    public List<String> toPublicUrls(List<String> references) {
        return references.stream()
                .map(this::toPublicUrl)
                .filter(url -> !url.isBlank())
                .toList();
    }

    private void validateContentType(String contentType) {
        List<String> allowedContentTypes = uploadProperties.allowedContentTypes();
        if (allowedContentTypes != null && !allowedContentTypes.isEmpty() && !allowedContentTypes.contains(contentType)) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "Unsupported contentType");
        }
    }

    private String sanitizeFileName(String fileName) {
        String trimmed = fileName == null ? "" : fileName.trim();
        String sanitized = trimmed.replaceAll("[^A-Za-z0-9._-]", "-");
        while (sanitized.contains("..")) {
            sanitized = sanitized.replace("..", ".");
        }
        sanitized = sanitized.replaceAll("^[.-]+", "");
        if (sanitized.isBlank()) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "fileName is invalid");
        }
        return sanitized;
    }

    private String extractExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0) {
            return "";
        }
        return fileName.substring(lastDot);
    }

    private String joinPath(String... parts) {
        return String.join("/", parts).replaceAll("/+", "/");
    }
}
