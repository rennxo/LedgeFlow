package com.ledgeflow.wallet.service;

import com.ledgeflow.wallet.dto.ledger.IntegrityCheckResponse;
import com.ledgeflow.wallet.dto.ledger.LedgerEntryResponse;
import com.ledgeflow.wallet.dto.ledger.ReconciliationReport;
import com.ledgeflow.wallet.entity.LedgerEntry;
import com.ledgeflow.wallet.entity.Transaction;
import com.ledgeflow.wallet.enums.EntryType;
import com.ledgeflow.wallet.enums.LedgerAccountType;
import com.ledgeflow.wallet.repository.LedgerEntryRepository;
import com.ledgeflow.wallet.repository.TransactionRepository;
import com.ledgeflow.wallet.util.HashUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LedgerService {

    public static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransactionRepository transactionRepository;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository,
                         TransactionRepository transactionRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public List<LedgerEntry> recordDoubleEntry(
            Transaction transaction,
            String debitWalletNumber,
            LedgerAccountType debitAccountType,
            BigDecimal debitBalanceAfter,
            String creditWalletNumber,
            LedgerAccountType creditAccountType,
            BigDecimal creditBalanceAfter,
            BigDecimal amount) {

        Optional<LedgerEntry> lastEntryOpt = ledgerEntryRepository.findTopByOrderBySequenceNumberDesc();
        long lastSeq = lastEntryOpt.map(LedgerEntry::getSequenceNumber).orElse(0L);
        String lastHash = lastEntryOpt.map(LedgerEntry::getEntryHash).orElse(GENESIS_HASH);

        // 1. First Leg: DEBIT entry
        long seq1 = lastSeq + 1;
        String debitHash = calculateHash(lastHash, seq1, debitWalletNumber, EntryType.DEBIT, amount, debitBalanceAfter);
        LedgerEntry debitEntry = new LedgerEntry(
                transaction,
                debitWalletNumber,
                debitAccountType,
                EntryType.DEBIT,
                amount,
                debitBalanceAfter,
                seq1,
                lastHash,
                debitHash
        );
        LedgerEntry savedDebit = ledgerEntryRepository.save(debitEntry);

        // 2. Second Leg: CREDIT entry (chained from the debit entry's hash)
        long seq2 = seq1 + 1;
        String creditHash = calculateHash(debitHash, seq2, creditWalletNumber, EntryType.CREDIT, amount, creditBalanceAfter);
        LedgerEntry creditEntry = new LedgerEntry(
                transaction,
                creditWalletNumber,
                creditAccountType,
                EntryType.CREDIT,
                amount,
                creditBalanceAfter,
                seq2,
                debitHash,
                creditHash
        );
        LedgerEntry savedCredit = ledgerEntryRepository.save(creditEntry);

        return List.of(savedDebit, savedCredit);
    }

    public String calculateHash(String prevHash, long sequence, String walletNumber,
                                EntryType entryType, BigDecimal amount, BigDecimal balanceAfter) {
        String payload = prevHash + ":" + sequence + ":" + walletNumber + ":" +
                entryType.name() + ":" + amount.setScale(4).toPlainString() + ":" +
                balanceAfter.setScale(4).toPlainString();
        return HashUtil.sha256(payload);
    }

    @Transactional(readOnly = true)
    public IntegrityCheckResponse verifyLedgerIntegrity() {
        List<LedgerEntry> entries = ledgerEntryRepository.findAllByOrderBySequenceNumberAsc();
        List<String> inconsistencies = new ArrayList<>();

        if (entries.isEmpty()) {
            return new IntegrityCheckResponse(true, 0, "Ledger is empty. No records to verify.", inconsistencies);
        }

        String expectedPrevHash = GENESIS_HASH;
        long expectedSeq = 1L;

        for (LedgerEntry entry : entries) {
            // Verify sequence continuity
            if (entry.getSequenceNumber() != expectedSeq) {
                inconsistencies.add(String.format("Sequence gap at entry ID %d: expected sequence #%d but found #%d",
                        entry.getId(), expectedSeq, entry.getSequenceNumber()));
            }

            // Verify previous hash chaining
            if (!expectedPrevHash.equalsIgnoreCase(entry.getPreviousHash())) {
                inconsistencies.add(String.format("Broken hash chain at sequence #%d (ID %d): expected previous hash [%s], but found [%s]",
                        entry.getSequenceNumber(), entry.getId(), expectedPrevHash, entry.getPreviousHash()));
            }

            // Recompute SHA-256 and check for tampering
            String recomputedHash = calculateHash(
                    entry.getPreviousHash(),
                    entry.getSequenceNumber(),
                    entry.getWalletNumber(),
                    entry.getEntryType(),
                    entry.getAmount(),
                    entry.getBalanceAfter()
            );

            if (!recomputedHash.equalsIgnoreCase(entry.getEntryHash())) {
                inconsistencies.add(String.format("Cryptographic signature mismatch at sequence #%d (ID %d)! Data was tampered! Expected hash [%s], found [%s]",
                        entry.getSequenceNumber(), entry.getId(), recomputedHash, entry.getEntryHash()));
            }

            expectedPrevHash = entry.getEntryHash();
            expectedSeq = entry.getSequenceNumber() + 1;
        }

        boolean isValid = inconsistencies.isEmpty();
        String message = isValid
                ? "All ledger blocks successfully verified. Cryptographic chain is authentic and untampered."
                : "Integrity check failed! Discrepancies detected in ledger chain.";

        return new IntegrityCheckResponse(isValid, entries.size(), message, inconsistencies);
    }

    @Transactional(readOnly = true)
    public ReconciliationReport reconcileLedger() {
        BigDecimal totalDebits = ledgerEntryRepository.sumAmountByEntryType(EntryType.DEBIT);
        BigDecimal totalCredits = ledgerEntryRepository.sumAmountByEntryType(EntryType.CREDIT);
        long totalEntries = ledgerEntryRepository.count();
        long totalTransactions = transactionRepository.count();

        return new ReconciliationReport(totalDebits, totalCredits, totalEntries, totalTransactions);
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntryResponse> getEntriesByWalletNumber(String walletNumber, Pageable pageable) {
        return ledgerEntryRepository.findByWalletNumberOrderBySequenceNumberDesc(walletNumber, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> getEntriesForTransaction(Long transactionId) {
        return ledgerEntryRepository.findByTransactionIdOrderBySequenceNumberAsc(transactionId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> getRecentEntries() {
        List<LedgerEntry> list = ledgerEntryRepository.findAllByOrderBySequenceNumberAsc();
        int size = list.size();
        int fromIndex = Math.max(0, size - 50);
        return list.subList(fromIndex, size).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public LedgerEntryResponse mapToResponse(LedgerEntry entry) {
        LedgerEntryResponse resp = new LedgerEntryResponse();
        resp.setId(entry.getId());
        resp.setTransactionReference(entry.getTransaction() != null ? entry.getTransaction().getTransactionReference() : null);
        resp.setWalletNumber(entry.getWalletNumber());
        resp.setAccountType(entry.getAccountType());
        resp.setEntryType(entry.getEntryType());
        resp.setAmount(entry.getAmount());
        resp.setBalanceAfter(entry.getBalanceAfter());
        resp.setSequenceNumber(entry.getSequenceNumber());
        resp.setPreviousHash(entry.getPreviousHash());
        resp.setEntryHash(entry.getEntryHash());
        resp.setTimestamp(entry.getTimestamp());
        return resp;
    }
}
