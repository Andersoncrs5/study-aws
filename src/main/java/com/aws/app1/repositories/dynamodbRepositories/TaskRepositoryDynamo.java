package com.aws.app1.repositories.dynamodbRepositories;

import com.aws.app1.controller.dynamodbController.DTOs.UpdateTaskDTO;
import com.aws.app1.entities.dynamodbEntities.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.*;

@Repository
public class TaskRepositoryDynamo {

    private final String tableName = "tasks";

    @Autowired
    private DynamoDbClient dynamoDbClient;

    public void save(Task task) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("taskId", AttributeValue.fromS(task.getTaskId()));
        item.put("userId", AttributeValue.fromS(task.getUserId()));
        item.put("title", AttributeValue.fromS(task.getTitle()));
        item.put("description", AttributeValue.fromS(task.getDescription()));
        item.put("done", AttributeValue.fromBool(task.isDone()));
        item.put("createdAt", AttributeValue.fromS(task.getCreatedAt().toString()));
        item.put("updatedAt", AttributeValue.fromS(task.getUpdatedAt().toString()));

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();
        
        dynamoDbClient.putItem(request);
    }

    public Optional<Task> findById(String taskId) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("taskId", AttributeValue.fromS(taskId));
        
        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(item)
                .build();

        Map<String, AttributeValue> response = dynamoDbClient.getItem(request).item();

        if (response.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(mapToTask(response));
    }

    public void deleteById(String taskId) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("taskId", AttributeValue.fromS(taskId));

        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(item)
                .build();

        dynamoDbClient.deleteItem(request);
    }

    public List<Task> listByUserId(String userId, int limit) {
        ScanRequest request = ScanRequest.builder()
                .tableName(tableName)
                .filterExpression("userId = :uid")
                .expressionAttributeValues(Map.of(":uid", AttributeValue.fromS(userId)))
                .limit(limit)
                .build();

        ScanResponse response = dynamoDbClient.scan(request);

        List<Task> tasks = new ArrayList<>();

        for (Map<String, AttributeValue> item : response.items()) {
            tasks.add(mapToTask(item));
        }

        return tasks;
    }

    public void update(String taskId, UpdateTaskDTO task) {
        Map<String, AttributeValue> values = new HashMap<>();

        values.put(":title", AttributeValue.fromS(task.title()));
        values.put(":description", AttributeValue.fromS(task.description()));
        values.put(":done", AttributeValue.fromBool(task.done()));
        values.put(":updatedAt", AttributeValue.fromS(Instant.now().toString()));

        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        "taskId", AttributeValue.fromS(taskId)))
                .updateExpression("SET title = :title, description = :description, done = :done, updatedAt = :updatedAt")
                .expressionAttributeValues(values)
                .build();

        dynamoDbClient.updateItem(request);
    }

    private Task mapToTask(Map<String, AttributeValue> item) {
        return Task.builder()
                .taskId(item.get("taskId").s())
                .userId(item.get("userId").s())
                .title(item.get("title").s())
                .description(item.get("description").s())
                .done(item.get("done").bool())
                .createdAt(Instant.parse(item.get("createdAt").s()))
                .updatedAt(Instant.parse(item.get("updatedAt").s()))
                .build();
    }

}