package org.example.mapper;

import org.example.entity.Account;
import org.example.model.BankAccountResponse;
import org.example.model.CreateBankAccountRequest;
import org.example.model.UpdateBankAccountRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * MapStruct mapper for Account entity and DTOs
 * Handles mapping between different account representations
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AccountMapper {

    /**
     * Map CreateBankAccountRequest to Account entity
     * Ignores accountNumber, balance, userId, and timestamps (will be set by service)
     */
    @Mapping(target = "accountNumber", ignore = true)
    @Mapping(target = "balance", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdTimestamp", ignore = true)
    @Mapping(target = "updatedTimestamp", ignore = true)
    @Mapping(target = "currency", constant = "GBP")
    @Mapping(target = "sortCode", constant = "_10_10_10")
    Account toEntity(CreateBankAccountRequest createBankAccountRequest);

    /**
     * Map Account entity to BankAccountResponse
     */
    @Mapping(target = "accountType", source = "accountType")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "sortCode", source = "sortCode")
    BankAccountResponse toResponse(Account account);

    /**
     * Map list of Account entities to list of BankAccountResponse
     */
    List<BankAccountResponse> toResponseList(List<Account> accounts);

    /**
     * Update existing Account entity with UpdateBankAccountRequest data
     * Only updates non-null values from the request
     */
    @Mapping(target = "accountNumber", ignore = true)
    @Mapping(target = "balance", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "sortCode", ignore = true)
    @Mapping(target = "createdTimestamp", ignore = true)
    @Mapping(target = "updatedTimestamp", ignore = true)
    void updateEntityFromRequest(UpdateBankAccountRequest updateBankAccountRequest, @MappingTarget Account account);
}

