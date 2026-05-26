package com.company.emoji.media.storage;

import com.company.emoji.media.UploadProperties;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class LocalStorageUploadSigner implements StorageUploadSigner {
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String EXPIRES_AT_HEADER = "X-Emoji-Storage-Expires-At";
    private static final String SIGNATURE_HEADER = "X-Emoji-Storage-Signature";

    private final UploadProperties uploadProperties;

    public LocalStorageUploadSigner(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    @Override
    public SignedUploadPolicy signPut(String objectKey, String contentType, int expiresInSeconds) {
        Instant expiresAt = Instant.now().plusSeconds(expiresInSeconds);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(CONTENT_TYPE_HEADER, contentType);
        headers.put(EXPIRES_AT_HEADER, expiresAt.toString());
        headers.put(SIGNATURE_HEADER, sign(objectKey, contentType, expiresAt));
        return new SignedUploadPolicy(toSignedUploadUrl(objectKey), "PUT", Map.copyOf(headers));
    }

    private String toSignedUploadUrl(String objectKey) {
        String baseUrl = uploadProperties.signedUploadBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = uploadProperties.publicUploadBaseUrl();
        }
        return baseUrl.replaceAll("/$", "") + "/" + objectKey;
    }

    private String sign(String objectKey, String contentType, Instant expiresAt) {
        String payload = objectKey + "\n" + contentType + "\n" + expiresAt + "\n" + signingSecret();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable", exception);
        }
    }

    private String signingSecret() {
        String secret = uploadProperties.uploadSigningSecret();
        return secret == null || secret.isBlank() ? "local-upload-signing-secret" : secret;
    }
}
