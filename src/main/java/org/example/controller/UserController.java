package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.api.UserApi;
import org.example.model.*;
import org.example.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * User Controller implementing the generated UserApi interface
 * Handles user management operations with proper authentication and authorization
 */
@RestController
@RequestMapping("/v1/users")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
@Slf4j
public class UserController implements UserApi {

    private final UserService userService;

    @Override
    public ResponseEntity<UserResponse> createUser(@Valid CreateUserRequest createUserRequest) {
        log.info("Creating user with email: {}", createUserRequest.getEmail());

        UserResponse userResponse = userService.createUser(createUserRequest);
        return new ResponseEntity<>(userResponse, HttpStatus.CREATED);
        // Note: EmailAlreadyExistsException is now handled by GlobalExceptionHandler
    }

    @Override
    public ResponseEntity<UserResponse> fetchUserByID(String userId) {
        String authenticatedUserId = getCurrentUserId();
        if (authenticatedUserId == null) {
            throw new org.springframework.security.core.AuthenticationException("User not authenticated") {};
        }

        // Check if user is trying to access their own data
        if (!userId.equals(authenticatedUserId)) {
            log.warn("User {} attempted to access data for user {}", authenticatedUserId, userId);
            throw new org.springframework.security.access.AccessDeniedException("Access denied to user data");
        }

        UserResponse userResponse = userService.findById(userId);
        return ResponseEntity.ok(userResponse);
        // Note: UserNotFoundException is now handled by GlobalExceptionHandler
    }

    @Override
    public ResponseEntity<UserResponse> updateUserByID(String userId, @Valid UpdateUserRequest updateUserRequest) {
        String authenticatedUserId = getCurrentUserId();
        if (authenticatedUserId == null) {
            throw new org.springframework.security.core.AuthenticationException("User not authenticated") {};
        }

        // Check if user is trying to update their own data
        if (!userId.equals(authenticatedUserId)) {
            log.warn("User {} attempted to update data for user {}", authenticatedUserId, userId);
            throw new org.springframework.security.access.AccessDeniedException("Access denied to user data");
        }

        UserResponse userResponse = userService.updateUser(userId, updateUserRequest);
        return ResponseEntity.ok(userResponse);
        // Note: UserNotFoundException and EmailAlreadyExistsException are handled by GlobalExceptionHandler
    }

    @Override
    public ResponseEntity<Void> deleteUserByID(String userId) {
        String authenticatedUserId = getCurrentUserId();
        if (authenticatedUserId == null) {
            throw new org.springframework.security.core.AuthenticationException("User not authenticated") {};
        }

        // Check if user is trying to delete their own data
        if (!userId.equals(authenticatedUserId)) {
            log.warn("User {} attempted to delete user {}", authenticatedUserId, userId);
            throw new org.springframework.security.access.AccessDeniedException("Access denied to user data");
        }

        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
        // Note: UserNotFoundException and UserHasAssociatedAccountsException are handled by GlobalExceptionHandler
    }

    /**
     * Helper method to get current authenticated user ID from JWT token
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal();
        }
        return null;
    }
}
