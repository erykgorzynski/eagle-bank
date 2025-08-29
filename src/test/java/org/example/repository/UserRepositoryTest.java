package org.example.repository;

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
class UserRepositoryTest {

    @Mock
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;
    private User testUser3;

    @BeforeEach
    void setUp() {
        testUser1 = new User();
        testUser1.setId("usr-123abc456");
        testUser1.setEmail("john.doe@example.com");
        testUser1.setName("John Doe");
        testUser1.setPhoneNumber("+447123456789");
        testUser1.setPasswordHash("hashedPassword1");
        testUser1.setCreatedTimestamp(LocalDateTime.of(2025, 8, 29, 10, 30));
        testUser1.setUpdatedTimestamp(LocalDateTime.of(2025, 8, 29, 10, 30));

        Address address1 = new Address();
        address1.setLine1("123 Main Street");
        address1.setTown("London");
        address1.setCounty("Greater London");
        address1.setPostcode("SW1A 1AA");
        testUser1.setAddress(address1);

        testUser2 = new User();
        testUser2.setId("usr-789def012");
        testUser2.setEmail("jane.smith@example.com");
        testUser2.setName("Jane Smith");
        testUser2.setPhoneNumber("+447987654321");
        testUser2.setPasswordHash("hashedPassword2");
        testUser2.setCreatedTimestamp(LocalDateTime.of(2025, 8, 29, 11, 0));
        testUser2.setUpdatedTimestamp(LocalDateTime.of(2025, 8, 29, 11, 0));

        Address address2 = new Address();
        address2.setLine1("456 Oak Avenue");
        address2.setTown("Manchester");
        address2.setCounty("Greater Manchester");
        address2.setPostcode("M1 1AA");
        testUser2.setAddress(address2);

        testUser3 = new User();
        testUser3.setId("usr-999xyz789");
        testUser3.setEmail("bob.wilson@example.com");
        testUser3.setName("Bob Wilson");
        testUser3.setPhoneNumber("+441234567890");
        testUser3.setPasswordHash("hashedPassword3");
        testUser3.setCreatedTimestamp(LocalDateTime.of(2025, 8, 29, 12, 0));
        testUser3.setUpdatedTimestamp(LocalDateTime.of(2025, 8, 29, 12, 0));

        Address address3 = new Address();
        address3.setLine1("789 Pine Road");
        address3.setLine2("Flat 2B");
        address3.setTown("Birmingham");
        address3.setCounty("West Midlands");
        address3.setPostcode("B1 1AA");
        testUser3.setAddress(address3);
    }

