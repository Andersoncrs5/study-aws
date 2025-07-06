package com.aws.app1.controller.S3.S3DTOs;

import java.util.Map;

public record InitiateMultipartUploadRequest(
        String bucketName,
        String key,
        String contentType,
        Map<String, String> metadata
) {}

