package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.*;
import org.example.service.AccountService;
import org.example.service.TransactionService;
import org.example.exception.AccountNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AccountController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
    excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org\\.example\\.security\\..*"))
@Import(GlobalExceptionHandler.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @MockBean
    private TransactionService transactionService;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateBankAccountRequest createBankAccountRequest;
    private UpdateBankAccountRequest updateBankAccountRequest;
    private BankAccountResponse bankAccountResponse;
    private ListBankAccountsResponse listBankAccountsResponse;
    private CreateTransactionRequest createTransactionRequest;
    private TransactionResponse transactionResponse;
    private ListTransactionsResponse listTransactionsResponse;

    @BeforeEach
    void setUp() {
        createBankAccountRequest = new CreateBankAccountRequest()
                .name("Savings Account")
                .accountType(CreateBankAccountRequest.AccountTypeEnum.PERSONAL);

        updateBankAccountRequest = new UpdateBankAccountRequest()
                .name("Updated Savings Account");

        bankAccountResponse = new BankAccountResponse()
                .accountNumber("01234567")
                .name("Savings Account")
                .accountType(BankAccountResponse.AccountTypeEnum.PERSONAL)
                .sortCode(BankAccountResponse.SortCodeEnum._10_10_10)
                .currency(BankAccountResponse.CurrencyEnum.GBP)
                .balance(1000.00)
                .createdTimestamp(LocalDateTime.now())
                .updatedTimestamp(LocalDateTime.now());

        listBankAccountsResponse = new ListBankAccountsResponse()
                .addAccountsItem(bankAccountResponse);

        createTransactionRequest = new CreateTransactionRequest()
                .amount(100.00)
                .currency(CreateTransactionRequest.CurrencyEnum.GBP)
                .type(CreateTransactionRequest.TypeEnum.DEPOSIT)
                .reference("Test transaction");

        transactionResponse = new TransactionResponse()
                .id("tan-1")
                .amount(100.00)
                .currency(TransactionResponse.CurrencyEnum.GBP)
                .type(TransactionResponse.TypeEnum.DEPOSIT)
                .reference("Test transaction")
                .createdTimestamp(LocalDateTime.now());

        listTransactionsResponse = new ListTransactionsResponse()
                .addTransactionsItem(transactionResponse);
    }

    // ============= ACCOUNT OPERATIONS TESTS =============

    @Test
    void createAccountSuccessfully() throws Exception {
        String userId = "usr-1234567890";
        mockAuthenticatedUser(userId);
        when(accountService.createAccount(eq(userId), any(CreateBankAccountRequest.class)))
                .thenReturn(bankAccountResponse);

        mockMvc.perform(post("/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBankAccountRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value("01234567"))
                .andExpect(jsonPath("$.name").value("Savings Account"))
                .andExpect(jsonPath("$.accountType").value("personal"))
                .andExpect(jsonPath("$.currency").value("GBP"));

        verify(accountService).createAccount(eq(userId), any(CreateBankAccountRequest.class));
    }

    @Test
    void createAccountWithInvalidRequestReturnsValidationError() throws Exception {
        String userId = "usr-1234567890";
        mockAuthenticatedUser(userId);
        CreateBankAccountRequest invalidRequest = new CreateBankAccountRequest();

        mockMvc.perform(post("/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(accountService, never()).createAccount(any(), any());
    }

    @Test
    void createAccountThrowsAuthenticationExceptionWhenNotAuthenticated() throws Exception {
        mockUnauthenticatedUser();

        mockMvc.perform(post("/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBankAccountRequest)))
                .andExpect(status().isUnauthorized());

        verify(accountService, never()).createAccount(any(), any());
    }

    @Test
    void listAccountsSuccessfullyReturnsUserAccounts() throws Exception {
        String userId = "usr-1234567890";
        mockAuthenticatedUser(userId);
        when(accountService.findAccountsByUserId(userId)).thenReturn(listBankAccountsResponse);

        mockMvc.perform(get("/v1/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts").isArray())
                .andExpect(jsonPath("$.accounts[0].accountNumber").value("01234567"));

        verify(accountService).findAccountsByUserId(userId);
    }

    @Test
    void listAccountsThrowsAuthenticationExceptionWhenNotAuthenticated() throws Exception {
        mockUnauthenticatedUser();

        mockMvc.perform(get("/v1/accounts"))
                .andExpect(status().isUnauthorized());

        verify(accountService, never()).findAccountsByUserId(any());
    }

    @Test
    void fetchAccountByAccountNumberSuccessfully() throws Exception {
        String userId = "usr-1234567890";
        String accountNumber = "01234567";
        mockAuthenticatedUser(userId);
        when(accountService.findByAccountNumber(accountNumber, userId)).thenReturn(bankAccountResponse);

        mockMvc.perform(get("/v1/accounts/{accountNumber}", accountNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("01234567"))
                .andExpect(jsonPath("$.name").value("Savings Account"));

        verify(accountService).findByAccountNumber(accountNumber, userId);
    }

    @Test
    void fetchAccountByAccountNumberThrowsAuthenticationExceptionWhenNotAuthenticated() throws Exception {
        String accountNumber = "01234567";
        mockUnauthenticatedUser();

        mockMvc.perform(get("/v1/accounts/{accountNumber}", accountNumber))
                .andExpect(status().isUnauthorized());

        verify(accountService, never()).findByAccountNumber(any(), any());
    }

    @Test
    void fetchAccountByAccountNumberThrowsAccountNotFoundExceptionWhenAccountDoesNotExist() throws Exception {
        String userId = "usr-1234567890";
        String accountNumber = "01999999";
        mockAuthenticatedUser(userId);
        when(accountService.findByAccountNumber(accountNumber, userId))
                .thenThrow(new AccountNotFoundException(accountNumber));

        mockMvc.perform(get("/v1/accounts/{accountNumber}", accountNumber))
                .andExpect(status().isNotFound());

        verify(accountService).findByAccountNumber(accountNumber, userId);
    }

    @Test
    void fetchAccountByAccountNumberThrowsForbiddenWhenUserAccessesOtherUserAccount() throws Exception {
        String userId = "usr-1234567890";
        String accountNumber = "01234567";
        mockAuthenticatedUser(userId);
        when(accountService.findByAccountNumber(accountNumber, userId))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Access denied"));

        mockMvc.perform(get("/v1/accounts/{accountNumber}", accountNumber))
                .andExpect(status().isForbidden());

        verify(accountService).findByAccountNumber(accountNumber, userId);
    }

    @Test
    void updateAccountByAccountNumberSuccessfully() throws Exception {
        String userId = "usr-1234567890";
        String accountNumber = "01234567";
        mockAuthenticatedUser(userId);
        when(accountService.updateAccount(eq(accountNumber), eq(userId), any(UpdateBankAccountRequest.class)))
                .thenReturn(bankAccountResponse);

        mockMvc.perform(patch("/v1/accounts/{accountNumber}", accountNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBankAccountRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("01234567"));

        verify(accountService).updateAccount(eq(accountNumber), eq(userId), any(UpdateBankAccountRequest.class));
    }

    @Test
    void updateAccountByAccountNumberWithInvalidRequestReturnsValidationError() throws Exception {
        String userId = "usr-1234567890";
        String accountNumber = "01234567";
        mockAuthenticatedUser(userId);

        // Create an invalid update request with invalid accountType (not in enum)
        String invalidJson = """
                {
                    "name": "Updated Account Name",
                    "accountType": "business"
                }
                """;

        mockMvc.perform(patch("/v1/accounts/{accountNumber}", accountNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(accountService, never()).updateAccount(any(), any(), any());
    }

    @Test
    void updateAccountByAccountNumberWithEmptyRequestSucceeds() throws Exception {
        String userId = "usr-1234567890";
        String accountNumber = "01234567";
        mockAuthenticatedUser(userId);

        // Empty request should be valid since no fields are required
        UpdateBankAccountRequest emptyRequest = new UpdateBankAccountRequest();
        when(accountService.updateAccount(eq(accountNumber), eq(userId), any(UpdateBankAccountRequest.class)))
                .thenReturn(bankAccountResponse);

        mockMvc.perform(patch("/v1/accounts/{accountNumber}", accountNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("01234567"));

        verify(accountService).updateAccount(eq(accountNumber), eq(userId), any(UpdateBankAccountRequest.class));
    }

    @Test
    void updateAccountByAccountNumberThrowsAuthenticationExceptionWhenNotAuthenticated() throws Exception {
        String accountNumber = "01234567";
        mockUnauthenticatedUser();

        mockMvc.perform(patch("/v1/accounts/{accountNumber}", accountNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBankAccountRequest)))
                .andExpect(status().isUnauthorized());

        verify(accountService, never()).updateAccount(any(), any(), any());
    }

    @Test
    void updateAccountByAccountNumberThrowsAccountNotFoundExceptionWhenAccountDoesNotExist() throws Exception {
        String userId = "usr-1234567890";
        String accountNumber = "01999999";
        mockAuthenticatedUser(userId);
        when(accountService.updateAccount(eq(accountNumber), eq(userId), any(UpdateBankAccountRequest.class)))
                .thenThrow(new AccountNotFoundException(accountNumber));

        mockMvc.perform(patch("/v1/accounts/{accountNumber}", accountNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBankAccountRequest)))
                .andExpect(status().isNotFound());

        verify(accountService).updateAccount(eq(accountNumber), eq(userId), any(UpdateBankAccountRequest.class));
    }

    @Test
    void updateAccountByAccountNumberThrowsForbiddenWhenUserUpdatesOtherUserAccount() throws Exception {
        String userId = "usr-1234567890";
        String accountNumber = "01234567";
        mockAuthenticatedUser(userId);
        when(accountService.updateAccount(eq(accountNumber), eq(userId), any(UpdateBankAccountRequest.class)))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Access denied"));

        mockMvc.perform(patch("/v1/accounts/{accountNumber}", accountNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBankAccountRequest)))
                .andExpect(status().isForbidden());

        verify(accountService).updateAccount(eq(accountNumber), eq(userId), any(UpdateBankAccountRequest.class));
    }

    @Test
    void deleteAccountByAccountNumberSuccessfully() throws Exception {
        String userId = "usr-1234567890";
        String accountNumber = "01234567";
        mockAuthenticatedUser(userId);
        doNothing().when(accountService).deleteAccount(accountNumber, userId);

        mockMvc.perform(delete("/v1/accounts/{accountNumber}", accountNumber))
                .andExpect(status().isNoContent());

        verify(accountService).deleteAccount(accountNumber, userId);
    }

    @Test
    void deleteAccountByAccountNumberThrowsAuthenticationExceptionWhenNotAuthenticated() throws Exception {
        String accountNumber = "01234567";
        mockUnauthenticatedUser();

        mockMvc.perform(delete("/v1/accounts/{accountNumber}", accountNumber))
                .andExpect(status().isUnauthorized());

        verify(accountService, never()).deleteAccount(any(), any());
    }

    @Test
    void deleteAccountByAccountNumberThrowsAccountNotFoundExceptionWhenAccountDoesNotExist() throws Exception {
        String userId = "usr-1234567890";
        String accountNumber = "01999999";
        mockAuthenticatedUser(userId);
        doThrow(new AccountNotFoundException(accountNumber))
                .when(accountService).deleteAccount(accountNumber, userId);

        mockMvc.perform(delete("/v1/accounts/{accountNumber}", accountNumber))
                .andExpect(status().isNotFound());

        verify(accountService).deleteAccount(accountNumber, userId);
    }

    @Test
    void deleteAccountByAccountNumberThrowsForbiddenWhenUserDeletesOtherUserAccount() throws Exception {
        String userId = "usr-1234567890";
        String accountNumber = "01234567";
        mockAuthenticatedUser(userId);
        doThrow(new org.springframework.security.access.AccessDeniedException("Access denied"))
                .when(accountService).deleteAccount(accountNumber, userId);

        mockMvc.perform(delete("/v1/accounts/{accountNumber}", accountNumber))
                .andExpect(status().isForbidden());

        verify(accountService).deleteAccount(accountNumber, userId);
    }

    // ============= TRANSACTION OPERATIONS TESTS =============

    @Test
    void createTransactionSuccessfully() throws Exception {
        String userId = "usr-1234567890";
        String accountNumber = "01234567";
        mockAuthenticatedUser(userId);
        when(transactionService.createTransaction(eq(accountNumber), any(CreateTransactionRequest.class), eq(userId)))
                .thenReturn(transactionResponse);

        mockMvc.perform(post("/v1/accounts/{accountNumber}/transactions", accountNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTransactionRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("tan-1"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.type").value("deposit"));

        verify(transactionService).createTransaction(eq(accountNumber), any(CreateTransactionRequest.class), eq(userId));
    }

    @Test
    void createWithdrawalTransactionSuccessfully() throws Exception {
        String userId = "usr-1234567890";
        String accountNumber = "01234567";
        mockAuthenticatedUser(userId);

        CreateTransactionRequest withdrawalRequest = new CreateTransactionRequest()
                .amount(50.00)
                .currency(CreateTransactionRequest.CurrencyEnum.GBP)
                .type(CreateTransactionRequest.TypeEnum.WITHDRAWAL)
                .reference("Test withdrawal");

        TransactionResponse withdrawalResponse = new TransactionResponse()
                .id("tan-2")
                .amount(50.00)
                .currency(TransactionResponse.CurrencyEnum.GBP)
                .type(TransactionResponse.TypeEnum.WITHDRAWAL)
                .reference("Test withdrawal")
                .createdTimestamp(LocalDateTime.now());

        when(transactionService.createTransaction(eq(accountNumber), any(CreateTransactionRequest.class), eq(userId)))
                .thenReturn(withdrawalResponse);

        mockMvc.perform(post("/v1/accounts/{accountNumber}/transactions", accountNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawalRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("tan-2"))
                .andExpect(jsonPath("$.amount").value(50.00))
                .andExpect(jsonPath("$.type").value("withdrawal"));

        verify(transactionService).createTransaction(eq(accountNumber), any(CreateTransactionRequest.class), eq(userId));
    }

    @Test
    void createTransactionWithInsufficientFundsReturnsUnprocessableEntity() throws Exception {
        String userId = "usr-1234567890";
        String accountNumber = "01234567";
        mockAuthenticatedUser(userId);

        CreateTransactionRequest withdrawalRequest = new CreateTransactionRequest()
                .amount(2000.00)
                .currency(CreateTransactionRequest.CurrencyEnum.GBP)
                .type(CreateTransactionRequest.TypeEnum.WITHDRAWAL)
                .reference("Large withdrawal");

        when(transactionService.createTransaction(eq(accountNumber), any(CreateTransactionRequest.class), eq(userId)))
                .thenThrow(new org.example.exception.InsufficientFundsException("Insufficient funds for withdrawal"));

        mockMvc.perform(post("/v1/accounts/{accountNumber}/transactions", accountNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawalRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Insufficient funds for withdrawal"));

        verify(transactionService).createTransaction(eq(accountNumber), any(CreateTransactionRequest.class), eq(userId));
    }

    @Test
    void createTransactionOnNonExistentAccountReturnsNotFound() throws Exception {
        String userId = "usr-1234567890";
        String accountNumber = "01999999";
        mockAuthenticatedUser(userId);

        when(transactionService.createTransaction(eq(accountNumber), any(CreateTransactionRequest.class), eq(userId)))
                .thenThrow(new AccountNotFoundException(accountNumber));

        mockMvc.perform(post("/v1/accounts/{accountNumber}/transactions", accountNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTransactionRequest)))
                .andExpect(status().isNotFound());

        verify(transactionService).createTransaction(eq(accountNumber), any(CreateTransactionRequest.class), eq(userId));
    }

    @Test
    void createTransactionOnOtherUserAccountReturnsForbidden() throws Exception {
        String userId = "usr-1234567890";
        String accountNumber = "01234567";
        mockAuthenticatedUser(userId);

        when(transactionService.createTransaction(eq(accountNumber), any(CreateTransactionRequest.class), eq(userId)))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Access denied"));

        mockMvc.perform(post("/v1/accounts/{accountNumber}/transactions", accountNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTransactionRequest)))
                .andExpect(status().isForbidden());

        verify(transactionService).createTransaction(eq(accountNumber), any(CreateTransactionRequest.class), eq(userId));
    }

    @Test
    void createTransactionWithInvalidRequestReturnsValidationError() throws Exception {
        String userId = "usr-1234567890";
        String accountNumber = "01234567";
        mockAuthenticatedUser(userId);
        CreateTransactionRequest invalidRequest = new CreateTransactionRequest();

        mockMvc.perform(post("/v1/accounts/{accountNumber}/transactions", accountNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(transactionService, never()).createTransaction(any(), any(), any());
    }

    @Test
    void createTransactionThrowsAuthenticationExceptionWhenNotAuthenticated() throws Exception {
        String accountNumber = "01234567";
        mockUnauthenticatedUser();

        mockMvc.perform(post("/v1/accounts/{accountNumber}/transactions", accountNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTransactionRequest)))
                .andExpect(status().isUnauthorized());

        verify(transactionService, never()).createTransaction(any(), any(), any());
    }

    @Test
    void listAccountTransactionSuccessfully() throws Exception {
        String userId = "usr-1234567890";
        String accountNumber = "01234567";
        mockAuthenticatedUser(userId);
        when(transactionService.findByAccountNumber(accountNumber, userId))
                .thenReturn(listTransactionsResponse);

        mockMvc.perform(get("/v1/accounts/{accountNumber}/transactions", accountNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions").isArray())
                .andExpect(jsonPath("$.transactions[0].id").value("tan-1"));

        verify(transactionService).findByAccountNumber(accountNumber, userId);
    }

    @Test
    void listAccountTransactionOnNonExistentAccountReturnsNotFound() throws Exception {
        String userId = "usr-1234567890";
        String accountNumber = "01999999";
        mockAuthenticatedUser(userId);

        when(transactionService.findByAccountNumber(accountNumber, userId))
                .thenThrow(new AccountNotFoundException(accountNumber));

        mockMvc.perform(get("/v1/accounts/{accountNumber}/transactions", accountNumber))
                .andExpect(status().isNotFound());

        verify(transactionService).findByAccountNumber(accountNumber, userId);
    }

    @Test
    void listAccountTransactionOnOtherUserAccountReturnsForbidden() throws Exception {
        String userId = "usr-1234567890";
        String accountNumber = "01234567";
        mockAuthenticatedUser(userId);

        when(transactionService.findByAccountNumber(accountNumber, userId))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Access denied"));

        mockMvc.perform(get("/v1/accounts/{accountNumber}/transactions", accountNumber))
                .andExpect(status().isForbidden());

        verify(transactionService).findByAccountNumber(accountNumber, userId);
    }

    @Test
    void listAccountTransactionThrowsAuthenticationExceptionWhenNotAuthenticated() throws Exception {
        String accountNumber = "01234567";
        mockUnauthenticatedUser();

        mockMvc.perform(get("/v1/accounts/{accountNumber}/transactions", accountNumber))
                .andExpect(status().isUnauthorized());

        verify(transactionService, never()).findByAccountNumber(any(), any());
    }

    @Test
    void fetchAccountTransactionByIDSuccessfully() throws Exception {
        String userId = "usr-1234567890";
        String accountNumber = "01234567";
        String transactionId = "tan-1";
        mockAuthenticatedUser(userId);
        when(transactionService.findByIdAndAccountNumber(transactionId, accountNumber, userId))
                .thenReturn(transactionResponse);

        mockMvc.perform(get("/v1/accounts/{accountNumber}/transactions/{transactionId}",
                        accountNumber, transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transactionId));

        verify(transactionService).findByIdAndAccountNumber(transactionId, accountNumber, userId);
    }

    @Test
    void fetchAccountTransactionByIDOnNonExistentAccountReturnsNotFound() throws Exception {
        String userId = "usr-1234567890";
        String accountNumber = "01999999";
        String transactionId = "tan-1";
        mockAuthenticatedUser(userId);

        when(transactionService.findByIdAndAccountNumber(transactionId, accountNumber, userId))
                .thenThrow(new AccountNotFoundException(accountNumber));

        mockMvc.perform(get("/v1/accounts/{accountNumber}/transactions/{transactionId}",
                        accountNumber, transactionId))
                .andExpect(status().isNotFound());

        verify(transactionService).findByIdAndAccountNumber(transactionId, accountNumber, userId);
    }

    @Test
    void fetchAccountTransactionByIDOnOtherUserAccountReturnsForbidden() throws Exception {
        String userId = "usr-1234567890";
        String accountNumber = "01234567";
        String transactionId = "tan-1";
        mockAuthenticatedUser(userId);

        when(transactionService.findByIdAndAccountNumber(transactionId, accountNumber, userId))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Access denied"));

        mockMvc.perform(get("/v1/accounts/{accountNumber}/transactions/{transactionId}",
                        accountNumber, transactionId))
                .andExpect(status().isForbidden());

        verify(transactionService).findByIdAndAccountNumber(transactionId, accountNumber, userId);
    }

    @Test
    void fetchAccountTransactionByIDWithNonExistentTransactionReturnsNotFound() throws Exception {
        String userId = "usr-1234567890";
        String accountNumber = "01234567";
        String transactionId = "tan-9";
        mockAuthenticatedUser(userId);

        when(transactionService.findByIdAndAccountNumber(transactionId, accountNumber, userId))
                .thenThrow(new org.example.exception.TransactionNotFoundException(transactionId));

        mockMvc.perform(get("/v1/accounts/{accountNumber}/transactions/{transactionId}",
                        accountNumber, transactionId))
                .andExpect(status().isNotFound());

        verify(transactionService).findByIdAndAccountNumber(transactionId, accountNumber, userId);
    }

    @Test
    void fetchAccountTransactionByIDWithWrongAccountAssociationReturnsNotFound() throws Exception {
        String userId = "usr-1234567890";
        String accountNumber = "01234567";
        String transactionId = "tan-1";
        mockAuthenticatedUser(userId);

        when(transactionService.findByIdAndAccountNumber(transactionId, accountNumber, userId))
                .thenThrow(new org.example.exception.TransactionNotFoundException(transactionId));

        mockMvc.perform(get("/v1/accounts/{accountNumber}/transactions/{transactionId}",
                        accountNumber, transactionId))
                .andExpect(status().isNotFound());

        verify(transactionService).findByIdAndAccountNumber(transactionId, accountNumber, userId);
    }

    @Test
    void fetchAccountTransactionByIDThrowsAuthenticationExceptionWhenNotAuthenticated() throws Exception {
        String accountNumber = "01234567";
        String transactionId = "tan-1";
        mockUnauthenticatedUser();

        mockMvc.perform(get("/v1/accounts/{accountNumber}/transactions/{transactionId}",
                        accountNumber, transactionId))
                .andExpect(status().isUnauthorized());

        verify(transactionService, never()).findByIdAndAccountNumber(any(), any(), any());
    }

    // ============= HELPER METHODS =============

    private void mockAuthenticatedUser(String userId) {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userId);
        SecurityContextHolder.setContext(securityContext);
    }

    private void mockUnauthenticatedUser() {
        SecurityContextHolder.clearContext();
    }
}
