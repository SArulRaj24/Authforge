package com.arul.authforge.dto;

import java.time.OffsetDateTime;

public class AuditLogResponse {

    private Long id;
    private Long userId;
    private String eventType;
    private String ipAddress;
    private String userAgent;
    private String details;
    private OffsetDateTime createdAt;

    public AuditLogResponse(
            Long id,
            Long userId,
            String eventType,
            String ipAddress,
            String userAgent,
            String details,
            OffsetDateTime createdAt) {

        this.id = id;
        this.userId = userId;
        this.eventType = eventType;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.details = details;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getDetails() {
        return details;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}