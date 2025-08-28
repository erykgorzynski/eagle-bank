package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.User;
import org.example.exception.EmailAlreadyExistsException;
import org.example.exception.UserHasAssociatedAccountsException;
import org.example.exception.UserNotFoundException;
import org.example.mapper.UserMapper;
import org.example.model.CreateUserRequest;
import org.example.model.UpdateUserRequest;
import org.example.model.UserResponse;
import org.example.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Service class for User management operations
 * All operations are userId-centric except authentication
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create a new user with unique email validation and password hashing
     */
    public UserResponse createUser(CreateUserRequest createUserRequest) {
        log.info("Creating user with email: {}", createUserRequest.getEmail());

        // Check email uniqueness
        if (userRepository.existsByEmail(createUserRequest.getEmail())) {
            throw new EmailAlreadyExistsException(createUserRequest.getEmail());
        }

        // Map request to entity
        User user = userMapper.toEntity(createUserRequest);

        // Generate userId with usr- prefix
        user.setId(generateUserId());

        // Hash password
        user.setPasswordHash(passwordEncoder.encode(createUserRequest.getPassword()));

        // Save user
        User savedUser = userRepository.save(user);

        log.info("Successfully created user with ID: {}", savedUser.getId());
        return userMapper.toResponse(savedUser);
    }

    /**
     * Find user by userId (primary operation)
     */
    @Transactional(readOnly = true)
    public UserResponse findById(String userId) {
        log.info("Finding user by ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return userMapper.toResponse(user);
    }

    /**
     * Update user by userId
     */
    public UserResponse updateUser(String userId, UpdateUserRequest updateUserRequest) {
        log.info("Updating user with ID: {}", userId);

        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Check email uniqueness if email is being updated
        if (updateUserRequest.getEmail() != null &&
            !updateUserRequest.getEmail().equals(existingUser.getEmail()) &&
            userRepository.existsByEmail(updateUserRequest.getEmail())) {
            throw new EmailAlreadyExistsException(updateUserRequest.getEmail());
        }

        // Update entity with non-null values from request
        userMapper.updateEntityFromRequest(updateUserRequest, existingUser);

        User updatedUser = userRepository.save(existingUser);

        log.info("Successfully updated user with ID: {}", userId);
        return userMapper.toResponse(updatedUser);
    }

    /**
     * Delete user by userId
     */
    public void deleteUser(String userId) {
        log.info("Deleting user with ID: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        // TODO: Check if user has associated bank accounts
        // This will be implemented when account management is integrated
        // For now, we'll assume no accounts exist
        boolean hasAssociatedAccounts = false; // accountService.hasAccountsForUser(userId);

        if (hasAssociatedAccounts) {
            throw new UserHasAssociatedAccountsException(userId);
        }

        userRepository.deleteById(userId);
        log.info("Successfully deleted user with ID: {}", userId);
    }

    /**
     * Find user by email (for authentication only)
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        log.debug("Finding user by email for authentication: {}", email);
        return userRepository.findByEmail(email);
    }

    /**
     * Generate unique userId with usr- prefix
     */
    private String generateUserId() {
        return "usr-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    /**
     * Check if user exists by userId
     */
    @Transactional(readOnly = true)
    public boolean existsById(String userId) {
        return userRepository.existsById(userId);
    }
}
