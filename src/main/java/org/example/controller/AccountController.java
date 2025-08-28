package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.api.AccountApi;
import org.example.api.TransactionApi;
import org.example.model.*;
import org.example.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Account Controller implementing both AccountApi and TransactionApi interfaces
 * Handles account and transaction management with proper authentication and authorization
 */
@RestController
@RequestMapping("/v1/accounts")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
@Slf4j
public class AccountController implements AccountApi, TransactionApi {

    private final AccountService accountService;

    // ============= ACCOUNT OPERATIONS =============

    @Override
    public ResponseEntity<BankAccountResponse> createAccount(@Valid CreateBankAccountRequest createBankAccountRequest) {
        String authenticatedUserId = getCurrentUserId();
        if (authenticatedUserId == null) {
            throw new org.springframework.security.core.AuthenticationException("User not authenticated") {};
        }

        BankAccountResponse account = accountService.createAccount(authenticatedUserId, createBankAccountRequest);
        return new ResponseEntity<>(account, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Void> deleteAccountByAccountNumber(String accountNumber) {
        String authenticatedUserId = getCurrentUserId();
        if (authenticatedUserId == null) {
            throw new org.springframework.security.core.AuthenticationException("User not authenticated") {};
        }

        accountService.deleteAccount(accountNumber, authenticatedUserId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<BankAccountResponse> fetchAccountByAccountNumber(String accountNumber) {
        String authenticatedUserId = getCurrentUserId();
        if (authenticatedUserId == null) {
            throw new org.springframework.security.core.AuthenticationException("User not authenticated") {};
        }

        BankAccountResponse account = accountService.findByAccountNumber(accountNumber, authenticatedUserId);
        return ResponseEntity.ok(account);
    }

    @Override
    public ResponseEntity<ListBankAccountsResponse> listAccounts() {
        String authenticatedUserId = getCurrentUserId();
        if (authenticatedUserId == null) {
            throw new org.springframework.security.core.AuthenticationException("User not authenticated") {};
        }

        ListBankAccountsResponse response = accountService.findAccountsByUserId(authenticatedUserId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<BankAccountResponse> updateAccountByAccountNumber(
            String accountNumber,
            @Valid UpdateBankAccountRequest updateBankAccountRequest) {
        String authenticatedUserId = getCurrentUserId();
        if (authenticatedUserId == null) {
            throw new org.springframework.security.core.AuthenticationException("User not authenticated") {};
        }

        BankAccountResponse account = accountService.updateAccount(accountNumber, authenticatedUserId, updateBankAccountRequest);
        return ResponseEntity.ok(account);
    }

    // ============= TRANSACTION OPERATIONS =============

    @Override
    public ResponseEntity<TransactionResponse> createTransaction(
            String accountNumber,
            @Valid CreateTransactionRequest createTransactionRequest) {
        String authenticatedUserId = getCurrentUserId();
        if (authenticatedUserId == null) {
            throw new org.springframework.security.core.AuthenticationException("User not authenticated") {};
        }

        // TODO: Verify account ownership
        if (!isAccountOwnedByUser(accountNumber, authenticatedUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied to account");
        }

        // TODO: Validate account exists and handle insufficient funds
        // BankAccountResponse account = accountService.findByAccountNumber(accountNumber);
        // if (account == null) {
        //     throw new AccountNotFoundException(accountNumber);
        // }
        //
        // if (CreateTransactionRequest.TypeEnum.WITHDRAWAL.equals(createTransactionRequest.getType())) {
        //     double currentBalance = account.getBalance();
        //     if (currentBalance < createTransactionRequest.getAmount()) {
        //         throw new InsufficientFundsException("Insufficient funds for withdrawal");
        //     }
        // }

        // TODO: Call transaction service
        // TransactionResponse transaction = transactionService.createTransaction(
        //     accountNumber, createTransactionRequest, authenticatedUserId
        // );

        // Example response - replace with actual implementation
        TransactionResponse response = new TransactionResponse();
        response.setId("tan-" + System.currentTimeMillis());
        response.setAmount(createTransactionRequest.getAmount());
        response.setCurrency(TransactionResponse.CurrencyEnum.GBP);
        response.setType(TransactionResponse.TypeEnum.valueOf(createTransactionRequest.getType().name()));
        response.setReference(createTransactionRequest.getReference());
        response.setUserId(authenticatedUserId);
        response.setCreatedTimestamp(LocalDateTime.now());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<ListTransactionsResponse> listAccountTransaction(String accountNumber) {
        String authenticatedUserId = getCurrentUserId();
        if (authenticatedUserId == null) {
            throw new org.springframework.security.core.AuthenticationException("User not authenticated") {};
        }

        // TODO: Verify account ownership
        if (!isAccountOwnedByUser(accountNumber, authenticatedUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied to account");
        }

        // TODO: Call transaction service
        // List<TransactionResponse> transactions = transactionService.findByAccountNumber(accountNumber);

        // Example response - replace with actual implementation
        ListTransactionsResponse response = new ListTransactionsResponse();
        response.setTransactions(new ArrayList<>());

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<TransactionResponse> fetchAccountTransactionByID(
            String accountNumber,
            String transactionId) {
        String authenticatedUserId = getCurrentUserId();
        if (authenticatedUserId == null) {
            throw new org.springframework.security.core.AuthenticationException("User not authenticated") {};
        }

        // TODO: Verify account ownership
        if (!isAccountOwnedByUser(accountNumber, authenticatedUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied to account");
        }

        // TODO: Call transaction service and validate transaction exists
        // TransactionResponse transaction = transactionService.findByIdAndAccountNumber(
        //     transactionId, accountNumber
        // );
        // if (transaction == null) {
        //     throw new TransactionNotFoundException(transactionId);
        // }

        // Example response - replace with actual implementation
        TransactionResponse response = new TransactionResponse();
        response.setId(transactionId);
        response.setAmount(100.0);
        response.setCurrency(TransactionResponse.CurrencyEnum.GBP);
        response.setType(TransactionResponse.TypeEnum.DEPOSIT);
        response.setReference("Example transaction");
        response.setUserId(authenticatedUserId);
        response.setCreatedTimestamp(LocalDateTime.now().minusHours(1));

        return ResponseEntity.ok(response);
    }

    // ============= HELPER METHODS =============

    /**
     * Helper method to get current authenticated user ID from JWT token
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * Helper method to check if an account is owned by the specified user
     * Now uses actual AccountService for ownership verification
     */
    private boolean isAccountOwnedByUser(String accountNumber, String userId) {
        return accountService.isAccountOwnedByUser(accountNumber, userId);
    }
}
