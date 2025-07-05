package com.aws.app1.controller.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for file upload request with metadata")
public class UploadRequestMetadataDTO extends UploadRequestDTO {
    @NotNull
    private Long userId;
}
