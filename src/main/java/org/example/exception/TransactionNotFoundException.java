package org.example.exception;

/**
 * Exception thrown when a transaction with the specified ID is not found
 */
public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(String transactionId) {
        super("Transaction not found with ID: " + transactionId);
    }

    public TransactionNotFoundException(String transactionId, String accountNumber) {
        super("Transaction with ID " + transactionId + " not found on account " + accountNumber);
    }
}
