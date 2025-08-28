package org.example.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

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
public class Account {

    @Id
    @Column(name = "account_number", nullable = false)
    private String accountNumber; // Pattern: ^01\d{6}$

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Column(name = "balance", nullable = false, precision = 10, scale = 2)
    private Double balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "sort_code", nullable = false)
    private SortCode sortCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "user_id", nullable = false)
    private String userId; // References User.id (usr-[A-Za-z0-9]+)

    @CreationTimestamp
    @Column(name = "created_timestamp", nullable = false, updatable = false)
    private LocalDateTime createdTimestamp;

    @UpdateTimestamp
    @Column(name = "updated_timestamp", nullable = false)
    private LocalDateTime updatedTimestamp;

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
