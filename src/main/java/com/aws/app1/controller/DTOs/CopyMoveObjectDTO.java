package com.aws.app1.controller.DTOs;

import jakarta.validation.constraints.NotBlank;

public record CopyMoveObjectDTO(
        @NotBlank
        String sourceBucket,
        @NotBlank
        String sourceKey,
        @NotBlank
        String destinationBucket,
        @NotBlank
        String destinationKey
) {
}
