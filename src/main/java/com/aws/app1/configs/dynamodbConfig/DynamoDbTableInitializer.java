package com.aws.app1.configs.dynamodbConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;


@Component
public class DynamoDbTableInitializer implements CommandLineRunner {

    @Autowired
    private DynamoDbClient dynamoDbClient;

    @Override
    public void run(String... args) {
        createUserTable();
        createTaskTable();
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
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName("taskId")
                            .attributeType(ScalarAttributeType.S)
                            .build())
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName("userId")
                            .attributeType(ScalarAttributeType.S)
                            .build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build();

            dynamoDbClient.createTable(request);
            System.out.println("Table tasks created!");
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
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build();

            dynamoDbClient.createTable(request);
            System.out.println("Tabela 'users_metric' criada!");
        }
    }

    private boolean tableExists(String tableName) {
        ListTablesResponse tables = dynamoDbClient.listTables();
        return !tables.tableNames().contains(tableName);
    }

}
