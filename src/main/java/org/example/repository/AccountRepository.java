package org.example.repository;

import org.example.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Account entity operations
 * Primary key operations use accountNumber (String) as the identifier
 * Updated to leverage JPA relationships instead of string foreign keys
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    /**
     * Find all accounts belonging to a specific user using JPA relationship
     * @param userId the user ID
     * @return list of accounts owned by the user
     */
    @Query("SELECT a FROM Account a WHERE a.user.id = :userId")
    List<Account> findByUserId(@Param("userId") String userId);

    /**
     * Alternative method using property expression (JPA will handle the relationship)
     * @param userId the user ID
     * @return list of accounts owned by the user
     */
    List<Account> findByUser_Id(String userId);

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
     * Check if a user has any accounts using JPA relationship (for user deletion validation)
     * @param userId the user ID
     * @return true if user has any accounts
     */
    @Query("SELECT COUNT(a) > 0 FROM Account a WHERE a.user.id = :userId")
    boolean existsByUserId(@Param("userId") String userId);

    /**
     * Alternative method using property expression
     * @param userId the user ID
     * @return true if user has any accounts
     */
    boolean existsByUser_Id(String userId);

    /**
     * Find accounts with their user information eagerly loaded
     * @param accountNumber the account number
     * @return Optional containing account with user loaded
     */
    @Query("SELECT a FROM Account a JOIN FETCH a.user WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberWithUser(@Param("accountNumber") String accountNumber);

    // Note: findById(String accountNumber) and existsById(String accountNumber) are inherited from JpaRepository
}
