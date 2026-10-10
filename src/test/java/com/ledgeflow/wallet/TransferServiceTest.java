package com.ledgeflow.wallet;

import com.ledgeflow.wallet.dto.transaction.TransferRequest;
import com.ledgeflow.wallet.entity.User;
import com.ledgeflow.wallet.entity.Wallet;
import com.ledgeflow.wallet.enums.AccountStatus;
import com.ledgeflow.wallet.enums.Currency;
import com.ledgeflow.wallet.enums.KycTier;
import com.ledgeflow.wallet.enums.Role;
import com.ledgeflow.wallet.exception.InsufficientFundsException;
import com.ledgeflow.wallet.exception.KycLimitExceededException;
import com.ledgeflow.wallet.repository.WalletRepository;
import com.ledgeflow.wallet.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TransferServiceTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private WalletRepository walletRepository;

    private Wallet senderWallet;
    private Wallet recipientWallet;

    @BeforeEach
    void setUp() {
        senderWallet = walletRepository.findByWalletNumber("LF-100001").orElseThrow();
        recipientWallet = walletRepository.findByWalletNumber("LF-100002").orElseThrow();
    }

    @Test
    @DisplayName("Should transfer funds atomically between wallets")
    void testSuccessfulTransfer() {
        BigDecimal initialSenderBalance = senderWallet.getBalance();
        BigDecimal initialRecipientBalance = recipientWallet.getBalance();
        BigDecimal transferAmount = new BigDecimal("50.00");

        var response = transferService.transfer(
                new TransferRequest(senderWallet.getWalletNumber(), recipientWallet.getWalletNumber(), transferAmount, "Test transfer"),
                "TEST-IDEMP-001"
        );

        assertNotNull(response);
        assertEquals("COMPLETED", response.getStatus().name());

        Wallet updatedSender = walletRepository.findByWalletNumber(senderWallet.getWalletNumber()).orElseThrow();
        Wallet updatedRecipient = walletRepository.findByWalletNumber(recipientWallet.getWalletNumber()).orElseThrow();

        assertEquals(0, initialSenderBalance.subtract(transferAmount).compareTo(updatedSender.getBalance()));
        assertEquals(0, initialRecipientBalance.add(transferAmount).compareTo(updatedRecipient.getBalance()));
    }

    @Test
    @DisplayName("Should throw InsufficientFundsException when sender does not have enough balance")
    void testInsufficientFunds() {
        BigDecimal transferAmount = new BigDecimal("9999999.00");

        assertThrows(InsufficientFundsException.class, () -> {
            transferService.transfer(
                    new TransferRequest(senderWallet.getWalletNumber(), recipientWallet.getWalletNumber(), transferAmount, "Excessive transfer"),
                    "TEST-IDEMP-INSUFFICIENT"
            );
        });
    }

    @Test
    @DisplayName("Should throw KycLimitExceededException when amount exceeds tier limit")
    void testKycLimitExceeded() {
        // TIER_2 limit is 5000.00. Trying to send 6000.00 should fail.
        BigDecimal excessiveAmount = new BigDecimal("6000.00");

        assertThrows(KycLimitExceededException.class, () -> {
            transferService.transfer(
                    new TransferRequest(senderWallet.getWalletNumber(), recipientWallet.getWalletNumber(), excessiveAmount, "Over limit transfer"),
                    "TEST-IDEMP-KYC"
            );
        });
    }

    @Test
    @DisplayName("Should return cached response when reusing Idempotency-Key (Preventing double debit)")
    void testIdempotencyPreventsDoubleCharge() {
        BigDecimal initialBalance = walletRepository.findByWalletNumber(senderWallet.getWalletNumber()).orElseThrow().getBalance();
        BigDecimal transferAmount = new BigDecimal("10.00");
        String idempotencyKey = "TEST-IDEMP-RETRY-KEY";

        // First call
        var resp1 = transferService.transfer(
                new TransferRequest(senderWallet.getWalletNumber(), recipientWallet.getWalletNumber(), transferAmount, "Payment 1"),
                idempotencyKey
        );

        // Second call with same idempotency key
        var resp2 = transferService.transfer(
                new TransferRequest(senderWallet.getWalletNumber(), recipientWallet.getWalletNumber(), transferAmount, "Payment 1"),
                idempotencyKey
        );

        assertEquals(resp1.getTransactionReference(), resp2.getTransactionReference());

        Wallet finalSender = walletRepository.findByWalletNumber(senderWallet.getWalletNumber()).orElseThrow();
        // Balance should have decreased only once by 10.00
        assertEquals(0, initialBalance.subtract(transferAmount).compareTo(finalSender.getBalance()));
    }
}
