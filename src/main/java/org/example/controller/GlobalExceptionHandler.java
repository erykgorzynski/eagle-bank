package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.exception.AccountNotFoundException;
import org.example.exception.InsufficientFundsException;
import org.example.exception.TransactionNotFoundException;
import org.example.exception.UserHasAssociatedAccountsException;
import org.example.exception.UserNotFoundException;
import org.example.model.BadRequestErrorResponse;
import org.example.model.BadRequestErrorResponseDetailsInner;
import org.example.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

/**
 * Global exception handler for the Eagle Bank API
 * Returns proper error responses according to the OpenAPI specification
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException e) {
        log.warn("User not found: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(UserHasAssociatedAccountsException.class)
    public ResponseEntity<ErrorResponse> handleUserHasAssociatedAccountsException(UserHasAssociatedAccountsException e) {
        log.warn("User has associated accounts: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFoundException(AccountNotFoundException e) {
        log.warn("Account not found: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransactionNotFoundException(TransactionNotFoundException e) {
        log.warn("Transaction not found: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFundsException(InsufficientFundsException e) {
        log.warn("Insufficient funds: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(e.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e) {
        log.warn("Authentication failed: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage("Access token is missing or invalid");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            org.springframework.security.authentication.BadCredentialsException e) {
        log.warn("Authentication failed: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage("Invalid email or password");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage("The user is not allowed to access this resource");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BadRequestErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("Validation failed: {}", e.getMessage());

        BadRequestErrorResponse errorResponse = new BadRequestErrorResponse();
        errorResponse.setMessage("Invalid details supplied");

        List<BadRequestErrorResponseDetailsInner> details = new ArrayList<>();
        e.getBindingResult().getFieldErrors().forEach(error -> {
            BadRequestErrorResponseDetailsInner detail = new BadRequestErrorResponseDetailsInner();
            detail.setField(error.getField());
            detail.setMessage(error.getDefaultMessage());
            detail.setType("VALIDATION_ERROR");
            details.add(detail);
        });

        errorResponse.setDetails(details);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BadRequestErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Invalid argument: {}", e.getMessage());

        BadRequestErrorResponse errorResponse = new BadRequestErrorResponse();
        errorResponse.setMessage("Invalid details supplied");

        BadRequestErrorResponseDetailsInner detail = new BadRequestErrorResponseDetailsInner();
        detail.setField("request");
        detail.setMessage(e.getMessage());
        detail.setType("INVALID_ARGUMENT");

        errorResponse.setDetails(List.of(detail));
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<BadRequestErrorResponse> handleMissingServletRequestParameterException(
            org.springframework.web.bind.MissingServletRequestParameterException e) {
        log.warn("Missing request parameter: {}", e.getMessage());

        BadRequestErrorResponse errorResponse = new BadRequestErrorResponse();
        errorResponse.setMessage("Invalid details supplied");

        BadRequestErrorResponseDetailsInner detail = new BadRequestErrorResponseDetailsInner();
        detail.setField(e.getParameterName());
        detail.setMessage("Required parameter '" + e.getParameterName() + "' is missing");
        detail.setType("MISSING_PARAMETER");

        errorResponse.setDetails(List.of(detail));
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<BadRequestErrorResponse> handleHttpMessageNotReadableException(
            org.springframework.http.converter.HttpMessageNotReadableException e) {
        log.warn("Invalid request body: {}", e.getMessage());

        BadRequestErrorResponse errorResponse = new BadRequestErrorResponse();
        errorResponse.setMessage("Invalid details supplied");

        BadRequestErrorResponseDetailsInner detail = new BadRequestErrorResponseDetailsInner();
        detail.setField("request");
        detail.setMessage("Invalid request body format");
        detail.setType("INVALID_FORMAT");

        errorResponse.setDetails(List.of(detail));
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage("An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
