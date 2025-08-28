package org.example.exception;

/**
 * Exception thrown when attempting to create a user with an email that already exists
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("User already exists with email: " + email);
    }
}
