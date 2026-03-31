package com.company.emoji.media;

import org.springframework.stereotype.Service;

import com.company.emoji.media.dto.UploadPolicyRequest;
import com.company.emoji.media.dto.UploadPolicyResponse;

@Service
public class UploadPolicyService {
    private final MediaAssetService mediaAssetService;
    private final MediaMetadataService mediaMetadataService;

    public UploadPolicyService(MediaAssetService mediaAssetService, MediaMetadataService mediaMetadataService) {
        this.mediaAssetService = mediaAssetService;
        this.mediaMetadataService = mediaMetadataService;
    }

    public UploadPolicyResponse createPolicy(UploadPolicyRequest request) {
        UploadPolicyResponse response = mediaAssetService.createUploadPolicy(request);
        mediaMetadataService.recordSourcePolicy(response.objectKey(), request.contentType());
        return response;
    }
}
