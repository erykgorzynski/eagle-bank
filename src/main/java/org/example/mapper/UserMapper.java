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
 * Handles mapping between different user representations
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    /**
     * Map CreateUserRequest to User entity
     * Ignores id, passwordHash, and timestamps (will be set by service)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "createdTimestamp", ignore = true)
    @Mapping(target = "updatedTimestamp", ignore = true)
    User toEntity(CreateUserRequest createUserRequest);

    /**
     * Map User entity to UserResponse
     * Excludes passwordHash for security
     */
    @Mapping(target = "address", source = "address")
    UserResponse toResponse(User user);

    /**
     * Update existing User entity with UpdateUserRequest data
     * Only updates non-null values from the request
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "createdTimestamp", ignore = true)
    @Mapping(target = "updatedTimestamp", ignore = true)
    void updateEntityFromRequest(UpdateUserRequest updateUserRequest, @MappingTarget User user);
}
