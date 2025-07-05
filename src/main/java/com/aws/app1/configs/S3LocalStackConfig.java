package com.aws.app1.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class S3LocalStackConfig {
    private static final String LOCALSTACK_ENDPOINT = "http://localhost:4566";
    private static final String AWS_REGION = "us-east-1";
    private static final String AWS_ACCESS_KEY = "test";
    private static final String AWS_SECRET_KEY = "test";

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(LOCALSTACK_ENDPOINT))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(AWS_ACCESS_KEY, AWS_SECRET_KEY)))
                .region(Region.of(AWS_REGION))
                .forcePathStyle(true)
                .build();
    }

}
