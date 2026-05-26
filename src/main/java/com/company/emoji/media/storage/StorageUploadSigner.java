package com.company.emoji.media.storage;

public interface StorageUploadSigner {
    SignedUploadPolicy signPut(String objectKey, String contentType, int expiresInSeconds);
}
