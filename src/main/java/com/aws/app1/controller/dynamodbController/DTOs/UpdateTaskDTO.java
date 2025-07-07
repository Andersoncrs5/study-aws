package com.aws.app1.controller.dynamodbController.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskDTO(
        @NotBlank
        String title,

        @NotBlank
        String description,

        @NotNull
        Boolean done
) {
}
