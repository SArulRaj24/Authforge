
package com.arul.authforge.service;

import com.arul.authforge.entity.AuditLog;
import com.arul.authforge.entity.User;
import com.arul.authforge.repository.AuditLogRepository;
import com.arul.authforge.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(
            AuditLogRepository auditLogRepository,
            UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEvent(
            Long userId,
            String eventType,
            String details) {

        HttpServletRequest request = getCurrentRequest();

        String ipAddress = request != null
                ? request.getRemoteAddr()
                : null;

        String userAgent = request != null
                ? request.getHeader("User-Agent")
                : null;

        if (userAgent != null && userAgent.length() > 500) {
            userAgent = userAgent.substring(0, 500);
        }

        User user = null;

        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        AuditLog auditLog = new AuditLog(
                user,
                eventType,
                ipAddress,
                userAgent,
                details
        );

        auditLogRepository.save(auditLog);
    }

    private HttpServletRequest getCurrentRequest() {
        if (RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }

        return null;
    }
}
