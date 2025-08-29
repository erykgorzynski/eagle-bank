package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.Account;
import org.example.entity.User;
import org.example.exception.AccountNotFoundException;
import org.example.exception.UserNotFoundException;
import org.example.mapper.AccountMapper;
import org.example.model.BankAccountResponse;
import org.example.model.CreateBankAccountRequest;
import org.example.model.ListBankAccountsResponse;
import org.example.model.UpdateBankAccountRequest;
import org.example.repository.AccountRepository;
import org.example.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service class for Account management operations
 * Updated to use JPA relationships instead of manual foreign key management
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;

    /**
     * Create a new bank account for the authenticated user
     */
    public BankAccountResponse createAccount(String userId, CreateBankAccountRequest createBankAccountRequest) {
        log.info("Creating account for user: {}", userId);

        // Find the user entity (required for JPA relationship)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Map request to entity
        Account account = accountMapper.toEntity(createBankAccountRequest);

        // Set system-generated fields
        account.setAccountNumber(generateUniqueAccountNumber());
        account.setBalance(0.0); // New accounts start with zero balance
        account.setCurrency(Account.Currency.GBP);
        account.setSortCode(Account.SortCode._10_10_10);

        // Use the bidirectional relationship helper method instead of manual setting
        user.addAccount(account); // This properly sets both sides of the relationship

        // Save user (cascade will save the account as well due to CascadeType.ALL)
        User savedUser = userRepository.save(user);

        // Get the saved account from the user's accounts list
        Account savedAccount = savedUser.getAccounts().stream()
                .filter(acc -> acc.getAccountNumber().equals(account.getAccountNumber()))
                .findFirst()
                .orElse(account);

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

        // Verify ownership using JPA relationship
        validateAccountOwnership(account, userId);

        return accountMapper.toResponse(account);
    }

    /**
     * List all accounts for a specific user using JPA relationship
     */
    @Transactional(readOnly = true)
    public ListBankAccountsResponse findAccountsByUserId(String userId) {
        log.info("Finding all accounts for user: {}", userId);

        // Use JPA relationship query
        List<Account> accounts = accountRepository.findByUser_Id(userId);
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

        // Verify ownership using JPA relationship
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

        // Verify ownership using JPA relationship
        validateAccountOwnership(account, userId);

        // Check if account has transactions before deletion
        if (!account.getTransactions().isEmpty()) {
            log.warn("Cannot delete account {} as it has {} transactions", accountNumber, account.getTransactions().size());
            throw new IllegalStateException("Cannot delete account with existing transactions");
        }

        // Check if account balance is zero
        if (account.getBalance() != 0.0) {
            log.warn("Cannot delete account {} as it has non-zero balance: {}", accountNumber, account.getBalance());
            throw new IllegalStateException("Cannot delete account with non-zero balance");
        }

        // Use the bidirectional relationship helper method for proper cleanup
        User user = account.getUser();
        user.removeAccount(account); // This properly removes both sides of the relationship

        // Save user (cascade will handle the account deletion due to orphanRemoval = true)
        userRepository.save(user);

        log.info("Successfully deleted account {} for user {}", accountNumber, userId);
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
     * Validate that the account is owned by the specified user using JPA relationship
     */
    private void validateAccountOwnership(Account account, String userId) {
        if (!account.getUser().getId().equals(userId)) {
            log.warn("User {} attempted to access account {} owned by user {}",
                     userId, account.getAccountNumber(), account.getUser().getId());
            throw new AccessDeniedException("Access denied to account");
        }
    }
}
