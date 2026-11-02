package com.arul.authforge.repository;

import com.arul.authforge.entity.AuditLog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long> {

    @Query("""
            SELECT a
            FROM AuditLog a
            WHERE (:userId IS NULL OR a.user.id = :userId)
              AND (:eventType IS NULL OR a.eventType = :eventType)
            """)
    Page<AuditLog> searchAuditLogs(
            @Param("userId") Long userId,
            @Param("eventType") String eventType,
            Pageable pageable
    );
}