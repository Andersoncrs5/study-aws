package com.aws.app1.controller;

import com.aws.app1.controller.DTOs.*;
import com.aws.app1.services.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/s3")
public class S3Controller {

    private final S3Service service;

    @PostMapping
    public ResponseEntity<?> createBucket(@RequestBody CreateBucketDTO dto) {
        this.service.createBucket(dto.bucketName());
        return ResponseEntity.ok("Bucket created");
    }

    @PostMapping(value = "/put-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> putFile(@ModelAttribute UploadRequestDTO requestDTO) {

        String bucketName = requestDTO.getBucketName();
        MultipartFile file = requestDTO.getFile();
        String key = requestDTO.getKey();
        String content = requestDTO.getContent();

        if (file == null || file.isEmpty() || bucketName == null || key == null || bucketName.isEmpty() || key.isEmpty()) {
            return ResponseEntity.badRequest().body("File and metadata (bucketName, key) are required.");
        }

        service.checkBucketExists(bucketName);

        service.putObject(bucketName, key, file, content);

        return ResponseEntity.ok("File sended to bucket!");
    }

    @GetMapping("/{bucketName}")
    public ResponseEntity<?> listObjectInBucket(@PathVariable String bucketName ){
        List<String> bucket = this.service.listObjects(bucketName);

        return ResponseEntity.ok(bucket);
    }

    @GetMapping
    public ResponseEntity<?> listBuckets() {
        List<String> buckets = this.service.listAllBucket();

        return ResponseEntity.ok(buckets);
    }

    @DeleteMapping("/delete-file")
    public ResponseEntity<?> deleteKey(@RequestBody DeleteFileDTO dto) {
        this.service.deleteObject(dto.BucketName(), dto.key());

        return ResponseEntity.ok("File deleted");
    }

    @DeleteMapping("/delete-bucket/{name}")
    public ResponseEntity<?> deleteBucket(@PathVariable String name) {
        this.service.deleteBucket(name);

        return ResponseEntity.ok("Bucket deleted");
    }

    @GetMapping("/download-file")
    public ResponseEntity<?> downloadFile(
            @RequestParam String bucketName,
            @RequestParam String key) {

        try (ResponseInputStream<GetObjectResponse> s3ObjectStream = service.downloadFile(bucketName, key)) {

            if (s3ObjectStream == null) {
                return ResponseEntity.notFound().build();
            }

            byte[] fileBytes = s3ObjectStream.readAllBytes();

            String contentType = s3ObjectStream.response().contentType();

            if (contentType.isBlank()) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            String fileName = key.substring(key.lastIndexOf('/') + 1);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(fileBytes);
        } catch (IOException e) {
            System.err.println("Erro de I/O ao ler o stream do arquivo S3: " + e.getMessage());
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @PostMapping("/copy-objects")
    public ResponseEntity<?> copyObject(@RequestBody CopyMoveObjectDTO dto) {
        service.copyObject(dto.sourceBucket(), dto.sourceKey(), dto.destinationBucket(), dto.destinationKey());

        return ResponseEntity.ok("Object copied successfully!");
    }

    @PostMapping("/move-object")
    @Operation(summary = "Move an object from one location to another (copy then delete)")
    public ResponseEntity<String> moveObject(@RequestBody CopyMoveObjectDTO dto) {
        try {
            service.moveObject(dto.sourceBucket(), dto.sourceKey(), dto.destinationBucket(), dto.destinationKey());
            return ResponseEntity.ok("Object moved successfully!");
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body("Error moving object: " + e.getMessage());
        }
    }

    @PostMapping(value = "/put-file-metadata", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> putFileWithMetadata(@ModelAttribute UploadRequestMetadataDTO requestDTO) {
        String bucketName = requestDTO.getBucketName();
        String key = requestDTO.getKey();
        MultipartFile file = requestDTO.getFile();
        Long userId = requestDTO.getUserId();

        if (file == null || file.isEmpty() || bucketName.isEmpty() || key.isEmpty()) {
            return ResponseEntity.badRequest().body("File and metadata (bucketName, key) are required.");
        }

        service.checkBucketExists(bucketName);
        service.putObjectWithMetadata(bucketName, key, file, userId);

        return ResponseEntity.ok("File sended to bucket with metadata!");
    }

    @GetMapping("/object-metadata")
    public ResponseEntity<?> getHeadObject(@RequestParam String bucketName, @RequestParam String key) {
        return ResponseEntity.ok(
                service.getHeaderObject(bucketName, key)
        );
    }

    @GetMapping("/metadada-bucket")
    public ResponseEntity<?> getResponse(@RequestParam String bucketName) {
        return ResponseEntity.ok(
                this.service.getHeaderBucket(bucketName)
        );
    }

    @GetMapping("/list-by-user")
    @Operation(summary = "List all objects in a bucket uploaded by a specific user ID")
    public ResponseEntity<List<String>> listObjectsByUserId(
            @RequestParam String bucketName,
            @RequestParam Long userId,
            @RequestParam(required = false) String prefix) {
        try {
            service.checkBucketExists(bucketName);
            List<String> files = service.listObjectsByUserId(bucketName, userId, prefix);
            return ResponseEntity.ok(files);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Collections.singletonList("Erro inesperado ao listar arquivos: " + e.getMessage()));
        }
    }

    @GetMapping("/url")
    public ResponseEntity<String> getUrl(
            @RequestParam String bucket,
            @RequestParam String key,
            @RequestParam Long day
    ) {
        URL url = service.generatePresignedDownloadUrl(bucket, key, day);
        return ResponseEntity.ok(url.toString());
    }

}
