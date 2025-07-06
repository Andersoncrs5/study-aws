package com.aws.app1.controller.S3.S3DTOs;

import jakarta.validation.constraints.NotBlank;

public record DeleteFileDTO(
        @NotBlank
        String BucketName,
        @NotBlank
        String key
) {
}
