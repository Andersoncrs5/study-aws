package com.aws.app1.repositories.dynamodbRepositories;

import com.aws.app1.entities.dynamodbEntities.UserMetric;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class UserMetricRepository {
    private final String tableName = "users_metric";
    @Autowired
    private DynamoDbClient dynamoDbClient;

    public void save(UserMetric metric) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("userId", AttributeValue.fromS(metric.getUserId()));
        item.put("allTasks", AttributeValue.fromN(String.valueOf(metric.getAllTasks())));
        item.put("allTasksDone", AttributeValue.fromN(String.valueOf(metric.getAllTasksDone())));
        item.put("allTasksNotDone", AttributeValue.fromN(String.valueOf(metric.getAllTasksNotDone())));
        item.put("createdAt", AttributeValue.fromS(metric.getCreatedAt().toString()));
        item.put("updatedAt", AttributeValue.fromS(metric.getUpdatedAt().toString()));

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();

        PutItemResponse response = dynamoDbClient.putItem(request);
    }

    public Optional<UserMetric> findByUserId(String userId) {
        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("userId", AttributeValue.fromS(userId)))
                .build();

        Map<String, AttributeValue> item = dynamoDbClient.getItem(request).item();

        if (item == null || item.isEmpty()) {
            return Optional.empty();
        }

        UserMetric metric = mapToUserMetric(item);
        return Optional.of(metric);
    }

    public void update(UserMetric metric) {
        Map<String, AttributeValue> key = Map.of(
                "userId", AttributeValue.fromS(metric.getUserId())
        );

        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":allTasks", AttributeValue.fromN(metric.getAllTasks().toString()));
        values.put(":allTasksDone", AttributeValue.fromN(metric.getAllTasksDone().toString()));
        values.put(":allTasksNotDone", AttributeValue.fromN(metric.getAllTasksNotDone().toString()));
        values.put(":updatedAt", AttributeValue.fromS(metric.getUpdatedAt().toString()));

        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .updateExpression("SET allTasks = :allTasks, allTasksDone = :allTasksDone, allTasksNotDone = :allTasksNotDone, updatedAt = :updatedAt")
                .expressionAttributeValues(values)
                .build();

        dynamoDbClient.updateItem(request);
    }

    private UserMetric mapToUserMetric(Map<String, AttributeValue> item) {
        return UserMetric.builder()
                .userId(item.get("userId").s())
                .allTasks(Long.parseLong(item.get("allTasks").n()))
                .allTasksDone(Long.parseLong(item.get("allTasksDone").n()))
                .allTasksNotDone(Long.parseLong(item.get("allTasksNotDone").n()))
                .createdAt(Instant.parse(item.get("createdAt").s()))
                .updatedAt(Instant.parse(item.get("updatedAt").s()))
                .build();
    }
}
