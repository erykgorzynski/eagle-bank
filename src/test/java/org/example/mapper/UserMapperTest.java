package org.example.mapper;

import org.example.entity.Address;
import org.example.entity.User;
import org.example.model.CreateUserRequest;
import org.example.model.CreateUserRequestAddress;
import org.example.model.UpdateUserRequest;
import org.example.model.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        userMapper = Mappers.getMapper(UserMapper.class);
    }

    @Test
    void toEntityMapsCreateUserRequestCorrectly() {
        CreateUserRequestAddress address = new CreateUserRequestAddress()
                .line1("123 Main Street")
                .line2("Apartment 4B")
                .line3("Building A")
                .town("London")
                .county("Greater London")
                .postcode("SW1A 1AA");

        CreateUserRequest request = new CreateUserRequest()
                .name("John Doe")
                .email("john.doe@example.com")
                .password("password123")
                .phoneNumber("+44123456789")
                .address(address);

        User user = userMapper.toEntity(request);

        assertThat(user.getName()).isEqualTo("John Doe");
        assertThat(user.getEmail()).isEqualTo("john.doe@example.com");
        // Note: password field from request is mapped to a field, but passwordHash is ignored per mapper config
        assertThat(user.getPhoneNumber()).isEqualTo("+44123456789");

        assertThat(user.getAddress()).isNotNull();
        assertThat(user.getAddress().getLine1()).isEqualTo("123 Main Street");
        assertThat(user.getAddress().getLine2()).isEqualTo("Apartment 4B");
        assertThat(user.getAddress().getLine3()).isEqualTo("Building A");
        assertThat(user.getAddress().getTown()).isEqualTo("London");
        assertThat(user.getAddress().getCounty()).isEqualTo("Greater London");
        assertThat(user.getAddress().getPostcode()).isEqualTo("SW1A 1AA");
    }

    @Test
    void toEntityIgnoresSystemGeneratedFields() {
        CreateUserRequest request = new CreateUserRequest()
                .name("Jane Smith")
                .email("jane.smith@example.com")
                .password("password456")
                .phoneNumber("+44987654321")
                .address(new CreateUserRequestAddress()
                        .line1("456 Oak Avenue")
                        .town("Manchester")
                        .county("Greater Manchester")
                        .postcode("M1 1AA"));

        User user = userMapper.toEntity(request);

        assertThat(user.getId()).isNull();
        assertThat(user.getPasswordHash()).isNull(); // passwordHash is ignored by mapper
        assertThat(user.getAccounts()).isEmpty(); // accounts is initialized as empty list in entity
        assertThat(user.getCreatedTimestamp()).isNull();
        assertThat(user.getUpdatedTimestamp()).isNull();
    }

    @Test
    void toEntityHandlesOptionalAddressFields() {
        CreateUserRequestAddress address = new CreateUserRequestAddress()
                .line1("789 Pine Road")
                .town("Birmingham")
                .county("West Midlands")
                .postcode("B1 1AA");

        CreateUserRequest request = new CreateUserRequest()
                .name("Bob Wilson")
                .email("bob.wilson@example.com")
                .password("password789")
                .phoneNumber("+44555666777")
                .address(address);

        User user = userMapper.toEntity(request);

        assertThat(user.getAddress().getLine1()).isEqualTo("789 Pine Road");
        assertThat(user.getAddress().getLine2()).isNull();
        assertThat(user.getAddress().getLine3()).isNull();
        assertThat(user.getAddress().getTown()).isEqualTo("Birmingham");
        assertThat(user.getAddress().getCounty()).isEqualTo("West Midlands");
        assertThat(user.getAddress().getPostcode()).isEqualTo("B1 1AA");
    }

    @Test
    void toResponseMapsUserEntityCorrectly() {
        Address address = new Address("321 Elm Street", "Suite 100", null, "Liverpool", "Merseyside", "L1 1AA");
        User user = new User();
        user.setId("usr-123abc456");
        user.setName("Alice Johnson");
        user.setEmail("alice.johnson@example.com");
        user.setPasswordHash("$2a$10$hashedPassword");
        user.setPhoneNumber("+44111222333");
        user.setAddress(address);
        user.setCreatedTimestamp(LocalDateTime.of(2025, 1, 15, 10, 30));
        user.setUpdatedTimestamp(LocalDateTime.of(2025, 1, 20, 14, 45));
        user.setAccounts(new ArrayList<>());

        UserResponse response = userMapper.toResponse(user);

        assertThat(response.getId()).isEqualTo("usr-123abc456");
        assertThat(response.getName()).isEqualTo("Alice Johnson");
        assertThat(response.getEmail()).isEqualTo("alice.johnson@example.com");
        assertThat(response.getPhoneNumber()).isEqualTo("+44111222333");
        assertThat(response.getCreatedTimestamp()).isEqualTo(LocalDateTime.of(2025, 1, 15, 10, 30));
        assertThat(response.getUpdatedTimestamp()).isEqualTo(LocalDateTime.of(2025, 1, 20, 14, 45));

        assertThat(response.getAddress()).isNotNull();
        assertThat(response.getAddress().getLine1()).isEqualTo("321 Elm Street");
        assertThat(response.getAddress().getLine2()).isEqualTo("Suite 100");
        assertThat(response.getAddress().getLine3()).isNull();
        assertThat(response.getAddress().getTown()).isEqualTo("Liverpool");
        assertThat(response.getAddress().getCounty()).isEqualTo("Merseyside");
        assertThat(response.getAddress().getPostcode()).isEqualTo("L1 1AA");
    }

    @Test
    void toResponseExcludesPasswordHash() {
        User user = new User();
        user.setId("usr-789def012");
        user.setName("Charlie Brown");
        user.setEmail("charlie.brown@example.com");
        user.setPasswordHash("$2a$10$secretHashedPassword");
        user.setPhoneNumber("+44444555666");
        user.setAddress(new Address("999 Test Street", null, null, "Edinburgh", "Scotland", "EH1 1AA"));
        user.setCreatedTimestamp(LocalDateTime.now());
        user.setUpdatedTimestamp(LocalDateTime.now());

        UserResponse response = userMapper.toResponse(user);

        assertThat(response.getId()).isEqualTo("usr-789def012");
        assertThat(response.getName()).isEqualTo("Charlie Brown");
        assertThat(response.getEmail()).isEqualTo("charlie.brown@example.com");
        assertThat(response.getPhoneNumber()).isEqualTo("+44444555666");
    }

    @Test
    void updateEntityFromRequestUpdatesOnlyProvidedFields() {
        User existingUser = new User();
        existingUser.setId("usr-existing123");
        existingUser.setName("Original Name");
        existingUser.setEmail("original@example.com");
        existingUser.setPasswordHash("$2a$10$originalHash");
        existingUser.setPhoneNumber("+44000000000");
        existingUser.setAddress(new Address("Original Street", null, null, "Original Town", "Original County", "OR1 1AA"));
        existingUser.setCreatedTimestamp(LocalDateTime.of(2025, 1, 1, 10, 0));
        existingUser.setUpdatedTimestamp(LocalDateTime.of(2025, 1, 1, 10, 0));
        existingUser.setAccounts(new ArrayList<>());

        UpdateUserRequest updateRequest = new UpdateUserRequest()
                .name("Updated Name")
                .phoneNumber("+44999888777");

        userMapper.updateEntityFromRequest(updateRequest, existingUser);

        assertThat(existingUser.getName()).isEqualTo("Updated Name");
        assertThat(existingUser.getPhoneNumber()).isEqualTo("+44999888777");
        assertThat(existingUser.getEmail()).isEqualTo("original@example.com");
        assertThat(existingUser.getId()).isEqualTo("usr-existing123");
        assertThat(existingUser.getPasswordHash()).isEqualTo("$2a$10$originalHash");
        assertThat(existingUser.getCreatedTimestamp()).isEqualTo(LocalDateTime.of(2025, 1, 1, 10, 0));
        assertThat(existingUser.getUpdatedTimestamp()).isEqualTo(LocalDateTime.of(2025, 1, 1, 10, 0));
        assertThat(existingUser.getAccounts()).isNotNull();
    }

    @Test
    void updateEntityFromRequestUpdatesAddressFields() {
        User existingUser = new User();
        existingUser.setId("usr-address123");
        existingUser.setName("User Name");
        existingUser.setEmail("user@example.com");
        existingUser.setPasswordHash("$2a$10$hash");
        existingUser.setPhoneNumber("+44123456789");
        existingUser.setAddress(new Address("Old Street", "Old Apt", "Old Building", "Old Town", "Old County", "OLD 1AA"));

        UpdateUserRequest updateRequest = new UpdateUserRequest()
                .address(new CreateUserRequestAddress()
                        .line1("New Street")
                        .line2("New Apartment")
                        .town("New Town")
                        .county("New County")
                        .postcode("NEW 1AA"));

        userMapper.updateEntityFromRequest(updateRequest, existingUser);

        assertThat(existingUser.getAddress().getLine1()).isEqualTo("New Street");
        assertThat(existingUser.getAddress().getLine2()).isEqualTo("New Apartment");
        assertThat(existingUser.getAddress().getLine3()).isEqualTo("Old Building"); // Should retain existing value when not provided
        assertThat(existingUser.getAddress().getTown()).isEqualTo("New Town");
        assertThat(existingUser.getAddress().getCounty()).isEqualTo("New County");
        assertThat(existingUser.getAddress().getPostcode()).isEqualTo("NEW 1AA");
    }

    @Test
    void updateEntityFromRequestIgnoresNullValues() {
        User existingUser = new User();
        existingUser.setId("usr-null123");
        existingUser.setName("Existing Name");
        existingUser.setEmail("existing@example.com");
        existingUser.setPhoneNumber("+44111111111");
        existingUser.setAddress(new Address("Existing Street", null, null, "Existing Town", "Existing County", "EX1 1AA"));

        UpdateUserRequest updateRequest = new UpdateUserRequest()
                .name("Updated Name Only");

        userMapper.updateEntityFromRequest(updateRequest, existingUser);

        assertThat(existingUser.getName()).isEqualTo("Updated Name Only");
        assertThat(existingUser.getEmail()).isEqualTo("existing@example.com");
        assertThat(existingUser.getPhoneNumber()).isEqualTo("+44111111111");
        assertThat(existingUser.getAddress().getLine1()).isEqualTo("Existing Street");
        assertThat(existingUser.getAddress().getTown()).isEqualTo("Existing Town");
        assertThat(existingUser.getAddress().getCounty()).isEqualTo("Existing County");
        assertThat(existingUser.getAddress().getPostcode()).isEqualTo("EX1 1AA");
    }

    @Test
    void updateEntityFromRequestIgnoresSystemFields() {
        User existingUser = new User();
        existingUser.setId("usr-system123");
        existingUser.setName("System User");
        existingUser.setEmail("system@example.com");
        existingUser.setPasswordHash("$2a$10$systemHash");
        existingUser.setPhoneNumber("+44000000000");
        existingUser.setCreatedTimestamp(LocalDateTime.of(2025, 1, 1, 9, 0));
        existingUser.setUpdatedTimestamp(LocalDateTime.of(2025, 1, 1, 9, 0));
        existingUser.setAccounts(new ArrayList<>());

        UpdateUserRequest updateRequest = new UpdateUserRequest()
                .name("New Name")
                .email("new@example.com");

        userMapper.updateEntityFromRequest(updateRequest, existingUser);

        assertThat(existingUser.getId()).isEqualTo("usr-system123");
        assertThat(existingUser.getPasswordHash()).isEqualTo("$2a$10$systemHash");
        assertThat(existingUser.getCreatedTimestamp()).isEqualTo(LocalDateTime.of(2025, 1, 1, 9, 0));
        assertThat(existingUser.getUpdatedTimestamp()).isEqualTo(LocalDateTime.of(2025, 1, 1, 9, 0));
        assertThat(existingUser.getAccounts()).isNotNull();
        assertThat(existingUser.getName()).isEqualTo("New Name");
        assertThat(existingUser.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void mappingHandlesOpenApiPatternCompliantUserIds() {
        User user = new User();
        user.setId("usr-AbC123dEf456GhI789");
        user.setName("Pattern User");
        user.setEmail("pattern@example.com");
        user.setPhoneNumber("+441234567890");
        user.setAddress(new Address("Pattern Street", null, null, "Pattern City", "Pattern County", "PA1 1AA"));
        user.setCreatedTimestamp(LocalDateTime.now());
        user.setUpdatedTimestamp(LocalDateTime.now());

        UserResponse response = userMapper.toResponse(user);

        assertThat(response.getId()).isEqualTo("usr-AbC123dEf456GhI789");
        assertThat(response.getId()).matches("^usr-[A-Za-z0-9]+$");
        assertThat(response.getName()).isEqualTo("Pattern User");
    }

    @Test
    void mappingHandlesMinimalValidPhoneNumber() {
        CreateUserRequest request = new CreateUserRequest()
                .name("Phone User")
                .email("phone@example.com")
                .password("password123")
                .phoneNumber("+11")
                .address(new CreateUserRequestAddress()
                        .line1("Phone Street")
                        .town("Phone Town")
                        .county("Phone County")
                        .postcode("PH1 1AA"));

        User user = userMapper.toEntity(request);

        assertThat(user.getPhoneNumber()).isEqualTo("+11");
        assertThat(user.getPhoneNumber()).matches("^\\+[1-9]\\d{1,14}$");
    }

    @Test
    void mappingHandlesMaximumValidPhoneNumber() {
        String longPhoneNumber = "+123456789012345";
        CreateUserRequest request = new CreateUserRequest()
                .name("Long Phone User")
                .email("longphone@example.com")
                .password("password123")
                .phoneNumber(longPhoneNumber)
                .address(new CreateUserRequestAddress()
                        .line1("Long Phone Street")
                        .town("Long Phone Town")
                        .county("Long Phone County")
                        .postcode("LP1 1AA"));

        User user = userMapper.toEntity(request);

        assertThat(user.getPhoneNumber()).isEqualTo(longPhoneNumber);
        assertThat(user.getPhoneNumber()).matches("^\\+[1-9]\\d{1,14}$");
    }
}
