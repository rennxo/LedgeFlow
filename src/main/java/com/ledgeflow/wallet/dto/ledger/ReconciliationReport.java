package com.ledgeflow.wallet.dto.ledger;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReconciliationReport {

    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private BigDecimal discrepancy;
    private boolean balanced;
    private long totalLedgerEntries;
    private long totalTransactions;
    private LocalDateTime generatedAt;

    public ReconciliationReport() {
        this.generatedAt = LocalDateTime.now();
    }

    public ReconciliationReport(BigDecimal totalDebits, BigDecimal totalCredits,
                                long totalLedgerEntries, long totalTransactions) {
        this.totalDebits = totalDebits;
        this.totalCredits = totalCredits;
        this.discrepancy = totalDebits.subtract(totalCredits).abs();
        this.balanced = this.discrepancy.compareTo(BigDecimal.ZERO) == 0;
        this.totalLedgerEntries = totalLedgerEntries;
        this.totalTransactions = totalTransactions;
        this.generatedAt = LocalDateTime.now();
    }

    public BigDecimal getTotalDebits() {
        return totalDebits;
    }

    public void setTotalDebits(BigDecimal totalDebits) {
        this.totalDebits = totalDebits;
    }

    public BigDecimal getTotalCredits() {
        return totalCredits;
    }

    public void setTotalCredits(BigDecimal totalCredits) {
        this.totalCredits = totalCredits;
    }

    public BigDecimal getDiscrepancy() {
        return discrepancy;
    }

    public void setDiscrepancy(BigDecimal discrepancy) {
        this.discrepancy = discrepancy;
    }

    public boolean isBalanced() {
        return balanced;
    }

    public void setBalanced(boolean balanced) {
        this.balanced = balanced;
    }

    public long getTotalLedgerEntries() {
        return totalLedgerEntries;
    }

    public void setTotalLedgerEntries(long totalLedgerEntries) {
        this.totalLedgerEntries = totalLedgerEntries;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
