package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.CreateUserRequest;
import org.example.model.CreateUserRequestAddress;
import org.example.model.UpdateUserRequest;
import org.example.model.UserResponse;
import org.example.service.UserService;
import org.example.exception.UserNotFoundException;
import org.example.exception.UserHasAssociatedAccountsException;
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

@WebMvcTest(controllers = UserController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
    excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org\\.example\\.security\\..*"))
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateUserRequest createUserRequest;
    private UpdateUserRequest updateUserRequest;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        CreateUserRequestAddress address = new CreateUserRequestAddress()
                .line1("123 Main St")
                .line2("Apartment 4B")
                .town("London")
                .county("Greater London")
                .postcode("SW1A 1AA");

        createUserRequest = new CreateUserRequest(
                "John Doe",
                address,
                "+442079460958",
                "john.doe@example.com",
                "password123"
        );

        updateUserRequest = new UpdateUserRequest()
                .name("Jane Doe")
                .phoneNumber("+442079460959");

        userResponse = new UserResponse(
                "usr-1234567890",
                "John Doe",
                address,
                "+442079460958",
                "john.doe@example.com",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    void createUserSuccessfully() throws Exception {
        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("usr-1234567890"))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));

        verify(userService).createUser(any(CreateUserRequest.class));
    }

    @Test
    void createUserWithInvalidRequestReturnsValidationError() throws Exception {
        CreateUserRequest invalidRequest = new CreateUserRequest();

        mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUser(any(CreateUserRequest.class));
    }

    @Test
    void fetchUserByIdSuccessfullyWhenUserAccessesOwnData() throws Exception {
        String userId = "usr-1234567890";
        mockAuthenticatedUser(userId);
        when(userService.findById(userId)).thenReturn(userResponse);

        mockMvc.perform(get("/v1/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("John Doe"));

        verify(userService).findById(userId);
    }

    @Test
    void fetchUserByIdThrowsAuthenticationExceptionWhenNotAuthenticated() throws Exception {
        String userId = "usr-1234567890";
        mockUnauthenticatedUser();

        mockMvc.perform(get("/v1/users/{userId}", userId))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).findById(any());
    }

    @Test
    void fetchUserByIdThrowsAccessDeniedWhenUserAccessesOtherUserData() throws Exception {
        String authenticatedUserId = "usr-1234567890";
        String requestedUserId = "usr-0987654321";
        mockAuthenticatedUser(authenticatedUserId);
        when(userService.findById(requestedUserId)).thenReturn(userResponse);

        mockMvc.perform(get("/v1/users/{userId}", requestedUserId))
                .andExpect(status().isForbidden());

        verify(userService).findById(requestedUserId);
    }

    @Test
    void fetchUserByIdThrowsUserNotFoundExceptionWhenUserDoesNotExist() throws Exception {
        String userId = "usr-nonexistent";
        mockAuthenticatedUser(userId);
        when(userService.findById(userId)).thenThrow(new UserNotFoundException(userId));

        mockMvc.perform(get("/v1/users/{userId}", userId))
                .andExpect(status().isNotFound());

        verify(userService).findById(userId);
    }

    @Test
    void updateUserByIdSuccessfullyWhenUserUpdatesOwnData() throws Exception {
        String userId = "usr-1234567890";
        mockAuthenticatedUser(userId);
        when(userService.findById(userId)).thenReturn(userResponse);
        when(userService.updateUser(eq(userId), any(UpdateUserRequest.class))).thenReturn(userResponse);

        mockMvc.perform(patch("/v1/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId));

        verify(userService).findById(userId);
        verify(userService).updateUser(eq(userId), any(UpdateUserRequest.class));
    }

    @Test
    void updateUserByIdThrowsAuthenticationExceptionWhenNotAuthenticated() throws Exception {
        String userId = "usr-1234567890";
        mockUnauthenticatedUser();

        mockMvc.perform(patch("/v1/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).findById(any());
        verify(userService, never()).updateUser(any(), any());
    }

    @Test
    void updateUserByIdThrowsAccessDeniedWhenUserUpdatesOtherUserData() throws Exception {
        String authenticatedUserId = "usr-1234567890";
        String requestedUserId = "usr-0987654321";
        mockAuthenticatedUser(authenticatedUserId);
        when(userService.findById(requestedUserId)).thenReturn(userResponse);

        mockMvc.perform(patch("/v1/users/{userId}", requestedUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isForbidden());

        verify(userService).findById(requestedUserId);
        verify(userService, never()).updateUser(any(), any());
    }

    @Test
    void updateUserByIdThrowsUserNotFoundExceptionWhenUserDoesNotExist() throws Exception {
        String userId = "usr-nonexistent";
        mockAuthenticatedUser(userId);
        when(userService.findById(userId)).thenThrow(new UserNotFoundException(userId));

        mockMvc.perform(patch("/v1/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isNotFound());

        verify(userService).findById(userId);
        verify(userService, never()).updateUser(any(), any());
    }

    @Test
    void updateUserByIdWithInvalidRequestReturnsValidationError() throws Exception {
        String userId = "usr-1234567890";
        mockAuthenticatedUser(userId);
        UpdateUserRequest invalidRequest = new UpdateUserRequest().email("invalid-email");

        mockMvc.perform(patch("/v1/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).findById(any());
        verify(userService, never()).updateUser(any(), any());
    }

    @Test
    void deleteUserByIdSuccessfullyWhenUserDeletesOwnData() throws Exception {
        String userId = "usr-1234567890";
        mockAuthenticatedUser(userId);
        when(userService.findById(userId)).thenReturn(userResponse);
        doNothing().when(userService).deleteUser(userId);

        mockMvc.perform(delete("/v1/users/{userId}", userId))
                .andExpect(status().isNoContent());

        verify(userService).findById(userId);
        verify(userService).deleteUser(userId);
    }

    @Test
    void deleteUserByIdThrowsAuthenticationExceptionWhenNotAuthenticated() throws Exception {
        String userId = "usr-1234567890";
        mockUnauthenticatedUser();

        mockMvc.perform(delete("/v1/users/{userId}", userId))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).findById(any());
        verify(userService, never()).deleteUser(any());
    }

    @Test
    void deleteUserByIdThrowsAccessDeniedWhenUserDeletesOtherUserData() throws Exception {
        String authenticatedUserId = "usr-1234567890";
        String requestedUserId = "usr-0987654321";
        mockAuthenticatedUser(authenticatedUserId);
        when(userService.findById(requestedUserId)).thenReturn(userResponse);

        mockMvc.perform(delete("/v1/users/{userId}", requestedUserId))
                .andExpect(status().isForbidden());

        verify(userService).findById(requestedUserId);
        verify(userService, never()).deleteUser(any());
    }

    @Test
    void deleteUserByIdThrowsUserNotFoundExceptionWhenUserDoesNotExist() throws Exception {
        String userId = "usr-nonexistent";
        mockAuthenticatedUser(userId);
        when(userService.findById(userId)).thenThrow(new UserNotFoundException(userId));

        mockMvc.perform(delete("/v1/users/{userId}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());

        verify(userService).findById(userId);
        verify(userService, never()).deleteUser(any());
    }

    @Test
    void deleteUserByIdThrowsUserHasAssociatedAccountsExceptionWhenUserHasAccounts() throws Exception {
        String userId = "usr-1234567890";
        mockAuthenticatedUser(userId);
        when(userService.findById(userId)).thenReturn(userResponse);
        doThrow(new UserHasAssociatedAccountsException(userId)).when(userService).deleteUser(userId);

        mockMvc.perform(delete("/v1/users/{userId}", userId))
                .andExpect(status().isConflict());

        verify(userService).findById(userId);
        verify(userService).deleteUser(userId);
    }

    @Test
    void getCurrentUserIdReturnsNullWhenAuthenticationIsNull() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/v1/users/usr-1234567890"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCurrentUserIdReturnsNullWhenPrincipalIsNotString() throws Exception {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new Object());
        SecurityContextHolder.setContext(securityContext);

        mockMvc.perform(get("/v1/users/usr-1234567890"))
                .andExpect(status().isUnauthorized());
    }

    // === Missing Test Scenarios ===

    @Test
    void fetchUserByIdThrowsAuthenticationExceptionWhenTokenMissing() throws Exception {
        String userId = "usr-1234567890";
        // No authentication setup - simulating missing bearer token

        mockMvc.perform(get("/v1/users/{userId}", userId))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).findById(any());
    }

    @Test
    void updateUserByIdThrowsAuthenticationExceptionWhenTokenMissing() throws Exception {
        String userId = "usr-1234567890";
        // No authentication setup - simulating missing bearer token

        mockMvc.perform(patch("/v1/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).findById(any());
        verify(userService, never()).updateUser(any(), any());
    }

    @Test
    void deleteUserByIdThrowsAuthenticationExceptionWhenTokenMissing() throws Exception {
        String userId = "usr-1234567890";
        // No authentication setup - simulating missing bearer token

        mockMvc.perform(delete("/v1/users/{userId}", userId))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).findById(any());
        verify(userService, never()).deleteUser(any());
    }

    @Test
    void updateUserByIdWithCompletelyInvalidDataReturnsValidationError() throws Exception {
        String userId = "usr-1234567890";
        mockAuthenticatedUser(userId);

        // Create request with multiple invalid fields
        UpdateUserRequest completelyInvalidRequest = new UpdateUserRequest()
                .email("not-an-email")
                .phoneNumber("invalid-phone")
                .name(""); // empty name

        mockMvc.perform(patch("/v1/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completelyInvalidRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).findById(any());
        verify(userService, never()).updateUser(any(), any());
    }

    @Test
    void updateUserByIdWithInvalidPhoneNumberReturnsValidationError() throws Exception {
        String userId = "usr-1234567890";
        mockAuthenticatedUser(userId);

        // Test phone number that doesn't match pattern ^\+[1-9]\d{1,14}$
        UpdateUserRequest invalidPhoneRequest = new UpdateUserRequest()
                .phoneNumber("123456789"); // missing + and starts with non 1-9

        mockMvc.perform(patch("/v1/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPhoneRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).findById(any());
        verify(userService, never()).updateUser(any(), any());
    }

    @Test
    void deleteUserByIdWhenUserHasNoBankAccountsSucceeds() throws Exception {
        String userId = "usr-1234567890";
        mockAuthenticatedUser(userId);
        when(userService.findById(userId)).thenReturn(userResponse);
        doNothing().when(userService).deleteUser(userId); // No exception = no bank accounts

        mockMvc.perform(delete("/v1/users/{userId}", userId))
                .andExpect(status().isNoContent());

        verify(userService).findById(userId);
        verify(userService).deleteUser(userId);
    }

    @Test
    void createUserWithInvalidPhoneNumberReturnsValidationError() throws Exception {
        CreateUserRequestAddress address = new CreateUserRequestAddress()
                .line1("123 Main St")
                .town("London")
                .county("Greater London")
                .postcode("SW1A 1AA");

        // Test phone number that doesn't match pattern ^\+[1-9]\d{1,14}$
        CreateUserRequest invalidPhoneRequest = new CreateUserRequest(
                "John Doe",
                address,
                "123456789", // missing + and doesn't start with 1-9
                "john.doe@example.com",
                "password123"
        );

        mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPhoneRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUser(any(CreateUserRequest.class));
    }

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
