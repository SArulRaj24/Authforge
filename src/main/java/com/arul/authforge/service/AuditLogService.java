package com.arul.authforge.service;

import com.arul.authforge.dto.AuditLogResponse;
import com.arul.authforge.entity.AuditLog;
import com.arul.authforge.repository.AuditLogRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(
            Long userId,
            String eventType,
            Pageable pageable) {

        Page<AuditLog> auditLogs =
                auditLogRepository.searchAuditLogs(
                        userId,
                        eventType,
                        pageable
                );

        return auditLogs.map(this::convertToResponse);
    }

    private AuditLogResponse convertToResponse(AuditLog auditLog) {

        Long userId = auditLog.getUser() != null
                ? auditLog.getUser().getId()
                : null;

        return new AuditLogResponse(
                auditLog.getId(),
                userId,
                auditLog.getEventType(),
                auditLog.getIpAddress(),
                auditLog.getUserAgent(),
                auditLog.getDetails(),
                auditLog.getCreatedAt()
        );
    }
}