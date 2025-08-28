package org.example.exception;

/**
 * Exception thrown when an account with the specified accountNumber is not found
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String accountNumber) {
        super("Account not found with account number: " + accountNumber);
    }
}
