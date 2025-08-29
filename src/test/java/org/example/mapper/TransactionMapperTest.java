package org.example.mapper;

import org.example.entity.Account;
import org.example.entity.Transaction;
import org.example.entity.User;
import org.example.model.CreateTransactionRequest;
import org.example.model.TransactionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionMapperTest {

    private TransactionMapper transactionMapper;

    @BeforeEach
    void setUp() {
        transactionMapper = Mappers.getMapper(TransactionMapper.class);
    }

    @Test
    void toEntityMapsCreateTransactionRequestCorrectly() {
        CreateTransactionRequest request = new CreateTransactionRequest()
                .amount(150.75)
                .currency(CreateTransactionRequest.CurrencyEnum.GBP)
                .type(CreateTransactionRequest.TypeEnum.DEPOSIT)
                .reference("Test deposit transaction");

        Transaction transaction = transactionMapper.toEntity(request);

        assertThat(transaction.getAmount()).isEqualTo(150.75);
        assertThat(transaction.getCurrency()).isEqualTo(Transaction.Currency.GBP);
        assertThat(transaction.getType()).isEqualTo(Transaction.TransactionType.DEPOSIT);
        assertThat(transaction.getReference()).isEqualTo("Test deposit transaction");
    }

    @Test
    void toEntityIgnoresSystemGeneratedFields() {
        CreateTransactionRequest request = new CreateTransactionRequest()
                .amount(200.00)
                .currency(CreateTransactionRequest.CurrencyEnum.GBP)
                .type(CreateTransactionRequest.TypeEnum.WITHDRAWAL)
                .reference("System test");

        Transaction transaction = transactionMapper.toEntity(request);

        assertThat(transaction.getId()).isNull();
        assertThat(transaction.getAccount()).isNull();
        assertThat(transaction.getCreatedTimestamp()).isNull();
    }

    @Test
    void toEntityHandlesOptionalReferenceField() {
        CreateTransactionRequest request = new CreateTransactionRequest()
                .amount(99.99)
                .currency(CreateTransactionRequest.CurrencyEnum.GBP)
                .type(CreateTransactionRequest.TypeEnum.DEPOSIT);

        Transaction transaction = transactionMapper.toEntity(request);

        assertThat(transaction.getAmount()).isEqualTo(99.99);
        assertThat(transaction.getCurrency()).isEqualTo(Transaction.Currency.GBP);
        assertThat(transaction.getType()).isEqualTo(Transaction.TransactionType.DEPOSIT);
        assertThat(transaction.getReference()).isNull();
    }

    @Test
    void toEntityMapsWithdrawalTypeCorrectly() {
        CreateTransactionRequest request = new CreateTransactionRequest()
                .amount(500.00)
                .currency(CreateTransactionRequest.CurrencyEnum.GBP)
                .type(CreateTransactionRequest.TypeEnum.WITHDRAWAL)
                .reference("Withdrawal test");

        Transaction transaction = transactionMapper.toEntity(request);

        assertThat(transaction.getType()).isEqualTo(Transaction.TransactionType.WITHDRAWAL);
        assertThat(transaction.getAmount()).isEqualTo(500.00);
        assertThat(transaction.getCurrency()).isEqualTo(Transaction.Currency.GBP);
    }

    @Test
    void toResponseMapsTransactionEntityCorrectly() {
        User user = new User();
        user.setId("usr-123abc456");

        Account account = new Account();
        account.setAccountNumber("01234567");
        account.setUser(user);

        Transaction transaction = new Transaction();
        transaction.setId("tan-987def654");
        transaction.setAmount(250.50);
        transaction.setCurrency(Transaction.Currency.GBP);
        transaction.setType(Transaction.TransactionType.DEPOSIT);
        transaction.setReference("Response mapping test");
        transaction.setCreatedTimestamp(LocalDateTime.of(2025, 8, 29, 15, 30));
        transaction.setAccount(account);

        TransactionResponse response = transactionMapper.toResponse(transaction);

        assertThat(response.getId()).isEqualTo("tan-987def654");
        assertThat(response.getAmount()).isEqualTo(250.50);
        assertThat(response.getCurrency()).isEqualTo(TransactionResponse.CurrencyEnum.GBP);
        assertThat(response.getType()).isEqualTo(TransactionResponse.TypeEnum.DEPOSIT);
        assertThat(response.getReference()).isEqualTo("Response mapping test");
        assertThat(response.getCreatedTimestamp()).isEqualTo(LocalDateTime.of(2025, 8, 29, 15, 30));
        assertThat(response.getUserId()).isEqualTo("usr-123abc456");
    }

    @Test
    void toResponseMapsWithdrawalTypeCorrectly() {
        User user = new User();
        user.setId("usr-789xyz012");

        Account account = new Account();
        account.setAccountNumber("01765432");
        account.setUser(user);

        Transaction transaction = new Transaction();
        transaction.setId("tan-321fed987");
        transaction.setAmount(75.25);
        transaction.setCurrency(Transaction.Currency.GBP);
        transaction.setType(Transaction.TransactionType.WITHDRAWAL);
        transaction.setReference("ATM withdrawal");
        transaction.setCreatedTimestamp(LocalDateTime.of(2025, 8, 29, 12, 15));
        transaction.setAccount(account);

        TransactionResponse response = transactionMapper.toResponse(transaction);

        assertThat(response.getType()).isEqualTo(TransactionResponse.TypeEnum.WITHDRAWAL);
        assertThat(response.getAmount()).isEqualTo(75.25);
        assertThat(response.getUserId()).isEqualTo("usr-789xyz012");
    }

    @Test
    void toResponseHandlesNullReference() {
        User user = new User();
        user.setId("usr-nullref123");

        Account account = new Account();
        account.setAccountNumber("01111111");
        account.setUser(user);

        Transaction transaction = new Transaction();
        transaction.setId("tan-nullref456");
        transaction.setAmount(100.00);
        transaction.setCurrency(Transaction.Currency.GBP);
        transaction.setType(Transaction.TransactionType.DEPOSIT);
        transaction.setReference(null);
        transaction.setCreatedTimestamp(LocalDateTime.now());
        transaction.setAccount(account);

        TransactionResponse response = transactionMapper.toResponse(transaction);

        assertThat(response.getReference()).isNull();
        assertThat(response.getId()).isEqualTo("tan-nullref456");
        assertThat(response.getUserId()).isEqualTo("usr-nullref123");
    }

    @Test
    void toResponseListMapsMultipleTransactionsCorrectly() {
        User user = new User();
        user.setId("usr-listtest");

        Account account = new Account();
        account.setAccountNumber("01888888");
        account.setUser(user);

        Transaction transaction1 = new Transaction();
        transaction1.setId("tan-first123");
        transaction1.setAmount(100.00);
        transaction1.setCurrency(Transaction.Currency.GBP);
        transaction1.setType(Transaction.TransactionType.DEPOSIT);
        transaction1.setReference("First transaction");
        transaction1.setCreatedTimestamp(LocalDateTime.of(2025, 8, 29, 10, 0));
        transaction1.setAccount(account);

        Transaction transaction2 = new Transaction();
        transaction2.setId("tan-second456");
        transaction2.setAmount(50.00);
        transaction2.setCurrency(Transaction.Currency.GBP);
        transaction2.setType(Transaction.TransactionType.WITHDRAWAL);
        transaction2.setReference("Second transaction");
        transaction2.setCreatedTimestamp(LocalDateTime.of(2025, 8, 29, 11, 0));
        transaction2.setAccount(account);

        List<Transaction> transactions = List.of(transaction1, transaction2);
        List<TransactionResponse> responses = transactionMapper.toResponseList(transactions);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo("tan-first123");
        assertThat(responses.get(0).getType()).isEqualTo(TransactionResponse.TypeEnum.DEPOSIT);
        assertThat(responses.get(1).getId()).isEqualTo("tan-second456");
        assertThat(responses.get(1).getType()).isEqualTo(TransactionResponse.TypeEnum.WITHDRAWAL);
    }

    @Test
    void toResponseListHandlesEmptyList() {
        List<Transaction> emptyTransactions = List.of();
        List<TransactionResponse> responses = transactionMapper.toResponseList(emptyTransactions);

        assertThat(responses).isEmpty();
    }

    @Test
    void mapTransactionTypeHandlesNullValue() {
        TransactionResponse.TypeEnum result = transactionMapper.mapTransactionType(null);

        assertThat(result).isNull();
    }

    @Test
    void mapTransactionTypeConvertsDepositCorrectly() {
        TransactionResponse.TypeEnum result = transactionMapper.mapTransactionType(Transaction.TransactionType.DEPOSIT);

        assertThat(result).isEqualTo(TransactionResponse.TypeEnum.DEPOSIT);
    }

    @Test
    void mapTransactionTypeConvertsWithdrawalCorrectly() {
        TransactionResponse.TypeEnum result = transactionMapper.mapTransactionType(Transaction.TransactionType.WITHDRAWAL);

        assertThat(result).isEqualTo(TransactionResponse.TypeEnum.WITHDRAWAL);
    }

    @Test
    void mapRequestTypeHandlesNullValue() {
        Transaction.TransactionType result = transactionMapper.mapRequestType(null);

        assertThat(result).isNull();
    }

    @Test
    void mapRequestTypeConvertsDepositCorrectly() {
        Transaction.TransactionType result = transactionMapper.mapRequestType(CreateTransactionRequest.TypeEnum.DEPOSIT);

        assertThat(result).isEqualTo(Transaction.TransactionType.DEPOSIT);
    }

    @Test
    void mapRequestTypeConvertsWithdrawalCorrectly() {
        Transaction.TransactionType result = transactionMapper.mapRequestType(CreateTransactionRequest.TypeEnum.WITHDRAWAL);

        assertThat(result).isEqualTo(Transaction.TransactionType.WITHDRAWAL);
    }

    @Test
    void mapCurrencyHandlesNullValue() {
        TransactionResponse.CurrencyEnum result = transactionMapper.mapCurrency(null);

        assertThat(result).isNull();
    }

    @Test
    void mapCurrencyConvertsGbpCorrectly() {
        TransactionResponse.CurrencyEnum result = transactionMapper.mapCurrency(Transaction.Currency.GBP);

        assertThat(result).isEqualTo(TransactionResponse.CurrencyEnum.GBP);
    }

    @Test
    void mapRequestCurrencyHandlesNullValue() {
        Transaction.Currency result = transactionMapper.mapRequestCurrency(null);

        assertThat(result).isNull();
    }

    @Test
    void mapRequestCurrencyConvertsGbpCorrectly() {
        Transaction.Currency result = transactionMapper.mapRequestCurrency(CreateTransactionRequest.CurrencyEnum.GBP);

        assertThat(result).isEqualTo(Transaction.Currency.GBP);
    }

    @Test
    void mappingHandlesOpenApiPatternCompliantTransactionIds() {
        User user = new User();
        user.setId("usr-AbC123dEf456");

        Account account = new Account();
        account.setAccountNumber("01234567");
        account.setUser(user);

        Transaction transaction = new Transaction();
        transaction.setId("tan-AbC123dEf456GhI789");
        transaction.setAmount(1000.00);
        transaction.setCurrency(Transaction.Currency.GBP);
        transaction.setType(Transaction.TransactionType.DEPOSIT);
        transaction.setReference("Pattern compliance test");
        transaction.setCreatedTimestamp(LocalDateTime.now());
        transaction.setAccount(account);

        TransactionResponse response = transactionMapper.toResponse(transaction);

        assertThat(response.getId()).isEqualTo("tan-AbC123dEf456GhI789");
        assertThat(response.getId()).matches("^tan-[A-Za-z0-9]+$");
        assertThat(response.getUserId()).matches("^usr-[A-Za-z0-9]+$");
    }

    @Test
    void mappingHandlesMinimumValidAmount() {
        CreateTransactionRequest request = new CreateTransactionRequest()
                .amount(0.01)
                .currency(CreateTransactionRequest.CurrencyEnum.GBP)
                .type(CreateTransactionRequest.TypeEnum.DEPOSIT)
                .reference("Minimum amount test");

        Transaction transaction = transactionMapper.toEntity(request);

        assertThat(transaction.getAmount()).isEqualTo(0.01);
        assertThat(transaction.getCurrency()).isEqualTo(Transaction.Currency.GBP);
    }

    @Test
    void mappingHandlesMaximumValidAmount() {
        CreateTransactionRequest request = new CreateTransactionRequest()
                .amount(10000.00)
                .currency(CreateTransactionRequest.CurrencyEnum.GBP)
                .type(CreateTransactionRequest.TypeEnum.WITHDRAWAL)
                .reference("Maximum amount test");

        Transaction transaction = transactionMapper.toEntity(request);

        assertThat(transaction.getAmount()).isEqualTo(10000.00);
        assertThat(transaction.getType()).isEqualTo(Transaction.TransactionType.WITHDRAWAL);
    }

    @Test
    void toResponseExtractsUserIdFromAccountRelationship() {
        User user = new User();
        user.setId("usr-relationship456");

        Account account = new Account();
        account.setAccountNumber("01999999");
        account.setUser(user);

        Transaction transaction = new Transaction();
        transaction.setId("tan-relationship789");
        transaction.setAmount(333.33);
        transaction.setCurrency(Transaction.Currency.GBP);
        transaction.setType(Transaction.TransactionType.DEPOSIT);
        transaction.setCreatedTimestamp(LocalDateTime.now());
        transaction.setAccount(account);

        TransactionResponse response = transactionMapper.toResponse(transaction);

        assertThat(response.getUserId()).isEqualTo("usr-relationship456");
    }
}
