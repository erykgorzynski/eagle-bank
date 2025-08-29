package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.User;
import org.example.model.LoginResponse;
import org.example.model.LoginUserRequest;
import org.example.security.JwtService;
import org.example.service.UserService;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
    excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org\\.example\\.security\\..*"))
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private LoginUserRequest validLoginRequest;
    private User validUser;

    @BeforeEach
    void setUp() {
        validLoginRequest = new LoginUserRequest()
                .email("test@example.com")
                .password("validPassword123");

        validUser = new User();
        validUser.setId("usr-1234567890");
        validUser.setEmail("test@example.com");
        validUser.setPasswordHash("$2a$10$hashedPassword");
    }

    @Test
    void loginUserSuccessfullyReturnsTokenAndUserId() throws Exception {
        String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(validUser));
        when(passwordEncoder.matches("validPassword123", validUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken(validUser.getId())).thenReturn(expectedToken);

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(expectedToken))
                .andExpect(jsonPath("$.userId").value("usr-1234567890"));

        verify(userService).findByEmail("test@example.com");
        verify(passwordEncoder).matches("validPassword123", validUser.getPasswordHash());
        verify(jwtService).generateToken(validUser.getId());
    }

    @Test
    void loginUserWithInvalidEmailReturnsUnauthorized() throws Exception {
        when(userService.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        LoginUserRequest invalidEmailRequest = new LoginUserRequest()
                .email("nonexistent@example.com")
                .password("anyPassword");

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmailRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        verify(userService).findByEmail("nonexistent@example.com");
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void loginUserWithInvalidPasswordReturnsUnauthorized() throws Exception {
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(validUser));
        when(passwordEncoder.matches("wrongPassword", validUser.getPasswordHash())).thenReturn(false);

        LoginUserRequest invalidPasswordRequest = new LoginUserRequest()
                .email("test@example.com")
                .password("wrongPassword");

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPasswordRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        verify(userService).findByEmail("test@example.com");
        verify(passwordEncoder).matches("wrongPassword", validUser.getPasswordHash());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void loginUserWithMissingEmailReturnsBadRequest() throws Exception {
        LoginUserRequest missingEmailRequest = new LoginUserRequest()
                .password("validPassword123");

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingEmailRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).findByEmail(any());
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void loginUserWithMissingPasswordReturnsBadRequest() throws Exception {
        LoginUserRequest missingPasswordRequest = new LoginUserRequest()
                .email("test@example.com");

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingPasswordRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).findByEmail(any());
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void loginUserWithInvalidEmailFormatReturnsBadRequest() throws Exception {
        LoginUserRequest invalidEmailFormatRequest = new LoginUserRequest()
                .email("invalid-email-format")
                .password("validPassword123");

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmailFormatRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).findByEmail(any());
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void loginUserWithEmptyEmailReturnsUnauthorized() throws Exception {
        when(userService.findByEmail("")).thenReturn(Optional.empty());

        LoginUserRequest emptyEmailRequest = new LoginUserRequest()
                .email("")
                .password("validPassword123");

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyEmailRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        verify(userService).findByEmail("");
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void loginUserWithEmptyPasswordReturnsUnauthorized() throws Exception {
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(validUser));
        when(passwordEncoder.matches("", validUser.getPasswordHash())).thenReturn(false);

        LoginUserRequest emptyPasswordRequest = new LoginUserRequest()
                .email("test@example.com")
                .password("");

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyPasswordRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        verify(userService).findByEmail("test@example.com");
        verify(passwordEncoder).matches("", validUser.getPasswordHash());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void loginUserWithInvalidJsonReturnsBadRequest() throws Exception {
        String invalidJson = "{ \"email\": \"test@example.com\", \"password\": }";

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(userService, never()).findByEmail(any());
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void loginUserWhenUserServiceThrowsExceptionReturnsInternalServerError() throws Exception {
        when(userService.findByEmail("test@example.com")).thenThrow(new RuntimeException("Database connection error"));

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));

        verify(userService).findByEmail("test@example.com");
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void loginUserWhenJwtServiceThrowsExceptionReturnsInternalServerError() throws Exception {
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(validUser));
        when(passwordEncoder.matches("validPassword123", validUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken(validUser.getId())).thenThrow(new RuntimeException("JWT generation failed"));

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));

        verify(userService).findByEmail("test@example.com");
        verify(passwordEncoder).matches("validPassword123", validUser.getPasswordHash());
        verify(jwtService).generateToken(validUser.getId());
    }

    @Test
    void loginUserWithNullUserFromServiceReturnsUnauthorized() throws Exception {
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        verify(userService).findByEmail("test@example.com");
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void loginUserWithSpecialCharactersInPasswordSucceeds() throws Exception {
        String complexPassword = "P@ssw0rd!#$%";
        String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.complex.token";

        LoginUserRequest complexPasswordRequest = new LoginUserRequest()
                .email("test@example.com")
                .password(complexPassword);

        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(validUser));
        when(passwordEncoder.matches(complexPassword, validUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken(validUser.getId())).thenReturn(expectedToken);

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(complexPasswordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(expectedToken))
                .andExpect(jsonPath("$.userId").value("usr-1234567890"));

        verify(userService).findByEmail("test@example.com");
        verify(passwordEncoder).matches(complexPassword, validUser.getPasswordHash());
        verify(jwtService).generateToken(validUser.getId());
    }
}
