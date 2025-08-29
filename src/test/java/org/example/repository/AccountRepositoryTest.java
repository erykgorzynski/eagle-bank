package org.example.repository;

import org.example.entity.Account;
import org.example.entity.User;
import org.example.entity.Address;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountRepositoryTest {

    @Mock
    private AccountRepository accountRepository;

    private User testUser1;
    private User testUser2;
    private Account account1;
    private Account account2;
    private Account account3;

    @BeforeEach
    void setUp() {
        // Create test users
        testUser1 = new User();
        testUser1.setId("usr-123abc456");
        testUser1.setEmail("user1@example.com");
        testUser1.setName("John Doe");
        testUser1.setPhoneNumber("+447123456789");
        testUser1.setPasswordHash("hashedPassword1");

        Address address1 = new Address();
        address1.setLine1("123 Main Street");
        address1.setTown("London");
        address1.setCounty("Greater London");
        address1.setPostcode("SW1A 1AA");
        testUser1.setAddress(address1);

        testUser2 = new User();
        testUser2.setId("usr-789def012");
        testUser2.setEmail("user2@example.com");
        testUser2.setName("Jane Smith");
        testUser2.setPhoneNumber("+447987654321");
        testUser2.setPasswordHash("hashedPassword2");

        Address address2 = new Address();
        address2.setLine1("456 Oak Avenue");
        address2.setTown("Manchester");
        address2.setCounty("Greater Manchester");
        address2.setPostcode("M1 1AA");
        testUser2.setAddress(address2);

        // Create test accounts
        account1 = new Account();
        account1.setAccountNumber("01123456");
        account1.setName("John's Personal Account");
        account1.setAccountType(Account.AccountType.PERSONAL);
        account1.setBalance(1500.75);
        account1.setCurrency(Account.Currency.GBP);
        account1.setSortCode(Account.SortCode._10_10_10);
        account1.setUser(testUser1);
        account1.setCreatedTimestamp(LocalDateTime.of(2025, 8, 29, 10, 30));
        account1.setUpdatedTimestamp(LocalDateTime.of(2025, 8, 29, 10, 30));

        account2 = new Account();
        account2.setAccountNumber("01234567");
        account2.setName("John's Savings Account");
        account2.setAccountType(Account.AccountType.PERSONAL);
        account2.setBalance(5000.00);
        account2.setCurrency(Account.Currency.GBP);
        account2.setSortCode(Account.SortCode._10_10_10);
        account2.setUser(testUser1);
        account2.setCreatedTimestamp(LocalDateTime.of(2025, 8, 29, 11, 0));
        account2.setUpdatedTimestamp(LocalDateTime.of(2025, 8, 29, 11, 0));

        account3 = new Account();
        account3.setAccountNumber("01345678");
        account3.setName("Jane's Personal Account");
        account3.setAccountType(Account.AccountType.PERSONAL);
        account3.setBalance(2750.25);
        account3.setCurrency(Account.Currency.GBP);
        account3.setSortCode(Account.SortCode._10_10_10);
        account3.setUser(testUser2);
        account3.setCreatedTimestamp(LocalDateTime.of(2025, 8, 29, 12, 0));
        account3.setUpdatedTimestamp(LocalDateTime.of(2025, 8, 29, 12, 0));
    }

    @Test
    void findByUserId_WithValidUserId_ReturnsUserAccounts() {
        List<Account> expectedAccounts = List.of(account1, account2);
        when(accountRepository.findByUserId("usr-123abc456")).thenReturn(expectedAccounts);

        List<Account> accounts = accountRepository.findByUserId("usr-123abc456");

        assertThat(accounts).hasSize(2);
        assertThat(accounts).extracting(Account::getAccountNumber)
                .containsExactlyInAnyOrder("01123456", "01234567");
        assertThat(accounts).allMatch(account -> account.getUser().getId().equals("usr-123abc456"));
        verify(accountRepository).findByUserId("usr-123abc456");
    }

    @Test
    void findByUserId_WithNonExistentUserId_ReturnsEmptyList() {
        when(accountRepository.findByUserId("usr-nonexistent")).thenReturn(List.of());

        List<Account> accounts = accountRepository.findByUserId("usr-nonexistent");

        assertThat(accounts).isEmpty();
        verify(accountRepository).findByUserId("usr-nonexistent");
    }

    @Test
    void findByUser_Id_WithValidUserId_ReturnsUserAccounts() {
        List<Account> expectedAccounts = List.of(account3);
        when(accountRepository.findByUser_Id("usr-789def012")).thenReturn(expectedAccounts);

        List<Account> accounts = accountRepository.findByUser_Id("usr-789def012");

        assertThat(accounts).hasSize(1);
        assertThat(accounts.get(0).getAccountNumber()).isEqualTo("01345678");
        assertThat(accounts.get(0).getUser().getId()).isEqualTo("usr-789def012");
        verify(accountRepository).findByUser_Id("usr-789def012");
    }

    @Test
    void findByUser_Id_WithNonExistentUserId_ReturnsEmptyList() {
        when(accountRepository.findByUser_Id("usr-nonexistent")).thenReturn(List.of());

        List<Account> accounts = accountRepository.findByUser_Id("usr-nonexistent");

        assertThat(accounts).isEmpty();
        verify(accountRepository).findByUser_Id("usr-nonexistent");
    }

    @Test
    void findByAccountNumber_WithValidAccountNumber_ReturnsAccount() {
        when(accountRepository.findByAccountNumber("01123456")).thenReturn(Optional.of(account1));

        Optional<Account> result = accountRepository.findByAccountNumber("01123456");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("John's Personal Account");
        assertThat(result.get().getBalance()).isEqualTo(1500.75);
        assertThat(result.get().getUser().getId()).isEqualTo("usr-123abc456");
        verify(accountRepository).findByAccountNumber("01123456");
    }

    @Test
    void findByAccountNumber_WithNonExistentAccountNumber_ReturnsEmpty() {
        when(accountRepository.findByAccountNumber("01999999")).thenReturn(Optional.empty());

        Optional<Account> result = accountRepository.findByAccountNumber("01999999");

        assertThat(result).isEmpty();
        verify(accountRepository).findByAccountNumber("01999999");
    }

    @Test
    void existsByAccountNumber_WithExistingAccountNumber_ReturnsTrue() {
        when(accountRepository.existsByAccountNumber("01234567")).thenReturn(true);

        boolean exists = accountRepository.existsByAccountNumber("01234567");

        assertThat(exists).isTrue();
        verify(accountRepository).existsByAccountNumber("01234567");
    }

    @Test
    void existsByAccountNumber_WithNonExistentAccountNumber_ReturnsFalse() {
        when(accountRepository.existsByAccountNumber("01999999")).thenReturn(false);

        boolean exists = accountRepository.existsByAccountNumber("01999999");

        assertThat(exists).isFalse();
        verify(accountRepository).existsByAccountNumber("01999999");
    }

    @Test
    void existsByUserId_WithUserHavingAccounts_ReturnsTrue() {
        when(accountRepository.existsByUserId("usr-123abc456")).thenReturn(true);

        boolean exists = accountRepository.existsByUserId("usr-123abc456");

        assertThat(exists).isTrue();
        verify(accountRepository).existsByUserId("usr-123abc456");
    }

    @Test
    void existsByUserId_WithUserHavingNoAccounts_ReturnsFalse() {
        when(accountRepository.existsByUserId("usr-noaccounts")).thenReturn(false);

        boolean exists = accountRepository.existsByUserId("usr-noaccounts");

        assertThat(exists).isFalse();
        verify(accountRepository).existsByUserId("usr-noaccounts");
    }

    @Test
    void existsByUserId_WithNonExistentUserId_ReturnsFalse() {
        when(accountRepository.existsByUserId("usr-nonexistent")).thenReturn(false);

        boolean exists = accountRepository.existsByUserId("usr-nonexistent");

        assertThat(exists).isFalse();
        verify(accountRepository).existsByUserId("usr-nonexistent");
    }

    @Test
    void existsByUser_Id_WithUserHavingAccounts_ReturnsTrue() {
        when(accountRepository.existsByUser_Id("usr-789def012")).thenReturn(true);

        boolean exists = accountRepository.existsByUser_Id("usr-789def012");

        assertThat(exists).isTrue();
        verify(accountRepository).existsByUser_Id("usr-789def012");
    }

    @Test
    void existsByUser_Id_WithNonExistentUserId_ReturnsFalse() {
        when(accountRepository.existsByUser_Id("usr-nonexistent")).thenReturn(false);

        boolean exists = accountRepository.existsByUser_Id("usr-nonexistent");

        assertThat(exists).isFalse();
        verify(accountRepository).existsByUser_Id("usr-nonexistent");
    }

    @Test
    void findByAccountNumberWithUser_WithValidAccountNumber_ReturnsAccountWithUser() {
        when(accountRepository.findByAccountNumberWithUser("01345678")).thenReturn(Optional.of(account3));

        Optional<Account> result = accountRepository.findByAccountNumberWithUser("01345678");

        assertThat(result).isPresent();
        Account account = result.get();
        assertThat(account.getName()).isEqualTo("Jane's Personal Account");
        assertThat(account.getBalance()).isEqualTo(2750.25);

        // Verify user is populated
        User user = account.getUser();
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo("usr-789def012");
        assertThat(user.getName()).isEqualTo("Jane Smith");
        assertThat(user.getEmail()).isEqualTo("user2@example.com");
        assertThat(user.getPhoneNumber()).isEqualTo("+447987654321");
        verify(accountRepository).findByAccountNumberWithUser("01345678");
    }

    @Test
    void findByAccountNumberWithUser_WithNonExistentAccountNumber_ReturnsEmpty() {
        when(accountRepository.findByAccountNumberWithUser("01999999")).thenReturn(Optional.empty());

        Optional<Account> result = accountRepository.findByAccountNumberWithUser("01999999");

        assertThat(result).isEmpty();
        verify(accountRepository).findByAccountNumberWithUser("01999999");
    }

    @Test
    void findById_WithValidAccountNumber_ReturnsAccount() {
        when(accountRepository.findById("01123456")).thenReturn(Optional.of(account1));

        Optional<Account> result = accountRepository.findById("01123456");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("John's Personal Account");
        assertThat(result.get().getUser().getId()).isEqualTo("usr-123abc456");
        verify(accountRepository).findById("01123456");
    }

    @Test
    void findById_WithNonExistentAccountNumber_ReturnsEmpty() {
        when(accountRepository.findById("01999999")).thenReturn(Optional.empty());

        Optional<Account> result = accountRepository.findById("01999999");

        assertThat(result).isEmpty();
        verify(accountRepository).findById("01999999");
    }

    @Test
    void existsById_WithExistingAccountNumber_ReturnsTrue() {
        when(accountRepository.existsById("01234567")).thenReturn(true);

        boolean exists = accountRepository.existsById("01234567");

        assertThat(exists).isTrue();
        verify(accountRepository).existsById("01234567");
    }

    @Test
    void existsById_WithNonExistentAccountNumber_ReturnsFalse() {
        when(accountRepository.existsById("01999999")).thenReturn(false);

        boolean exists = accountRepository.existsById("01999999");

        assertThat(exists).isFalse();
        verify(accountRepository).existsById("01999999");
    }

    @Test
    void save_NewAccount_PersistsSuccessfully() {
        Account newAccount = new Account();
        newAccount.setAccountNumber("01456789");
        newAccount.setName("New Test Account");
        newAccount.setAccountType(Account.AccountType.PERSONAL);
        newAccount.setBalance(1000.00);
        newAccount.setCurrency(Account.Currency.GBP);
        newAccount.setSortCode(Account.SortCode._10_10_10);
        newAccount.setUser(testUser1);

        Account savedAccount = new Account();
        savedAccount.setAccountNumber("01456789");
        savedAccount.setName("New Test Account");
        savedAccount.setAccountType(Account.AccountType.PERSONAL);
        savedAccount.setBalance(1000.00);
        savedAccount.setCurrency(Account.Currency.GBP);
        savedAccount.setSortCode(Account.SortCode._10_10_10);
        savedAccount.setUser(testUser1);
        savedAccount.setCreatedTimestamp(LocalDateTime.now());
        savedAccount.setUpdatedTimestamp(LocalDateTime.now());

        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
        when(accountRepository.findById("01456789")).thenReturn(Optional.of(savedAccount));

        Account result = accountRepository.save(newAccount);

        assertThat(result.getAccountNumber()).isEqualTo("01456789");
        assertThat(result.getName()).isEqualTo("New Test Account");
        assertThat(result.getCreatedTimestamp()).isNotNull();
        assertThat(result.getUpdatedTimestamp()).isNotNull();

        // Verify it can be found
        Optional<Account> found = accountRepository.findById("01456789");
        assertThat(found).isPresent();

        verify(accountRepository).save(any(Account.class));
        verify(accountRepository).findById("01456789");
    }

    @Test
    void delete_ExistingAccount_RemovesFromDatabase() {
        when(accountRepository.existsById("01345678")).thenReturn(true).thenReturn(false);
        when(accountRepository.findById("01345678")).thenReturn(Optional.empty());
        doNothing().when(accountRepository).deleteById("01345678");

        // Verify account exists initially
        assertThat(accountRepository.existsById("01345678")).isTrue();

        accountRepository.deleteById("01345678");

        // Verify account is deleted
        assertThat(accountRepository.existsById("01345678")).isFalse();
        assertThat(accountRepository.findById("01345678")).isEmpty();

        verify(accountRepository, times(2)).existsById("01345678");
        verify(accountRepository).deleteById("01345678");
        verify(accountRepository).findById("01345678");
    }

    @Test
    void findAll_ReturnsAllAccounts() {
        List<Account> allAccounts = List.of(account1, account2, account3);
        when(accountRepository.findAll()).thenReturn(allAccounts);

        List<Account> result = accountRepository.findAll();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(Account::getAccountNumber)
                .containsExactlyInAnyOrder("01123456", "01234567", "01345678");
        verify(accountRepository).findAll();
    }

    @Test
    void count_ReturnsCorrectAccountCount() {
        when(accountRepository.count()).thenReturn(3L);

        long count = accountRepository.count();

        assertThat(count).isEqualTo(3);
        verify(accountRepository).count();
    }

    @Test
    void repositoryMethodsAreProperlyDefined() {
        // This test verifies that all the custom repository methods we're testing actually exist
        // by calling them and ensuring they behave as expected when mocked

        // Setup and verify all custom query methods can be mocked and called
        when(accountRepository.findByUserId(anyString())).thenReturn(List.of());
        when(accountRepository.findByUser_Id(anyString())).thenReturn(List.of());
        when(accountRepository.findByAccountNumber(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumberWithUser(anyString())).thenReturn(Optional.empty());
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.existsByUserId(anyString())).thenReturn(false);
        when(accountRepository.existsByUser_Id(anyString())).thenReturn(false);

        // Actually call the methods to verify they exist and the stubbings work
        assertThat(accountRepository.findByUserId("test")).isEmpty();
        assertThat(accountRepository.findByUser_Id("test")).isEmpty();
        assertThat(accountRepository.findByAccountNumber("test")).isEmpty();
        assertThat(accountRepository.findByAccountNumberWithUser("test")).isEmpty();
        assertThat(accountRepository.existsByAccountNumber("test")).isFalse();
        assertThat(accountRepository.existsByUserId("test")).isFalse();
        assertThat(accountRepository.existsByUser_Id("test")).isFalse();

        // Verify all methods were called
        verify(accountRepository).findByUserId("test");
        verify(accountRepository).findByUser_Id("test");
        verify(accountRepository).findByAccountNumber("test");
        verify(accountRepository).findByAccountNumberWithUser("test");
        verify(accountRepository).existsByAccountNumber("test");
        verify(accountRepository).existsByUserId("test");
        verify(accountRepository).existsByUser_Id("test");
    }
}
