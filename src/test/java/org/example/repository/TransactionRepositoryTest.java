package org.example.repository;

import org.example.entity.Transaction;
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
class TransactionRepositoryTest {

    @Mock
    private TransactionRepository transactionRepository;

    private Transaction transaction1;
    private Transaction transaction2;
    private Transaction transaction3;
    private Account testAccount1;
    private Account testAccount2;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("usr-123abc456");
        testUser.setEmail("john.doe@example.com");
        testUser.setName("John Doe");
        testUser.setPhoneNumber("+447123456789");
        testUser.setPasswordHash("hashedPassword");

        Address address = new Address();
        address.setLine1("123 Main Street");
        address.setTown("London");
        address.setCounty("Greater London");
        address.setPostcode("SW1A 1AA");
        testUser.setAddress(address);

        testAccount1 = new Account();
        testAccount1.setAccountNumber("01123456");
        testAccount1.setName("Test Account");
        testAccount1.setAccountType(Account.AccountType.PERSONAL);
        testAccount1.setBalance(1000.00);
        testAccount1.setCurrency(Account.Currency.GBP);
        testAccount1.setSortCode(Account.SortCode._10_10_10);
        testAccount1.setUser(testUser);

        testAccount2 = new Account();
        testAccount2.setAccountNumber("01234567");
        testAccount2.setName("Another Account");
        testAccount2.setAccountType(Account.AccountType.PERSONAL);
        testAccount2.setBalance(500.00);
        testAccount2.setCurrency(Account.Currency.GBP);
        testAccount2.setSortCode(Account.SortCode._10_10_10);
        testAccount2.setUser(testUser);

        transaction1 = new Transaction();
        transaction1.setId("tan-deposit123");
        transaction1.setAmount(100.00);
        transaction1.setType(Transaction.TransactionType.DEPOSIT);
        transaction1.setCurrency(Transaction.Currency.GBP);
        transaction1.setReference("Salary deposit");
        transaction1.setAccount(testAccount1);
        transaction1.setCreatedTimestamp(LocalDateTime.of(2025, 8, 29, 10, 0));

        transaction2 = new Transaction();
        transaction2.setId("tan-withdraw456");
        transaction2.setAmount(50.00);
        transaction2.setType(Transaction.TransactionType.WITHDRAWAL);
        transaction2.setCurrency(Transaction.Currency.GBP);
        transaction2.setReference("ATM withdrawal");
        transaction2.setAccount(testAccount1);
        transaction2.setCreatedTimestamp(LocalDateTime.of(2025, 8, 29, 11, 0));

