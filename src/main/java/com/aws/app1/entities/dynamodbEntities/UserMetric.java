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
public class UserMetric {
    private String userId;
    private Long allTasks;
    private Long allTasksDone;
    private Long allTasksNotDone;
    private Instant createdAt;
    private Instant updatedAt;
}
