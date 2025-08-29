package org.example.mapper;

import org.example.entity.User;
import org.example.model.CreateUserRequest;
import org.example.model.UpdateUserRequest;
import org.example.model.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for User entity and DTOs
 * Updated to handle JPA relationships properly
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    /**
     * Map CreateUserRequest to User entity
     * Ignores fields that will be set by service layer including JPA relationships
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "accounts", ignore = true) // JPA relationship - managed by JPA
    @Mapping(target = "createdTimestamp", ignore = true)
    @Mapping(target = "updatedTimestamp", ignore = true)
    User toEntity(CreateUserRequest createUserRequest);

    /**
     * Map User entity to UserResponse
     * Excludes passwordHash for security and ignores JPA relationships
     */
    @Mapping(target = "address", source = "address")
    UserResponse toResponse(User user);

    /**
     * Update existing User entity with UpdateUserRequest data
     * Only updates non-null values from the request, ignores relationships and system fields
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "accounts", ignore = true) // JPA relationship - don't modify
    @Mapping(target = "createdTimestamp", ignore = true)
    @Mapping(target = "updatedTimestamp", ignore = true)
    void updateEntityFromRequest(UpdateUserRequest updateUserRequest, @MappingTarget User user);
}
