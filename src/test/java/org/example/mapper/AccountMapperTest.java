package org.example.mapper;

import org.example.entity.Account;
import org.example.entity.User;
import org.example.model.BankAccountResponse;
import org.example.model.CreateBankAccountRequest;
import org.example.model.UpdateBankAccountRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccountMapperTest {

    private AccountMapper accountMapper;

    @BeforeEach
    void setUp() {
        accountMapper = Mappers.getMapper(AccountMapper.class);
    }

    @Test
    void toEntityMapsCreateBankAccountRequestCorrectly() {
        CreateBankAccountRequest request = new CreateBankAccountRequest()
                .name("My Savings Account")
                .accountType(CreateBankAccountRequest.AccountTypeEnum.PERSONAL);

        Account account = accountMapper.toEntity(request);

        assertThat(account.getName()).isEqualTo("My Savings Account");
        assertThat(account.getAccountType()).isEqualTo(Account.AccountType.PERSONAL);
        assertThat(account.getCurrency()).isEqualTo(Account.Currency.GBP);
        assertThat(account.getSortCode()).isEqualTo(Account.SortCode._10_10_10);
    }

    @Test
    void toEntityIgnoresSystemGeneratedFields() {
        CreateBankAccountRequest request = new CreateBankAccountRequest()
                .name("Test Account")
                .accountType(CreateBankAccountRequest.AccountTypeEnum.PERSONAL);

        Account account = accountMapper.toEntity(request);

        assertThat(account.getAccountNumber()).isNull();
        assertThat(account.getBalance()).isNull();
        assertThat(account.getUser()).isNull();
        assertThat(account.getTransactions()).isEmpty(); // Changed from isNull() to isEmpty()
        assertThat(account.getCreatedTimestamp()).isNull();
        assertThat(account.getUpdatedTimestamp()).isNull();
    }

    @Test
    void toEntitySetsConstantFieldsCorrectly() {
        CreateBankAccountRequest request = new CreateBankAccountRequest()
                .name("Constant Fields Test")
                .accountType(CreateBankAccountRequest.AccountTypeEnum.PERSONAL);

        Account account = accountMapper.toEntity(request);

        assertThat(account.getCurrency()).isEqualTo(Account.Currency.GBP);
        assertThat(account.getSortCode()).isEqualTo(Account.SortCode._10_10_10);
    }

    @Test
    void toResponseMapsBankAccountEntityCorrectly() {
        User user = new User();
        user.setId("usr-123abc456");

        Account account = new Account();
        account.setAccountNumber("01234567");
        account.setName("Personal Current Account");
        account.setAccountType(Account.AccountType.PERSONAL);
        account.setBalance(1500.75);
        account.setCurrency(Account.Currency.GBP);
        account.setSortCode(Account.SortCode._10_10_10);
        account.setCreatedTimestamp(LocalDateTime.of(2025, 8, 29, 10, 30));
        account.setUpdatedTimestamp(LocalDateTime.of(2025, 8, 29, 15, 45));
        account.setUser(user);
        account.setTransactions(new ArrayList<>());

        BankAccountResponse response = accountMapper.toResponse(account);

        assertThat(response.getAccountNumber()).isEqualTo("01234567");
        assertThat(response.getName()).isEqualTo("Personal Current Account");
        assertThat(response.getAccountType()).isEqualTo(BankAccountResponse.AccountTypeEnum.PERSONAL);
        assertThat(response.getBalance()).isEqualTo(1500.75);
        assertThat(response.getCurrency()).isEqualTo(BankAccountResponse.CurrencyEnum.GBP);
        assertThat(response.getSortCode()).isEqualTo(BankAccountResponse.SortCodeEnum._10_10_10);
        assertThat(response.getCreatedTimestamp()).isEqualTo(LocalDateTime.of(2025, 8, 29, 10, 30));
        assertThat(response.getUpdatedTimestamp()).isEqualTo(LocalDateTime.of(2025, 8, 29, 15, 45));
    }

    @Test
    void toResponseHandlesZeroBalance() {
        Account account = new Account();
        account.setAccountNumber("01000000");
        account.setName("Zero Balance Account");
        account.setAccountType(Account.AccountType.PERSONAL);
        account.setBalance(0.00);
        account.setCurrency(Account.Currency.GBP);
        account.setSortCode(Account.SortCode._10_10_10);
        account.setCreatedTimestamp(LocalDateTime.now());
        account.setUpdatedTimestamp(LocalDateTime.now());

        BankAccountResponse response = accountMapper.toResponse(account);

        assertThat(response.getBalance()).isEqualTo(0.00);
        assertThat(response.getAccountNumber()).isEqualTo("01000000");
    }

    @Test
    void toResponseHandlesMaximumBalance() {
        Account account = new Account();
        account.setAccountNumber("01999999");
        account.setName("Maximum Balance Account");
        account.setAccountType(Account.AccountType.PERSONAL);
        account.setBalance(10000.00);
        account.setCurrency(Account.Currency.GBP);
        account.setSortCode(Account.SortCode._10_10_10);
        account.setCreatedTimestamp(LocalDateTime.now());
        account.setUpdatedTimestamp(LocalDateTime.now());

        BankAccountResponse response = accountMapper.toResponse(account);

        assertThat(response.getBalance()).isEqualTo(10000.00);
        assertThat(response.getAccountNumber()).isEqualTo("01999999");
    }

    @Test
    void toResponseListMapsMultipleAccountsCorrectly() {
        Account account1 = new Account();
        account1.setAccountNumber("01111111");
        account1.setName("First Account");
        account1.setAccountType(Account.AccountType.PERSONAL);
        account1.setBalance(1000.00);
        account1.setCurrency(Account.Currency.GBP);
        account1.setSortCode(Account.SortCode._10_10_10);
        account1.setCreatedTimestamp(LocalDateTime.of(2025, 8, 29, 9, 0));
        account1.setUpdatedTimestamp(LocalDateTime.of(2025, 8, 29, 9, 0));

        Account account2 = new Account();
        account2.setAccountNumber("01222222");
        account2.setName("Second Account");
        account2.setAccountType(Account.AccountType.PERSONAL);
        account2.setBalance(2500.50);
        account2.setCurrency(Account.Currency.GBP);
        account2.setSortCode(Account.SortCode._10_10_10);
        account2.setCreatedTimestamp(LocalDateTime.of(2025, 8, 29, 10, 0));
        account2.setUpdatedTimestamp(LocalDateTime.of(2025, 8, 29, 10, 0));

        List<Account> accounts = List.of(account1, account2);
        List<BankAccountResponse> responses = accountMapper.toResponseList(accounts);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getAccountNumber()).isEqualTo("01111111");
        assertThat(responses.get(0).getName()).isEqualTo("First Account");
        assertThat(responses.get(0).getBalance()).isEqualTo(1000.00);
        assertThat(responses.get(1).getAccountNumber()).isEqualTo("01222222");
        assertThat(responses.get(1).getName()).isEqualTo("Second Account");
        assertThat(responses.get(1).getBalance()).isEqualTo(2500.50);
    }

    @Test
    void toResponseListHandlesEmptyList() {
        List<Account> emptyAccounts = List.of();
        List<BankAccountResponse> responses = accountMapper.toResponseList(emptyAccounts);

        assertThat(responses).isEmpty();
    }

    @Test
    void updateEntityFromRequestUpdatesOnlyProvidedFields() {
        Account existingAccount = new Account();
        existingAccount.setAccountNumber("01333333");
        existingAccount.setName("Original Name");
        existingAccount.setAccountType(Account.AccountType.PERSONAL);
        existingAccount.setBalance(5000.00);
        existingAccount.setCurrency(Account.Currency.GBP);
        existingAccount.setSortCode(Account.SortCode._10_10_10);
        existingAccount.setCreatedTimestamp(LocalDateTime.of(2025, 8, 1, 12, 0));
        existingAccount.setUpdatedTimestamp(LocalDateTime.of(2025, 8, 1, 12, 0));

        UpdateBankAccountRequest updateRequest = new UpdateBankAccountRequest()
                .name("Updated Account Name");

        accountMapper.updateEntityFromRequest(updateRequest, existingAccount);

        assertThat(existingAccount.getName()).isEqualTo("Updated Account Name");
        assertThat(existingAccount.getAccountNumber()).isEqualTo("01333333");
        assertThat(existingAccount.getBalance()).isEqualTo(5000.00);
        assertThat(existingAccount.getCurrency()).isEqualTo(Account.Currency.GBP);
        assertThat(existingAccount.getSortCode()).isEqualTo(Account.SortCode._10_10_10);
        assertThat(existingAccount.getCreatedTimestamp()).isEqualTo(LocalDateTime.of(2025, 8, 1, 12, 0));
        assertThat(existingAccount.getUpdatedTimestamp()).isEqualTo(LocalDateTime.of(2025, 8, 1, 12, 0));
    }

    @Test
    void updateEntityFromRequestUpdatesAccountType() {
        Account existingAccount = new Account();
        existingAccount.setAccountNumber("01444444");
        existingAccount.setName("Type Update Test");
        existingAccount.setAccountType(Account.AccountType.PERSONAL);
        existingAccount.setBalance(1000.00);

        UpdateBankAccountRequest updateRequest = new UpdateBankAccountRequest()
                .accountType(UpdateBankAccountRequest.AccountTypeEnum.PERSONAL);

        accountMapper.updateEntityFromRequest(updateRequest, existingAccount);

        assertThat(existingAccount.getAccountType()).isEqualTo(Account.AccountType.PERSONAL);
        assertThat(existingAccount.getName()).isEqualTo("Type Update Test");
        assertThat(existingAccount.getAccountNumber()).isEqualTo("01444444");
        assertThat(existingAccount.getBalance()).isEqualTo(1000.00);
    }

    @Test
    void updateEntityFromRequestIgnoresSystemFields() {
        Account existingAccount = new Account();
        existingAccount.setAccountNumber("01555555");
        existingAccount.setName("System Fields Test");
        existingAccount.setAccountType(Account.AccountType.PERSONAL);
        existingAccount.setBalance(3000.00);
        existingAccount.setCurrency(Account.Currency.GBP);
        existingAccount.setSortCode(Account.SortCode._10_10_10);
        existingAccount.setCreatedTimestamp(LocalDateTime.of(2025, 7, 1, 10, 0));
        existingAccount.setUpdatedTimestamp(LocalDateTime.of(2025, 7, 1, 10, 0));

        UpdateBankAccountRequest updateRequest = new UpdateBankAccountRequest()
                .name("Updated Name")
                .accountType(UpdateBankAccountRequest.AccountTypeEnum.PERSONAL);

        accountMapper.updateEntityFromRequest(updateRequest, existingAccount);

        assertThat(existingAccount.getAccountNumber()).isEqualTo("01555555");
        assertThat(existingAccount.getBalance()).isEqualTo(3000.00);
        assertThat(existingAccount.getCurrency()).isEqualTo(Account.Currency.GBP);
        assertThat(existingAccount.getSortCode()).isEqualTo(Account.SortCode._10_10_10);
        assertThat(existingAccount.getCreatedTimestamp()).isEqualTo(LocalDateTime.of(2025, 7, 1, 10, 0));
        assertThat(existingAccount.getUpdatedTimestamp()).isEqualTo(LocalDateTime.of(2025, 7, 1, 10, 0));
        assertThat(existingAccount.getName()).isEqualTo("Updated Name");
    }

    @Test
    void updateEntityFromRequestIgnoresNullValues() {
        Account existingAccount = new Account();
        existingAccount.setAccountNumber("01666666");
        existingAccount.setName("Null Values Test");
        existingAccount.setAccountType(Account.AccountType.PERSONAL);
        existingAccount.setBalance(2000.00);

        UpdateBankAccountRequest updateRequest = new UpdateBankAccountRequest()
                .name("Updated Name Only");

        accountMapper.updateEntityFromRequest(updateRequest, existingAccount);

        assertThat(existingAccount.getName()).isEqualTo("Updated Name Only");
        assertThat(existingAccount.getAccountType()).isEqualTo(Account.AccountType.PERSONAL);
        assertThat(existingAccount.getAccountNumber()).isEqualTo("01666666");
        assertThat(existingAccount.getBalance()).isEqualTo(2000.00);
    }

    @Test
    void mapAccountTypeHandlesNullValue() {
        BankAccountResponse.AccountTypeEnum result = accountMapper.mapAccountType(null);

        assertThat(result).isNull();
    }

    @Test
    void mapAccountTypeConvertsPersonalCorrectly() {
        BankAccountResponse.AccountTypeEnum result = accountMapper.mapAccountType(Account.AccountType.PERSONAL);

        assertThat(result).isEqualTo(BankAccountResponse.AccountTypeEnum.PERSONAL);
    }

    @Test
    void mapCurrencyHandlesNullValue() {
        BankAccountResponse.CurrencyEnum result = accountMapper.mapCurrency(null);

        assertThat(result).isNull();
    }

    @Test
    void mapCurrencyConvertsGbpCorrectly() {
        BankAccountResponse.CurrencyEnum result = accountMapper.mapCurrency(Account.Currency.GBP);

        assertThat(result).isEqualTo(BankAccountResponse.CurrencyEnum.GBP);
    }

    @Test
    void mapSortCodeHandlesNullValue() {
        BankAccountResponse.SortCodeEnum result = accountMapper.mapSortCode(null);

        assertThat(result).isNull();
    }

    @Test
    void mapSortCodeConverts10_10_10Correctly() {
        BankAccountResponse.SortCodeEnum result = accountMapper.mapSortCode(Account.SortCode._10_10_10);

        assertThat(result).isEqualTo(BankAccountResponse.SortCodeEnum._10_10_10);
    }

    @Test
    void mappingHandlesOpenApiPatternCompliantAccountNumbers() {
        Account account = new Account();
        account.setAccountNumber("01123456");
        account.setName("Pattern Compliance Test");
        account.setAccountType(Account.AccountType.PERSONAL);
        account.setBalance(500.00);
        account.setCurrency(Account.Currency.GBP);
        account.setSortCode(Account.SortCode._10_10_10);
        account.setCreatedTimestamp(LocalDateTime.now());
        account.setUpdatedTimestamp(LocalDateTime.now());

        BankAccountResponse response = accountMapper.toResponse(account);

        assertThat(response.getAccountNumber()).isEqualTo("01123456");
        assertThat(response.getAccountNumber()).matches("^01\\d{6}$");
        assertThat(response.getName()).isEqualTo("Pattern Compliance Test");
    }

    @Test
    void mappingHandlesMinimumValidBalance() {
        Account account = new Account();
        account.setAccountNumber("01000001");
        account.setName("Minimum Balance Test");
        account.setAccountType(Account.AccountType.PERSONAL);
        account.setBalance(0.01);
        account.setCurrency(Account.Currency.GBP);
        account.setSortCode(Account.SortCode._10_10_10);
        account.setCreatedTimestamp(LocalDateTime.now());
        account.setUpdatedTimestamp(LocalDateTime.now());

        BankAccountResponse response = accountMapper.toResponse(account);

        assertThat(response.getBalance()).isEqualTo(0.01);
        assertThat(response.getBalance()).isGreaterThanOrEqualTo(0.00);
        assertThat(response.getBalance()).isLessThanOrEqualTo(10000.00);
    }

    @Test
    void mappingHandlesAccountNameVariations() {
        CreateBankAccountRequest request1 = new CreateBankAccountRequest()
                .name("Personal Bank Account")
                .accountType(CreateBankAccountRequest.AccountTypeEnum.PERSONAL);

        CreateBankAccountRequest request2 = new CreateBankAccountRequest()
                .name("My Account")
                .accountType(CreateBankAccountRequest.AccountTypeEnum.PERSONAL);

        Account account1 = accountMapper.toEntity(request1);
        Account account2 = accountMapper.toEntity(request2);

        assertThat(account1.getName()).isEqualTo("Personal Bank Account");
        assertThat(account2.getName()).isEqualTo("My Account");
    }
}