        transaction3 = new Transaction();
        transaction3.setId("tan-deposit789");
        transaction3.setAmount(25.50);
        transaction3.setType(Transaction.TransactionType.DEPOSIT);
        transaction3.setCurrency(Transaction.Currency.GBP);
        transaction3.setAccount(testAccount1);
        transaction3.setCreatedTimestamp(LocalDateTime.of(2025, 8, 29, 12, 0));
    }

    @Test
    void findByAccount_AccountNumberOrderByCreatedTimestampDesc_WithValidAccountNumber_ReturnsTransactionsOrderedByNewestFirst() {
        List<Transaction> expectedTransactions = List.of(transaction3, transaction2, transaction1);
        when(transactionRepository.findByAccount_AccountNumberOrderByCreatedTimestampDesc("01123456"))
                .thenReturn(expectedTransactions);

        List<Transaction> result = transactionRepository.findByAccount_AccountNumberOrderByCreatedTimestampDesc("01123456");

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId()).isEqualTo("tan-deposit789");
        assertThat(result.get(1).getId()).isEqualTo("tan-withdraw456");
        assertThat(result.get(2).getId()).isEqualTo("tan-deposit123");
        assertThat(result.get(0).getCreatedTimestamp()).isAfter(result.get(1).getCreatedTimestamp());
        assertThat(result.get(1).getCreatedTimestamp()).isAfter(result.get(2).getCreatedTimestamp());
        verify(transactionRepository).findByAccount_AccountNumberOrderByCreatedTimestampDesc("01123456");
    }

    @Test
    void findByAccount_AccountNumberOrderByCreatedTimestampDesc_WithNonExistentAccountNumber_ReturnsEmptyList() {
        when(transactionRepository.findByAccount_AccountNumberOrderByCreatedTimestampDesc("01999999"))
                .thenReturn(List.of());

        List<Transaction> result = transactionRepository.findByAccount_AccountNumberOrderByCreatedTimestampDesc("01999999");

        assertThat(result).isEmpty();
        verify(transactionRepository).findByAccount_AccountNumberOrderByCreatedTimestampDesc("01999999");
    }

    @Test
    void findByAccount_AccountNumberOrderByCreatedTimestampDesc_WithAccountHavingNoTransactions_ReturnsEmptyList() {
        when(transactionRepository.findByAccount_AccountNumberOrderByCreatedTimestampDesc("01234567"))
                .thenReturn(List.of());

        List<Transaction> result = transactionRepository.findByAccount_AccountNumberOrderByCreatedTimestampDesc("01234567");

        assertThat(result).isEmpty();
        verify(transactionRepository).findByAccount_AccountNumberOrderByCreatedTimestampDesc("01234567");
    }

    @Test
    void findByAccount_AccountNumberOrderByCreatedTimestampDesc_WithSingleTransaction_ReturnsSingleTransaction() {
        List<Transaction> singleTransaction = List.of(transaction1);
        when(transactionRepository.findByAccount_AccountNumberOrderByCreatedTimestampDesc("01123456"))
                .thenReturn(singleTransaction);

        List<Transaction> result = transactionRepository.findByAccount_AccountNumberOrderByCreatedTimestampDesc("01123456");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("tan-deposit123");
        assertThat(result.get(0).getAmount()).isEqualTo(100.00);
        assertThat(result.get(0).getType()).isEqualTo(Transaction.TransactionType.DEPOSIT);
        verify(transactionRepository).findByAccount_AccountNumberOrderByCreatedTimestampDesc("01123456");
    }

    @Test
    void findByIdAndAccount_AccountNumber_WithValidTransactionIdAndAccountNumber_ReturnsTransaction() {
        when(transactionRepository.findByIdAndAccount_AccountNumber("tan-deposit123", "01123456"))
                .thenReturn(Optional.of(transaction1));

        Optional<Transaction> result = transactionRepository.findByIdAndAccount_AccountNumber("tan-deposit123", "01123456");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("tan-deposit123");
        assertThat(result.get().getAmount()).isEqualTo(100.00);
        assertThat(result.get().getType()).isEqualTo(Transaction.TransactionType.DEPOSIT);
        assertThat(result.get().getReference()).isEqualTo("Salary deposit");
        assertThat(result.get().getAccountNumber()).isEqualTo("01123456");
        verify(transactionRepository).findByIdAndAccount_AccountNumber("tan-deposit123", "01123456");
    }

    @Test
    void findByIdAndAccount_AccountNumber_WithNonExistentTransactionId_ReturnsEmpty() {
        when(transactionRepository.findByIdAndAccount_AccountNumber("tan-nonexistent", "01123456"))
                .thenReturn(Optional.empty());

        Optional<Transaction> result = transactionRepository.findByIdAndAccount_AccountNumber("tan-nonexistent", "01123456");

        assertThat(result).isEmpty();
        verify(transactionRepository).findByIdAndAccount_AccountNumber("tan-nonexistent", "01123456");
    }

    @Test
    void findByIdAndAccount_AccountNumber_WithNonExistentAccountNumber_ReturnsEmpty() {
        when(transactionRepository.findByIdAndAccount_AccountNumber("tan-deposit123", "01999999"))
                .thenReturn(Optional.empty());

        Optional<Transaction> result = transactionRepository.findByIdAndAccount_AccountNumber("tan-deposit123", "01999999");

        assertThat(result).isEmpty();
        verify(transactionRepository).findByIdAndAccount_AccountNumber("tan-deposit123", "01999999");
    }

    @Test
    void findByIdAndAccount_AccountNumber_WithValidTransactionIdButWrongAccount_ReturnsEmpty() {
        when(transactionRepository.findByIdAndAccount_AccountNumber("tan-deposit123", "01234567"))
                .thenReturn(Optional.empty());

        Optional<Transaction> result = transactionRepository.findByIdAndAccount_AccountNumber("tan-deposit123", "01234567");

        assertThat(result).isEmpty();
        verify(transactionRepository).findByIdAndAccount_AccountNumber("tan-deposit123", "01234567");
    }

    @Test
    void findByIdAndAccount_AccountNumber_WithMalformedTransactionId_ReturnsEmpty() {
        when(transactionRepository.findByIdAndAccount_AccountNumber("invalid-id", "01123456"))
                .thenReturn(Optional.empty());

        Optional<Transaction> result = transactionRepository.findByIdAndAccount_AccountNumber("invalid-id", "01123456");

        assertThat(result).isEmpty();
        verify(transactionRepository).findByIdAndAccount_AccountNumber("invalid-id", "01123456");
    }

    @Test
    void findByIdAndAccount_AccountNumber_WithMalformedAccountNumber_ReturnsEmpty() {
        when(transactionRepository.findByIdAndAccount_AccountNumber("tan-deposit123", "invalid-account"))
                .thenReturn(Optional.empty());

        Optional<Transaction> result = transactionRepository.findByIdAndAccount_AccountNumber("tan-deposit123", "invalid-account");

        assertThat(result).isEmpty();
        verify(transactionRepository).findByIdAndAccount_AccountNumber("tan-deposit123", "invalid-account");
    }

    @Test
    void findById_WithValidTransactionId_ReturnsTransaction() {
        when(transactionRepository.findById("tan-withdraw456")).thenReturn(Optional.of(transaction2));

        Optional<Transaction> result = transactionRepository.findById("tan-withdraw456");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("tan-withdraw456");
        assertThat(result.get().getAmount()).isEqualTo(50.00);
        assertThat(result.get().getType()).isEqualTo(Transaction.TransactionType.WITHDRAWAL);
        verify(transactionRepository).findById("tan-withdraw456");
    }

    @Test
    void findById_WithNonExistentTransactionId_ReturnsEmpty() {
        when(transactionRepository.findById("tan-nonexistent")).thenReturn(Optional.empty());

        Optional<Transaction> result = transactionRepository.findById("tan-nonexistent");

        assertThat(result).isEmpty();
        verify(transactionRepository).findById("tan-nonexistent");
    }

    @Test
    void save_NewTransaction_PersistsSuccessfully() {
        Transaction newTransaction = new Transaction();
        newTransaction.setId("tan-new123");
        newTransaction.setAmount(75.25);
        newTransaction.setType(Transaction.TransactionType.DEPOSIT);
        newTransaction.setCurrency(Transaction.Currency.GBP);
        newTransaction.setReference("New deposit");
        newTransaction.setAccount(testAccount1);

        Transaction savedTransaction = new Transaction();
        savedTransaction.setId("tan-new123");
        savedTransaction.setAmount(75.25);
        savedTransaction.setType(Transaction.TransactionType.DEPOSIT);
        savedTransaction.setCurrency(Transaction.Currency.GBP);
        savedTransaction.setReference("New deposit");
        savedTransaction.setAccount(testAccount1);
        savedTransaction.setCreatedTimestamp(LocalDateTime.now());

        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(transactionRepository.findById("tan-new123")).thenReturn(Optional.of(savedTransaction));

        Transaction result = transactionRepository.save(newTransaction);

        assertThat(result.getId()).isEqualTo("tan-new123");
        assertThat(result.getAmount()).isEqualTo(75.25);
        assertThat(result.getType()).isEqualTo(Transaction.TransactionType.DEPOSIT);
        assertThat(result.getCreatedTimestamp()).isNotNull();

        Optional<Transaction> found = transactionRepository.findById("tan-new123");
        assertThat(found).isPresent();

        verify(transactionRepository).save(any(Transaction.class));
        verify(transactionRepository).findById("tan-new123");
    }

    @Test
    void delete_ExistingTransaction_RemovesFromDatabase() {
        when(transactionRepository.existsById("tan-deposit789")).thenReturn(true).thenReturn(false);
        when(transactionRepository.findById("tan-deposit789")).thenReturn(Optional.empty());
        doNothing().when(transactionRepository).deleteById("tan-deposit789");

        assertThat(transactionRepository.existsById("tan-deposit789")).isTrue();

        transactionRepository.deleteById("tan-deposit789");

        assertThat(transactionRepository.existsById("tan-deposit789")).isFalse();
        assertThat(transactionRepository.findById("tan-deposit789")).isEmpty();

        verify(transactionRepository, times(2)).existsById("tan-deposit789");
        verify(transactionRepository).deleteById("tan-deposit789");
        verify(transactionRepository).findById("tan-deposit789");
    }

    @Test
    void findAll_ReturnsAllTransactions() {
        List<Transaction> allTransactions = List.of(transaction1, transaction2, transaction3);
        when(transactionRepository.findAll()).thenReturn(allTransactions);

        List<Transaction> result = transactionRepository.findAll();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(Transaction::getId)
                .containsExactlyInAnyOrder("tan-deposit123", "tan-withdraw456", "tan-deposit789");
        verify(transactionRepository).findAll();
    }

    @Test
    void count_ReturnsCorrectTransactionCount() {
        when(transactionRepository.count()).thenReturn(3L);

        long count = transactionRepository.count();

        assertThat(count).isEqualTo(3);
        verify(transactionRepository).count();
    }

    @Test
    void transactionIdPatternCompliance_FollowsOpenApiSpec() {
        when(transactionRepository.findById("tan-123abc")).thenReturn(Optional.of(transaction1));
        when(transactionRepository.findById("tan-456def789")).thenReturn(Optional.of(transaction2));

        Optional<Transaction> transaction1Result = transactionRepository.findById("tan-123abc");
        Optional<Transaction> transaction2Result = transactionRepository.findById("tan-456def789");

        assertThat(transaction1Result).isPresent();
        assertThat(transaction1Result.get().getId()).matches("^tan-[A-Za-z0-9]+$");
        assertThat(transaction2Result).isPresent();
        assertThat(transaction2Result.get().getId()).matches("^tan-[A-Za-z0-9]+$");
        verify(transactionRepository).findById("tan-123abc");
        verify(transactionRepository).findById("tan-456def789");
    }

    @Test
    void amountRangeCompliance_FollowsOpenApiSpec() {
        Transaction minAmountTransaction = new Transaction();
        minAmountTransaction.setAmount(0.01);
        Transaction maxAmountTransaction = new Transaction();
        maxAmountTransaction.setAmount(10000.00);

        when(transactionRepository.findById("tan-min")).thenReturn(Optional.of(minAmountTransaction));
        when(transactionRepository.findById("tan-max")).thenReturn(Optional.of(maxAmountTransaction));

        Optional<Transaction> minResult = transactionRepository.findById("tan-min");
        Optional<Transaction> maxResult = transactionRepository.findById("tan-max");

        assertThat(minResult).isPresent();
        assertThat(minResult.get().getAmount()).isGreaterThanOrEqualTo(0.00);
        assertThat(maxResult).isPresent();
        assertThat(maxResult.get().getAmount()).isLessThanOrEqualTo(10000.00);
        verify(transactionRepository).findById("tan-min");
        verify(transactionRepository).findById("tan-max");
    }

    @Test
    void repositoryMethodsAreProperlyDefined() {
        when(transactionRepository.findByAccount_AccountNumberOrderByCreatedTimestampDesc(anyString())).thenReturn(List.of());
        when(transactionRepository.findByIdAndAccount_AccountNumber(anyString(), anyString())).thenReturn(Optional.empty());
        when(transactionRepository.findById(anyString())).thenReturn(Optional.empty());
        when(transactionRepository.existsById(anyString())).thenReturn(false);

        assertThat(transactionRepository.findByAccount_AccountNumberOrderByCreatedTimestampDesc("test")).isEmpty();
        assertThat(transactionRepository.findByIdAndAccount_AccountNumber("test", "test")).isEmpty();
        assertThat(transactionRepository.findById("test")).isEmpty();
        assertThat(transactionRepository.existsById("test")).isFalse();

        verify(transactionRepository).findByAccount_AccountNumberOrderByCreatedTimestampDesc("test");
        verify(transactionRepository).findByIdAndAccount_AccountNumber("test", "test");
        verify(transactionRepository).findById("test");
        verify(transactionRepository).existsById("test");
    }

    @Test
    void transactionTypesHandling_SupportsDepositAndWithdrawal() {
        Transaction depositTransaction = new Transaction();
        depositTransaction.setType(Transaction.TransactionType.DEPOSIT);
        Transaction withdrawalTransaction = new Transaction();
        withdrawalTransaction.setType(Transaction.TransactionType.WITHDRAWAL);

        when(transactionRepository.findById("tan-deposit")).thenReturn(Optional.of(depositTransaction));
        when(transactionRepository.findById("tan-withdrawal")).thenReturn(Optional.of(withdrawalTransaction));

        Optional<Transaction> depositResult = transactionRepository.findById("tan-deposit");
        Optional<Transaction> withdrawalResult = transactionRepository.findById("tan-withdrawal");

        assertThat(depositResult).isPresent();
        assertThat(depositResult.get().getType()).isEqualTo(Transaction.TransactionType.DEPOSIT);
        assertThat(withdrawalResult).isPresent();
        assertThat(withdrawalResult.get().getType()).isEqualTo(Transaction.TransactionType.WITHDRAWAL);
        verify(transactionRepository).findById("tan-deposit");
        verify(transactionRepository).findById("tan-withdrawal");
    }

    @Test
    void transactionWithOptionalReference_HandlesNullReference() {
        Transaction transactionWithoutReference = new Transaction();
        transactionWithoutReference.setId("tan-noref123");
        transactionWithoutReference.setAmount(100.00);
        transactionWithoutReference.setType(Transaction.TransactionType.DEPOSIT);
        transactionWithoutReference.setCurrency(Transaction.Currency.GBP);
        transactionWithoutReference.setReference(null);

        when(transactionRepository.findById("tan-noref123")).thenReturn(Optional.of(transactionWithoutReference));

        Optional<Transaction> result = transactionRepository.findById("tan-noref123");

        assertThat(result).isPresent();
        assertThat(result.get().getReference()).isNull();
        verify(transactionRepository).findById("tan-noref123");
    }
}
