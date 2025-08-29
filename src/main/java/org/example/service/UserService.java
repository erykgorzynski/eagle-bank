package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.User;
import org.example.exception.UserHasAssociatedAccountsException;
import org.example.exception.UserNotFoundException;
import org.example.mapper.UserMapper;
import org.example.model.CreateUserRequest;
import org.example.model.UpdateUserRequest;
import org.example.model.UserResponse;
import org.example.repository.UserRepository;
import org.example.repository.AccountRepository;
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
    private final AccountRepository accountRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create a new user with password hashing
     */
    public UserResponse createUser(CreateUserRequest createUserRequest) {
        log.info("Creating user with email: {}", createUserRequest.getEmail());

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

        // Check if user has associated bank accounts
        boolean hasAssociatedAccounts = accountRepository.existsByUserId(userId);
        log.debug("User {} has associated accounts: {}", userId, hasAssociatedAccounts);

        if (hasAssociatedAccounts) {
            log.warn("Cannot delete user {} - has associated bank accounts", userId);
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
}
