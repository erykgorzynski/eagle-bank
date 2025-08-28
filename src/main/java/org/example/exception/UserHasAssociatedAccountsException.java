package org.example.exception;

/**
 * Exception thrown when attempting to delete a user who has associated bank accounts
 */
public class UserHasAssociatedAccountsException extends RuntimeException {

    public UserHasAssociatedAccountsException(String userId) {
        super("Cannot delete user " + userId + " because they have associated bank accounts");
    }
}
