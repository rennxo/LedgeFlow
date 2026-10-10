package com.ledgeflow.wallet.service;

import com.ledgeflow.wallet.entity.IdempotencyKeyRecord;
import com.ledgeflow.wallet.exception.IdempotencyConflictException;
import com.ledgeflow.wallet.repository.IdempotencyKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;

    public IdempotencyService(IdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<IdempotencyKeyRecord> checkAndLock(String key, String requestHash) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }

        Optional<IdempotencyKeyRecord> existing = repository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            IdempotencyKeyRecord record = existing.get();
            if (Boolean.TRUE.equals(record.getInProgress())) {
                throw new IdempotencyConflictException("A request with key '" + key + "' is currently being processed.");
            }
            if (!record.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException("Idempotency key reused with mismatched request payload.");
            }
            return Optional.of(record);
        }

        IdempotencyKeyRecord newRecord = new IdempotencyKeyRecord(
                key,
                requestHash,
                LocalDateTime.now().plusHours(24)
        );
        repository.save(newRecord);
        return Optional.empty();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(String key, String responseBody, int httpStatus) {
        if (key == null || key.isBlank()) {
            return;
        }

        repository.findByIdempotencyKey(key).ifPresent(record -> {
            record.setInProgress(false);
            record.setResponseBody(responseBody);
            record.setHttpStatus(httpStatus);
            repository.save(record);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseLockOnFailure(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        repository.deleteById(key);
    }
}
