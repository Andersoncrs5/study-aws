package com.aws.app1.controller.S3.S3DTOs;

import java.util.List;

public record BulkDeleteResponseDTO(
        List<String> deletedKeys,
        List<String> errorKeys,
        String message
) {}
