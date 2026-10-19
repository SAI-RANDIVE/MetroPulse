package com.metropulse.service;

import com.metropulse.entity.AppUser;
import com.metropulse.entity.AuditLog;
import com.metropulse.enums.AuditAction;
import com.metropulse.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/** Records administrative actions. Callers must never pass passwords, hashes, tokens or secrets as details. */
@Service
public class AuditService {

    private final AuditLogRepository auditLogs;
    private final Clock clock;

    public AuditService(AuditLogRepository auditLogs, Clock clock) {
        this.auditLogs = auditLogs;
        this.clock = clock;
    }

    @Transactional
    public void record(AppUser actor, AuditAction action, String entityType, Long entityId, String details) {
        auditLogs.save(new AuditLog(actor, action, entityType, entityId, details, clock.instant()));
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> page(Pageable pageable) {
        return auditLogs.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> latest() {
        return auditLogs.findTop8ByOrderByCreatedAtDesc();
    }
}
