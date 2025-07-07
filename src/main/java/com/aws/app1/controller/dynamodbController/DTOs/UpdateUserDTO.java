package com.aws.app1.controller.dynamodbController.DTOs;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserDTO(
        @NotBlank
        String password
) {
}
