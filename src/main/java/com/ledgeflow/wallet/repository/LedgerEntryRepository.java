package com.ledgeflow.wallet.repository;

import com.ledgeflow.wallet.entity.LedgerEntry;
import com.ledgeflow.wallet.enums.EntryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    Optional<LedgerEntry> findTopByOrderBySequenceNumberDesc();

    List<LedgerEntry> findByTransactionIdOrderBySequenceNumberAsc(Long transactionId);

    Page<LedgerEntry> findByWalletNumberOrderBySequenceNumberDesc(String walletNumber, Pageable pageable);

    List<LedgerEntry> findAllByOrderBySequenceNumberAsc();

    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM LedgerEntry l WHERE l.entryType = :entryType")
    BigDecimal sumAmountByEntryType(@Param("entryType") EntryType entryType);

    @Query("SELECT COUNT(l) FROM LedgerEntry l WHERE l.walletNumber = :walletNumber")
    long countByWalletNumber(@Param("walletNumber") String walletNumber);
}
