package org.example.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Transaction entity representing financial transactions in the Eagle Bank system
 * Primary key is id with pattern ^tan-[A-Za-z0-9]+$
 */
@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "account") // Prevent circular reference in toString
public class Transaction {

    @Id
    @Column(name = "id", nullable = false)
    private String id; // Pattern: ^tan-[A-Za-z0-9]+$

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false)
    private Currency currency;

    @Column(name = "reference")
    private String reference;

    @CreationTimestamp
    @Column(name = "created_timestamp", nullable = false, updatable = false)
    private LocalDateTime createdTimestamp;

    // JPA Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_number", nullable = false, foreignKey = @ForeignKey(name = "fk_transaction_account"))
    private Account account;

    // Convenience methods for backward compatibility
    public String getAccountNumber() {
        return account != null ? account.getAccountNumber() : null;
    }

    public String getUserId() {
        return account != null ? account.getUserId() : null;
    }

    public enum TransactionType {
        DEPOSIT,
        WITHDRAWAL
    }

    public enum Currency {
        GBP
    }
}
