package com.aws.app1.controller.S3.S3DTOs;

import software.amazon.awssdk.services.s3.model.CompletedPart;

public record PartInfo(int partNumber, String etag) {
    public CompletedPart toCompletedPart() {
        return CompletedPart.builder().partNumber(partNumber).eTag(etag).build();
    }
}
