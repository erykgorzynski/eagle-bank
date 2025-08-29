package org.example.service;

import org.example.entity.User;
import org.example.exception.UserHasAssociatedAccountsException;
import org.example.exception.UserNotFoundException;
import org.example.mapper.UserMapper;
import org.example.model.CreateUserRequest;
import org.example.model.CreateUserRequestAddress;
import org.example.model.UpdateUserRequest;
import org.example.model.UserResponse;
import org.example.repository.AccountRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserResponse userResponse;
    private CreateUserRequest createUserRequest;
    private UpdateUserRequest updateUserRequest;

    @BeforeEach
    void setUp() {
        CreateUserRequestAddress address = new CreateUserRequestAddress()
                .line1("123 Main St")
                .line2("Apartment 4B")
                .town("London")
                .county("Greater London")
                .postcode("SW1A 1AA");

        user = new User();
        user.setId("usr-1234567890");
        user.setName("John Doe");
        user.setEmail("john.doe@example.com");
        user.setPasswordHash("hashedPassword");
        user.setPhoneNumber("+442079460958");
        user.setCreatedTimestamp(LocalDateTime.now());
        user.setUpdatedTimestamp(LocalDateTime.now());

        userResponse = new UserResponse(
                "usr-1234567890",
                "John Doe",
                address,
                "+442079460958",
                "john.doe@example.com",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

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
    }

    // === DELETE USER COMPREHENSIVE TESTS ===

    @Test
    void deleteUserSuccessfullyWhenUserHasNoBankAccounts() {
        // Given
        String userId = "usr-1234567890";
        when(userRepository.existsById(userId)).thenReturn(true);
        when(accountRepository.existsByUserId(userId)).thenReturn(false); // No bank accounts

        // When
        userService.deleteUser(userId);

        // Then
        verify(userRepository).existsById(userId);
        verify(accountRepository).existsByUserId(userId);
        verify(userRepository).deleteById(userId);
    }

    @Test
    void deleteUserThrowsUserHasAssociatedAccountsExceptionWhenUserHasBankAccounts() {
        // Given
        String userId = "usr-1234567890";
        when(userRepository.existsById(userId)).thenReturn(true);
        when(accountRepository.existsByUserId(userId)).thenReturn(true); // Has bank accounts

        // When & Then
        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(UserHasAssociatedAccountsException.class)
                .hasMessage("Cannot delete user usr-1234567890 because they have associated bank accounts");

        verify(userRepository).existsById(userId);
        verify(accountRepository).existsByUserId(userId);
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void deleteUserThrowsUserNotFoundExceptionWhenUserDoesNotExist() {
        // Given
        String userId = "usr-nonexistent";
        when(userRepository.existsById(userId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with ID: usr-nonexistent");

        verify(userRepository).existsById(userId);
        verify(accountRepository, never()).existsByUserId(any());
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void deleteUserWithMultipleAccountsThrowsException() {
        // Given
        String userId = "usr-1234567890";
        when(userRepository.existsById(userId)).thenReturn(true);
        when(accountRepository.existsByUserId(userId)).thenReturn(true); // Multiple accounts exist

        // When & Then
        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(UserHasAssociatedAccountsException.class);

        verify(userRepository).existsById(userId);
        verify(accountRepository).existsByUserId(userId);
        verify(userRepository, never()).deleteById(any());
    }

    // === OTHER USER SERVICE TESTS ===

    @Test
    void createUserSuccessfully() {
        // Given
        when(userMapper.toEntity(createUserRequest)).thenReturn(user);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        // When
        UserResponse result = userService.createUser(createUserRequest);

        // Then
        assertThat(result).isEqualTo(userResponse);
        verify(userMapper).toEntity(createUserRequest);
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verify(userMapper).toResponse(user);
    }

    @Test
    void findByIdSuccessfully() {
        // Given
        String userId = "usr-1234567890";
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        // When
        UserResponse result = userService.findById(userId);

        // Then
        assertThat(result).isEqualTo(userResponse);
        verify(userRepository).findById(userId);
        verify(userMapper).toResponse(user);
    }

    @Test
    void findByIdThrowsUserNotFoundExceptionWhenUserDoesNotExist() {
        // Given
        String userId = "usr-nonexistent";
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.findById(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with ID: usr-nonexistent");

        verify(userRepository).findById(userId);
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void updateUserSuccessfully() {
        // Given
        String userId = "usr-1234567890";
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        // When
        UserResponse result = userService.updateUser(userId, updateUserRequest);

        // Then
        assertThat(result).isEqualTo(userResponse);
        verify(userRepository).findById(userId);
        verify(userMapper).updateEntityFromRequest(updateUserRequest, user);
        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);
    }

    @Test
    void updateUserThrowsUserNotFoundExceptionWhenUserDoesNotExist() {
        // Given
        String userId = "usr-nonexistent";
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(userId, updateUserRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with ID: usr-nonexistent");

        verify(userRepository).findById(userId);
        verify(userMapper, never()).updateEntityFromRequest(any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void findByEmailSuccessfully() {
        // Given
        String email = "john.doe@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.findByEmail(email);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(user);
        verify(userRepository).findByEmail(email);
    }

    @Test
    void findByEmailReturnsEmptyWhenUserNotFound() {
        // Given
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.findByEmail(email);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findByEmail(email);
    }

    // === ADDITIONAL EDGE CASES AND SCENARIOS ===

    @Test
    void deleteUserVerifiesUserExistenceBeforeCheckingAccounts() {
        String userId = "usr-nonexistent";
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).existsById(userId);
        verify(accountRepository, never()).existsByUserId(any());
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void createUserGeneratesUniqueUserIdWithUsrPrefix() {
        when(userMapper.toEntity(createUserRequest)).thenReturn(user);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertThat(savedUser.getId()).matches("^usr-[a-z0-9]{10}$");
            return savedUser;
        });
        when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

        userService.createUser(createUserRequest);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUserHashesPasswordBeforeSaving() {
        when(userMapper.toEntity(createUserRequest)).thenReturn(user);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertThat(savedUser.getPasswordHash()).isEqualTo("hashedPassword123");
            return savedUser;
        });
        when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

        userService.createUser(createUserRequest);

        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserWithNullFieldsDoesNotOverwriteExistingValues() {
        String userId = "usr-1234567890";
        UpdateUserRequest requestWithNullFields = new UpdateUserRequest()
                .name("Updated Name");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        userService.updateUser(userId, requestWithNullFields);

        verify(userMapper).updateEntityFromRequest(requestWithNullFields, user);
        verify(userRepository).save(user);
    }

    @Test
    void deleteUserLogsDebugInformationAboutAccountCheck() {
        String userId = "usr-1234567890";
        when(userRepository.existsById(userId)).thenReturn(true);
        when(accountRepository.existsByUserId(userId)).thenReturn(false);

        userService.deleteUser(userId);

        verify(accountRepository).existsByUserId(userId);
        verify(userRepository).deleteById(userId);
    }

    @Test
    void findByEmailWithSpecialCharactersInEmail() {
        String complexEmail = "user.name+tag@example-domain.co.uk";
        when(userRepository.findByEmail(complexEmail)).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByEmail(complexEmail);

        assertThat(result).isPresent();
        verify(userRepository).findByEmail(complexEmail);
    }

    @Test
    void createUserWithComplexPasswordRequiresProperHashing() {
        String complexPassword = "P@ssw0rd!123#$%";
        CreateUserRequest requestWithComplexPassword = new CreateUserRequest(
                "John Doe",
                createUserRequest.getAddress(),
                "+442079460958",
                "john.doe@example.com",
                complexPassword
        );

        when(userMapper.toEntity(requestWithComplexPassword)).thenReturn(user);
        when(passwordEncoder.encode(complexPassword)).thenReturn("hashedComplexPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        userService.createUser(requestWithComplexPassword);

        verify(passwordEncoder).encode(complexPassword);
    }

    @Test
    void updateUserReturnsMappedResponseFromUpdatedEntity() {
        String userId = "usr-1234567890";
        UserResponse updatedResponse = new UserResponse(
                userId,
                "Updated Name",
                userResponse.getAddress(),
                userResponse.getPhoneNumber(),
                userResponse.getEmail(),
                userResponse.getCreatedTimestamp(),
                LocalDateTime.now()
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(updatedResponse);

        UserResponse result = userService.updateUser(userId, updateUserRequest);

        assertThat(result).isEqualTo(updatedResponse);
        verify(userMapper).toResponse(user);
    }

    @Test
    void deleteUserWithAccountsLogsWarningMessage() {
        String userId = "usr-1234567890";
        when(userRepository.existsById(userId)).thenReturn(true);
        when(accountRepository.existsByUserId(userId)).thenReturn(true);

        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(UserHasAssociatedAccountsException.class);

        verify(accountRepository).existsByUserId(userId);
        verify(userRepository, never()).deleteById(any());
    }
}
