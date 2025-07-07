package com.aws.app1.services.dynamodbService;

import com.aws.app1.controller.dynamodbController.DTOs.UpdateTaskDTO;
import com.aws.app1.entities.dynamodbEntities.Task;
import com.aws.app1.repositories.dynamodbRepositories.TaskRepositoryDynamo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskServiceDynamodb {

    @Autowired
    private TaskRepositoryDynamo taskRepository;

    public Task createTask(String userId, String title, String description) {
        Task task = Task.builder()
                .taskId(UUID.randomUUID().toString())
                .userId(userId)
                .title(title)
                .description(description)
                .done(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        taskRepository.save(task);
        return task;
    }

    public Task findById(String taskId) {
        Optional<Task> optionalTask = taskRepository.findById(taskId);

        if (optionalTask.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        return optionalTask.get();
    }

    public List<Task> listByUser(String userId, int limit) {
        return taskRepository.listByUserId(userId, limit);
    }

    public void deleteTask(String taskId) {
        taskRepository.deleteById(taskId);
    }

    public void updateTask(String taskId, UpdateTaskDTO task) {
        taskRepository.update(taskId, task);
    }
}