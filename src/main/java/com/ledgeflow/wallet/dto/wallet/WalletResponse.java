package com.ledgeflow.wallet.dto.wallet;

import com.ledgeflow.wallet.enums.AccountStatus;
import com.ledgeflow.wallet.enums.Currency;
import com.ledgeflow.wallet.enums.KycTier;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletResponse {

    private Long id;
    private String walletNumber;
    private String username;
    private Currency currency;
    private BigDecimal balance;
    private BigDecimal availableBalance;
    private BigDecimal heldBalance;
    private KycTier kycTier;
    private BigDecimal dailyTransferLimit;
    private AccountStatus status;
    private LocalDateTime createdAt;

    public WalletResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWalletNumber() {
        return walletNumber;
    }

    public void setWalletNumber(String walletNumber) {
        this.walletNumber = walletNumber;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public BigDecimal getHeldBalance() {
        return heldBalance;
    }

    public void setHeldBalance(BigDecimal heldBalance) {
        this.heldBalance = heldBalance;
    }

    public KycTier getKycTier() {
        return kycTier;
    }

    public void setKycTier(KycTier kycTier) {
        this.kycTier = kycTier;
    }

    public BigDecimal getDailyTransferLimit() {
        return dailyTransferLimit;
    }

    public void setDailyTransferLimit(BigDecimal dailyTransferLimit) {
        this.dailyTransferLimit = dailyTransferLimit;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
