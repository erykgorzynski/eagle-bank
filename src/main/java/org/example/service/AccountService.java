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

    public BankAccountResponse createAccount(String userId, CreateBankAccountRequest createBankAccountRequest) {
        log.info("Creating account for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Account account = accountMapper.toEntity(createBankAccountRequest);

        account.setAccountNumber(generateUniqueAccountNumber());
        account.setBalance(0.0);
        account.setCurrency(Account.Currency.GBP);
        account.setSortCode(Account.SortCode._10_10_10);

        user.addAccount(account);

        User savedUser = userRepository.save(user);

        Account savedAccount = savedUser.getAccounts().stream()
                .filter(acc -> acc.getAccountNumber().equals(account.getAccountNumber()))
                .findFirst()
                .orElse(account);

        log.info("Successfully created account {} for user {}", savedAccount.getAccountNumber(), userId);
        return accountMapper.toResponse(savedAccount);
    }

    @Transactional(readOnly = true)
    public BankAccountResponse findByAccountNumber(String accountNumber, String userId) {
        log.info("Finding account {} for user {}", accountNumber, userId);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        validateAccountOwnership(account, userId);

        return accountMapper.toResponse(account);
    }

    @Transactional(readOnly = true)
    public ListBankAccountsResponse findAccountsByUserId(String userId) {
        log.info("Finding all accounts for user: {}", userId);

        List<Account> accounts = accountRepository.findByUser_Id(userId);
        List<BankAccountResponse> accountResponses = accountMapper.toResponseList(accounts);

        ListBankAccountsResponse response = new ListBankAccountsResponse();
        response.setAccounts(accountResponses);

        log.info("Found {} accounts for user {}", accountResponses.size(), userId);
        return response;
    }

    public BankAccountResponse updateAccount(String accountNumber, String userId, UpdateBankAccountRequest updateBankAccountRequest) {
        log.info("Updating account {} for user {}", accountNumber, userId);

        Account existingAccount = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        validateAccountOwnership(existingAccount, userId);

        accountMapper.updateEntityFromRequest(updateBankAccountRequest, existingAccount);

        Account updatedAccount = accountRepository.save(existingAccount);

        log.info("Successfully updated account {} for user {}", accountNumber, userId);
        return accountMapper.toResponse(updatedAccount);
    }

    public void deleteAccount(String accountNumber, String userId) {
        log.info("Deleting account {} for user {}", accountNumber, userId);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        validateAccountOwnership(account, userId);

        if (!account.getTransactions().isEmpty()) {
            log.warn("Cannot delete account {} as it has {} transactions", accountNumber, account.getTransactions().size());
            throw new IllegalStateException("Cannot delete account with existing transactions");
        }

        if (account.getBalance() != 0.0) {
            log.warn("Cannot delete account {} as it has non-zero balance: {}", accountNumber, account.getBalance());
            throw new IllegalStateException("Cannot delete account with non-zero balance");
        }

        User user = account.getUser();
        user.removeAccount(account);

        userRepository.save(user);

        log.info("Successfully deleted account {} for user {}", accountNumber, userId);
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        int attempts = 0;
        final int maxAttempts = 100;

        do {
            int randomNumber = (int) (Math.random() * 1000000);
            accountNumber = String.format("01%06d", randomNumber);
            attempts++;

            if (attempts >= maxAttempts) {
                String uuid = UUID.randomUUID().toString().replaceAll("-", "");
                accountNumber = "01" + uuid.substring(0, 6).toUpperCase();
                break;
            }
        } while (accountRepository.existsByAccountNumber(accountNumber));

        log.debug("Generated unique account number: {} in {} attempts", accountNumber, attempts);
        return accountNumber;
    }

    private void validateAccountOwnership(Account account, String userId) {
        if (!account.getUser().getId().equals(userId)) {
            log.warn("User {} attempted to access account {} owned by user {}",
                     userId, account.getAccountNumber(), account.getUser().getId());
            throw new AccessDeniedException("Access denied to account");
        }
    }
}
