package com.aws.app1.controller.S3;

import com.aws.app1.controller.S3.S3DTOs.*;
import com.aws.app1.services.S3.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/s3")
public class S3Controller {

    private final S3Service service;
    private final S3Client s3Client;

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

    @PostMapping("/presigned-upload-url")
    @Operation(summary = "Generate a pre-signed URL for direct object upload from frontend to S3")
    public ResponseEntity<String> getPresignedUploadUrl(@RequestBody PresignedUploadUrlRequestDTO requestDTO) {
        String presignedUrl = service.generatePresignedUploadUrl(
                requestDTO.bucketName(),
                requestDTO.key(),
                requestDTO.expirationSeconds(),
                requestDTO.contentType(),
                requestDTO.userId()
        );

        return ResponseEntity.ok(presignedUrl);
    }

    @PostMapping("/versioning/enable")
    @Operation(summary = "Enable versioning for an S3 bucket")
    public ResponseEntity<String> enableVersioning(@RequestParam String bucketName) {
        service.enableBucketVersioning(bucketName);
        return ResponseEntity.ok("Versioning enabled for bucket '" + bucketName + "'!");
    }

    @PostMapping("/versioning/suspend")
    @Operation(summary = "Suspend versioning for an S3 bucket")
    public ResponseEntity<String> suspendVersioning(@RequestParam String bucketName) {
        service.checkBucketExists(bucketName);
        service.BucketVersioning(bucketName);
        return ResponseEntity.ok("Versioning suspended for bucket '" + bucketName + "'!");
    }

    @GetMapping("/versions/download")
    @Operation(summary = "Download a specific version of an object from a versioned bucket")
    public ResponseEntity<byte[]> downloadSpecificObjectVersion(
            @RequestParam String bucketName,
            @RequestParam String key,
            @RequestParam String versionId) {
        try {
            service.checkBucketExists(bucketName);


            String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            try {

                HeadObjectRequest headRequest = HeadObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .versionId(versionId)
                        .build();
                HeadObjectResponse headResponse = s3Client.headObject(headRequest);
                if (headResponse.contentType() != null) {
                    contentType = headResponse.contentType();
                }
            } catch (Exception e) {
                System.err.println("Could not determine Content-Type for version " + versionId + ": " + e.getMessage());
            }

            try (InputStream inputStream = service.downloadSpecificObjectVersion(bucketName, key, versionId)) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType(contentType));
                headers.setContentDispositionFormData("attachment", key + "_version_" + versionId.substring(0, Math.min(versionId.length(), 8))); // Limita o ID para legibilidade

                byte[] fileContent = inputStream.readAllBytes();
                headers.setContentLength(fileContent.length);

                return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
            } catch (IOException e) {
                System.err.println("Erro de I/O ao ler o stream da versão do arquivo: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Erro inesperado ao baixar a versão do arquivo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/versions/delete-specific")
    @Operation(summary = "Delete a specific version of an object from a versioned bucket permanently")
    public ResponseEntity<String> deleteSpecificObjectVersion(
            @RequestParam String bucketName,
            @RequestParam String key,
            @RequestParam String versionId) {
        service.checkBucketExists(bucketName);
        service.deleteSpecificObjectVersion(bucketName, key, versionId);
        return ResponseEntity.ok("Version '" + versionId + "' of object '" + key + "' deleted permanently!");
    }

    @GetMapping("/versions/list")
    @Operation(summary = "List all versions of a specific object in a versioned bucket")
    public ResponseEntity<List<ObjectVersion>> listObjectVersions(
                                                                   @RequestParam String bucketName,
                                                                   @RequestParam String key) {
        service.checkBucketExists(bucketName);
        List<ObjectVersion> versions = service.listObjectVersions(bucketName, key);
        return ResponseEntity.ok(versions);
    }

    @PostMapping("/object/make-public")
    @Operation(summary = "Make an existing S3 object publicly readable (requires public bucket configuration)")
    public ResponseEntity<String> makeObjectPublic(
            @RequestParam String bucketName,
            @RequestParam String key) {
        service.checkBucketExists(bucketName);
        service.makeObjectPublic(bucketName, key);
        return ResponseEntity.ok("Object '" + key + "' in bucket '" + bucketName + "' is now public!");
    }

    @GetMapping("/object/public-url")
    @Operation(summary = "Get the public URL for an S3 object (requires public object and bucket configuration)")
    public ResponseEntity<String> getPublicObjectUrl(
            @RequestParam String bucketName,
            @RequestParam String key) {
        service.checkBucketExists(bucketName);
        String publicUrl = service.getPublicObjectUrl(bucketName, key);
        return ResponseEntity.ok(publicUrl);
    }

    @PostMapping("/multipart/initiate")
    @Operation(summary = "Initiate a multipart upload and get an Upload ID")
    public ResponseEntity<String> initiateMultipartUpload(@RequestBody InitiateMultipartUploadRequest request) {
        try {
            service.checkBucketExists(request.bucketName());
            Map<String, String> metadata = new HashMap<>();

            if (request.metadata() != null) {
                metadata.putAll(request.metadata());
            }

            String uploadId = service.initiateMultipartUpload(
                    request.bucketName(),
                    request.key(),
                    request.contentType(),
                    metadata
            ).get();

            return ResponseEntity.ok(uploadId);
        } catch (InterruptedException | ExecutionException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao iniciar upload multipart: " + e.getMessage(), e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro inesperado: " + e.getMessage());
        }
    }

