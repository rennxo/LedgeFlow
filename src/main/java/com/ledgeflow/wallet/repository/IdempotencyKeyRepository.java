package com.ledgeflow.wallet.repository;

import com.ledgeflow.wallet.entity.IdempotencyKeyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyRecord, String> {
    Optional<IdempotencyKeyRecord> findByIdempotencyKey(String idempotencyKey);
    void deleteByExpiresAtBefore(LocalDateTime now);
}
