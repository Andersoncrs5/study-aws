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
import java.nio.file.Paths;
import java.util.List;
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


}
