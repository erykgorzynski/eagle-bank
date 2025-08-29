package org.example.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
public class Transaction {

    @Id
    @Column(name = "id", nullable = false)
    private String id; // Pattern: ^tan-[A-Za-z0-9]+$

    @Column(name = "account_number", nullable = false)
    private String accountNumber; // References Account.accountNumber

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false)
    private Currency currency;

    @Column(name = "reference")
    private String reference;

    @Column(name = "user_id", nullable = false)
    private String userId; // References User.id (usr-[A-Za-z0-9]+)

    @CreationTimestamp
    @Column(name = "created_timestamp", nullable = false, updatable = false)
    private LocalDateTime createdTimestamp;

    public enum TransactionType {
        DEPOSIT,
        WITHDRAWAL
    }

    public enum Currency {
        GBP
    }
}
