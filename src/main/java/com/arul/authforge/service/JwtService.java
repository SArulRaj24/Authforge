package com.arul.authforge.service;

import com.arul.authforge.entity.User;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final long accessTokenTtl;

    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${auth.jwt.access-token-ttl-seconds}")
            long accessTokenTtl) {

        this.jwtEncoder = jwtEncoder;
        this.accessTokenTtl = accessTokenTtl;
    }

    public String generateAccessToken(User user) {

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(accessTokenTtl);

        String roles = user.getRoles()
                .stream()
                .map(role -> role.getName())
                .collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("authforge")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("roles", roles)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

    public long getAccessTokenTtl() {
        return accessTokenTtl;
    }
}