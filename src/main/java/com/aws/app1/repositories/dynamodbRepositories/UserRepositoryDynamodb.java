package com.aws.app1.repositories.dynamodbRepositories;

import com.aws.app1.controller.dynamodbController.DTOs.UpdateUserDTO;
import com.aws.app1.entities.dynamodbEntities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.*;

@Repository
public class UserRepositoryDynamodb {
    private final String tableName = "users";
    @Autowired
    private DynamoDbClient dynamoDbClient;

    public void save(User user) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("userId", AttributeValue.fromS(user.getUserId()));
        item.put("name", AttributeValue.fromS(user.getName()));
        item.put("email", AttributeValue.fromS(user.getEmail()));
        item.put("password", AttributeValue.fromS(user.getPassword()));
        item.put("createdAt", AttributeValue.fromS(user.getCreatedAt().toString()));
        item.put("updatedAt", AttributeValue.fromS(user.getUpdatedAt().toString()));

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
    }

    public Boolean existsEmail(String email) {
        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("email", AttributeValue.fromS(email)))
                .build();

        Map<String, AttributeValue> item = dynamoDbClient.getItem(request).item();

        return !item.isEmpty();
    }

    public void deleteById(String userId) {
        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("userId", AttributeValue.fromS(userId)))
                .build();

        dynamoDbClient.deleteItem(request);
    }

    public Optional<User> findById(String userId) {
        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("userId", AttributeValue.fromS(userId)))
                .build();

        Map<String, AttributeValue> item = dynamoDbClient.getItem(request).item();

        if (item.isEmpty()) {
            return Optional.empty();
        }

        User user = mapToUser(item);
        return Optional.of(user);
    }

    public void update(String userId, UpdateUserDTO dto) {

        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":password", AttributeValue.fromS(dto.password()));
        values.put(":updatedAt", AttributeValue.fromS(Instant.now().toString()));

        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("userId", AttributeValue.fromS(userId)))
                .updateExpression("SET password = :password, updatedAt = :updatedAt")
                .expressionAttributeValues(values)
                .build();

        dynamoDbClient.updateItem(request);
    }

    public List<User> listUser() {
        ScanRequest request = ScanRequest.builder()
                .tableName(tableName)
                .build();

        ScanResponse scan = dynamoDbClient.scan(request);

        List<User> users = new ArrayList<>();

        for (Map<String, AttributeValue> map : scan.items()) {
            users.add(mapToUser(map));
        }

        return users;
    }

    private User mapToUser(Map<String, AttributeValue> item) {
        return User.builder()
                .userId(item.get("userId").s())
                .name(item.get("name").s())
                .email(item.get("email").s())
                .password(item.get("password").s())
                .createdAt(Instant.parse(item.get("createdAt").s()))
                .updatedAt(Instant.parse(item.get("updatedAt").s()))
                .build();
    }
}
