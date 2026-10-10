package com.ledgeflow.wallet.service;

import com.ledgeflow.wallet.entity.AuditLog;
import com.ledgeflow.wallet.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String action, String performedBy, String details, String ipAddress) {
        AuditLog log = new AuditLog(action, performedBy, details, ipAddress);
        auditLogRepository.save(log);
    }
}
