package com.company.emoji.media;

import org.springframework.stereotype.Service;

import com.company.emoji.media.dto.UploadPolicyRequest;
import com.company.emoji.media.dto.UploadPolicyResponse;

@Service
public class UploadPolicyService {
    private final MediaAssetService mediaAssetService;

    public UploadPolicyService(MediaAssetService mediaAssetService) {
        this.mediaAssetService = mediaAssetService;
    }

    public UploadPolicyResponse createPolicy(UploadPolicyRequest request) {
        return mediaAssetService.createUploadPolicy(request);
    }
}
