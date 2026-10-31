package com.arul.authforge.service;

import com.arul.authforge.entity.RefreshToken;
import com.arul.authforge.entity.User;
import com.arul.authforge.repository.RefreshTokenRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenTtlDays;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${auth.jwt.refresh-token-ttl-days}")
            long refreshTokenTtlDays) {

        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    @Transactional
    public String createRefreshToken(User user) {

        // 1. Generate 32 random bytes
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);

        // 2. Convert bytes into a URL-safe token
        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        // 3. Generate SHA-256 hash
        String tokenHash = hashToken(rawToken);

        // 4. Create a new token family
        UUID familyId = UUID.randomUUID();

        // 5. Calculate expiration
        OffsetDateTime expiresAt =
                OffsetDateTime.now().plusDays(refreshTokenTtlDays);

        // 6. Create refresh-token entity
        RefreshToken refreshToken = new RefreshToken(
                user,
                tokenHash,
                familyId,
                expiresAt
        );

        // 7. Store token hash in PostgreSQL
        refreshTokenRepository.save(refreshToken);

        // 8. Return original token to the client
        return rawToken;
    }

    public String hashToken(String rawToken) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(
                    rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    exception
            );
        }
    }
}