package com.arul.authforge.controller;

import com.arul.authforge.dto.AuditLogResponse;
import com.arul.authforge.service.AuditLogService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public Page<AuditLogResponse> getAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative"
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100"
            );
        }

        if (userId != null && userId < 1) {
            throw new IllegalArgumentException(
                    "User ID must be positive"
            );
        }

        if (eventType != null) {
            eventType = eventType.trim();

            if (eventType.isEmpty() || eventType.length() > 50) {
                throw new IllegalArgumentException(
                        "Invalid event type"
                );
            }
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return auditLogService.getAuditLogs(
                userId,
                eventType,
                pageable
        );
    }
}