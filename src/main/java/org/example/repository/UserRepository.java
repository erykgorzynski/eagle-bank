package org.example.repository;

import org.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity operations
 * Primary key operations use userId (String) as the identifier
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Find user by email address (used for authentication/login)
     * @param email the email address
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user exists with the given email (for uniqueness validation)
     * @param email the email address
     * @return true if user exists with this email
     */
    boolean existsByEmail(String email);

    // Note: findById(String userId) and existsById(String userId) are inherited from JpaRepository
}
