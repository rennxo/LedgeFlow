package com.ledgeflow.wallet.repository;

import com.ledgeflow.wallet.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionReference(String transactionReference);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT t FROM Transaction t WHERE t.sourceWallet.id = :walletId OR t.targetWallet.id = :walletId ORDER BY t.createdAt DESC")
    Page<Transaction> findByWalletIdOrderByCreatedAtDesc(@Param("walletId") Long walletId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.sourceWallet.walletNumber = :walletNumber OR t.targetWallet.walletNumber = :walletNumber ORDER BY t.createdAt DESC")
    Page<Transaction> findByWalletNumberOrderByCreatedAtDesc(@Param("walletNumber") String walletNumber, Pageable pageable);
}
