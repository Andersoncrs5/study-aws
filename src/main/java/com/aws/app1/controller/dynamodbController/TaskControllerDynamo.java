package com.aws.app1.controller.dynamodbController;

import com.aws.app1.controller.dynamodbController.DTOs.UpdateTaskDTO;
import com.aws.app1.entities.dynamodbEntities.Task;
import com.aws.app1.services.dynamodbService.TaskServiceDynamodb;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/dynamo/task")
public class TaskControllerDynamo {

    private final TaskServiceDynamodb taskService;

    @PostMapping("create")
    public ResponseEntity<Task> create(@RequestBody Task taskRequest) {
        Task task = taskService.createTask(
                taskRequest.getUserId(),
                taskRequest.getTitle(),
                taskRequest.getDescription()
        );
        return ResponseEntity.ok(task);
    }

    @GetMapping("/getById/{taskId}")
    public ResponseEntity<Task> getById(@PathVariable String taskId) {
        Task task = taskService.findById(taskId);
        return ResponseEntity.ok(task);
    }

    @GetMapping("/getByUser/{userId}/{limit}")
    public ResponseEntity<List<Task>> getByUser(@PathVariable String userId, @PathVariable int limit) {
        return ResponseEntity.ok(taskService.listByUser(userId, limit));
    }

    @PutMapping("/update/{taskId}")
    public ResponseEntity<?> update(@PathVariable String taskId, @RequestBody UpdateTaskDTO taskRequest) {
        taskService.updateTask(taskId, taskRequest);
        return ResponseEntity.ok("Updated");
    }

    @DeleteMapping("/delete/{taskId}")
    public ResponseEntity<Void> delete(@PathVariable String taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }
}