package com.aws.app1.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class S3Service {

    @Autowired
    private S3Client s3Client;
    @Autowired
    private S3Presigner s3Presigner;

    @Async
    public void createBucket(String bucketName) {
        try {
            CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            CreateBucketResponse bucket = s3Client.createBucket(createBucketRequest);

            System.out.println("Bucket created with success");
        } catch (S3Exception e) {
            System.out.println("Error the create new bucket: " + e.getMessage());
        }
    }

    @Async
    public void checkBucketExists(String bucketName) {
        try {
            HeadBucketRequest head = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

            s3Client.headBucket(head);
        } catch (NoSuchBucketException e) {
            System.out.println("Bucket '" + bucketName + "' NÃO existe.");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (S3Exception e) {
            System.err.println("Erro ao verificar a existência do bucket '" + bucketName + "': " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @Async
    public List<String> listAllBucket() {
        try {
            ListBucketsRequest list = ListBucketsRequest.builder().build();

            ListBucketsResponse response = s3Client.listBuckets(list);

            return response.buckets().stream()
                    .map(Bucket::name)
                    .collect(Collectors.toList());
        } catch (S3Exception e) {
            System.out.println("Error the create new bucket: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Async
    public void putObject(String bucketName, String key, MultipartFile filePath, String content) {
        try {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(content)
                    .build(), RequestBody.fromInputStream(filePath.getInputStream(), filePath.getSize()));

            System.out.println("Objeto '" + key + "' enviado para o bucket '" + bucketName + "' com sucesso!");
        } catch (S3Exception e) {
            System.out.println("Error the to put file in bucket: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error the upload file");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error the upload file");
        }
    }

    @Async
    public List<String> listObjects(String buckteName) {
        try {
            ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                    .bucket(buckteName)
                    .build();

            ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);

            return listObjectsV2Response.contents().stream()
                    .map(S3Object::key)
                    .collect(Collectors.toList());
        } catch (S3Exception e) {
            System.out.println("Error the to list file in bucket: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Async
    public void deleteObject(String bucketName, String key) {
        try {
            DeleteObjectRequest deletedObject = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deletedObject);
            System.out.println("Objeto '" + key + "' excluído do bucket '" + bucketName + "' com sucesso!");
        } catch (S3Exception e) {
            System.out.println("Error the to delete file in bucket: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Async
    public void deleteBucket(String bucketName) {
        try {
            listObjects(bucketName).forEach(key -> deleteObject(bucketName, key));

            DeleteBucketRequest deleteObjectRequest = DeleteBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

            s3Client.deleteBucket(deleteObjectRequest);
        } catch (S3Exception e) {
            System.out.println("Error the to list file in bucket: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Async
    public ResponseInputStream<GetObjectResponse> downloadFile(String bucketName, String key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            System.out.println("Tentando baixar o objeto '" + key + "' do bucket '" + bucketName + "'...");
            return s3Client.getObject(getObjectRequest);
        } catch (NoSuchKeyException e) {
            System.err.println("Arquivo '" + key + "' não encontrado no bucket '" + bucketName + "': " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error the download file");
        } catch (S3Exception e) {
            System.err.println("Erro ao baixar o arquivo '" + key + "' do bucket '" + bucketName + "': " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error the download file");
        }
    }

    @Async
    public void copyObject(String sourceBucket, String sourceKey, String destinationBucket, String destinationKey) {
        try {
            CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                    .sourceBucket(sourceBucket)
                    .sourceKey(sourceKey)
                    .destinationBucket(destinationBucket)
                    .destinationKey(destinationKey)
                    .build();

            s3Client.copyObject(copyObjectRequest);

            System.out.printf("Objeto '%s/%s' copiado para '%s/%s' com sucesso!%n",
                    sourceBucket, sourceKey, destinationBucket, destinationKey);
        } catch (S3Exception e) {
            System.err.printf("Erro ao copiar objeto '%s/%s': %s%n", sourceBucket, sourceKey, e.getMessage());
            throw new RuntimeException("Erro ao copiar objeto no S3: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error the upload file");
        }
    }

    @Async
    public void moveObject(String sourceBucket, String sourceKey, String destinationBucket, String destinationKey) {
        try {
            copyObject(sourceBucket, sourceKey, destinationBucket, destinationKey);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(sourceBucket)
                    .key(sourceKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            System.out.printf("Objeto original '%s/%s' deletado com sucesso após mover.%n", sourceBucket, sourceKey);
        } catch (S3Exception e) {
            System.err.printf("Erro ao copiar objeto '%s/%s': %s%n", sourceBucket, sourceKey, e.getMessage());
            throw new RuntimeException("Erro ao copiar objeto no S3: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error the upload file");
        }
    }

    @Async
    public void putObjectWithMetadata(String bucketName, String key, MultipartFile file, Long userId) {
        String contentType = file.getContentType();
        if (contentType.isBlank() || (!contentType.startsWith("image/") && !contentType.equals("application/pdf") && !contentType.equals("text/plain"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de arquivo não suportado. Apenas imagens, PDFs e textos são permitidos.");
        }

        try {
            Map<String, String> customMetadata = new HashMap<>();

            customMetadata.put("userId", String.valueOf(userId));

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .metadata(customMetadata)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            System.out.println("Objeto '" + key + "' enviado para o bucket '" + bucketName + "' com sucesso (com metadados)!");
        } catch (S3Exception e) {
            System.err.println("Erro ao enviar objeto S3 com metadados: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao enviar arquivo para o bucket S3: " + e.getMessage(), e);
        } catch (IOException e) {
            System.err.println("Erro de I/O ao ler o arquivo: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro de I/O ao processar o arquivo para upload: " + e.getMessage(), e);
        }
    }

    @Async
    public Map<String, String> getHeaderObject(String bucketName, String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            return s3Client.headObject(headObjectRequest).metadata();
        } catch (S3Exception e) {
            System.err.println("Erro ao enviar objeto S3 com metadados: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao enviar arquivo para o bucket S3: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro de I/O ao processar o arquivo para upload: " + e.getMessage(), e);
        }
    }

    @Async
    public List<String> listObjectsByUserId(String bucketName, Long userId, String prefix) {
        List<String> userFiles = new ArrayList<>();
        String targetUserIdString = String.valueOf(userId);

        try {
            ListObjectsV2Request.Builder listObjectsRequestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucketName);

            listObjectsRequestBuilder.prefix(prefix);
            ListObjectsV2Request listObjectsRequest = listObjectsRequestBuilder.build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listObjectsRequest);

            if (listResponse.contents().isEmpty()) {
                System.out.printf("Nenhum objeto encontrado no bucket '%s'%s%n", bucketName, (prefix != null ? " com prefixo '" + prefix + "'" : ""));
                return Collections.emptyList();
            }

            for (S3Object s3Object : listResponse.contents()) {
                String objectKey = s3Object.key();
                try {
                    Map<String, String> metadata = getHeaderObject(bucketName, objectKey);

                    if (metadata != null && metadata.containsKey("userId") && metadata.get("userId").equals(targetUserIdString)) {
                        userFiles.add(objectKey);
                    }
                } catch (ResponseStatusException e) {

                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        System.err.printf("Objeto '%s/%s' não encontrado durante verificação de metadados, ignorando.%n", bucketName, objectKey);
                    } else {
                        throw e;
                    }
                }
            }

            System.out.printf("Encontrados %d arquivos para o userId '%s' no bucket '%s'%s.%n",
                    userFiles.size(), targetUserIdString, bucketName, (prefix != null ? " com prefixo '" + prefix + "'" : ""));
            return userFiles;

        } catch (S3Exception e) {
            System.err.printf("Erro ao listar objetos por userId no bucket '%s': %s%n", bucketName, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao listar arquivos por usuário no S3: " + e.getMessage(), e);
        }
    }

    @Async
    public String getHeaderBucket(String bucketName) {
        HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                .bucket(bucketName)
                .build();

        HeadBucketResponse headBucketResponse = s3Client.headBucket(headBucketRequest);
        return headBucketResponse.bucketRegion();
    }

    @Async
    public URL generatePresignedDownloadUrl(String bucketName, String key, long expirationDays) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofDays(expirationDays))
                    .getObjectRequest(getObjectRequest)
                    .build();

            return s3Presigner.presignGetObject(presignRequest).url();
        } catch (S3Exception e) {
            System.err.printf("Erro ao gerar URL pré-assinada para '%s/%s' (S3Client): %s%n", bucketName, key, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao gerar URL pré-assinada: " + e.getMessage(), e);
        }
    }

//================================================= VERSIONING =========================================================

    @Async
    public void enableBucketVersioning(String bucketName) {
        try {
            PutBucketVersioningRequest putBucketVersioningRequest = PutBucketVersioningRequest.builder()
                    .bucket(bucketName)
                    .versioningConfiguration(VersioningConfiguration.builder()
                            .status(BucketVersioningStatus.ENABLED)
                            .build())
                    .build();

            s3Client.putBucketVersioning(putBucketVersioningRequest);
            System.out.println("Versionamento habilitado para o bucket '" + bucketName + "'.");
        } catch (S3Exception e) {
            System.err.println("Erro ao habilitar versionamento para o bucket '" + bucketName + "': " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao configurar versionamento do bucket: " + e.getMessage(), e);
        }
    }

    @Async
    public void BucketVersioning(String bucketName) {
        try {
            PutBucketVersioningRequest putBucketVersioningRequest = PutBucketVersioningRequest.builder()
                    .bucket(bucketName)
                    .versioningConfiguration(VersioningConfiguration.builder()
                            .status(BucketVersioningStatus.SUSPENDED)
                            .build())
                    .build();

            s3Client.putBucketVersioning(putBucketVersioningRequest);
            System.out.println("Versionamento suspenso para o bucket '" + bucketName + "'.");
        } catch (S3Exception e) {
            System.err.println("Erro ao suspender versionamento para o bucket '" + bucketName + "': " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao configurar versionamento do bucket: " + e.getMessage(), e);
        }
    }

    @Async
    public List<ObjectVersion> listObjectVersions(String bucketName, String key) {
        List<ObjectVersion> versions = new ArrayList<>();

        try {
            ListObjectVersionsRequest listObjectVersionsRequest = ListObjectVersionsRequest.builder()
                    .bucket(bucketName)
                    .prefix(key)
                    .build();

            ListObjectVersionsResponse response;
            String nextVersionIdMarker = null;

            do {
                response = s3Client.listObjectVersions(listObjectVersionsRequest);
                versions.addAll(response.versions());
                nextVersionIdMarker = response.nextVersionIdMarker();
                listObjectVersionsRequest = listObjectVersionsRequest.toBuilder().keyMarker(nextVersionIdMarker).build();
            } while (response.isTruncated());

            System.out.printf("Encontradas %d versões para o objeto '%s' no bucket '%s'.%n", versions.size(), key, bucketName);
            return versions;
        } catch (S3Exception e) {
            System.err.printf("Erro ao listar versões do objeto '%s/%s': %s%n", bucketName, key, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao listar versões do objeto S3: " + e.getMessage(), e);
        }


    }

    @Async
    public InputStream downloadSpecificObjectVersion(String bucketName, String key, String versionId) {
        try {

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .versionId(versionId)
                    .build();

            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);

            System.out.printf("Versão '%s' do arquivo '%s/%s' baixada para o stream com sucesso!%n", versionId, bucketName, key);
            return s3Object;
        } catch (NoSuchKeyException e) {
            System.err.printf("Versão '%s' do objeto '%s/%s' não encontrada ao tentar baixar o arquivo.%n", versionId, bucketName, key);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Versão '" + versionId + "' do arquivo '" + key + "' não encontrada.", e);
        } catch (S3Exception e) {
            System.err.printf("Erro ao baixar versão '%s' do arquivo '%s/%s': %s%n", versionId, bucketName, key, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao baixar versão do arquivo S3: " + e.getMessage(), e);
        }
    }

    @Async
    public void deleteSpecificObjectVersion(String bucketName, String key, String versionId) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .versionId(versionId)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            System.out.printf("Versão '%s' do objeto '%s/%s' excluída permanentemente com sucesso.%n", versionId, bucketName, key);
        } catch (NoSuchKeyException e) {
            System.err.printf("Versão '%s' do objeto '%s/%s' não encontrada ao tentar deletar o arquivo.%n", versionId, bucketName, key);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Versão '" + versionId + "' do arquivo '" + key + "' não encontrada.", e);
        } catch (S3Exception e) {
            System.err.printf("Erro ao excluir versão '%s' do objeto '%s/%s': %s%n", versionId, bucketName, key, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao excluir versão do objeto S3: " + e.getMessage(), e);
        }
    }

//============================================ SOFTDELETES/ LIFECYCLE POLICES ==========================================

    @Async
    public void applyLifecyclePolicy(String bucketName, String prefix) {
        try {
            LifecycleRule rule1 = LifecycleRule.builder()
                    .id("MoveToStandardIAAndExpireCurrentVersions")
                    .filter(f -> f.prefix(prefix))
                    .status(String.valueOf(ReplicationRuleStatus.ENABLED))
                    .transitions(Transition.builder()
                            .days(30)
                            .storageClass(TransitionStorageClass.STANDARD_IA)
                            .build())
                    .expiration(LifecycleExpiration.builder().days(365).build())
                    .build();

            LifecycleRule rule2 = LifecycleRule.builder()
                    .id("ExpireNonCurrentVersions")
                    .filter(f -> f.prefix(prefix))
                    .status(String.valueOf(ReplicationRuleStatus.ENABLED))
                    .noncurrentVersionTransitions(NoncurrentVersionTransition.builder()
                            .noncurrentDays(60)
                            .storageClass(TransitionStorageClass.GLACIER)
                            .build())
                    .noncurrentVersionExpiration(NoncurrentVersionExpiration.builder()
                            .noncurrentDays(365)
                            .build())
                    .build();

            LifecycleRule rule3 = LifecycleRule.builder()
                    .id("AbortIncompleteMultipartUploads")
                    .filter(f -> f.prefix(prefix))
                    .abortIncompleteMultipartUpload(AbortIncompleteMultipartUpload.builder()
                            .daysAfterInitiation(7)
                            .build())
                    .build();

            BucketLifecycleConfiguration lifecycleConfiguration = BucketLifecycleConfiguration.builder()
                    .rules(rule1, rule2, rule3)
                    .build();

            PutBucketLifecycleConfigurationRequest putLifecycleConfigurationRequest = PutBucketLifecycleConfigurationRequest.builder()
                    .bucket(bucketName)
                    .lifecycleConfiguration(lifecycleConfiguration)
                    .build();

            s3Client.putBucketLifecycleConfiguration(putLifecycleConfigurationRequest);
            System.out.printf("Política de ciclo de vida aplicada ao bucket '%s' (prefixo: %s).%n", bucketName, prefix);
        } catch (S3Exception e) {
            System.err.printf("Erro ao aplicar política de ciclo de vida ao bucket '%s': %s%n", bucketName, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao configurar política de ciclo de vida S3: " + e.getMessage(), e);
        }
    }

    @Async
    public String generatePresignedUploadUrl(String bucketName, String key, long expirationSeconds, String contentType, Long userId) {
        try {
            Map<String, String> metadata = new HashMap<>();

            if (userId != null) { metadata.put("userId", userId.toString()); }

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .metadata(metadata)
                    .contentType(contentType)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .putObjectRequest(putObjectRequest)
                    .signatureDuration(Duration.ofSeconds(expirationSeconds))
                    .build();

            PresignedPutObjectRequest presignedPutObjectRequest = s3Presigner.presignPutObject(presignRequest);
            String url = presignedPutObjectRequest.url().toString();
            System.out.printf("URL pré-assinada para upload de '%s/%s' gerada com sucesso: %s%n", bucketName, key, url);
            return url;
        } catch (S3Exception e) {
            System.err.printf("Erro ao gerar URL pré-assinada para upload de '%s/%s': %s%n", bucketName, key, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao gerar URL pré-assinada para upload: " + e.getMessage(), e);
        }
    }

//================================================== OBJECT WITH PUBLIC ================================================

    @Async
    public void makeObjectPublic(String bucketName, String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(key).build());

            PutObjectAclRequest putObjectAclRequest = PutObjectAclRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            s3Client.putObjectAcl(putObjectAclRequest);
            System.out.printf("Objeto '%s/%s' foi tornado público com sucesso.%n", bucketName, key);
        } catch (NoSuchKeyException e) {
            System.err.printf("Objeto '%s/%s' não encontrado para ser tornado público.%n", bucketName, key);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Objeto '" + key + "' não encontrado no bucket '" + bucketName + "'.", e);
        } catch (S3Exception e) {
            System.err.printf("Erro ao tornar objeto '%s/%s' público: %s%n", bucketName, key, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao configurar acesso público para o objeto S3: " + e.getMessage(), e);
        }
    }

    @Async
    public String getPublicObjectUrl(String bucketName, String key) {
        try {
            URL url = new URL("http://localhost:4566/" + bucketName + "/" + key);
            return url.toString();
        } catch (java.net.MalformedURLException e) {
            System.err.printf("Erro ao construir URL pública para '%s/%s': %s%n", bucketName, key, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao gerar URL pública.", e);
        }
    }


}
