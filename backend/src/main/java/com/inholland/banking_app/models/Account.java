package com.inholland.banking_app.models;

import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.AccountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_user_id", nullable = false)
    private User customer;

    @Column(nullable = false, unique = true, length = 34)
    private String iban;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(name = "absolute_transfer_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal absoluteTransferLimit;

    @Column(name = "daily_transfer_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal dailyTransferLimit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    public boolean isOwnedBy(User user) {
        return this.customer.getId().equals(user.getId());
    }

    public boolean isClosed() {
        return this.status == AccountStatus.CLOSED;
    }

    public void updateLimits(BigDecimal absoluteLimit, BigDecimal dailyLimit) {
        if (isClosed()) {
            throw new IllegalStateException("Cannot update a closed account");
        }
        if (absoluteLimit != null) {
            this.absoluteTransferLimit = absoluteLimit;
        }
        if (dailyLimit != null) {
            this.dailyTransferLimit = dailyLimit;
        }
    }

    public void close() {
        if (isClosed()) {
            throw new IllegalStateException("Account is already closed");
        }
        this.status = AccountStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }
}