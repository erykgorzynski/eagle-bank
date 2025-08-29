package org.example.service;

import org.example.entity.Account;
import org.example.entity.Transaction;
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
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountService accountService;

    private User user;
    private Account account;
    private BankAccountResponse bankAccountResponse;
    private CreateBankAccountRequest createBankAccountRequest;
    private UpdateBankAccountRequest updateBankAccountRequest;
    private ListBankAccountsResponse listBankAccountsResponse;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId("usr-1234567890");
        user.setName("John Doe");
        user.setEmail("john.doe@example.com");
        user.setAccounts(new ArrayList<>());

        account = new Account();
        account.setAccountNumber("01234567");
        account.setName("Savings Account");
        account.setAccountType(Account.AccountType.PERSONAL);
        account.setBalance(1000.00);
        account.setCurrency(Account.Currency.GBP);
        account.setSortCode(Account.SortCode._10_10_10);
        account.setUser(user);
        account.setTransactions(new ArrayList<>());
        account.setCreatedTimestamp(LocalDateTime.now());
        account.setUpdatedTimestamp(LocalDateTime.now());

        bankAccountResponse = new BankAccountResponse()
                .accountNumber("01234567")
                .name("Savings Account")
                .accountType(BankAccountResponse.AccountTypeEnum.PERSONAL)
                .balance(1000.00)
                .currency(BankAccountResponse.CurrencyEnum.GBP)
                .sortCode(BankAccountResponse.SortCodeEnum._10_10_10)
                .createdTimestamp(LocalDateTime.now())
                .updatedTimestamp(LocalDateTime.now());

        createBankAccountRequest = new CreateBankAccountRequest()
                .name("Savings Account")
                .accountType(CreateBankAccountRequest.AccountTypeEnum.PERSONAL);

        updateBankAccountRequest = new UpdateBankAccountRequest()
                .name("Updated Savings Account");

        listBankAccountsResponse = new ListBankAccountsResponse()
                .addAccountsItem(bankAccountResponse);
    }

    // === CREATE ACCOUNT TESTS ===

    @Test
    void createAccountSuccessfullyForValidUser() {
        String userId = "usr-1234567890";
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountMapper.toEntity(createBankAccountRequest)).thenReturn(account);
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.getAccounts().add(account);
            return savedUser;
        });
        when(accountMapper.toResponse(account)).thenReturn(bankAccountResponse);

        BankAccountResponse result = accountService.createAccount(userId, createBankAccountRequest);

        assertThat(result).isEqualTo(bankAccountResponse);
        verify(userRepository).findById(userId);
        verify(accountMapper).toEntity(createBankAccountRequest);
        verify(userRepository).save(any(User.class));
        verify(accountMapper).toResponse(account);
    }

    @Test
    void createAccountThrowsUserNotFoundExceptionWhenUserDoesNotExist() {
        String userId = "usr-nonexistent";
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.createAccount(userId, createBankAccountRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with ID: usr-nonexistent");

        verify(userRepository).findById(userId);
        verify(accountMapper, never()).toEntity(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void createAccountGeneratesUniqueAccountNumberWithCorrectFormat() {
        String userId = "usr-1234567890";
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountMapper.toEntity(createBankAccountRequest)).thenReturn(account);
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            Account savedAccount = savedUser.getAccounts().get(0);
            assertThat(savedAccount.getAccountNumber()).matches("^01\\d{6}$");
            return savedUser;
        });
        when(accountMapper.toResponse(any(Account.class))).thenReturn(bankAccountResponse);

        accountService.createAccount(userId, createBankAccountRequest);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void createAccountSetsSystemGeneratedFieldsCorrectly() {
        String userId = "usr-1234567890";
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountMapper.toEntity(createBankAccountRequest)).thenReturn(account);
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            Account savedAccount = savedUser.getAccounts().get(0);
            assertThat(savedAccount.getBalance()).isEqualTo(0.0);
            assertThat(savedAccount.getCurrency()).isEqualTo(Account.Currency.GBP);
            assertThat(savedAccount.getSortCode()).isEqualTo(Account.SortCode._10_10_10);
            return savedUser;
        });
        when(accountMapper.toResponse(any(Account.class))).thenReturn(bankAccountResponse);

        accountService.createAccount(userId, createBankAccountRequest);

        verify(userRepository).save(any(User.class));
    }

    // === FIND ACCOUNT BY ACCOUNT NUMBER TESTS ===

    @Test
    void findByAccountNumberSuccessfullyForAccountOwner() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(accountMapper.toResponse(account)).thenReturn(bankAccountResponse);

        BankAccountResponse result = accountService.findByAccountNumber(accountNumber, userId);

        assertThat(result).isEqualTo(bankAccountResponse);
        verify(accountRepository).findByAccountNumber(accountNumber);
        verify(accountMapper).toResponse(account);
    }

    @Test
    void findByAccountNumberThrowsAccountNotFoundExceptionWhenAccountDoesNotExist() {
        String accountNumber = "01999999";
        String userId = "usr-1234567890";
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.findByAccountNumber(accountNumber, userId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Account not found with account number: 01999999");

        verify(accountRepository).findByAccountNumber(accountNumber);
        verify(accountMapper, never()).toResponse(any());
    }

    @Test
    void findByAccountNumberThrowsAccessDeniedExceptionWhenUserDoesNotOwnAccount() {
        String accountNumber = "01234567";
        String otherUserId = "usr-0987654321";
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.findByAccountNumber(accountNumber, otherUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied to account");

        verify(accountRepository).findByAccountNumber(accountNumber);
        verify(accountMapper, never()).toResponse(any());
    }

    // === FIND ACCOUNTS BY USER ID TESTS ===

    @Test
    void findAccountsByUserIdSuccessfullyReturnsUserAccounts() {
        String userId = "usr-1234567890";
        List<Account> accounts = List.of(account);
        List<BankAccountResponse> accountResponses = List.of(bankAccountResponse);
        when(accountRepository.findByUser_Id(userId)).thenReturn(accounts);
        when(accountMapper.toResponseList(accounts)).thenReturn(accountResponses);

        ListBankAccountsResponse result = accountService.findAccountsByUserId(userId);

        assertThat(result.getAccounts()).hasSize(1);
        assertThat(result.getAccounts().get(0)).isEqualTo(bankAccountResponse);
        verify(accountRepository).findByUser_Id(userId);
        verify(accountMapper).toResponseList(accounts);
    }

    @Test
    void findAccountsByUserIdReturnsEmptyListWhenUserHasNoAccounts() {
        String userId = "usr-1234567890";
        List<Account> emptyAccounts = List.of();
        List<BankAccountResponse> emptyResponses = List.of();
        when(accountRepository.findByUser_Id(userId)).thenReturn(emptyAccounts);
        when(accountMapper.toResponseList(emptyAccounts)).thenReturn(emptyResponses);

        ListBankAccountsResponse result = accountService.findAccountsByUserId(userId);

        assertThat(result.getAccounts()).isEmpty();
        verify(accountRepository).findByUser_Id(userId);
        verify(accountMapper).toResponseList(emptyAccounts);
    }

    // === UPDATE ACCOUNT TESTS ===

    @Test
    void updateAccountSuccessfullyForAccountOwner() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        when(accountMapper.toResponse(account)).thenReturn(bankAccountResponse);

        BankAccountResponse result = accountService.updateAccount(accountNumber, userId, updateBankAccountRequest);

        assertThat(result).isEqualTo(bankAccountResponse);
        verify(accountRepository).findByAccountNumber(accountNumber);
        verify(accountMapper).updateEntityFromRequest(updateBankAccountRequest, account);
        verify(accountRepository).save(account);
        verify(accountMapper).toResponse(account);
    }

    @Test
    void updateAccountThrowsAccountNotFoundExceptionWhenAccountDoesNotExist() {
        String accountNumber = "01999999";
        String userId = "usr-1234567890";
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.updateAccount(accountNumber, userId, updateBankAccountRequest))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Account not found with account number: 01999999");

        verify(accountRepository).findByAccountNumber(accountNumber);
        verify(accountMapper, never()).updateEntityFromRequest(any(), any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void updateAccountThrowsAccessDeniedExceptionWhenUserDoesNotOwnAccount() {
        String accountNumber = "01234567";
        String otherUserId = "usr-0987654321";
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.updateAccount(accountNumber, otherUserId, updateBankAccountRequest))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied to account");

        verify(accountRepository).findByAccountNumber(accountNumber);
        verify(accountMapper, never()).updateEntityFromRequest(any(), any());
        verify(accountRepository, never()).save(any());
    }

    // === DELETE ACCOUNT TESTS ===

    @Test
    void deleteAccountSuccessfullyWhenAccountHasZeroBalanceAndNoTransactions() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        account.setBalance(0.0);
        account.setTransactions(new ArrayList<>());
        user.getAccounts().add(account);
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(userRepository.save(user)).thenReturn(user);

        accountService.deleteAccount(accountNumber, userId);

        verify(accountRepository).findByAccountNumber(accountNumber);
        verify(userRepository).save(user);
    }

    @Test
    void deleteAccountThrowsAccountNotFoundExceptionWhenAccountDoesNotExist() {
        String accountNumber = "01999999";
        String userId = "usr-1234567890";
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.deleteAccount(accountNumber, userId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Account not found with account number: 01999999");

        verify(accountRepository).findByAccountNumber(accountNumber);
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteAccountThrowsAccessDeniedExceptionWhenUserDoesNotOwnAccount() {
        String accountNumber = "01234567";
        String otherUserId = "usr-0987654321";
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.deleteAccount(accountNumber, otherUserId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied to account");

        verify(accountRepository).findByAccountNumber(accountNumber);
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteAccountThrowsIllegalStateExceptionWhenAccountHasTransactions() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        account.setBalance(0.0);
        Transaction transaction = new Transaction();
        account.getTransactions().add(transaction);
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.deleteAccount(accountNumber, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot delete account with existing transactions");

        verify(accountRepository).findByAccountNumber(accountNumber);
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteAccountThrowsIllegalStateExceptionWhenAccountHasNonZeroBalance() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        account.setBalance(100.0);
        account.setTransactions(new ArrayList<>());
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.deleteAccount(accountNumber, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot delete account with non-zero balance");

        verify(accountRepository).findByAccountNumber(accountNumber);
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteAccountThrowsIllegalStateExceptionWhenAccountHasNegativeBalance() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        account.setBalance(-50.0);
        account.setTransactions(new ArrayList<>());
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.deleteAccount(accountNumber, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot delete account with non-zero balance");

        verify(accountRepository).findByAccountNumber(accountNumber);
        verify(userRepository, never()).save(any());
    }

    // === ADDITIONAL EDGE CASES AND SCENARIOS ===

    @Test
    void createAccountGeneratesUniqueAccountNumberAfterCollisions() {
        String userId = "usr-1234567890";
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountMapper.toEntity(createBankAccountRequest)).thenReturn(account);
        when(accountRepository.existsByAccountNumber(anyString()))
                .thenReturn(true)  // First attempt collision
                .thenReturn(true)  // Second attempt collision
                .thenReturn(false); // Third attempt succeeds
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.getAccounts().add(account);
            return savedUser;
        });
        when(accountMapper.toResponse(any(Account.class))).thenReturn(bankAccountResponse);

        accountService.createAccount(userId, createBankAccountRequest);

        verify(accountRepository, times(3)).existsByAccountNumber(anyString());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createAccountUsesUuidFallbackWhenMaxAttemptsReached() {
        String userId = "usr-1234567890";
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountMapper.toEntity(createBankAccountRequest)).thenReturn(account);
        when(accountRepository.existsByAccountNumber(anyString()))
                .thenReturn(true);  // Always return true to force fallback
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            Account savedAccount = savedUser.getAccounts().get(0);
            assertThat(savedAccount.getAccountNumber()).startsWith("01");
            assertThat(savedAccount.getAccountNumber()).hasSize(8);
            return savedUser;
        });
        when(accountMapper.toResponse(any(Account.class))).thenReturn(bankAccountResponse);

        accountService.createAccount(userId, createBankAccountRequest);

        verify(accountRepository, times(99)).existsByAccountNumber(anyString());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void findAccountsByUserIdWithMultipleAccountsReturnsAllAccounts() {
        String userId = "usr-1234567890";
        Account secondAccount = new Account();
        secondAccount.setAccountNumber("01765432");
        List<Account> accounts = List.of(account, secondAccount);
        BankAccountResponse secondResponse = new BankAccountResponse().accountNumber("01765432");
        List<BankAccountResponse> accountResponses = List.of(bankAccountResponse, secondResponse);
        when(accountRepository.findByUser_Id(userId)).thenReturn(accounts);
        when(accountMapper.toResponseList(accounts)).thenReturn(accountResponses);

        ListBankAccountsResponse result = accountService.findAccountsByUserId(userId);

        assertThat(result.getAccounts()).hasSize(2);
        verify(accountRepository).findByUser_Id(userId);
        verify(accountMapper).toResponseList(accounts);
    }

    @Test
    void updateAccountWithNullFieldsDoesNotOverwriteExistingValues() {
        String accountNumber = "01234567";
        String userId = "usr-1234567890";
        UpdateBankAccountRequest partialUpdate = new UpdateBankAccountRequest().name("New Name");
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        when(accountMapper.toResponse(account)).thenReturn(bankAccountResponse);

        accountService.updateAccount(accountNumber, userId, partialUpdate);

        verify(accountMapper).updateEntityFromRequest(partialUpdate, account);
        verify(accountRepository).save(account);
    }

    @Test
    void deleteAccountVerifiesOwnershipBeforeCheckingBusinessRules() {
        String accountNumber = "01234567";
        String otherUserId = "usr-0987654321";
        account.setBalance(100.0);  // Non-zero balance
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.deleteAccount(accountNumber, otherUserId))
                .isInstanceOf(AccessDeniedException.class);

        verify(accountRepository).findByAccountNumber(accountNumber);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createAccountSetsTimestampsAutomatically() {
        String userId = "usr-1234567890";
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountMapper.toEntity(createBankAccountRequest)).thenReturn(account);
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.getAccounts().add(account);
            return savedUser;
        });
        when(accountMapper.toResponse(account)).thenReturn(bankAccountResponse);

        BankAccountResponse result = accountService.createAccount(userId, createBankAccountRequest);

        assertThat(result.getCreatedTimestamp()).isNotNull();
        assertThat(result.getUpdatedTimestamp()).isNotNull();
        verify(userRepository).save(any(User.class));
    }
}
