package com.aws.app1.configs.dynamodbConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Arrays;
import java.util.Collections;


@Component
public class DynamoDbTableInitializer implements CommandLineRunner {

    @Autowired
    private DynamoDbClient dynamoDbClient;

    @Override
    public void run(String... args) {
        createUserTable();
        createTaskTable();
        createUserMetric();
    }

    private void createUserTable() {
        String tableName = "users";

        if (tableExists(tableName)) {
            CreateTableRequest request = CreateTableRequest.builder()
                    .tableName(tableName)
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("userId")
                            .keyType(KeyType.HASH)
                            .build())
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName("userId")
                            .attributeType(ScalarAttributeType.S)
                            .build())
                    .provisionedThroughput(ProvisionedThroughput.builder()
                            .readCapacityUnits(20L)
                            .writeCapacityUnits(20L)
                            .build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build();

            dynamoDbClient.createTable(request);
            System.out.println("Table users created!");
        }

    }

    private void createTaskTable() {
        String tableName = "tasks";

        if (tableExists(tableName)) {
            CreateTableRequest request = CreateTableRequest.builder()
                    .tableName(tableName)
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("taskId")
                            .keyType(KeyType.HASH)
                            .build())
                    .attributeDefinitions(Collections.singletonList(
                            AttributeDefinition.builder()
                                    .attributeName("taskId")
                                    .attributeType(ScalarAttributeType.S)
                                    .build()
                    ))
                    .provisionedThroughput(ProvisionedThroughput.builder()
                            .writeCapacityUnits(20L)
                            .readCapacityUnits(20L)
                            .build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build();

            try {
                dynamoDbClient.createTable(request);
                System.out.println("Table " + tableName + " created successfully!");
            } catch (ResourceInUseException e) {
                System.out.println("Table " + tableName + " already exists (ResourceInUseException).");
            } catch (DynamoDbException e) {
                System.err.println("Error creating table " + tableName + ": " + e.getMessage());
                System.out.println(e.toString());
            }
        } else {
            System.out.println("Table " + tableName + " already exists, skipping creation.");
        }
    }

    private void createUserMetric() {
        String tableName = "users_metric";

        if (tableExists(tableName)) {
            CreateTableRequest request = CreateTableRequest.builder()
                    .tableName(tableName)
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("userId")
                            .keyType(KeyType.HASH)
                            .build())
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName("userId")
                            .attributeType(ScalarAttributeType.S)
                            .build())
                    .provisionedThroughput(ProvisionedThroughput.builder()
                            .readCapacityUnits(20L)
                            .writeCapacityUnits(20L)
                            .build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build();

            dynamoDbClient.createTable(request);
            System.out.println("Tabela 'users_metric' criada!");
        } else {
            System.out.println("Table user already exist!");
        }
    }

    private boolean tableExists(String tableName) {
        ListTablesResponse tables = dynamoDbClient.listTables();
        return !tables.tableNames().contains(tableName);
    }

}