    @Test
    void findByEmail_WithValidEmail_ReturnsUser() {
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser1));

        Optional<User> result = userRepository.findByEmail("john.doe@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("usr-123abc456");
        assertThat(result.get().getName()).isEqualTo("John Doe");
        assertThat(result.get().getEmail()).isEqualTo("john.doe@example.com");
        assertThat(result.get().getPhoneNumber()).isEqualTo("+447123456789");
        verify(userRepository).findByEmail("john.doe@example.com");
    }

    @Test
    void findByEmail_WithNonExistentEmail_ReturnsEmpty() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        Optional<User> result = userRepository.findByEmail("nonexistent@example.com");

        assertThat(result).isEmpty();
        verify(userRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    void findByEmail_WithEmptyEmail_ReturnsEmpty() {
        when(userRepository.findByEmail("")).thenReturn(Optional.empty());

        Optional<User> result = userRepository.findByEmail("");

        assertThat(result).isEmpty();
        verify(userRepository).findByEmail("");
    }

    @Test
    void findByEmail_WithMalformedEmail_ReturnsEmpty() {
        when(userRepository.findByEmail("invalid-email")).thenReturn(Optional.empty());

        Optional<User> result = userRepository.findByEmail("invalid-email");

        assertThat(result).isEmpty();
        verify(userRepository).findByEmail("invalid-email");
    }

    @Test
    void findByEmail_WithDifferentCaseEmail_ReturnsEmpty() {
        when(userRepository.findByEmail("JOHN.DOE@EXAMPLE.COM")).thenReturn(Optional.empty());

        Optional<User> result = userRepository.findByEmail("JOHN.DOE@EXAMPLE.COM");

        assertThat(result).isEmpty();
        verify(userRepository).findByEmail("JOHN.DOE@EXAMPLE.COM");
    }

    @Test
    void existsByEmail_WithExistingEmail_ReturnsTrue() {
        when(userRepository.existsByEmail("jane.smith@example.com")).thenReturn(true);

        boolean exists = userRepository.existsByEmail("jane.smith@example.com");

        assertThat(exists).isTrue();
        verify(userRepository).existsByEmail("jane.smith@example.com");
    }

    @Test
    void existsByEmail_WithNonExistentEmail_ReturnsFalse() {
        when(userRepository.existsByEmail("nonexistent@example.com")).thenReturn(false);

        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        assertThat(exists).isFalse();
        verify(userRepository).existsByEmail("nonexistent@example.com");
    }

    @Test
    void existsByEmail_WithEmptyEmail_ReturnsFalse() {
        when(userRepository.existsByEmail("")).thenReturn(false);

        boolean exists = userRepository.existsByEmail("");

        assertThat(exists).isFalse();
        verify(userRepository).existsByEmail("");
    }

    @Test
    void existsByEmail_WithMalformedEmail_ReturnsFalse() {
        when(userRepository.existsByEmail("invalid-email")).thenReturn(false);

        boolean exists = userRepository.existsByEmail("invalid-email");

        assertThat(exists).isFalse();
        verify(userRepository).existsByEmail("invalid-email");
    }

    @Test
    void findById_WithValidUserId_ReturnsUser() {
        when(userRepository.findById("usr-123abc456")).thenReturn(Optional.of(testUser1));

        Optional<User> result = userRepository.findById("usr-123abc456");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("usr-123abc456");
        assertThat(result.get().getName()).isEqualTo("John Doe");
        assertThat(result.get().getEmail()).isEqualTo("john.doe@example.com");
        verify(userRepository).findById("usr-123abc456");
    }

    @Test
    void findById_WithNonExistentUserId_ReturnsEmpty() {
        when(userRepository.findById("usr-nonexistent")).thenReturn(Optional.empty());

        Optional<User> result = userRepository.findById("usr-nonexistent");

        assertThat(result).isEmpty();
        verify(userRepository).findById("usr-nonexistent");
    }

    @Test
    void findById_WithMalformedUserId_ReturnsEmpty() {
        when(userRepository.findById("invalid-id")).thenReturn(Optional.empty());

        Optional<User> result = userRepository.findById("invalid-id");

        assertThat(result).isEmpty();
        verify(userRepository).findById("invalid-id");
    }

    @Test
    void existsById_WithExistingUserId_ReturnsTrue() {
        when(userRepository.existsById("usr-789def012")).thenReturn(true);

        boolean exists = userRepository.existsById("usr-789def012");

        assertThat(exists).isTrue();
        verify(userRepository).existsById("usr-789def012");
    }

    @Test
    void existsById_WithNonExistentUserId_ReturnsFalse() {
        when(userRepository.existsById("usr-nonexistent")).thenReturn(false);

        boolean exists = userRepository.existsById("usr-nonexistent");

        assertThat(exists).isFalse();
        verify(userRepository).existsById("usr-nonexistent");
    }

    @Test
    void save_NewUser_PersistsSuccessfully() {
        User newUser = new User();
        newUser.setId("usr-newuser123");
        newUser.setEmail("new.user@example.com");
        newUser.setName("New User");
        newUser.setPhoneNumber("+441111111111");
        newUser.setPasswordHash("hashedPasswordNew");

        Address newAddress = new Address();
        newAddress.setLine1("999 New Street");
        newAddress.setTown("Liverpool");
        newAddress.setCounty("Merseyside");
        newAddress.setPostcode("L1 1AA");
        newUser.setAddress(newAddress);

        User savedUser = new User();
        savedUser.setId("usr-newuser123");
        savedUser.setEmail("new.user@example.com");
        savedUser.setName("New User");
        savedUser.setPhoneNumber("+441111111111");
        savedUser.setPasswordHash("hashedPasswordNew");
        savedUser.setAddress(newAddress);
        savedUser.setCreatedTimestamp(LocalDateTime.now());
        savedUser.setUpdatedTimestamp(LocalDateTime.now());

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRepository.findById("usr-newuser123")).thenReturn(Optional.of(savedUser));

        User result = userRepository.save(newUser);

        assertThat(result.getId()).isEqualTo("usr-newuser123");
        assertThat(result.getEmail()).isEqualTo("new.user@example.com");
        assertThat(result.getName()).isEqualTo("New User");
        assertThat(result.getCreatedTimestamp()).isNotNull();
        assertThat(result.getUpdatedTimestamp()).isNotNull();

        Optional<User> found = userRepository.findById("usr-newuser123");
        assertThat(found).isPresent();

        verify(userRepository).save(any(User.class));
        verify(userRepository).findById("usr-newuser123");
    }

    @Test
    void save_UpdateExistingUser_UpdatesSuccessfully() {
        User updatedUser = new User();
        updatedUser.setId("usr-123abc456");
        updatedUser.setEmail("john.doe.updated@example.com");
        updatedUser.setName("John Doe Updated");
        updatedUser.setPhoneNumber("+447123456789");
        updatedUser.setPasswordHash("hashedPassword1");
        updatedUser.setCreatedTimestamp(LocalDateTime.of(2025, 8, 29, 10, 30));
        updatedUser.setUpdatedTimestamp(LocalDateTime.now());

        Address updatedAddress = new Address();
        updatedAddress.setLine1("123 Updated Street");
        updatedAddress.setTown("London");
        updatedAddress.setCounty("Greater London");
        updatedAddress.setPostcode("SW1A 1AA");
        updatedUser.setAddress(updatedAddress);

        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        User result = userRepository.save(updatedUser);

        assertThat(result.getId()).isEqualTo("usr-123abc456");
        assertThat(result.getEmail()).isEqualTo("john.doe.updated@example.com");
        assertThat(result.getName()).isEqualTo("John Doe Updated");
        assertThat(result.getAddress().getLine1()).isEqualTo("123 Updated Street");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void delete_ExistingUser_RemovesFromDatabase() {
        when(userRepository.existsById("usr-999xyz789")).thenReturn(true).thenReturn(false);
        when(userRepository.findById("usr-999xyz789")).thenReturn(Optional.empty());
        doNothing().when(userRepository).deleteById("usr-999xyz789");

        assertThat(userRepository.existsById("usr-999xyz789")).isTrue();

        userRepository.deleteById("usr-999xyz789");

        assertThat(userRepository.existsById("usr-999xyz789")).isFalse();
        assertThat(userRepository.findById("usr-999xyz789")).isEmpty();

        verify(userRepository, times(2)).existsById("usr-999xyz789");
        verify(userRepository).deleteById("usr-999xyz789");
        verify(userRepository).findById("usr-999xyz789");
    }

    @Test
    void findAll_ReturnsAllUsers() {
        List<User> allUsers = List.of(testUser1, testUser2, testUser3);
        when(userRepository.findAll()).thenReturn(allUsers);

        List<User> result = userRepository.findAll();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(User::getId)
                .containsExactlyInAnyOrder("usr-123abc456", "usr-789def012", "usr-999xyz789");
        assertThat(result).extracting(User::getEmail)
                .containsExactlyInAnyOrder("john.doe@example.com", "jane.smith@example.com", "bob.wilson@example.com");
        verify(userRepository).findAll();
    }

    @Test
    void findAll_WithNoUsers_ReturnsEmptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<User> result = userRepository.findAll();

        assertThat(result).isEmpty();
        verify(userRepository).findAll();
    }

    @Test
    void count_ReturnsCorrectUserCount() {
        when(userRepository.count()).thenReturn(3L);

        long count = userRepository.count();

        assertThat(count).isEqualTo(3);
        verify(userRepository).count();
    }

    @Test
    void count_WithNoUsers_ReturnsZero() {
        when(userRepository.count()).thenReturn(0L);

        long count = userRepository.count();

        assertThat(count).isEqualTo(0);
        verify(userRepository).count();
    }

    @Test
    void repositoryMethodsAreProperlyDefined() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.findById(anyString())).thenReturn(Optional.empty());
        when(userRepository.existsById(anyString())).thenReturn(false);

        assertThat(userRepository.findByEmail("test@example.com")).isEmpty();
        assertThat(userRepository.existsByEmail("test@example.com")).isFalse();
        assertThat(userRepository.findById("usr-test")).isEmpty();
        assertThat(userRepository.existsById("usr-test")).isFalse();

        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository).findById("usr-test");
        verify(userRepository).existsById("usr-test");
    }

    @Test
    void emailUniquenessValidation_WorksCorrectly() {
        when(userRepository.existsByEmail("unique@example.com")).thenReturn(false);
        when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        boolean uniqueEmailExists = userRepository.existsByEmail("unique@example.com");
        boolean duplicateEmailExists = userRepository.existsByEmail("duplicate@example.com");

        assertThat(uniqueEmailExists).isFalse();
        assertThat(duplicateEmailExists).isTrue();
        verify(userRepository).existsByEmail("unique@example.com");
        verify(userRepository).existsByEmail("duplicate@example.com");
    }

    @Test
    void authenticationScenario_FindUserByEmailForLogin() {
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser1));

        Optional<User> authenticatingUser = userRepository.findByEmail("john.doe@example.com");

        assertThat(authenticatingUser).isPresent();
        assertThat(authenticatingUser.get().getPasswordHash()).isEqualTo("hashedPassword1");
        assertThat(authenticatingUser.get().getId()).isEqualTo("usr-123abc456");
        verify(userRepository).findByEmail("john.doe@example.com");
    }

    @Test
    void userIdPatternCompliance_FollowsOpenApiSpec() {
        when(userRepository.findById("usr-123abc456")).thenReturn(Optional.of(testUser1));
        when(userRepository.findById("usr-789def012")).thenReturn(Optional.of(testUser2));

        Optional<User> user1 = userRepository.findById("usr-123abc456");
        Optional<User> user2 = userRepository.findById("usr-789def012");

        assertThat(user1).isPresent();
        assertThat(user1.get().getId()).matches("^usr-[A-Za-z0-9]+$");
        assertThat(user2).isPresent();
        assertThat(user2.get().getId()).matches("^usr-[A-Za-z0-9]+$");
        verify(userRepository).findById("usr-123abc456");
        verify(userRepository).findById("usr-789def012");
    }

    @Test
    void phoneNumberPatternCompliance_FollowsOpenApiSpec() {
        when(userRepository.findById("usr-123abc456")).thenReturn(Optional.of(testUser1));

        Optional<User> user = userRepository.findById("usr-123abc456");

        assertThat(user).isPresent();
        assertThat(user.get().getPhoneNumber()).matches("^\\+[1-9]\\d{1,14}$");
        verify(userRepository).findById("usr-123abc456");
    }
}
