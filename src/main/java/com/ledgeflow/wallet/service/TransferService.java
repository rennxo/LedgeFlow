package com.ledgeflow.wallet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgeflow.wallet.dto.transaction.DepositRequest;
import com.ledgeflow.wallet.dto.transaction.TransactionResponse;
import com.ledgeflow.wallet.dto.transaction.TransferRequest;
import com.ledgeflow.wallet.dto.transaction.WithdrawRequest;
import com.ledgeflow.wallet.entity.IdempotencyKeyRecord;
import com.ledgeflow.wallet.entity.Transaction;
import com.ledgeflow.wallet.entity.Wallet;
import com.ledgeflow.wallet.enums.AccountStatus;
import com.ledgeflow.wallet.enums.LedgerAccountType;
import com.ledgeflow.wallet.enums.TransactionStatus;
import com.ledgeflow.wallet.enums.TransactionType;
import com.ledgeflow.wallet.exception.InsufficientFundsException;
import com.ledgeflow.wallet.exception.KycLimitExceededException;
import com.ledgeflow.wallet.exception.ResourceNotFoundException;
import com.ledgeflow.wallet.exception.WalletFrozenException;
import com.ledgeflow.wallet.repository.TransactionRepository;
import com.ledgeflow.wallet.repository.WalletRepository;
import com.ledgeflow.wallet.util.HashUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransferService {

    public static final String PLATFORM_RESERVE_WALLET = "LF-PLATFORM-RESERVE";

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerService ledgerService;
    private final IdempotencyService idempotencyService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public TransferService(WalletRepository walletRepository,
                           TransactionRepository transactionRepository,
                           LedgerService ledgerService,
                           IdempotencyService idempotencyService,
                           AuditService auditService,
                           ObjectMapper objectMapper) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerService = ledgerService;
        this.idempotencyService = idempotencyService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public TransactionResponse deposit(DepositRequest request, String idempotencyKey) {
        String requestHash = HashUtil.sha256("DEPOSIT:" + request.getWalletNumber() + ":" + request.getAmount().toPlainString());
        Optional<IdempotencyKeyRecord> cached = idempotencyService.checkAndLock(idempotencyKey, requestHash);
        if (cached.isPresent()) {
            return deserializeCached(cached.get().getResponseBody(), TransactionResponse.class);
        }

        try {
            Wallet wallet = walletRepository.findByWalletNumberWithLock(request.getWalletNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet not found: " + request.getWalletNumber()));

            validateWalletActive(wallet);

            BigDecimal amount = request.getAmount().setScale(4, RoundingMode.HALF_EVEN);
            BigDecimal newBalance = wallet.getBalance().add(amount);
            wallet.setBalance(newBalance);
            walletRepository.save(wallet);

            String reference = generateTransactionReference();
            Transaction transaction = new Transaction(
                    reference,
                    idempotencyKey,
                    TransactionType.DEPOSIT,
                    null,
                    wallet,
                    amount,
                    BigDecimal.ZERO,
                    wallet.getCurrency(),
                    request.getDescription() != null ? request.getDescription() : "Wallet Deposit"
            );
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setCompletedAt(LocalDateTime.now());
            Transaction savedTrx = transactionRepository.save(transaction);

            // Double-entry: Debit Platform Cash Reserve, Credit User Wallet
            ledgerService.recordDoubleEntry(
                    savedTrx,
                    PLATFORM_RESERVE_WALLET,
                    LedgerAccountType.PLATFORM_CASH,
                    BigDecimal.ZERO,
                    wallet.getWalletNumber(),
                    LedgerAccountType.USER_WALLET,
                    newBalance,
                    amount
            );

            auditService.logAction("WALLET_DEPOSIT",
                    wallet.getUser() != null ? wallet.getUser().getUsername() : "SYSTEM",
                    "Deposited " + amount + " " + wallet.getCurrency() + " to wallet " + wallet.getWalletNumber(),
                    null);

            TransactionResponse response = mapToResponse(savedTrx);
            storeIdempotencySuccess(idempotencyKey, response);
            return response;
        } catch (Exception e) {
            idempotencyService.releaseLockOnFailure(idempotencyKey);
            throw e;
        }
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public TransactionResponse withdraw(WithdrawRequest request, String idempotencyKey) {
        String requestHash = HashUtil.sha256("WITHDRAW:" + request.getWalletNumber() + ":" + request.getAmount().toPlainString());
        Optional<IdempotencyKeyRecord> cached = idempotencyService.checkAndLock(idempotencyKey, requestHash);
        if (cached.isPresent()) {
            return deserializeCached(cached.get().getResponseBody(), TransactionResponse.class);
        }

        try {
            Wallet wallet = walletRepository.findByWalletNumberWithLock(request.getWalletNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet not found: " + request.getWalletNumber()));

            validateWalletActive(wallet);

            BigDecimal amount = request.getAmount().setScale(4, RoundingMode.HALF_EVEN);
            if (wallet.getAvailableBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException(String.format("Insufficient funds. Available: %s %s, Requested: %s %s",
                        wallet.getAvailableBalance(), wallet.getCurrency(), amount, wallet.getCurrency()));
            }

            BigDecimal newBalance = wallet.getBalance().subtract(amount);
            wallet.setBalance(newBalance);
            walletRepository.save(wallet);

            String reference = generateTransactionReference();
            Transaction transaction = new Transaction(
                    reference,
                    idempotencyKey,
                    TransactionType.WITHDRAWAL,
                    wallet,
                    null,
                    amount,
                    BigDecimal.ZERO,
                    wallet.getCurrency(),
                    request.getDescription() != null ? request.getDescription() : "Wallet Withdrawal"
            );
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setCompletedAt(LocalDateTime.now());
            Transaction savedTrx = transactionRepository.save(transaction);

            // Double-entry: Debit User Wallet, Credit Platform Cash Reserve
            ledgerService.recordDoubleEntry(
                    savedTrx,
                    wallet.getWalletNumber(),
                    LedgerAccountType.USER_WALLET,
                    newBalance,
                    PLATFORM_RESERVE_WALLET,
                    LedgerAccountType.PLATFORM_CASH,
                    BigDecimal.ZERO,
                    amount
            );

            auditService.logAction("WALLET_WITHDRAWAL",
                    wallet.getUser() != null ? wallet.getUser().getUsername() : "SYSTEM",
                    "Withdrew " + amount + " " + wallet.getCurrency() + " from wallet " + wallet.getWalletNumber(),
                    null);

            TransactionResponse response = mapToResponse(savedTrx);
            storeIdempotencySuccess(idempotencyKey, response);
            return response;
        } catch (Exception e) {
            idempotencyService.releaseLockOnFailure(idempotencyKey);
            throw e;
        }
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public TransactionResponse transfer(TransferRequest request, String idempotencyKey) {
        if (request.getSourceWalletNumber().equalsIgnoreCase(request.getTargetWalletNumber())) {
            throw new IllegalArgumentException("Cannot transfer funds to the same wallet.");
        }

        String requestHash = HashUtil.sha256("TRANSFER:" + request.getSourceWalletNumber() + ":"
                + request.getTargetWalletNumber() + ":" + request.getAmount().toPlainString());
        Optional<IdempotencyKeyRecord> cached = idempotencyService.checkAndLock(idempotencyKey, requestHash);
        if (cached.isPresent()) {
            return deserializeCached(cached.get().getResponseBody(), TransactionResponse.class);
        }

        try {
            Wallet sourceWalletLookup = walletRepository.findByWalletNumber(request.getSourceWalletNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Source wallet not found: " + request.getSourceWalletNumber()));
            Wallet targetWalletLookup = walletRepository.findByWalletNumber(request.getTargetWalletNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Target wallet not found: " + request.getTargetWalletNumber()));

            if (sourceWalletLookup.getCurrency() != targetWalletLookup.getCurrency()) {
                throw new IllegalArgumentException(String.format("Cross-currency transfers not supported (%s to %s).",
                        sourceWalletLookup.getCurrency(), targetWalletLookup.getCurrency()));
            }

            // DEADLOCK PREVENTION: Always acquire pessimistic row locks in deterministic ID order
            Long firstId = sourceWalletLookup.getId() < targetWalletLookup.getId() ? sourceWalletLookup.getId() : targetWalletLookup.getId();
            Long secondId = sourceWalletLookup.getId() < targetWalletLookup.getId() ? targetWalletLookup.getId() : sourceWalletLookup.getId();

            Wallet lockedFirst = walletRepository.findByIdWithLock(firstId)
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet lock acquisition failed for ID: " + firstId));
            Wallet lockedSecond = walletRepository.findByIdWithLock(secondId)
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet lock acquisition failed for ID: " + secondId));

            Wallet sourceWallet = lockedFirst.getId().equals(sourceWalletLookup.getId()) ? lockedFirst : lockedSecond;
            Wallet targetWallet = lockedFirst.getId().equals(targetWalletLookup.getId()) ? lockedFirst : lockedSecond;

            validateWalletActive(sourceWallet);
            validateWalletActive(targetWallet);

            BigDecimal amount = request.getAmount().setScale(4, RoundingMode.HALF_EVEN);

            // KYC Transfer limit check
            BigDecimal dailyLimit = sourceWallet.getKycTier().getDailyTransferLimit();
            if (amount.compareTo(dailyLimit) > 0) {
                throw new KycLimitExceededException(String.format("Amount %s exceeds daily transfer limit of %s for KYC %s",
                        amount, dailyLimit, sourceWallet.getKycTier().getLabel()));
            }

            // Balance check
            if (sourceWallet.getAvailableBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException(String.format("Insufficient funds in source wallet. Available: %s, Requested: %s",
                        sourceWallet.getAvailableBalance(), amount));
            }

            // Debit source & Credit target
            BigDecimal sourceNewBalance = sourceWallet.getBalance().subtract(amount);
            BigDecimal targetNewBalance = targetWallet.getBalance().add(amount);

            sourceWallet.setBalance(sourceNewBalance);
            targetWallet.setBalance(targetNewBalance);

            walletRepository.save(sourceWallet);
            walletRepository.save(targetWallet);

            String reference = generateTransactionReference();
            Transaction transaction = new Transaction(
                    reference,
                    idempotencyKey,
                    TransactionType.TRANSFER,
                    sourceWallet,
                    targetWallet,
                    amount,
                    BigDecimal.ZERO,
                    sourceWallet.getCurrency(),
                    request.getDescription() != null ? request.getDescription() : "Peer-to-peer transfer"
            );
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setCompletedAt(LocalDateTime.now());
            Transaction savedTrx = transactionRepository.save(transaction);

            // Double-entry bookkeeping: Debit sender, Credit receiver
            ledgerService.recordDoubleEntry(
                    savedTrx,
                    sourceWallet.getWalletNumber(),
                    LedgerAccountType.USER_WALLET,
                    sourceNewBalance,
                    targetWallet.getWalletNumber(),
                    LedgerAccountType.USER_WALLET,
                    targetNewBalance,
                    amount
            );

            auditService.logAction("P2P_TRANSFER",
                    sourceWallet.getUser() != null ? sourceWallet.getUser().getUsername() : "SYSTEM",
                    String.format("Transferred %s %s from %s to %s",
                            amount, sourceWallet.getCurrency(), sourceWallet.getWalletNumber(), targetWallet.getWalletNumber()),
                    null);

            TransactionResponse response = mapToResponse(savedTrx);
            storeIdempotencySuccess(idempotencyKey, response);
            return response;
        } catch (Exception e) {
            idempotencyService.releaseLockOnFailure(idempotencyKey);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsByWallet(String walletNumber, Pageable pageable) {
        return transactionRepository.findByWalletNumberOrderByCreatedAtDesc(walletNumber, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getByReference(String reference) {
        Transaction trx = transactionRepository.findByTransactionReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + reference));
        return mapToResponse(trx);
    }

    private void validateWalletActive(Wallet wallet) {
        if (wallet.getStatus() == AccountStatus.FROZEN) {
            throw new WalletFrozenException("Wallet " + wallet.getWalletNumber() + " is frozen. Transactions are disabled.");
        }
        if (wallet.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Wallet " + wallet.getWalletNumber() + " is not active (Status: " + wallet.getStatus() + ")");
        }
    }

    private String generateTransactionReference() {
        return "TRX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private void storeIdempotencySuccess(String key, TransactionResponse response) {
        if (key != null && !key.isBlank()) {
            try {
                String json = objectMapper.writeValueAsString(response);
                idempotencyService.complete(key, json, 200);
            } catch (Exception ignored) {
            }
        }
    }

    private <T> T deserializeCached(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize idempotency cached response", e);
        }
    }

    public TransactionResponse mapToResponse(Transaction trx) {
        TransactionResponse resp = new TransactionResponse();
        resp.setTransactionReference(trx.getTransactionReference());
        resp.setType(trx.getType());
        resp.setStatus(trx.getStatus());
        resp.setSourceWalletNumber(trx.getSourceWallet() != null ? trx.getSourceWallet().getWalletNumber() : null);
        resp.setTargetWalletNumber(trx.getTargetWallet() != null ? trx.getTargetWallet().getWalletNumber() : null);
        resp.setAmount(trx.getAmount());
        resp.setFee(trx.getFee());
        resp.setCurrency(trx.getCurrency());
        resp.setDescription(trx.getDescription());
        resp.setFailureReason(trx.getFailureReason());
        resp.setCreatedAt(trx.getCreatedAt());
        resp.setCompletedAt(trx.getCompletedAt());
        return resp;
    }
}