    @PostMapping(value = "/multipart/upload-part", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a single part of a multipart upload")
    public ResponseEntity<String> uploadPart(
            @RequestParam String bucketName,
            @RequestParam String key,
            @RequestParam String uploadId,
            @RequestParam int partNumber,
            @RequestParam("file") MultipartFile file) {

        try {
            service.checkBucketExists(bucketName);

            String eTag = service.uploadPart(
                    bucketName,
                    key,
                    uploadId,
                    partNumber,
                    file.getInputStream(),
                    file.getSize()
            ).get();

            return ResponseEntity.ok(eTag);
        } catch (InterruptedException | ExecutionException | IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao fazer upload da parte: " + e.getMessage(), e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro inesperado: " + e.getMessage());
        }
    }

    @PostMapping("/multipart/complete")
    @Operation(summary = "Complete a multipart upload")
    public ResponseEntity<String> completeMultipartUpload(@RequestBody CompleteMultipartUploadRequestDTO request) {
        try {
            service.checkBucketExists(request.bucketName());

            List<CompletedPart> completedParts = request.parts().stream()
                    .map(PartInfo::toCompletedPart)
                    .collect(Collectors.toList());

            String eTag = service.completeMultipartUpload(
                    request.bucketName(),
                    request.key(),
                    request.uploadId(),
                    completedParts
            ).get();

            return ResponseEntity.ok("Upload multipart concluído. ETag final: " + eTag);
        } catch (InterruptedException | ExecutionException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao completar upload multipart: " + e.getMessage(), e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro inesperado: " + e.getMessage());
        }
    }

    @DeleteMapping("/multipart/abort")
    @Operation(summary = "Abort an incomplete multipart upload")
    public ResponseEntity<String> abortMultipartUpload(
            @RequestParam String bucketName,
            @RequestParam String key,
            @RequestParam String uploadId) {
        try {
            service.checkBucketExists(bucketName);

            service.abortMultipartUpload(bucketName, key, uploadId).get();
            return ResponseEntity.ok("Upload multipart abortado com sucesso.");
        } catch (InterruptedException | ExecutionException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao abortar upload multipart: " + e.getMessage(), e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro inesperado: " + e.getMessage());
        }
    }

    @DeleteMapping("/batch-delete")
    @Operation(summary = "Delete multiple objects (or specific versions) from an S3 bucket in a single request")
    public ResponseEntity<?> deleteMultipleObjects(@RequestBody BulkDeleteRequest request) {
        try {
            service.checkBucketExists(request.bucketName());

            if (request.objectsToDelete().isEmpty()) {
                return ResponseEntity.badRequest().body(new BulkDeleteResponseDTO(Collections.emptyList(), Collections.emptyList(), "Nenhum objeto para deletar fornecido."));
            }
            if (request.objectsToDelete().size() > 1000) {
                return ResponseEntity.badRequest().body(new BulkDeleteResponseDTO(Collections.emptyList(), Collections.emptyList(), "Número máximo de objetos para deleção em massa é 1000."));
            }

            List<ObjectIdentifier> objectIdentifiers = request.objectsToDelete().stream()
                    .map(ObjectToDeleteInfo::toObjectIdentifier)
                    .toList();

            DeleteObjectsResponse response = service.deleteMultipleObjects(
                    request.bucketName(),
                    objectIdentifiers
            ).get();

            List<String> deletedKeys = response.deleted().stream()
                    .map(DeletedObject::key)
                    .toList();

            String message = String.format("Deleção em massa concluída: %d objetos deletados.", deletedKeys.size());
            return ResponseEntity.ok("Deleted items");
        } catch (InterruptedException | ExecutionException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao realizar deleção em massa: " + e.getMessage(), e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new BulkDeleteResponseDTO(Collections.emptyList(), Collections.emptyList(), "Erro inesperado: " + e.getMessage()));
        }
    }

}
