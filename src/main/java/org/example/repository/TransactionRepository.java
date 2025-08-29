package org.example.repository;

import org.example.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Transaction entity operations
 * Primary key operations use id (String) as the identifier
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    /**
     * Find all transactions for a specific account
     * @param accountNumber the account number
     * @return list of transactions for the account
     */
    List<Transaction> findByAccountNumber(String accountNumber);

    /**
     * Find transaction by ID and account number (for validation)
     * @param id the transaction ID
     * @param accountNumber the account number
     * @return Optional containing transaction if found on specified account
     */
    Optional<Transaction> findByIdAndAccountNumber(String id, String accountNumber);

    /**
     * Check if a transaction exists with the given ID and account number
     * @param id the transaction ID
     * @param accountNumber the account number
     * @return true if transaction exists on specified account
     */
    boolean existsByIdAndAccountNumber(String id, String accountNumber);

    /**
     * Find all transactions for a specific user (across all their accounts)
     * @param userId the user ID
     * @return list of transactions for the user
     */
    List<Transaction> findByUserId(String userId);

    /**
     * Find transactions by account number ordered by creation timestamp descending
     * @param accountNumber the account number
     * @return list of transactions ordered by newest first
     */
    List<Transaction> findByAccountNumberOrderByCreatedTimestampDesc(String accountNumber);
}
