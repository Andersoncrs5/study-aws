package com.aws.app1.controller.S3.S3DTOs;

import java.util.List;

public record CompleteMultipartUploadRequestDTO(
        String bucketName,
        String key,
        String uploadId,
        List<PartInfo> parts
) {}
