package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.Account;
import org.example.exception.AccountNotFoundException;
import org.example.mapper.AccountMapper;
import org.example.model.BankAccountResponse;
import org.example.model.CreateBankAccountRequest;
import org.example.model.ListBankAccountsResponse;
import org.example.model.UpdateBankAccountRequest;
import org.example.repository.AccountRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service class for Account management operations
 * All operations include user ownership validation for security
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    /**
     * Create a new bank account for the authenticated user
     */
    public BankAccountResponse createAccount(String userId, CreateBankAccountRequest createBankAccountRequest) {
        log.info("Creating account for user: {}", userId);

        // Map request to entity
        Account account = accountMapper.toEntity(createBankAccountRequest);

        // Set system-generated fields
        account.setAccountNumber(generateUniqueAccountNumber());
        account.setUserId(userId);
        account.setBalance(0.0); // New accounts start with zero balance
        account.setCurrency(Account.Currency.GBP);
        account.setSortCode(Account.SortCode._10_10_10);

        // Save account
        Account savedAccount = accountRepository.save(account);

        log.info("Successfully created account {} for user {}", savedAccount.getAccountNumber(), userId);
        return accountMapper.toResponse(savedAccount);
    }

    /**
     * Find account by account number with ownership validation
     */
    @Transactional(readOnly = true)
    public BankAccountResponse findByAccountNumber(String accountNumber, String userId) {
        log.info("Finding account {} for user {}", accountNumber, userId);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        // Verify ownership
        validateAccountOwnership(account, userId);

        return accountMapper.toResponse(account);
    }

    /**
     * List all accounts for a specific user
     */
    @Transactional(readOnly = true)
    public ListBankAccountsResponse findAccountsByUserId(String userId) {
        log.info("Finding all accounts for user: {}", userId);

        List<Account> accounts = accountRepository.findByUserId(userId);
        List<BankAccountResponse> accountResponses = accountMapper.toResponseList(accounts);

        ListBankAccountsResponse response = new ListBankAccountsResponse();
        response.setAccounts(accountResponses);

        log.info("Found {} accounts for user {}", accountResponses.size(), userId);
        return response;
    }

    /**
     * Update account by account number with ownership validation
     */
    public BankAccountResponse updateAccount(String accountNumber, String userId, UpdateBankAccountRequest updateBankAccountRequest) {
        log.info("Updating account {} for user {}", accountNumber, userId);

        Account existingAccount = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        // Verify ownership
        validateAccountOwnership(existingAccount, userId);

        // Update entity with non-null values from request
        accountMapper.updateEntityFromRequest(updateBankAccountRequest, existingAccount);

        Account updatedAccount = accountRepository.save(existingAccount);

        log.info("Successfully updated account {} for user {}", accountNumber, userId);
        return accountMapper.toResponse(updatedAccount);
    }

    /**
     * Delete account by account number with ownership validation
     */
    public void deleteAccount(String accountNumber, String userId) {
        log.info("Deleting account {} for user {}", accountNumber, userId);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        // Verify ownership
        validateAccountOwnership(account, userId);

        // TODO: Add business rules for account deletion
        // - Check if account has transactions
        // - Ensure account balance is zero
        // For now, we'll allow deletion

        accountRepository.delete(account);
        log.info("Successfully deleted account {} for user {}", accountNumber, userId);
    }

    /**
     * Check if an account is owned by a specific user
     */
    @Transactional(readOnly = true)
    public boolean isAccountOwnedByUser(String accountNumber, String userId) {
        return accountRepository.findByAccountNumber(accountNumber)
                .map(account -> account.getUserId().equals(userId))
                .orElse(false);
    }

    /**
     * Check if a user has any associated accounts (used for user deletion validation)
     */
    @Transactional(readOnly = true)
    public boolean hasAccountsForUser(String userId) {
        return accountRepository.existsByUserId(userId);
    }

    /**
     * Get account balance for transaction processing
     */
    @Transactional(readOnly = true)
    public double getAccountBalance(String accountNumber, String userId) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        validateAccountOwnership(account, userId);
        return account.getBalance();
    }

    /**
     * Update account balance (used by transaction service)
     */
    public void updateAccountBalance(String accountNumber, String userId, double newBalance) {
        log.info("Updating balance for account {} to {}", accountNumber, newBalance);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        validateAccountOwnership(account, userId);
        account.setBalance(newBalance);
        accountRepository.save(account);

        log.info("Successfully updated balance for account {}", accountNumber);
    }

    /**
     * Generate unique account number with format ^01\d{6}$
     */
    private String generateUniqueAccountNumber() {
        String accountNumber;
        int attempts = 0;
        final int maxAttempts = 100;

        do {
            // Generate 6-digit number after "01" prefix
            int randomNumber = (int) (Math.random() * 1000000);
            accountNumber = String.format("01%06d", randomNumber);
            attempts++;

            if (attempts >= maxAttempts) {
                // Fallback to UUID-based generation if random fails
                String uuid = UUID.randomUUID().toString().replaceAll("-", "");
                accountNumber = "01" + uuid.substring(0, 6).toUpperCase();
                break;
            }
        } while (accountRepository.existsByAccountNumber(accountNumber));

        log.debug("Generated unique account number: {} in {} attempts", accountNumber, attempts);
        return accountNumber;
    }

    /**
     * Validate that the account is owned by the specified user
     */
    private void validateAccountOwnership(Account account, String userId) {
        if (!account.getUserId().equals(userId)) {
            log.warn("User {} attempted to access account {} owned by user {}",
                     userId, account.getAccountNumber(), account.getUserId());
            throw new AccessDeniedException("Access denied to account");
        }
    }
}
