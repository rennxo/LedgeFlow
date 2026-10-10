package com.ledgeflow.wallet.dto.transaction;

import com.ledgeflow.wallet.enums.Currency;
import com.ledgeflow.wallet.enums.TransactionStatus;
import com.ledgeflow.wallet.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponse {

    private String transactionReference;
    private TransactionType type;
    private TransactionStatus status;
    private String sourceWalletNumber;
    private String targetWalletNumber;
    private BigDecimal amount;
    private BigDecimal fee;
    private Currency currency;
    private String description;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public TransactionResponse() {
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getSourceWalletNumber() {
        return sourceWalletNumber;
    }

    public void setSourceWalletNumber(String sourceWalletNumber) {
        this.sourceWalletNumber = sourceWalletNumber;
    }

    public String getTargetWalletNumber() {
        return targetWalletNumber;
    }

    public void setTargetWalletNumber(String targetWalletNumber) {
        this.targetWalletNumber = targetWalletNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
