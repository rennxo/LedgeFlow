package com.ledgeflow.wallet.dto.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class TransferRequest {

    @NotBlank(message = "Source wallet number is required")
    private String sourceWalletNumber;

    @NotBlank(message = "Target wallet number is required")
    private String targetWalletNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String description;

    public TransferRequest() {
    }

    public TransferRequest(String sourceWalletNumber, String targetWalletNumber, BigDecimal amount, String description) {
        this.sourceWalletNumber = sourceWalletNumber;
        this.targetWalletNumber = targetWalletNumber;
        this.amount = amount;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
