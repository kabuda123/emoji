package com.company.emoji.media;

import com.company.emoji.media.dto.UploadPolicyRequest;
import com.company.emoji.media.dto.UploadPolicyResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
public class UploadPolicyService {
    private final UploadProperties uploadProperties;

    public UploadPolicyService(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    public UploadPolicyResponse createPolicy(UploadPolicyRequest request) {
        String objectKey = uploadProperties.uploadPathPrefix() + "/" + LocalDate.now() + "/" + UUID.randomUUID() + "-" + request.fileName();
        String uploadUrl = uploadProperties.publicUploadBaseUrl().replaceAll("/$", "") + "/" + objectKey;
        return new UploadPolicyResponse(
                objectKey,
                uploadUrl,
                "PUT",
                Map.of("Content-Type", request.contentType()),
                uploadProperties.uploadExpiresInSeconds()
        );
    }
}