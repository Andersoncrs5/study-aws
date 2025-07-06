package com.aws.app1.controller.S3.S3DTOs;

import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

public record ObjectToDeleteInfo(String key, String versionId) { // versionId é opcional
    public ObjectIdentifier toObjectIdentifier() {
        if (versionId != null && !versionId.isBlank()) {
            return ObjectIdentifier.builder().key(key).versionId(versionId).build();
        }
        return ObjectIdentifier.builder().key(key).build();
    }
}
