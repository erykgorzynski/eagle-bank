package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.Account;
import org.example.entity.Transaction;
import org.example.exception.AccountNotFoundException;
import org.example.exception.InsufficientFundsException;
import org.example.exception.TransactionNotFoundException;
import org.example.mapper.TransactionMapper;
import org.example.model.CreateTransactionRequest;
import org.example.model.ListTransactionsResponse;
import org.example.model.TransactionResponse;
import org.example.repository.AccountRepository;
import org.example.repository.TransactionRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service class for Transaction management operations
 * Updated to use JPA relationships instead of manual foreign key management
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionMapper transactionMapper;

    public TransactionResponse createTransaction(String accountNumber, CreateTransactionRequest createTransactionRequest, String userId) {
        log.info("Creating transaction for account {} by user {}", accountNumber, userId);

        Account account = accountRepository.findByAccountNumberWithUser(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        validateAccountAccess(account, userId);

        double currentBalance = account.getBalance();

        if (CreateTransactionRequest.TypeEnum.WITHDRAWAL.equals(createTransactionRequest.getType())) {
            if (currentBalance < createTransactionRequest.getAmount()) {
                throw new InsufficientFundsException(accountNumber, createTransactionRequest.getAmount(), currentBalance);
            }
        }

        Transaction transaction = transactionMapper.toEntity(createTransactionRequest);

        transaction.setId(generateUniqueTransactionId());
        transaction.setCurrency(Transaction.Currency.GBP);

        account.addTransaction(transaction);

        double newBalance = calculateNewBalance(currentBalance, createTransactionRequest.getAmount(), createTransactionRequest.getType());

        account.setBalance(newBalance);

        Account savedAccount = accountRepository.save(account);

        Transaction savedTransaction = savedAccount.getTransactions().stream()
                .filter(t -> t.getId().equals(transaction.getId()))
                .findFirst()
                .orElse(transaction);

        log.info("Successfully created transaction {} for account {} with new balance {}",
                savedTransaction.getId(), accountNumber, newBalance);

        return transactionMapper.toResponse(savedTransaction);
    }

    @Transactional(readOnly = true)
    public ListTransactionsResponse findByAccountNumber(String accountNumber, String userId) {
        log.info("Finding transactions for account {} by user {}", accountNumber, userId);

        Account account = accountRepository.findByAccountNumberWithUser(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        validateAccountAccess(account, userId);

        List<Transaction> transactions = transactionRepository.findByAccount_AccountNumberOrderByCreatedTimestampDesc(accountNumber);
        List<TransactionResponse> transactionResponses = transactionMapper.toResponseList(transactions);

        ListTransactionsResponse response = new ListTransactionsResponse();
        response.setTransactions(transactionResponses);

        log.info("Found {} transactions for account {}", transactionResponses.size(), accountNumber);
        return response;
    }

    @Transactional(readOnly = true)
    public TransactionResponse findByIdAndAccountNumber(String transactionId, String accountNumber, String userId) {
        log.info("Finding transaction {} for account {} by user {}", transactionId, accountNumber, userId);

        Account account = accountRepository.findByAccountNumberWithUser(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        validateAccountAccess(account, userId);

        Transaction transaction = transactionRepository.findByIdAndAccount_AccountNumber(transactionId, accountNumber)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId, accountNumber));

        log.info("Found transaction {} for account {}", transactionId, accountNumber);
        return transactionMapper.toResponse(transaction);
    }

    private String generateUniqueTransactionId() {
        String transactionId;
        int attempts = 0;
        final int maxAttempts = 100;

        do {
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            transactionId = "tan-" + uuid.substring(0, 12);
            attempts++;

            if (attempts >= maxAttempts) {
                transactionId = "tan-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
                break;
            }
        } while (transactionRepository.existsById(transactionId));

        log.debug("Generated unique transaction ID: {} in {} attempts", transactionId, attempts);
        return transactionId;
    }

    private double calculateNewBalance(double currentBalance, double transactionAmount, CreateTransactionRequest.TypeEnum transactionType) {
        return switch (transactionType) {
            case DEPOSIT -> currentBalance + transactionAmount;
            case WITHDRAWAL -> currentBalance - transactionAmount;
        };
    }

    private void validateAccountAccess(Account account, String userId) {
        if (!account.getUser().getId().equals(userId)) {
            log.warn("User {} attempted to access account {} owned by user {}",
                     userId, account.getAccountNumber(), account.getUser().getId());
            throw new AccessDeniedException("Access denied to account");
        }
    }
}
