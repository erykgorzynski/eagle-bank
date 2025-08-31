package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.api.AccountApi;
import org.example.api.TransactionApi;
import org.example.model.*;
import org.example.service.AccountService;
import org.example.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



/**
 * Account Controller implementing both AccountApi and TransactionApi interfaces
 * Handles account and transaction management with proper authentication and authorization
 */
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
@Slf4j
public class AccountController extends BaseController implements AccountApi, TransactionApi {

    private final AccountService accountService;
    private final TransactionService transactionService;

    // ============= ACCOUNT OPERATIONS =============

    @Override
    public ResponseEntity<BankAccountResponse> createAccount(CreateBankAccountRequest createBankAccountRequest) {
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
            UpdateBankAccountRequest updateBankAccountRequest) {
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
            CreateTransactionRequest createTransactionRequest) {
        String authenticatedUserId = getCurrentUserId();
        if (authenticatedUserId == null) {
            throw new org.springframework.security.core.AuthenticationException("User not authenticated") {};
        }

        TransactionResponse transaction = transactionService.createTransaction(
            accountNumber, createTransactionRequest, authenticatedUserId
        );

        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<ListTransactionsResponse> listAccountTransaction(String accountNumber) {
        String authenticatedUserId = getCurrentUserId();
        if (authenticatedUserId == null) {
            throw new org.springframework.security.core.AuthenticationException("User not authenticated") {};
        }

        ListTransactionsResponse response = transactionService.findByAccountNumber(accountNumber, authenticatedUserId);

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

        TransactionResponse transaction = transactionService.findByIdAndAccountNumber(
            transactionId, accountNumber, authenticatedUserId
        );

        return ResponseEntity.ok(transaction);
    }
}
