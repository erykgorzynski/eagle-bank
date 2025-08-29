package org.example.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Account entity representing bank accounts in the Eagle Bank system
 * Primary key is accountNumber with pattern ^01\d{6}$
 */
@Entity
@Table(name = "accounts",
       uniqueConstraints = @UniqueConstraint(columnNames = "account_number"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"user", "transactions"}) // Prevent circular reference in toString
public class Account {

    @Id
    @Column(name = "account_number", nullable = false)
    private String accountNumber; // Pattern: ^01\d{6}$

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Column(name = "balance", nullable = false)
    private Double balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "sort_code", nullable = false)
    private SortCode sortCode;

    @Column(name = "name", nullable = false)
    private String name;

    @CreationTimestamp
    @Column(name = "created_timestamp", nullable = false, updatable = false)
    private LocalDateTime createdTimestamp;

    @UpdateTimestamp
    @Column(name = "updated_timestamp", nullable = false)
    private LocalDateTime updatedTimestamp;

    // JPA Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_account_user"))
    private User user;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

    // Helper methods for managing bidirectional relationships
    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        transaction.setAccount(this);
    }

    public void removeTransaction(Transaction transaction) {
        transactions.remove(transaction);
        transaction.setAccount(null);
    }

    // Convenience method to get user ID (for backward compatibility)
    public String getUserId() {
        return user != null ? user.getId() : null;
    }

    public enum AccountType {
        PERSONAL
    }

    public enum Currency {
        GBP
    }

    public enum SortCode {
        _10_10_10("10-10-10");

        private final String value;

        SortCode(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
