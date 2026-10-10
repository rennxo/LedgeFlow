package com.ledgeflow.wallet;

import com.ledgeflow.wallet.dto.ledger.IntegrityCheckResponse;
import com.ledgeflow.wallet.dto.ledger.ReconciliationReport;
import com.ledgeflow.wallet.entity.LedgerEntry;
import com.ledgeflow.wallet.repository.LedgerEntryRepository;
import com.ledgeflow.wallet.service.LedgerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LedgerIntegrityTest {

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Test
    @DisplayName("Ledger should be balanced: Total Debits == Total Credits")
    void testLedgerReconciliation() {
        ReconciliationReport report = ledgerService.reconcileLedger();
        assertNotNull(report);
        assertTrue(report.isBalanced(), "Ledger debits and credits must be exactly balanced");
        assertEquals(0, BigDecimal.ZERO.compareTo(report.getDiscrepancy()));
        assertTrue(report.getTotalLedgerEntries() > 0);
    }

    @Test
    @DisplayName("Cryptographic SHA-256 hash chain must be authentic and valid")
    void testCryptographicChainVerification() {
        IntegrityCheckResponse response = ledgerService.verifyLedgerIntegrity();
        assertNotNull(response);
        assertTrue(response.isValid(), "Cryptographic hash chain must be valid");
        assertTrue(response.getTotalEntriesChecked() > 0);
        assertTrue(response.getInconsistencies().isEmpty());
    }

    @Test
    @DisplayName("Should detect database tampering if a ledger entry is modified directly")
    void testTamperDetection() {
        List<LedgerEntry> entries = ledgerEntryRepository.findAllByOrderBySequenceNumberAsc();
        assertFalse(entries.isEmpty());

        LedgerEntry targetEntry = entries.get(0);
        BigDecimal originalAmount = targetEntry.getAmount();

        try {
            // Simulate malicious database tampering: altering the amount without re-signing hash
            targetEntry.setAmount(originalAmount.add(new BigDecimal("9999.00")));
            ledgerEntryRepository.save(targetEntry);

            IntegrityCheckResponse response = ledgerService.verifyLedgerIntegrity();
            assertFalse(response.isValid(), "Ledger integrity check must detect the tampered block");
            assertFalse(response.getInconsistencies().isEmpty());
            assertTrue(response.getInconsistencies().get(0).contains("signature mismatch")
                    || response.getInconsistencies().get(0).contains("tampered"));
        } finally {
            // Restore original amount
            targetEntry.setAmount(originalAmount);
            ledgerEntryRepository.save(targetEntry);
        }
    }
}
