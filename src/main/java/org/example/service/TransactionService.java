package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.Transaction;
import org.example.exception.AccountNotFoundException;
import org.example.exception.InsufficientFundsException;
import org.example.exception.TransactionNotFoundException;
import org.example.mapper.TransactionMapper;
import org.example.model.CreateTransactionRequest;
import org.example.model.ListTransactionsResponse;
import org.example.model.TransactionResponse;
import org.example.repository.TransactionRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service class for Transaction management operations
 * All operations include user ownership validation for security
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final AccountService accountService;

    /**
     * Create a new transaction with balance validation and account balance update
     */
    public TransactionResponse createTransaction(String accountNumber, CreateTransactionRequest createTransactionRequest, String userId) {
        log.info("Creating transaction for account {} by user {}", accountNumber, userId);

        // Validate account ownership and existence
        validateAccountAccess(accountNumber, userId);

        // Get current account balance
        double currentBalance = accountService.getAccountBalance(accountNumber, userId);

        // Validate sufficient funds for withdrawals
        if (CreateTransactionRequest.TypeEnum.WITHDRAWAL.equals(createTransactionRequest.getType())) {
            if (currentBalance < createTransactionRequest.getAmount()) {
                throw new InsufficientFundsException(accountNumber, createTransactionRequest.getAmount(), currentBalance);
            }
        }

        // Map request to entity
        Transaction transaction = transactionMapper.toEntity(createTransactionRequest);

        // Set system-generated fields
        transaction.setId(generateUniqueTransactionId());
        transaction.setAccountNumber(accountNumber);
        transaction.setUserId(userId);
        transaction.setCurrency(Transaction.Currency.GBP); // Only GBP supported

        // Calculate new balance based on transaction type
        double newBalance = calculateNewBalance(currentBalance, createTransactionRequest.getAmount(), createTransactionRequest.getType());

        // Save transaction and update account balance atomically
        Transaction savedTransaction = transactionRepository.save(transaction);
        accountService.updateAccountBalance(accountNumber, userId, newBalance);

        log.info("Successfully created transaction {} for account {} with new balance {}",
                savedTransaction.getId(), accountNumber, newBalance);

        return transactionMapper.toResponse(savedTransaction);
    }

    /**
     * List all transactions for a specific account with ownership validation
     */
    @Transactional(readOnly = true)
    public ListTransactionsResponse findByAccountNumber(String accountNumber, String userId) {
        log.info("Finding transactions for account {} by user {}", accountNumber, userId);

        // Validate account ownership
        validateAccountAccess(accountNumber, userId);

        // Get transactions ordered by newest first
        List<Transaction> transactions = transactionRepository.findByAccountNumberOrderByCreatedTimestampDesc(accountNumber);
        List<TransactionResponse> transactionResponses = transactionMapper.toResponseList(transactions);

        ListTransactionsResponse response = new ListTransactionsResponse();
        response.setTransactions(transactionResponses);

        log.info("Found {} transactions for account {}", transactionResponses.size(), accountNumber);
        return response;
    }

    /**
     * Find transaction by ID and account number with ownership validation
     */
    @Transactional(readOnly = true)
    public TransactionResponse findByIdAndAccountNumber(String transactionId, String accountNumber, String userId) {
        log.info("Finding transaction {} for account {} by user {}", transactionId, accountNumber, userId);

        // Validate account ownership
        validateAccountAccess(accountNumber, userId);

        // Find transaction by ID and account number
        Transaction transaction = transactionRepository.findByIdAndAccountNumber(transactionId, accountNumber)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId, accountNumber));

        log.info("Found transaction {} for account {}", transactionId, accountNumber);
        return transactionMapper.toResponse(transaction);
    }

    /**
     * Check if a transaction exists on a specific account
     */
    @Transactional(readOnly = true)
    public boolean existsByIdAndAccountNumber(String transactionId, String accountNumber) {
        return transactionRepository.existsByIdAndAccountNumber(transactionId, accountNumber);
    }

    /**
     * Generate unique transaction ID with tan- prefix
     */
    private String generateUniqueTransactionId() {
        String transactionId;
        int attempts = 0;
        final int maxAttempts = 100;

        do {
            // Generate UUID-based ID with tan- prefix
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            transactionId = "tan-" + uuid.substring(0, 12); // Use first 12 characters for shorter IDs
            attempts++;

            if (attempts >= maxAttempts) {
                // Fallback to timestamp-based generation if UUID fails
                transactionId = "tan-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
                break;
            }
        } while (transactionRepository.existsById(transactionId));

        log.debug("Generated unique transaction ID: {} in {} attempts", transactionId, attempts);
        return transactionId;
    }

    /**
     * Calculate new account balance based on transaction type
     */
    private double calculateNewBalance(double currentBalance, double transactionAmount, CreateTransactionRequest.TypeEnum transactionType) {
        return switch (transactionType) {
            case DEPOSIT -> currentBalance + transactionAmount;
            case WITHDRAWAL -> currentBalance - transactionAmount;
        };
    }

    /**
     * Validate that the user has access to the specified account
     */
    private void validateAccountAccess(String accountNumber, String userId) {
        // First check if account exists
        if (!accountService.accountExists(accountNumber)) {
            log.warn("Account {} does not exist", accountNumber);
            throw new AccountNotFoundException(accountNumber);
        }

        // Then check ownership
        if (!accountService.isAccountOwnedByUser(accountNumber, userId)) {
            log.warn("User {} attempted to access account {} which they don't own", userId, accountNumber);
            throw new AccessDeniedException("Access denied to account");
        }
    }
}
