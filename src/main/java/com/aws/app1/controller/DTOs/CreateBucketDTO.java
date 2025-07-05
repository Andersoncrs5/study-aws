package com.aws.app1.controller.DTOs;

import jakarta.validation.constraints.NotBlank;

public record CreateBucketDTO(
        @NotBlank
        String bucketName
) {
}
