package com.aws.app1.entities.dynamodbEntities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    private String taskId;
    private String userId;
    private String title;
    private String description;
    private boolean done;
    private Instant createdAt;
    private Instant updatedAt;
}
