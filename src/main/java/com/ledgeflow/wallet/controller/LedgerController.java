package com.ledgeflow.wallet.controller;

import com.ledgeflow.wallet.dto.common.ApiResponse;
import com.ledgeflow.wallet.dto.ledger.IntegrityCheckResponse;
import com.ledgeflow.wallet.dto.ledger.LedgerEntryResponse;
import com.ledgeflow.wallet.dto.ledger.ReconciliationReport;
import com.ledgeflow.wallet.service.LedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ledger")
@Tag(name = "Ledger", description = "Double-entry bookkeeping, cryptographic audit trail, and reconciliation")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent ledger entries", description = "Returns the most recent entries in the immutable ledger chain.")
    public ResponseEntity<ApiResponse<List<LedgerEntryResponse>>> getRecentEntries() {
        List<LedgerEntryResponse> entries = ledgerService.getRecentEntries();
        return ResponseEntity.ok(ApiResponse.ok("Ledger entries retrieved", entries));
    }

    @GetMapping("/wallet/{walletNumber}")
    @Operation(summary = "Get ledger entries for wallet", description = "Retrieves all debit and credit postings for a specific wallet.")
    public ResponseEntity<ApiResponse<Page<LedgerEntryResponse>>> getEntriesByWallet(
            @PathVariable String walletNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<LedgerEntryResponse> entries = ledgerService.getEntriesByWalletNumber(walletNumber, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok("Wallet ledger entries retrieved", entries));
    }

    @GetMapping("/transaction/{transactionId}")
    @Operation(summary = "Get ledger entries for transaction", description = "Retrieves the balanced dual entries for a specific transaction.")
    public ResponseEntity<ApiResponse<List<LedgerEntryResponse>>> getEntriesByTransaction(@PathVariable Long transactionId) {
        List<LedgerEntryResponse> entries = ledgerService.getEntriesForTransaction(transactionId);
        return ResponseEntity.ok(ApiResponse.ok("Transaction ledger entries retrieved", entries));
    }

    @GetMapping("/verify-integrity")
    @Operation(summary = "Verify ledger integrity", description = "Performs a cryptographic audit of the SHA-256 hash chain to verify zero tampering.")
    public ResponseEntity<ApiResponse<IntegrityCheckResponse>> verifyIntegrity() {
        IntegrityCheckResponse response = ledgerService.verifyLedgerIntegrity();
        return ResponseEntity.ok(ApiResponse.ok("Integrity check completed", response));
    }

    @GetMapping("/reconciliation")
    @Operation(summary = "Reconcile ledger accounts", description = "Verifies accounting equation: sum(Debits) equals sum(Credits).")
    public ResponseEntity<ApiResponse<ReconciliationReport>> getReconciliation() {
        ReconciliationReport report = ledgerService.reconcileLedger();
        return ResponseEntity.ok(ApiResponse.ok("Reconciliation report generated", report));
    }
}
