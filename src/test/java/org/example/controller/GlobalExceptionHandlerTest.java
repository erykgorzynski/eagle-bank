package org.example.controller;

import org.example.exception.*;
import org.example.model.ErrorResponse;
import org.example.model.BadRequestErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleUserNotFoundExceptionReturnsNotFoundWithErrorMessage() {
        UserNotFoundException exception = new UserNotFoundException("usr-123");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUserNotFoundException(exception);

        assertEquals(404, response.getStatusCode().value());
        assertEquals("User not found with ID: usr-123", response.getBody().getMessage());
    }

    @Test
    void handleUserHasAssociatedAccountsExceptionReturnsConflictWithErrorMessage() {
        UserHasAssociatedAccountsException exception = new UserHasAssociatedAccountsException("usr-123");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUserHasAssociatedAccountsException(exception);

        assertEquals(409, response.getStatusCode().value());
        assertEquals("Cannot delete user usr-123 because they have associated bank accounts", response.getBody().getMessage());
    }

    @Test
    void handleAccountNotFoundExceptionReturnsNotFoundWithErrorMessage() {
        AccountNotFoundException exception = new AccountNotFoundException("01234567");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleAccountNotFoundException(exception);

        assertEquals(404, response.getStatusCode().value());
        assertEquals("Account not found with account number: 01234567", response.getBody().getMessage());
    }

    @Test
    void handleTransactionNotFoundExceptionReturnsNotFoundWithErrorMessage() {
        TransactionNotFoundException exception = new TransactionNotFoundException("tan-1");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleTransactionNotFoundException(exception);

        assertEquals(404, response.getStatusCode().value());
        assertEquals("Transaction not found with ID: tan-1", response.getBody().getMessage());
    }

    @Test
    void handleInsufficientFundsExceptionReturnsUnprocessableEntityWithErrorMessage() {
        InsufficientFundsException exception = new InsufficientFundsException("Insufficient funds for withdrawal");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleInsufficientFundsException(exception);

        assertEquals(422, response.getStatusCode().value());
        assertEquals("Insufficient funds for withdrawal", response.getBody().getMessage());
    }

    @Test
    void handleAuthenticationExceptionReturnsUnauthorizedWithGenericMessage() {
        AuthenticationException exception = new AuthenticationException("Authentication failed") {};

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleAuthenticationException(exception);

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Access token is missing or invalid", response.getBody().getMessage());
    }

    @Test
    void handleBadCredentialsExceptionReturnsUnauthorizedWithSpecificMessage() {
        BadCredentialsException exception = new BadCredentialsException("Bad credentials");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBadCredentialsException(exception);

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Invalid email or password", response.getBody().getMessage());
    }

    @Test
    void handleAccessDeniedExceptionReturnsForbiddenWithGenericMessage() {
        AccessDeniedException exception = new AccessDeniedException("Access denied");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleAccessDeniedException(exception);

        assertEquals(403, response.getStatusCode().value());
        assertEquals("The user is not allowed to access this resource", response.getBody().getMessage());
    }

    @Test
    void handleMethodArgumentNotValidExceptionReturnsBadRequestWithValidationDetails() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("testObject", "testField", "Test error message");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<BadRequestErrorResponse> response = globalExceptionHandler.handleValidationException(exception);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Invalid details supplied", response.getBody().getMessage());
        assertEquals(1, response.getBody().getDetails().size());
        assertEquals("testField", response.getBody().getDetails().get(0).getField());
        assertEquals("Test error message", response.getBody().getDetails().get(0).getMessage());
        assertEquals("VALIDATION_ERROR", response.getBody().getDetails().get(0).getType());
    }

    @Test
    void handleIllegalArgumentExceptionReturnsBadRequestWithErrorDetails() {
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument provided");

        ResponseEntity<BadRequestErrorResponse> response = globalExceptionHandler.handleIllegalArgumentException(exception);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Invalid details supplied", response.getBody().getMessage());
        assertEquals(1, response.getBody().getDetails().size());
        assertEquals("request", response.getBody().getDetails().get(0).getField());
        assertEquals("Invalid argument provided", response.getBody().getDetails().get(0).getMessage());
        assertEquals("INVALID_ARGUMENT", response.getBody().getDetails().get(0).getType());
    }

    @Test
    void handleMissingServletRequestParameterExceptionReturnsBadRequestWithParameterDetails() {
        MissingServletRequestParameterException exception =
            new MissingServletRequestParameterException("testParam", "String");

        ResponseEntity<BadRequestErrorResponse> response = globalExceptionHandler.handleMissingServletRequestParameterException(exception);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Invalid details supplied", response.getBody().getMessage());
        assertEquals(1, response.getBody().getDetails().size());
        assertEquals("testParam", response.getBody().getDetails().get(0).getField());
        assertEquals("Required parameter 'testParam' is missing", response.getBody().getDetails().get(0).getMessage());
        assertEquals("MISSING_PARAMETER", response.getBody().getDetails().get(0).getType());
    }

    @Test
    void handleHttpMessageNotReadableExceptionReturnsBadRequestWithFormatError() {
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);
        when(exception.getMessage()).thenReturn("JSON parse error");

        ResponseEntity<BadRequestErrorResponse> response = globalExceptionHandler.handleHttpMessageNotReadableException(exception);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Invalid details supplied", response.getBody().getMessage());
        assertEquals(1, response.getBody().getDetails().size());
        assertEquals("request", response.getBody().getDetails().get(0).getField());
        assertEquals("Invalid request body format", response.getBody().getDetails().get(0).getMessage());
        assertEquals("INVALID_FORMAT", response.getBody().getDetails().get(0).getType());
    }

    @Test
    void handleGenericExceptionReturnsInternalServerErrorWithGenericMessage() {
        Exception exception = new RuntimeException("Unexpected error occurred");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGenericException(exception);

        assertEquals(500, response.getStatusCode().value());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
    }
}
