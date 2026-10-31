
package com.arul.authforge.service;

import com.arul.authforge.entity.RefreshToken;
import com.arul.authforge.entity.User;
import com.arul.authforge.repository.RefreshTokenRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
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

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenTtlDays;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${auth.jwt.refresh-token-ttl-days}")
            long refreshTokenTtlDays) {

        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    // Create the first refresh token during login
    @Transactional
    public String createRefreshToken(User user) {

        UUID familyId = UUID.randomUUID();

        return createRefreshToken(user, familyId);
    }

    // Create a token in an existing family during rotation
    private String createRefreshToken(User user, UUID familyId) {

        String rawToken = generateRawToken();

        String tokenHash = hashToken(rawToken);

        OffsetDateTime expiresAt =
                OffsetDateTime.now().plusDays(refreshTokenTtlDays);

        RefreshToken refreshToken = new RefreshToken(
                user,
                tokenHash,
                familyId,
                expiresAt
        );

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    // Validate and rotate an existing refresh token
    @Transactional(noRollbackFor = RefreshTokenReuseException.class)
    public RotationResult rotateRefreshToken(String rawToken) {

        String tokenHash = hashToken(rawToken);

        RefreshToken oldToken = refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new BadCredentialsException(
                        "Invalid refresh token"
                ));

        // Reuse detection
        if (oldToken.isRevoked()) {

            revokeTokenFamily(oldToken.getFamilyId());

            throw new RefreshTokenReuseException();
        }

        // Expiry validation
        if (oldToken.getExpiresAt().isBefore(OffsetDateTime.now())) {

            throw new BadCredentialsException(
                    "Refresh token has expired"
            );
        }

        User user = oldToken.getUser();

        // Preserve the original family ID
        UUID familyId = oldToken.getFamilyId();

        // Create the replacement token
        String newRawToken = createRefreshToken(user, familyId);

        // Find the newly created entity
        RefreshToken newToken = refreshTokenRepository
                .findByTokenHash(hashToken(newRawToken))
                .orElseThrow(() -> new IllegalStateException(
                        "New refresh token could not be saved"
                ));

        // Revoke the old token and link its replacement
        oldToken.revoke();
        oldToken.setReplacedByToken(newToken);

        refreshTokenRepository.save(oldToken);

        return new RotationResult(user, newRawToken);
    }

    // Revoke every token belonging to the same family
    private void revokeTokenFamily(UUID familyId) {

        OffsetDateTime now = OffsetDateTime.now();

        for (RefreshToken token :
                refreshTokenRepository.findAllByFamilyId(familyId)) {

            if (!token.isRevoked()
                    && token.getExpiresAt().isAfter(now)) {

                token.revoke();
                refreshTokenRepository.save(token);
            }
        }
    }

    // Generate a cryptographically random opaque token
    private String generateRawToken() {

        byte[] randomBytes = new byte[32];

        SECURE_RANDOM.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    // Store only the SHA-256 hash in the database
    public String hashToken(String rawToken) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(
                    rawToken.getBytes(
                            java.nio.charset.StandardCharsets.UTF_8
                    )
            );

            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException exception) {

            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    exception
            );
        }
    }

    // Result returned to AuthService
    public record RotationResult(
            User user,
            String rawToken
    ) {}


    // Revoke a refresh token during logout
    @Transactional
    public void revokeRefreshToken(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        String tokenHash = hashToken(rawToken);

        refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .ifPresent(token -> {
                    if (!token.isRevoked()) {
                        token.revoke();
                        refreshTokenRepository.save(token);
                    }
                });
    }

}
