package org.example.mapper;

import org.example.entity.Transaction;
import org.example.model.CreateTransactionRequest;
import org.example.model.TransactionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * MapStruct mapper for Transaction entity and DTOs
 * Updated to handle JPA relationships properly
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TransactionMapper {

    /**
     * Map CreateTransactionRequest to Transaction entity
     * Ignores fields that will be set by service layer including JPA relationships
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "createdTimestamp", ignore = true)
    @Mapping(target = "type", source = "type")
    @Mapping(target = "currency", source = "currency")
    Transaction toEntity(CreateTransactionRequest createTransactionRequest);

    /**
     * Map Transaction entity to TransactionResponse
     * Uses convenience methods for backward compatibility
     */
    @Mapping(target = "userId", expression = "java(transaction.getUserId())")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "currency", source = "currency")
    TransactionResponse toResponse(Transaction transaction);

    /**
     * Map list of Transaction entities to list of TransactionResponse
     */
    List<TransactionResponse> toResponseList(List<Transaction> transactions);

    /**
     * Map Transaction.TransactionType to TransactionResponse.TypeEnum
     */
    default TransactionResponse.TypeEnum mapTransactionType(Transaction.TransactionType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case DEPOSIT -> TransactionResponse.TypeEnum.DEPOSIT;
            case WITHDRAWAL -> TransactionResponse.TypeEnum.WITHDRAWAL;
        };
    }

    /**
     * Map CreateTransactionRequest.TypeEnum to Transaction.TransactionType
     */
    default Transaction.TransactionType mapRequestType(CreateTransactionRequest.TypeEnum type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case DEPOSIT -> Transaction.TransactionType.DEPOSIT;
            case WITHDRAWAL -> Transaction.TransactionType.WITHDRAWAL;
        };
    }

    /**
     * Map Transaction.Currency to TransactionResponse.CurrencyEnum
     */
    default TransactionResponse.CurrencyEnum mapCurrency(Transaction.Currency currency) {
        if (currency == null) {
            return null;
        }
        return switch (currency) {
            case GBP -> TransactionResponse.CurrencyEnum.GBP;
        };
    }

    /**
     * Map CreateTransactionRequest.CurrencyEnum to Transaction.Currency
     */
    default Transaction.Currency mapRequestCurrency(CreateTransactionRequest.CurrencyEnum currency) {
        if (currency == null) {
            return null;
        }
        return switch (currency) {
            case GBP -> Transaction.Currency.GBP;
        };
    }
}
