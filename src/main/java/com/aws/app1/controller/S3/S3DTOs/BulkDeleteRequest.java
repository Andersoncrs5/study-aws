package com.aws.app1.controller.S3.S3DTOs;

import java.util.List;

public record BulkDeleteRequest(
        String bucketName,
        List<ObjectToDeleteInfo> objectsToDelete
) {}

