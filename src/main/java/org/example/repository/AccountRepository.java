package org.example.repository;

import org.example.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Account entity operations
 * Primary key operations use accountNumber (String) as the identifier
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    /**
     * Find all accounts belonging to a specific user
     * @param userId the user ID
     * @return list of accounts owned by the user
     */
    List<Account> findByUserId(String userId);

    /**
     * Find account by account number
     * @param accountNumber the account number
     * @return Optional containing account if found
     */
    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Check if an account exists with the given account number (for uniqueness validation)
     * @param accountNumber the account number
     * @return true if account exists with this account number
     */
    boolean existsByAccountNumber(String accountNumber);

    /**
     * Check if a user has any accounts (for user deletion validation)
     * @param userId the user ID
     * @return true if user has any accounts
     */
    boolean existsByUserId(String userId);

    // Note: findById(String accountNumber) and existsById(String accountNumber) are inherited from JpaRepository
}
