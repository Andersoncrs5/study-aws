package com.aws.app1.controller.DTOs;

import jakarta.validation.constraints.NotBlank;

public record DeleteFileDTO(
        @NotBlank
        String BucketName,
        @NotBlank
        String key
) {
}
