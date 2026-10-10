package com.ledgeflow.wallet.controller;

import com.ledgeflow.wallet.dto.common.ApiResponse;
import com.ledgeflow.wallet.dto.transaction.DepositRequest;
import com.ledgeflow.wallet.dto.transaction.TransactionResponse;
import com.ledgeflow.wallet.dto.transaction.TransferRequest;
import com.ledgeflow.wallet.dto.transaction.WithdrawRequest;
import com.ledgeflow.wallet.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Deposit, withdrawal, and peer-to-peer money transfers")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransferService transferService;

    public TransactionController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/deposit")
    @Operation(summary = "Deposit funds", description = "Simulates funding a wallet via payment gateway. Posts dual ledger records.")
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
            @Valid @RequestBody DepositRequest request,
            @Parameter(description = "Optional idempotency key to prevent double charging")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        TransactionResponse response = transferService.deposit(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Funds deposited successfully", response));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw funds", description = "Debits funds from user wallet to external platform reserve.")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(
            @Valid @RequestBody WithdrawRequest request,
            @Parameter(description = "Optional idempotency key to prevent double charging")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        TransactionResponse response = transferService.withdraw(request, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.ok("Withdrawal processed successfully", response));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer money", description = "Executes an atomic P2P wallet transfer with pessimistic locking and dual ledger entries.")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @Valid @RequestBody TransferRequest request,
            @Parameter(description = "Optional idempotency key to prevent double charging")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        TransactionResponse response = transferService.transfer(request, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.ok("Transfer completed successfully", response));
    }

    @GetMapping("/{reference}")
    @Operation(summary = "Get transaction details", description = "Finds a transaction record by its unique reference code.")
    public ResponseEntity<ApiResponse<TransactionResponse>> getByReference(@PathVariable String reference) {
        TransactionResponse response = transferService.getByReference(reference);
        return ResponseEntity.ok(ApiResponse.ok("Transaction found", response));
    }

    @GetMapping("/wallet/{walletNumber}")
    @Operation(summary = "Get wallet transactions", description = "Lists transaction history for a specific wallet.")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getWalletTransactions(
            @PathVariable String walletNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<TransactionResponse> response = transferService.getTransactionsByWallet(walletNumber, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok("Transactions retrieved", response));
    }
}
