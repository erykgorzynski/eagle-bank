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
 * Updated to handle JPA relationships properly
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AccountMapper {

    /**
     * Map CreateBankAccountRequest to Account entity
     * Ignores fields that will be set by service layer including JPA relationships
     */
    @Mapping(target = "accountNumber", ignore = true)
    @Mapping(target = "balance", ignore = true)
    @Mapping(target = "user", ignore = true) // JPA relationship - set by service
    @Mapping(target = "transactions", ignore = true) // JPA relationship - managed by JPA
    @Mapping(target = "createdTimestamp", ignore = true)
    @Mapping(target = "updatedTimestamp", ignore = true)
    @Mapping(target = "currency", constant = "GBP")
    @Mapping(target = "sortCode", constant = "_10_10_10")
    Account toEntity(CreateBankAccountRequest createBankAccountRequest);

    /**
     * Map Account entity to BankAccountResponse
     * Maps only the available fields in the response model
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
     * Only updates non-null values from the request, ignores relationships and system fields
     */
    @Mapping(target = "accountNumber", ignore = true)
    @Mapping(target = "balance", ignore = true)
    @Mapping(target = "user", ignore = true) // JPA relationship - don't modify
    @Mapping(target = "transactions", ignore = true) // JPA relationship - don't modify
    @Mapping(target = "createdTimestamp", ignore = true)
    @Mapping(target = "updatedTimestamp", ignore = true)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "sortCode", ignore = true)
    void updateEntityFromRequest(UpdateBankAccountRequest updateBankAccountRequest, @MappingTarget Account account);

    /**
     * Map Account.AccountType to BankAccountResponse.AccountTypeEnum
     */
    default BankAccountResponse.AccountTypeEnum mapAccountType(Account.AccountType accountType) {
        if (accountType == null) {
            return null;
        }
        return switch (accountType) {
            case PERSONAL -> BankAccountResponse.AccountTypeEnum.PERSONAL;
        };
    }

    /**
     * Map Account.Currency to BankAccountResponse.CurrencyEnum
     */
    default BankAccountResponse.CurrencyEnum mapCurrency(Account.Currency currency) {
        if (currency == null) {
            return null;
        }
        return switch (currency) {
            case GBP -> BankAccountResponse.CurrencyEnum.GBP;
        };
    }

    /**
     * Map Account.SortCode to BankAccountResponse.SortCodeEnum
     */
    default BankAccountResponse.SortCodeEnum mapSortCode(Account.SortCode sortCode) {
        if (sortCode == null) {
            return null;
        }
        return switch (sortCode) {
            case _10_10_10 -> BankAccountResponse.SortCodeEnum._10_10_10;
        };
    }
}
