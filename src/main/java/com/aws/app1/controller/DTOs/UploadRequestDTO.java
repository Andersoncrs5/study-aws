package com.aws.app1.controller.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for file upload request with metadata")
public class UploadRequestDTO {

    @Schema(description = "File to be uploaded", type = "string", format = "binary")
    private MultipartFile file;

    @Schema(description = "Metadata for the file upload (bucketName and key)")
    private String bucketName;
    private String key;
}
