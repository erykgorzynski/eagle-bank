package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.api.UserApi;
import org.example.model.*;
import org.example.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

/**
 * User Controller implementing the generated UserApi interface
 * Handles user management operations with proper authentication and authorization
 */
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
@Slf4j
public class UserController extends BaseController implements UserApi {

    private final UserService userService;

    @Override
    public ResponseEntity<UserResponse> createUser(CreateUserRequest createUserRequest) {
        log.info("Creating user with email: {}", createUserRequest.getEmail());

        UserResponse userResponse = userService.createUser(createUserRequest);
        return new ResponseEntity<>(userResponse, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<UserResponse> fetchUserByID(String userId) {
        String authenticatedUserId = getCurrentUserId();
        if (authenticatedUserId == null) {
            throw new org.springframework.security.core.AuthenticationException("User not authenticated") {};
        }

        UserResponse userResponse = userService.findById(userId);

        if (!userId.equals(authenticatedUserId)) {
            log.warn("User {} attempted to access data for user {}", authenticatedUserId, userId);
            throw new org.springframework.security.access.AccessDeniedException("Access denied to user data");
        }

        return ResponseEntity.ok(userResponse);
    }

    @Override
    public ResponseEntity<UserResponse> updateUserByID(String userId, UpdateUserRequest updateUserRequest) {
        String authenticatedUserId = getCurrentUserId();
        if (authenticatedUserId == null) {
            throw new org.springframework.security.core.AuthenticationException("User not authenticated") {};
        }

        userService.findById(userId);

        if (!userId.equals(authenticatedUserId)) {
            log.warn("User {} attempted to update data for user {}", authenticatedUserId, userId);
            throw new org.springframework.security.access.AccessDeniedException("Access denied to user data");
        }

        UserResponse userResponse = userService.updateUser(userId, updateUserRequest);
        return ResponseEntity.ok(userResponse);
    }

    @Override
    public ResponseEntity<Void> deleteUserByID(String userId) {
        String authenticatedUserId = getCurrentUserId();
        if (authenticatedUserId == null) {
            throw new org.springframework.security.core.AuthenticationException("User not authenticated") {};
        }

        userService.findById(userId);

        if (!userId.equals(authenticatedUserId)) {
            log.warn("User {} attempted to delete user {}", authenticatedUserId, userId);
            throw new org.springframework.security.access.AccessDeniedException("Access denied to user data");
        }

        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
