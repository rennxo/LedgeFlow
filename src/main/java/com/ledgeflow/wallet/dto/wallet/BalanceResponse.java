package com.ledgeflow.wallet.dto.wallet;

import com.ledgeflow.wallet.enums.Currency;

import java.math.BigDecimal;

public class BalanceResponse {

    private String walletNumber;
    private Currency currency;
    private BigDecimal balance;
    private BigDecimal availableBalance;
    private BigDecimal heldBalance;

    public BalanceResponse() {
    }

    public BalanceResponse(String walletNumber, Currency currency, BigDecimal balance, BigDecimal availableBalance, BigDecimal heldBalance) {
        this.walletNumber = walletNumber;
        this.currency = currency;
        this.balance = balance;
        this.availableBalance = availableBalance;
        this.heldBalance = heldBalance;
    }

    public String getWalletNumber() {
        return walletNumber;
    }

    public void setWalletNumber(String walletNumber) {
        this.walletNumber = walletNumber;
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
}
