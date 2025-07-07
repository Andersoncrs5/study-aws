package com.aws.app1.controller.dynamodbController;

import com.aws.app1.controller.dynamodbController.DTOs.UpdateUserDTO;
import com.aws.app1.entities.dynamodbEntities.User;
import com.aws.app1.entities.dynamodbEntities.UserMetric;
import com.aws.app1.repositories.dynamodbRepositories.UserMetricRepository;
import com.aws.app1.services.dynamodbService.UserServiceDynamodb;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/dynamo/user")
public class UserControllerDynamo {

    private final UserServiceDynamodb userService;
    private final UserMetricRepository userMetricRepository;

    @PostMapping("create")
    public ResponseEntity<?> create(@RequestBody User userRequest) {
        User user = userService.create(userRequest.getName(), userRequest.getEmail(), userRequest.getPassword());
        UserMetric metric = UserMetric.builder().userId(user.getUserId()).build();
        metric.setCreatedAt(Instant.now());
        this.userMetricRepository.save(metric);

        return ResponseEntity.ok(user);
    }

    @GetMapping("/list-user")
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(this.userService.listUser());
    }

    @GetMapping("getById/{userId}")
    public ResponseEntity<?> getById(@PathVariable String userId) {
        User user = userService.get(userId);
        return ResponseEntity.ok(user);
    }

    @PutMapping("update/{userId}")
    public ResponseEntity<?> update(@PathVariable String userId, @RequestBody UpdateUserDTO updatedUser) {
        userService.updateUser(userId, updatedUser);
        return ResponseEntity.ok("Updated");
    }

    @DeleteMapping("delete/{userId}")
    public ResponseEntity<?> delete(@PathVariable String userId) {
        userService.delete(userId);
        return ResponseEntity.ok("User deleted");
    }

}
