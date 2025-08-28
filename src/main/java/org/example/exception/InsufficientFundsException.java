package org.example.exception;

/**
 * Exception thrown when a withdrawal transaction cannot be processed due to insufficient funds
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException(String accountNumber, double requestedAmount, double availableBalance) {
        super(String.format("Insufficient funds in account %s. Requested: %.2f, Available: %.2f",
            accountNumber, requestedAmount, availableBalance));
    }
}

