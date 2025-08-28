package org.example.exception;

/**
 * Exception thrown when a user with the specified userId is not found
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String userId) {
        super("User not found with ID: " + userId);
    }
}
