package com.arul.authforge.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash",
            nullable = false,
            unique = true,
            length = 64)
    private String tokenHash;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by_token_id")
    private RefreshToken replacedByToken;

    protected RefreshToken() {
    }

    public RefreshToken(User user,
                        String tokenHash,
                        UUID familyId,
                        OffsetDateTime expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.expiresAt = expiresAt;
        this.createdAt = OffsetDateTime.now();
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public void revoke() {
        this.revokedAt = OffsetDateTime.now();
    }

    public void setReplacedByToken(RefreshToken token) {
        this.replacedByToken = token;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public RefreshToken getReplacedByToken() {
        return replacedByToken;
    }
}