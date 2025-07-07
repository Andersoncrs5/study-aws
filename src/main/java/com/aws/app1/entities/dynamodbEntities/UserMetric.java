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
    private Long allTasks = 0L;
    private Long allTasksDone = 0L;
    private Long allTasksNotDone = 0L;
    private Instant createdAt;
    private Instant updatedAt;
}
