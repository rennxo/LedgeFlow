package com.ledgeflow.wallet.enums;

import java.math.BigDecimal;

public enum KycTier {
    TIER_1("Basic", new BigDecimal("500.00"), new BigDecimal("2000.00")),
    TIER_2("Verified", new BigDecimal("5000.00"), new BigDecimal("25000.00")),
    TIER_3("Enhanced", new BigDecimal("100000.00"), new BigDecimal("500000.00")),
    UNLIMITED("Enterprise", new BigDecimal("10000000.00"), new BigDecimal("100000000.00"));

    private final String label;
    private final BigDecimal dailyTransferLimit;
    private final BigDecimal maxBalance;

    KycTier(String label, BigDecimal dailyTransferLimit, BigDecimal maxBalance) {
        this.label = label;
        this.dailyTransferLimit = dailyTransferLimit;
        this.maxBalance = maxBalance;
    }

    public String getLabel() {
        return label;
    }

    public BigDecimal getDailyTransferLimit() {
        return dailyTransferLimit;
    }

    public BigDecimal getMaxBalance() {
        return maxBalance;
    }
}
