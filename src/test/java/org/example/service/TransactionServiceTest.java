package org.example.service;

import org.example.entity.Account;
import org.example.entity.Transaction;
import org.example.exception.InsufficientFundsException;
import org.example.exception.TransactionNotFoundException;
import org.example.mapper.TransactionMapper;
import org.example.model.CreateTransactionRequest;
import org.example.model.ListTransactionsResponse;
import org.example.model.TransactionResponse;
import org.example.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private TransactionService transactionService;

    private CreateTransactionRequest createDepositRequest;
    private CreateTransactionRequest createWithdrawalRequest;
    private Transaction savedTransaction;
    private TransactionResponse transactionResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
        createDepositRequest = new CreateTransactionRequest();
        createDepositRequest.setAmount(100.0);
        createDepositRequest.setType(CreateTransactionRequest.TypeEnum.DEPOSIT);
        createDepositRequest.setCurrency(CreateTransactionRequest.CurrencyEnum.GBP);
        createDepositRequest.setReference("Test deposit");

        createWithdrawalRequest = new CreateTransactionRequest();
        createWithdrawalRequest.setAmount(50.0);
        createWithdrawalRequest.setType(CreateTransactionRequest.TypeEnum.WITHDRAWAL);
        createWithdrawalRequest.setCurrency(CreateTransactionRequest.CurrencyEnum.GBP);
        createWithdrawalRequest.setReference("Test withdrawal");

        savedTransaction = new Transaction();
        savedTransaction.setId("tan-test123");
        savedTransaction.setAccountNumber("01123456");
        savedTransaction.setAmount(100.0);
        savedTransaction.setType(Transaction.TransactionType.DEPOSIT);
        savedTransaction.setCurrency(Transaction.Currency.GBP);
        savedTransaction.setReference("Test deposit");
        savedTransaction.setUserId("usr-test123");
        savedTransaction.setCreatedTimestamp(LocalDateTime.now());

        transactionResponse = new TransactionResponse();
        transactionResponse.setId("tan-test123");
        transactionResponse.setAmount(100.0);
        transactionResponse.setType(TransactionResponse.TypeEnum.DEPOSIT);
        transactionResponse.setCurrency(TransactionResponse.CurrencyEnum.GBP);
        transactionResponse.setReference("Test deposit");
        transactionResponse.setUserId("usr-test123");
        transactionResponse.setCreatedTimestamp(LocalDateTime.now());
    }

    @Test
    void createTransaction_Deposit_Success() {
        // Given
        String accountNumber = "01123456";
        String userId = "usr-test123";
        double currentBalance = 500.0;

        when(accountService.isAccountOwnedByUser(accountNumber, userId)).thenReturn(true);
        when(accountService.getAccountBalance(accountNumber, userId)).thenReturn(currentBalance);
        when(transactionMapper.toEntity(createDepositRequest)).thenReturn(new Transaction());
        when(transactionRepository.existsById(anyString())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(transactionMapper.toResponse(savedTransaction)).thenReturn(transactionResponse);

        // When
        TransactionResponse result = transactionService.createTransaction(accountNumber, createDepositRequest, userId);

        // Then
        assertNotNull(result);
        assertEquals("tan-test123", result.getId());
        assertEquals(100.0, result.getAmount());
        verify(accountService).updateAccountBalance(accountNumber, userId, 600.0); // 500 + 100
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createTransaction_Withdrawal_InsufficientFunds() {
        // Given
        String accountNumber = "01123456";
        String userId = "usr-test123";
        double currentBalance = 25.0; // Less than withdrawal amount

        when(accountService.isAccountOwnedByUser(accountNumber, userId)).thenReturn(true);
        when(accountService.getAccountBalance(accountNumber, userId)).thenReturn(currentBalance);

        // When & Then
        assertThrows(InsufficientFundsException.class, () ->
            transactionService.createTransaction(accountNumber, createWithdrawalRequest, userId)
        );

        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(accountService, never()).updateAccountBalance(anyString(), anyString(), anyDouble());
    }

    @Test
    void createTransaction_AccessDenied() {
        // Given
        String accountNumber = "01123456";
        String userId = "usr-test123";

        when(accountService.isAccountOwnedByUser(accountNumber, userId)).thenReturn(false);

        // When & Then
        assertThrows(AccessDeniedException.class, () ->
            transactionService.createTransaction(accountNumber, createDepositRequest, userId)
        );

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void findByAccountNumber_Success() {
        // Given
        String accountNumber = "01123456";
        String userId = "usr-test123";
        List<Transaction> transactions = Arrays.asList(savedTransaction);

        when(accountService.isAccountOwnedByUser(accountNumber, userId)).thenReturn(true);
        when(transactionRepository.findByAccountNumberOrderByCreatedTimestampDesc(accountNumber))
            .thenReturn(transactions);
        when(transactionMapper.toResponseList(transactions))
            .thenReturn(Arrays.asList(transactionResponse));

        // When
        ListTransactionsResponse result = transactionService.findByAccountNumber(accountNumber, userId);

        // Then
        assertNotNull(result);
        assertNotNull(result.getTransactions());
        assertEquals(1, result.getTransactions().size());
        assertEquals("tan-test123", result.getTransactions().get(0).getId());
    }

    @Test
    void findByIdAndAccountNumber_Success() {
        // Given
        String transactionId = "tan-test123";
        String accountNumber = "01123456";
        String userId = "usr-test123";

        when(accountService.isAccountOwnedByUser(accountNumber, userId)).thenReturn(true);
        when(transactionRepository.findByIdAndAccountNumber(transactionId, accountNumber))
            .thenReturn(Optional.of(savedTransaction));
        when(transactionMapper.toResponse(savedTransaction)).thenReturn(transactionResponse);

        // When
        TransactionResponse result = transactionService.findByIdAndAccountNumber(transactionId, accountNumber, userId);

        // Then
        assertNotNull(result);
        assertEquals("tan-test123", result.getId());
    }

    @Test
    void findByIdAndAccountNumber_NotFound() {
        // Given
        String transactionId = "tan-notfound";
        String accountNumber = "01123456";
        String userId = "usr-test123";

        when(accountService.isAccountOwnedByUser(accountNumber, userId)).thenReturn(true);
        when(transactionRepository.findByIdAndAccountNumber(transactionId, accountNumber))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(TransactionNotFoundException.class, () ->
            transactionService.findByIdAndAccountNumber(transactionId, accountNumber, userId)
        );
    }
}
