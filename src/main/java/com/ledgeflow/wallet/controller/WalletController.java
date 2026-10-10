package com.ledgeflow.wallet.controller;

import com.ledgeflow.wallet.dto.common.ApiResponse;
import com.ledgeflow.wallet.dto.wallet.BalanceResponse;
import com.ledgeflow.wallet.dto.wallet.CreateWalletRequest;
import com.ledgeflow.wallet.dto.wallet.WalletResponse;
import com.ledgeflow.wallet.enums.AccountStatus;
import com.ledgeflow.wallet.enums.KycTier;
import com.ledgeflow.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wallets")
@Tag(name = "Wallets", description = "Digital wallet management and balance endpoints")
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    @Operation(summary = "Create currency wallet", description = "Creates a secondary wallet for a given currency (USD, EUR, INR, GBP)")
    public ResponseEntity<ApiResponse<WalletResponse>> createWallet(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateWalletRequest request) {
        WalletResponse response = walletService.createWallet(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Wallet created successfully", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user's wallets", description = "Lists all wallets associated with the authenticated user.")
    public ResponseEntity<ApiResponse<List<WalletResponse>>> getMyWallets(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<WalletResponse> wallets = walletService.getWalletsByUsername(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Wallets retrieved", wallets));
    }

    @GetMapping("/{walletNumber}")
    @Operation(summary = "Get wallet details", description = "Fetches metadata and balance details for a specific wallet.")
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(@PathVariable String walletNumber) {
        WalletResponse wallet = walletService.getWalletByNumber(walletNumber);
        return ResponseEntity.ok(ApiResponse.ok("Wallet retrieved", wallet));
    }

    @GetMapping("/{walletNumber}/balance")
    @Operation(summary = "Get live balance", description = "Fetches the current and available balance for a wallet.")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(@PathVariable String walletNumber) {
        BalanceResponse balance = walletService.getBalance(walletNumber);
        return ResponseEntity.ok(ApiResponse.ok("Balance retrieved", balance));
    }

    @PatchMapping("/{walletNumber}/kyc")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Upgrade KYC tier", description = "Admin-only endpoint to elevate user's KYC tier.")
    public ResponseEntity<ApiResponse<WalletResponse>> updateKyc(
            @PathVariable String walletNumber,
            @RequestParam KycTier tier) {
        WalletResponse response = walletService.updateKycTier(walletNumber, tier);
        return ResponseEntity.ok(ApiResponse.ok("KYC tier updated", response));
    }

    @PatchMapping("/{walletNumber}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Update wallet status", description = "Admin-only endpoint to freeze or unfreeze a wallet.")
    public ResponseEntity<ApiResponse<WalletResponse>> updateStatus(
            @PathVariable String walletNumber,
            @RequestParam AccountStatus status) {
        WalletResponse response = walletService.setStatus(walletNumber, status);
        return ResponseEntity.ok(ApiResponse.ok("Wallet status updated", response));
    }
}
