package com.aws.app1.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class S3Service {

    @Autowired
    private S3Client s3Client;

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

    public void putObject(String bucketName, String key, MultipartFile filePath) {
        try {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build(), RequestBody.fromInputStream(filePath.getInputStream(), filePath.getSize() ));

            System.out.println("Objeto '" + key + "' enviado para o bucket '" + bucketName + "' com sucesso!");
        } catch (S3Exception e) {
            System.out.println("Error the to put file in bucket: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Error the upload file");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Error the upload file");
        }
    }

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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Error the download file");
        } catch (S3Exception e) {
            System.err.println("Erro ao baixar o arquivo '" + key + "' do bucket '" + bucketName + "': " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Error the download file");
        }
    }

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
        }
        catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Error the upload file");
        }
    }

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
        }
        catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Error the upload file");
        }
    }

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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Erro ao enviar arquivo para o bucket S3: " + e.getMessage(), e);
        } catch (IOException e) {
            System.err.println("Erro de I/O ao ler o arquivo: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Erro de I/O ao processar o arquivo para upload: " + e.getMessage(), e);
        }
    }

    public Map<String, String> getHeaderObject(String bucketName, String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            return s3Client.headObject(headObjectRequest).metadata();
        } catch (S3Exception e) {
            System.err.println("Erro ao enviar objeto S3 com metadados: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Erro ao enviar arquivo para o bucket S3: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Erro de I/O ao processar o arquivo para upload: " + e.getMessage(), e);
        }
    }

    public String getHeaderBucket(String bucketName) {
        HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                .bucket(bucketName)
                .build();

        HeadBucketResponse headBucketResponse = s3Client.headBucket(headBucketRequest);
        return headBucketResponse.bucketRegion();
    }
    
//    public String generatePresignedDownloadUrl(String bucketName, String key, long expirationSeconds) {
//        try {
//            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
//                    .bucket(bucketName)
//                    .key(key)
//                    .build();
//
//            URL presignedUrl = s3Client.presignUrl(b -> b
//                    .signatureDuration(Duration.ofSeconds(expirationSeconds))
//                    .getObject(getObjectRequest)
//            );
//
//            String url = presignedUrl.toString();
//            System.out.printf("URL pré-assinada para '%s/%s' gerada com sucesso usando S3Client: %s%n", bucketName, key, url);
//            return url;
//
//        } catch (S3Exception e) {
//            System.err.printf("Erro ao gerar URL pré-assinada para '%s/%s' (S3Client): %s%n", bucketName, key, e.getMessage());
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Erro ao gerar URL pré-assinada: " + e.getMessage(), e);
//        }
//    }

}
