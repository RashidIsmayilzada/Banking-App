package com.inholland.banking_app.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore
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

    public boolean hasSufficientBalance(BigDecimal amount) {
        return balance.subtract(amount).compareTo(absoluteTransferLimit) >= 0;
    }

    public boolean isClosed() {
        return this.status == AccountStatus.CLOSED;
    }

    public boolean isActive() {
        return this.status == AccountStatus.ACTIVE;
    }

    public boolean isFrozen() {
        return this.status == AccountStatus.FROZEN;
    }

    public void markFrozen() {
        this.status = AccountStatus.FROZEN;
    }

    public void unfreeze() {
        this.status = AccountStatus.ACTIVE;
    }


    public void applyLimits(BigDecimal absoluteLimit, BigDecimal dailyLimit) {
        if (absoluteLimit != null) {
            this.absoluteTransferLimit = absoluteLimit;
        }
        if (dailyLimit != null) {
            this.dailyTransferLimit = dailyLimit;
        }
    }

    public void markClosed() {
        this.status = AccountStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }
}
