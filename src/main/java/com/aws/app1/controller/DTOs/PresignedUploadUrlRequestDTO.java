package com.aws.app1.controller.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PresignedUploadUrlRequestDTO(
        @NotBlank
        String bucketName,

        @NotBlank
        String key,

        @NotNull
        Long userId,

        @NotBlank
        String contentType,

        @Schema(defaultValue = "3600")
        long expirationSeconds
) {}
