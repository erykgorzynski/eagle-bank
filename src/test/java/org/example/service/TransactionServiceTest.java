package org.example.service;

import org.example.entity.Account;
import org.example.entity.Transaction;
import org.example.entity.User;
import org.example.exception.AccountNotFoundException;
import org.example.exception.InsufficientFundsException;
import org.example.exception.TransactionNotFoundException;
import org.example.mapper.TransactionMapper;
import org.example.model.CreateTransactionRequest;
import org.example.model.ListTransactionsResponse;
import org.example.model.TransactionResponse;
import org.example.repository.AccountRepository;
import org.example.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionService transactionService;

    private User user;
    private Account account;
    private Transaction transaction;
    private CreateTransactionRequest createTransactionRequest;
    private TransactionResponse transactionResponse;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId("usr-1234567890");
        user.setName("John Doe");
        user.setEmail("john.doe@example.com");

        account = new Account();
        account.setAccountNumber("01234567");
        account.setName("Savings Account");
        account.setBalance(1000.00);
        account.setCurrency(Account.Currency.GBP);
        account.setUser(user);
        account.setTransactions(new ArrayList<>());
        account.setCreatedTimestamp(LocalDateTime.now());
        account.setUpdatedTimestamp(LocalDateTime.now());

        transaction = new Transaction();
        transaction.setId("tan-1234567890ab");
        transaction.setAmount(100.00);
        transaction.setCurrency(Transaction.Currency.GBP);
        transaction.setType(Transaction.TransactionType.DEPOSIT);
        transaction.setReference("Test transaction");
        transaction.setAccount(account);
        transaction.setCreatedTimestamp(LocalDateTime.now());

        createTransactionRequest = new CreateTransactionRequest()
                .amount(100.00)
                .currency(CreateTransactionRequest.CurrencyEnum.GBP)
                .type(CreateTransactionRequest.TypeEnum.DEPOSIT)
                .reference("Test transaction");

        transactionResponse = new TransactionResponse()
                .id("tan-1234567890ab")
                .amount(100.00)
                .currency(TransactionResponse.CurrencyEnum.GBP)
                .type(TransactionResponse.TypeEnum.DEPOSIT)
                .reference("Test transaction")
                .createdTimestamp(LocalDateTime.now());
    }

    // === CREATE TRANSACTION TESTS ===

    @Test
    void createDepositTransactionSuccessfully() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));
        when(transactionMapper.toEntity(createTransactionRequest)).thenReturn(transaction);
        when(transactionRepository.existsById(anyString())).thenReturn(false);
        when(accountRepository.save(account)).thenAnswer(invocation -> {
            Account savedAccount = invocation.getArgument(0);
            savedAccount.getTransactions().add(transaction);
            return savedAccount;
        });
        when(transactionMapper.toResponse(transaction)).thenReturn(transactionResponse);

        TransactionResponse result = transactionService.createTransaction(accountNumber, createTransactionRequest, userId);

        assertThat(result).isEqualTo(transactionResponse);
        assertThat(account.getBalance()).isEqualTo(1100.00);
        verify(accountRepository).findByAccountNumberWithUser(accountNumber);
        verify(transactionMapper).toEntity(createTransactionRequest);
        verify(accountRepository).save(account);
        verify(transactionMapper).toResponse(transaction);
    }

    @Test
    void createWithdrawalTransactionSuccessfully() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        CreateTransactionRequest withdrawalRequest = new CreateTransactionRequest()
                .amount(200.00)
                .currency(CreateTransactionRequest.CurrencyEnum.GBP)
                .type(CreateTransactionRequest.TypeEnum.WITHDRAWAL)
                .reference("Test withdrawal");

        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));
        when(transactionMapper.toEntity(withdrawalRequest)).thenReturn(transaction);
        when(transactionRepository.existsById(anyString())).thenReturn(false);
        when(accountRepository.save(account)).thenAnswer(invocation -> {
            Account savedAccount = invocation.getArgument(0);
            savedAccount.getTransactions().add(transaction);
            return savedAccount;
        });
        when(transactionMapper.toResponse(transaction)).thenReturn(transactionResponse);

        TransactionResponse result = transactionService.createTransaction(accountNumber, withdrawalRequest, userId);

        assertThat(result).isEqualTo(transactionResponse);
        assertThat(account.getBalance()).isEqualTo(800.00);
        verify(accountRepository).findByAccountNumberWithUser(accountNumber);
        verify(transactionMapper).toEntity(withdrawalRequest);
        verify(accountRepository).save(account);
        verify(transactionMapper).toResponse(transaction);
    }

    @Test
    void createTransactionThrowsAccountNotFoundExceptionWhenAccountDoesNotExist() {
        String accountNumber = "01999999";
        String userId = "usr-1234567890";
        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(accountNumber, createTransactionRequest, userId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Account not found with account number: 01999999");

        verify(accountRepository).findByAccountNumberWithUser(accountNumber);
        verify(transactionMapper, never()).toEntity(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void createTransactionThrowsAccessDeniedExceptionWhenUserDoesNotOwnAccount() {
        String accountNumber = "01234567";
        String otherUserId = "usr-0987654321";
        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> transactionService.createTransaction(accountNumber, createTransactionRequest, otherUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied to account");

        verify(accountRepository).findByAccountNumberWithUser(accountNumber);
        verify(transactionMapper, never()).toEntity(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void createWithdrawalTransactionThrowsInsufficientFundsExceptionWhenBalanceInsufficient() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        CreateTransactionRequest largeWithdrawalRequest = new CreateTransactionRequest()
                .amount(2000.00)
                .currency(CreateTransactionRequest.CurrencyEnum.GBP)
                .type(CreateTransactionRequest.TypeEnum.WITHDRAWAL)
                .reference("Large withdrawal");

        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> transactionService.createTransaction(accountNumber, largeWithdrawalRequest, userId))
                .isInstanceOf(InsufficientFundsException.class);

        verify(accountRepository).findByAccountNumberWithUser(accountNumber);
        verify(transactionMapper, never()).toEntity(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void createTransactionGeneratesUniqueTransactionIdWithTanPrefix() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));
        when(transactionMapper.toEntity(createTransactionRequest)).thenReturn(transaction);
        when(transactionRepository.existsById(anyString())).thenReturn(false);
        when(accountRepository.save(account)).thenAnswer(invocation -> {
            Account savedAccount = invocation.getArgument(0);
            Transaction savedTransaction = savedAccount.getTransactions().get(0);
            // Updated pattern to match OpenAPI specification: ^tan-[A-Za-z0-9]+$
            assertThat(savedTransaction.getId()).matches("^tan-[A-Za-z0-9]+$");
            return savedAccount;
        });
        when(transactionMapper.toResponse(any(Transaction.class))).thenReturn(transactionResponse);

        transactionService.createTransaction(accountNumber, createTransactionRequest, userId);

        verify(accountRepository).save(account);
    }

    @Test
    void createTransactionGeneratesUniqueTransactionIdAfterCollisions() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));
        when(transactionMapper.toEntity(createTransactionRequest)).thenReturn(transaction);
        when(transactionRepository.existsById(anyString()))
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);
        when(accountRepository.save(account)).thenAnswer(invocation -> {
            Account savedAccount = invocation.getArgument(0);
            savedAccount.getTransactions().add(transaction);
            return savedAccount;
        });
        when(transactionMapper.toResponse(transaction)).thenReturn(transactionResponse);

        transactionService.createTransaction(accountNumber, createTransactionRequest, userId);

        verify(transactionRepository, times(3)).existsById(anyString());
        verify(accountRepository).save(account);
    }

    @Test
    void createTransactionUsesTimestampFallbackWhenMaxAttemptsReached() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));
        when(transactionMapper.toEntity(createTransactionRequest)).thenReturn(transaction);
        when(transactionRepository.existsById(anyString())).thenReturn(true);
        when(accountRepository.save(account)).thenAnswer(invocation -> {
            Account savedAccount = invocation.getArgument(0);
            Transaction savedTransaction = savedAccount.getTransactions().get(0);
            assertThat(savedTransaction.getId()).startsWith("tan-");
            return savedAccount;
        });
        when(transactionMapper.toResponse(any(Transaction.class))).thenReturn(transactionResponse);

        transactionService.createTransaction(accountNumber, createTransactionRequest, userId);

        verify(transactionRepository, times(99)).existsById(anyString());
        verify(accountRepository).save(account);
    }

    @Test
    void createDepositTransactionWithZeroBalanceAccountSuccessfully() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        account.setBalance(0.00);
        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));
        when(transactionMapper.toEntity(createTransactionRequest)).thenReturn(transaction);
        when(transactionRepository.existsById(anyString())).thenReturn(false);
        when(accountRepository.save(account)).thenAnswer(invocation -> {
            Account savedAccount = invocation.getArgument(0);
            savedAccount.getTransactions().add(transaction);
            return savedAccount;
        });
        when(transactionMapper.toResponse(transaction)).thenReturn(transactionResponse);

        TransactionResponse result = transactionService.createTransaction(accountNumber, createTransactionRequest, userId);

        assertThat(result).isEqualTo(transactionResponse);
        assertThat(account.getBalance()).isEqualTo(100.00);
        verify(accountRepository).save(account);
    }

    @Test
    void createWithdrawalTransactionWithExactBalanceSuccessfully() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        CreateTransactionRequest exactBalanceWithdrawal = new CreateTransactionRequest()
                .amount(1000.00)
                .currency(CreateTransactionRequest.CurrencyEnum.GBP)
                .type(CreateTransactionRequest.TypeEnum.WITHDRAWAL)
                .reference("Exact balance withdrawal");

        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));
        when(transactionMapper.toEntity(exactBalanceWithdrawal)).thenReturn(transaction);
        when(transactionRepository.existsById(anyString())).thenReturn(false);
        when(accountRepository.save(account)).thenAnswer(invocation -> {
            Account savedAccount = invocation.getArgument(0);
            savedAccount.getTransactions().add(transaction);
            return savedAccount;
        });
        when(transactionMapper.toResponse(transaction)).thenReturn(transactionResponse);

        TransactionResponse result = transactionService.createTransaction(accountNumber, exactBalanceWithdrawal, userId);

        assertThat(result).isEqualTo(transactionResponse);
        assertThat(account.getBalance()).isEqualTo(0.00);
        verify(accountRepository).save(account);
    }

    // === FIND TRANSACTIONS BY ACCOUNT NUMBER TESTS ===

    @Test
    void findByAccountNumberSuccessfullyReturnsTransactions() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        List<Transaction> transactions = List.of(transaction);
        List<TransactionResponse> transactionResponses = List.of(transactionResponse);

        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));
        when(transactionRepository.findByAccount_AccountNumberOrderByCreatedTimestampDesc(accountNumber))
                .thenReturn(transactions);
        when(transactionMapper.toResponseList(transactions)).thenReturn(transactionResponses);

        ListTransactionsResponse result = transactionService.findByAccountNumber(accountNumber, userId);

        assertThat(result.getTransactions()).hasSize(1);
        assertThat(result.getTransactions().get(0)).isEqualTo(transactionResponse);
        verify(accountRepository).findByAccountNumberWithUser(accountNumber);
        verify(transactionRepository).findByAccount_AccountNumberOrderByCreatedTimestampDesc(accountNumber);
        verify(transactionMapper).toResponseList(transactions);
    }

    @Test
    void findByAccountNumberReturnsEmptyListWhenNoTransactionsExist() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        List<Transaction> emptyTransactions = List.of();
        List<TransactionResponse> emptyResponses = List.of();

        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));
        when(transactionRepository.findByAccount_AccountNumberOrderByCreatedTimestampDesc(accountNumber))
                .thenReturn(emptyTransactions);
        when(transactionMapper.toResponseList(emptyTransactions)).thenReturn(emptyResponses);

        ListTransactionsResponse result = transactionService.findByAccountNumber(accountNumber, userId);

        assertThat(result.getTransactions()).isEmpty();
        verify(accountRepository).findByAccountNumberWithUser(accountNumber);
        verify(transactionRepository).findByAccount_AccountNumberOrderByCreatedTimestampDesc(accountNumber);
        verify(transactionMapper).toResponseList(emptyTransactions);
    }

    @Test
    void findByAccountNumberThrowsAccountNotFoundExceptionWhenAccountDoesNotExist() {
        String accountNumber = "01999999";
        String userId = "usr-1234567890";
        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.findByAccountNumber(accountNumber, userId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Account not found with account number: 01999999");

        verify(accountRepository).findByAccountNumberWithUser(accountNumber);
        verify(transactionRepository, never()).findByAccount_AccountNumberOrderByCreatedTimestampDesc(any());
        verify(transactionMapper, never()).toResponseList(any());
    }

    @Test
    void findByAccountNumberThrowsAccessDeniedExceptionWhenUserDoesNotOwnAccount() {
        String accountNumber = "01234567";
        String otherUserId = "usr-0987654321";
        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> transactionService.findByAccountNumber(accountNumber, otherUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied to account");

        verify(accountRepository).findByAccountNumberWithUser(accountNumber);
        verify(transactionRepository, never()).findByAccount_AccountNumberOrderByCreatedTimestampDesc(any());
        verify(transactionMapper, never()).toResponseList(any());
    }

    @Test
    void findByAccountNumberWithMultipleTransactionsReturnsAllTransactions() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        Transaction secondTransaction = new Transaction();
        secondTransaction.setId("tan-0987654321bc");
        List<Transaction> transactions = List.of(transaction, secondTransaction);
        TransactionResponse secondResponse = new TransactionResponse().id("tan-0987654321bc");
        List<TransactionResponse> transactionResponses = List.of(transactionResponse, secondResponse);

        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));
        when(transactionRepository.findByAccount_AccountNumberOrderByCreatedTimestampDesc(accountNumber))
                .thenReturn(transactions);
        when(transactionMapper.toResponseList(transactions)).thenReturn(transactionResponses);

        ListTransactionsResponse result = transactionService.findByAccountNumber(accountNumber, userId);

        assertThat(result.getTransactions()).hasSize(2);
        verify(accountRepository).findByAccountNumberWithUser(accountNumber);
        verify(transactionRepository).findByAccount_AccountNumberOrderByCreatedTimestampDesc(accountNumber);
        verify(transactionMapper).toResponseList(transactions);
    }

    // === FIND TRANSACTION BY ID AND ACCOUNT NUMBER TESTS ===

    @Test
    void findByIdAndAccountNumberSuccessfullyReturnsTransaction() {
        String transactionId = "tan-1234567890ab";
        String accountNumber = "01234567";
        String userId = "usr-1234567890";

        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));
        when(transactionRepository.findByIdAndAccount_AccountNumber(transactionId, accountNumber))
                .thenReturn(Optional.of(transaction));
        when(transactionMapper.toResponse(transaction)).thenReturn(transactionResponse);

        TransactionResponse result = transactionService.findByIdAndAccountNumber(transactionId, accountNumber, userId);

        assertThat(result).isEqualTo(transactionResponse);
        verify(accountRepository).findByAccountNumberWithUser(accountNumber);
        verify(transactionRepository).findByIdAndAccount_AccountNumber(transactionId, accountNumber);
        verify(transactionMapper).toResponse(transaction);
    }

    @Test
    void findByIdAndAccountNumberThrowsAccountNotFoundExceptionWhenAccountDoesNotExist() {
        String transactionId = "tan-1234567890ab";
        String accountNumber = "01999999";
        String userId = "usr-1234567890";
        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.findByIdAndAccountNumber(transactionId, accountNumber, userId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Account not found with account number: 01999999");

        verify(accountRepository).findByAccountNumberWithUser(accountNumber);
        verify(transactionRepository, never()).findByIdAndAccount_AccountNumber(any(), any());
        verify(transactionMapper, never()).toResponse(any());
    }

    @Test
    void findByIdAndAccountNumberThrowsAccessDeniedExceptionWhenUserDoesNotOwnAccount() {
        String transactionId = "tan-1234567890ab";
        String accountNumber = "01234567";
        String otherUserId = "usr-0987654321";
        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> transactionService.findByIdAndAccountNumber(transactionId, accountNumber, otherUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied to account");

        verify(accountRepository).findByAccountNumberWithUser(accountNumber);
        verify(transactionRepository, never()).findByIdAndAccount_AccountNumber(any(), any());
        verify(transactionMapper, never()).toResponse(any());
    }

    @Test
    void findByIdAndAccountNumberThrowsTransactionNotFoundExceptionWhenTransactionDoesNotExist() {
        String transactionId = "tan-nonexistent";
        String accountNumber = "01234567";
        String userId = "usr-1234567890";

        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));
        when(transactionRepository.findByIdAndAccount_AccountNumber(transactionId, accountNumber))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.findByIdAndAccountNumber(transactionId, accountNumber, userId))
                .isInstanceOf(TransactionNotFoundException.class)
                .hasMessage("Transaction with ID tan-nonexistent not found on account 01234567");

        verify(accountRepository).findByAccountNumberWithUser(accountNumber);
        verify(transactionRepository).findByIdAndAccount_AccountNumber(transactionId, accountNumber);
        verify(transactionMapper, never()).toResponse(any());
    }

    @Test
    void findByIdAndAccountNumberThrowsTransactionNotFoundExceptionWhenTransactionBelongsToDifferentAccount() {
        String transactionId = "tan-1234567890ab";
        String wrongAccountNumber = "01765432";
        String userId = "usr-1234567890";
        Account wrongAccount = new Account();
        wrongAccount.setAccountNumber(wrongAccountNumber);
        wrongAccount.setUser(user);

        when(accountRepository.findByAccountNumberWithUser(wrongAccountNumber)).thenReturn(Optional.of(wrongAccount));
        when(transactionRepository.findByIdAndAccount_AccountNumber(transactionId, wrongAccountNumber))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.findByIdAndAccountNumber(transactionId, wrongAccountNumber, userId))
                .isInstanceOf(TransactionNotFoundException.class)
                .hasMessage("Transaction with ID tan-1234567890ab not found on account 01765432");

        verify(accountRepository).findByAccountNumberWithUser(wrongAccountNumber);
        verify(transactionRepository).findByIdAndAccount_AccountNumber(transactionId, wrongAccountNumber);
        verify(transactionMapper, never()).toResponse(any());
    }

    // === ADDITIONAL EDGE CASES AND SCENARIOS ===

    @Test
    void createTransactionSetsSystemGeneratedFieldsCorrectly() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));
        when(transactionMapper.toEntity(createTransactionRequest)).thenReturn(transaction);
        when(transactionRepository.existsById(anyString())).thenReturn(false);
        when(accountRepository.save(account)).thenAnswer(invocation -> {
            Account savedAccount = invocation.getArgument(0);
            Transaction savedTransaction = savedAccount.getTransactions().get(0);
            assertThat(savedTransaction.getCurrency()).isEqualTo(Transaction.Currency.GBP);
            assertThat(savedTransaction.getId()).isNotNull();
            return savedAccount;
        });
        when(transactionMapper.toResponse(any(Transaction.class))).thenReturn(transactionResponse);

        transactionService.createTransaction(accountNumber, createTransactionRequest, userId);

        verify(accountRepository).save(account);
    }

    @Test
    void createTransactionValidatesAccountOwnershipBeforeProcessing() {
        String accountNumber = "01234567";
        String otherUserId = "usr-0987654321";
        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> transactionService.createTransaction(accountNumber, createTransactionRequest, otherUserId))
                .isInstanceOf(AccessDeniedException.class);

        verify(accountRepository).findByAccountNumberWithUser(accountNumber);
        verify(transactionMapper, never()).toEntity(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void createWithdrawalTransactionFailsWhenAmountExceedsBalanceBySmallAmount() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        CreateTransactionRequest slightlyOverBalanceWithdrawal = new CreateTransactionRequest()
                .amount(1000.01)
                .currency(CreateTransactionRequest.CurrencyEnum.GBP)
                .type(CreateTransactionRequest.TypeEnum.WITHDRAWAL)
                .reference("Slightly over balance");

        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> transactionService.createTransaction(accountNumber, slightlyOverBalanceWithdrawal, userId))
                .isInstanceOf(InsufficientFundsException.class);

        verify(accountRepository).findByAccountNumberWithUser(accountNumber);
        verify(transactionMapper, never()).toEntity(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void findByAccountNumberVerifiesOwnershipBeforeRetrievingTransactions() {
        String accountNumber = "01234567";
        String otherUserId = "usr-0987654321";
        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> transactionService.findByAccountNumber(accountNumber, otherUserId))
                .isInstanceOf(AccessDeniedException.class);

        verify(accountRepository).findByAccountNumberWithUser(accountNumber);
        verify(transactionRepository, never()).findByAccount_AccountNumberOrderByCreatedTimestampDesc(any());
        verify(transactionMapper, never()).toResponseList(any());
    }

    @Test
    void findByIdAndAccountNumberVerifiesOwnershipBeforeRetrievingTransaction() {
        String transactionId = "tan-1234567890ab";
        String accountNumber = "01234567";
        String otherUserId = "usr-0987654321";
        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> transactionService.findByIdAndAccountNumber(transactionId, accountNumber, otherUserId))
                .isInstanceOf(AccessDeniedException.class);

        verify(accountRepository).findByAccountNumberWithUser(accountNumber);
        verify(transactionRepository, never()).findByIdAndAccount_AccountNumber(any(), any());
        verify(transactionMapper, never()).toResponse(any());
    }

    @Test
    void createTransactionUpdatesAccountBalanceCorrectlyForDeposit() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        double originalBalance = account.getBalance();
        double depositAmount = createTransactionRequest.getAmount();

        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));
        when(transactionMapper.toEntity(createTransactionRequest)).thenReturn(transaction);
        when(transactionRepository.existsById(anyString())).thenReturn(false);
        when(accountRepository.save(account)).thenAnswer(invocation -> {
            Account savedAccount = invocation.getArgument(0);
            assertThat(savedAccount.getBalance()).isEqualTo(originalBalance + depositAmount);
            savedAccount.getTransactions().add(transaction);
            return savedAccount;
        });
        when(transactionMapper.toResponse(transaction)).thenReturn(transactionResponse);

        transactionService.createTransaction(accountNumber, createTransactionRequest, userId);

        verify(accountRepository).save(account);
    }

    @Test
    void createTransactionUpdatesAccountBalanceCorrectlyForWithdrawal() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        double originalBalance = account.getBalance();
        CreateTransactionRequest withdrawalRequest = new CreateTransactionRequest()
                .amount(150.00)
                .currency(CreateTransactionRequest.CurrencyEnum.GBP)
                .type(CreateTransactionRequest.TypeEnum.WITHDRAWAL)
                .reference("Test withdrawal");

        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));
        when(transactionMapper.toEntity(withdrawalRequest)).thenReturn(transaction);
        when(transactionRepository.existsById(anyString())).thenReturn(false);
        when(accountRepository.save(account)).thenAnswer(invocation -> {
            Account savedAccount = invocation.getArgument(0);
            assertThat(savedAccount.getBalance()).isEqualTo(originalBalance - 150.00);
            savedAccount.getTransactions().add(transaction);
            return savedAccount;
        });
        when(transactionMapper.toResponse(transaction)).thenReturn(transactionResponse);

        transactionService.createTransaction(accountNumber, withdrawalRequest, userId);

        verify(accountRepository).save(account);
    }

    // === ADDITIONAL VALIDATION AND EDGE CASE TESTS ===

    @Test
    void createTransactionWithMinimumValidAmount() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        CreateTransactionRequest minAmountRequest = new CreateTransactionRequest()
                .amount(0.01)
                .currency(CreateTransactionRequest.CurrencyEnum.GBP)
                .type(CreateTransactionRequest.TypeEnum.DEPOSIT)
                .reference("Minimum amount deposit");

        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));
        when(transactionMapper.toEntity(minAmountRequest)).thenReturn(transaction);
        when(transactionRepository.existsById(anyString())).thenReturn(false);
        when(accountRepository.save(account)).thenAnswer(invocation -> {
            Account savedAccount = invocation.getArgument(0);
            savedAccount.getTransactions().add(transaction);
            return savedAccount;
        });
        when(transactionMapper.toResponse(transaction)).thenReturn(transactionResponse);

        TransactionResponse result = transactionService.createTransaction(accountNumber, minAmountRequest, userId);

        assertThat(result).isEqualTo(transactionResponse);
        verify(accountRepository).save(account);
    }

    @Test
    void createTransactionWithMaximumValidAmount() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        account.setBalance(10000.00); // Set high balance to allow max withdrawal
        CreateTransactionRequest maxAmountRequest = new CreateTransactionRequest()
                .amount(10000.00)
                .currency(CreateTransactionRequest.CurrencyEnum.GBP)
                .type(CreateTransactionRequest.TypeEnum.WITHDRAWAL)
                .reference("Maximum amount withdrawal");

        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));
        when(transactionMapper.toEntity(maxAmountRequest)).thenReturn(transaction);
        when(transactionRepository.existsById(anyString())).thenReturn(false);
        when(accountRepository.save(account)).thenAnswer(invocation -> {
            Account savedAccount = invocation.getArgument(0);
            savedAccount.getTransactions().add(transaction);
            return savedAccount;
        });
        when(transactionMapper.toResponse(transaction)).thenReturn(transactionResponse);

        TransactionResponse result = transactionService.createTransaction(accountNumber, maxAmountRequest, userId);

        assertThat(result).isEqualTo(transactionResponse);
        assertThat(account.getBalance()).isEqualTo(0.00);
        verify(accountRepository).save(account);
    }

    @Test
    void createTransactionWithOptionalReferenceField() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        CreateTransactionRequest requestWithoutReference = new CreateTransactionRequest()
                .amount(100.00)
                .currency(CreateTransactionRequest.CurrencyEnum.GBP)
                .type(CreateTransactionRequest.TypeEnum.DEPOSIT);
        // Note: reference is optional according to OpenAPI spec

        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));
        when(transactionMapper.toEntity(requestWithoutReference)).thenReturn(transaction);
        when(transactionRepository.existsById(anyString())).thenReturn(false);
        when(accountRepository.save(account)).thenAnswer(invocation -> {
            Account savedAccount = invocation.getArgument(0);
            savedAccount.getTransactions().add(transaction);
            return savedAccount;
        });
        when(transactionMapper.toResponse(transaction)).thenReturn(transactionResponse);

        TransactionResponse result = transactionService.createTransaction(accountNumber, requestWithoutReference, userId);

        assertThat(result).isEqualTo(transactionResponse);
        verify(accountRepository).save(account);
    }

    @Test
    void findByAccountNumberReturnsTransactionsInDescendingOrderByCreatedTimestamp() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";

        // Create multiple transactions with different timestamps
        Transaction olderTransaction = new Transaction();
        olderTransaction.setId("tan-older123");
        olderTransaction.setCreatedTimestamp(LocalDateTime.now().minusDays(1));

        Transaction newerTransaction = new Transaction();
        newerTransaction.setId("tan-newer456");
        newerTransaction.setCreatedTimestamp(LocalDateTime.now());

        List<Transaction> orderedTransactions = List.of(newerTransaction, olderTransaction); // Newest first
        List<TransactionResponse> orderedResponses = List.of(
            new TransactionResponse().id("tan-newer456"),
            new TransactionResponse().id("tan-older123")
        );

        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));
        when(transactionRepository.findByAccount_AccountNumberOrderByCreatedTimestampDesc(accountNumber))
                .thenReturn(orderedTransactions);
        when(transactionMapper.toResponseList(orderedTransactions)).thenReturn(orderedResponses);

        ListTransactionsResponse result = transactionService.findByAccountNumber(accountNumber, userId);

        assertThat(result.getTransactions()).hasSize(2);
        assertThat(result.getTransactions().get(0).getId()).isEqualTo("tan-newer456");
        assertThat(result.getTransactions().get(1).getId()).isEqualTo("tan-older123");
        verify(transactionRepository).findByAccount_AccountNumberOrderByCreatedTimestampDesc(accountNumber);
    }

    @Test
    void createTransactionHandlesUniqueIdGenerationWithMultipleCollisions() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));
        when(transactionMapper.toEntity(createTransactionRequest)).thenReturn(transaction);

        // Simulate multiple collisions before finding unique ID
        when(transactionRepository.existsById(anyString()))
                .thenReturn(true)   // 1st attempt - collision
                .thenReturn(true)   // 2nd attempt - collision
                .thenReturn(true)   // 3rd attempt - collision
                .thenReturn(true)   // 4th attempt - collision
                .thenReturn(false); // 5th attempt - success

        when(accountRepository.save(account)).thenAnswer(invocation -> {
            Account savedAccount = invocation.getArgument(0);
            savedAccount.getTransactions().add(transaction);
            return savedAccount;
        });
        when(transactionMapper.toResponse(transaction)).thenReturn(transactionResponse);

        transactionService.createTransaction(accountNumber, createTransactionRequest, userId);

        verify(transactionRepository, times(5)).existsById(anyString());
        verify(accountRepository).save(account);
    }

    @Test
    void createDepositTransactionWithLargeAccountBalance() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        account.setBalance(9999.99); // Near maximum balance
        CreateTransactionRequest smallDepositRequest = new CreateTransactionRequest()
                .amount(0.01)
                .currency(CreateTransactionRequest.CurrencyEnum.GBP)
                .type(CreateTransactionRequest.TypeEnum.DEPOSIT)
                .reference("Small deposit to large balance");

        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));
        when(transactionMapper.toEntity(smallDepositRequest)).thenReturn(transaction);
        when(transactionRepository.existsById(anyString())).thenReturn(false);
        when(accountRepository.save(account)).thenAnswer(invocation -> {
            Account savedAccount = invocation.getArgument(0);
            savedAccount.getTransactions().add(transaction);
            return savedAccount;
        });
        when(transactionMapper.toResponse(transaction)).thenReturn(transactionResponse);

        TransactionResponse result = transactionService.createTransaction(accountNumber, smallDepositRequest, userId);

        assertThat(result).isEqualTo(transactionResponse);
        assertThat(account.getBalance()).isEqualTo(10000.00); // Reaches maximum allowed balance
        verify(accountRepository).save(account);
    }

    @Test
    void createWithdrawalTransactionLeavingAccountWithZeroBalance() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        account.setBalance(50.00);
        CreateTransactionRequest fullWithdrawalRequest = new CreateTransactionRequest()
                .amount(50.00)
                .currency(CreateTransactionRequest.CurrencyEnum.GBP)
                .type(CreateTransactionRequest.TypeEnum.WITHDRAWAL)
                .reference("Full balance withdrawal");

        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));
        when(transactionMapper.toEntity(fullWithdrawalRequest)).thenReturn(transaction);
        when(transactionRepository.existsById(anyString())).thenReturn(false);
        when(accountRepository.save(account)).thenAnswer(invocation -> {
            Account savedAccount = invocation.getArgument(0);
            savedAccount.getTransactions().add(transaction);
            return savedAccount;
        });
        when(transactionMapper.toResponse(transaction)).thenReturn(transactionResponse);

        TransactionResponse result = transactionService.createTransaction(accountNumber, fullWithdrawalRequest, userId);

        assertThat(result).isEqualTo(transactionResponse);
        assertThat(account.getBalance()).isEqualTo(0.00);
        verify(accountRepository).save(account);
    }

    @Test
    void findByIdAndAccountNumberWithValidTransactionIdFormat() {
        String transactionId = "tan-AbC123dEf456"; // Mixed case alphanumeric as per OpenAPI spec
        String accountNumber = "01234567";
        String userId = "usr-1234567890";

        when(accountRepository.findByAccountNumberWithUser(accountNumber)).thenReturn(Optional.of(account));
        when(transactionRepository.findByIdAndAccount_AccountNumber(transactionId, accountNumber))
                .thenReturn(Optional.of(transaction));
        when(transactionMapper.toResponse(transaction)).thenReturn(transactionResponse);

        TransactionResponse result = transactionService.findByIdAndAccountNumber(transactionId, accountNumber, userId);

        assertThat(result).isEqualTo(transactionResponse);
        verify(transactionRepository).findByIdAndAccount_AccountNumber(transactionId, accountNumber);
    }
}
