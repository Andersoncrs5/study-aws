package com.aws.app1.services.dynamodbService;

import com.aws.app1.controller.dynamodbController.DTOs.UpdateUserDTO;
import com.aws.app1.entities.dynamodbEntities.User;
import com.aws.app1.repositories.dynamodbRepositories.UserRepositoryDynamodb;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class UserServiceDynamodb {

    private final UserRepositoryDynamodb repository;

    public User create(String name, String email, String password) {
        User user = User.builder()
                .userId(UUID.randomUUID().toString())
                .name(name)
                .email(email)
                .password(password)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        this.repository.save(user);
        return user;
    }

    public User get(String userId) {
        Optional<User> optional = this.repository.findById(userId);

        if (optional.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        return optional.get();
    }

    public void delete(String userId) {
        this.repository.deleteById(userId);
    }

    public void updateUser(String userId, UpdateUserDTO dto) {
        this.repository.update(userId, dto);
    }

    public List<User> listUser() {
        return this.repository.listUser();
    }

}
