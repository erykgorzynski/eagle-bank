package org.example.repository;

import org.example.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Transaction entity operations
 * Maps directly to the transaction API scenarios
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    /**
     * Find all transactions for a specific account ordered by creation timestamp descending
     * Used for: GET /v1/accounts/{accountId}/transactions
     * @param accountNumber the account number
     * @return list of transactions ordered by newest first
     */
    List<Transaction> findByAccount_AccountNumberOrderByCreatedTimestampDesc(String accountNumber);

    /**
     * Find a specific transaction by ID within a specific account
     * Used for: GET /v1/accounts/{accountId}/transactions/{transactionId}
     * @param id the transaction ID
     * @param accountNumber the account number
     * @return Optional containing transaction if found on specified account
     */
    Optional<Transaction> findByIdAndAccount_AccountNumber(String id, String accountNumber);
}
