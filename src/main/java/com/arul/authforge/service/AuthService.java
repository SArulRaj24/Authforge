
package com.arul.authforge.service;

import com.arul.authforge.dto.LoginRequest;
import com.arul.authforge.dto.LoginResponse;
import com.arul.authforge.dto.RegisterRequest;
import com.arul.authforge.dto.RegisterResponse;
import com.arul.authforge.dto.RefreshRequest;

import com.arul.authforge.entity.Role;
import com.arul.authforge.entity.User;

import com.arul.authforge.repository.RoleRepository;
import com.arul.authforge.repository.UserRepository;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;

    // Constructor injection
    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            AuditService auditService) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.auditService = auditService;
    }

    // Register an audit event after the current transaction commits
    private void auditAfterCommit(
            Long userId,
            String eventType,
            String details) {

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        auditService.logEvent(
                                userId,
                                eventType,
                                details
                        );
                    }
                }
        );
    }

    // User registration
    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Check whether email already exists
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists"
            );
        }

        // Hash password using BCrypt
        String hashedPassword =
                passwordEncoder.encode(request.getPassword());

        // Create user
        User user = new User(
                email,
                hashedPassword,
                request.getFullName().trim()
        );

        // Assign default ROLE_USER
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException(
                        "Default ROLE_USER was not found in the database"
                ));

        user.addRole(userRole);

        // Save user
        User savedUser = userRepository.save(user);

        // Record registration after the user transaction commits
        auditAfterCommit(
                savedUser.getId(),
                "USER_REGISTERED",
                "User registered using email and password"
        );

        // Extract assigned role names
        Set<String> roleNames = savedUser.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                roleNames
        );
    }

    // User login
    @Transactional
    public LoginResponse login(LoginRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElse(null);

        // Account not found
        if (user == null) {
            auditService.logEvent(
                    null,
                    "LOGIN_FAILURE",
                    "Login failed: account not found"
            );

            throw new BadCredentialsException(
                    "Invalid email or password"
            );
        }

        // Check account status
        if (!user.isEnabled()) {
            auditService.logEvent(
                    user.getId(),
                    "LOGIN_FAILURE",
                    "Login failed: account disabled"
            );

            throw new DisabledException("Account is disabled");
        }

        // Verify password
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash())) {

            auditService.logEvent(
                    user.getId(),
                    "LOGIN_FAILURE",
                    "Login failed: invalid password"
            );

            throw new BadCredentialsException(
                    "Invalid email or password"
            );
        }

        // Generate JWT access token
        String accessToken =
                jwtService.generateAccessToken(user);

        // Generate and persist refresh token
        String refreshToken =
                refreshTokenService.createRefreshToken(user);

        // Record successful login
        auditService.logEvent(
                user.getId(),
                "LOGIN_SUCCESS",
                "User logged in using email and password"
        );

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenTtl()
        );
    }

    // Refresh access token and rotate refresh token
    @Transactional(noRollbackFor = RefreshTokenReuseException.class)
    public LoginResponse refresh(RefreshRequest request) {

        // Validate and rotate the refresh token
        RefreshTokenService.RotationResult result =
                refreshTokenService.rotateRefreshToken(
                        request.getRefreshToken()
                );

        User user = result.user();

        // Generate a new access token
        String accessToken =
                jwtService.generateAccessToken(user);

        // Record successful token rotation
        auditService.logEvent(
                user.getId(),
                "TOKEN_REFRESHED",
                "Refresh token successfully rotated"
        );

        // Return new access and refresh tokens
        return new LoginResponse(
                accessToken,
                result.rawToken(),
                "Bearer",
                jwtService.getAccessTokenTtl()
        );
    }

    // User logout
    @Transactional
    public void logout(RefreshRequest request) {

        // Revoke the supplied refresh token
        refreshTokenService.revokeRefreshToken(
                request.getRefreshToken()
        );

        // Record logout request
        auditService.logEvent(
                null,
                "LOGOUT",
                "Logout request processed"
        );
    }

    // Google OAuth login
    @Transactional
    public LoginResponse loginWithGoogle(
            String googleSubject,
            String email,
            String fullName,
            boolean emailVerified) {

        // Verify Google email
        if (!emailVerified) {
            throw new IllegalArgumentException(
                    "Google email must be verified"
            );
        }

        // Validate required Google account information
        if (googleSubject == null || googleSubject.isBlank()
                || email == null || email.isBlank()) {

            throw new IllegalArgumentException(
                    "Required Google account information is missing"
            );
        }

        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        // Find existing user using Google's unique subject
        User user = userRepository
                .findByGoogleSubject(googleSubject)
                .orElse(null);

        // Create a new user if this Google account is not registered
        if (user == null) {

            // Prevent automatic linking with an existing local account
            if (userRepository.existsByEmail(normalizedEmail)) {
                throw new IllegalArgumentException(
                        "An account with this email already exists. "
                                + "Sign in using the existing login method first."
                );
            }

            // Fetch default role
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new IllegalStateException(
                            "Default ROLE_USER was not found"
                    ));

            // Create Google user
            user = new User(
                    normalizedEmail,
                    null,
                    fullName == null || fullName.isBlank()
                            ? "Google User"
                            : fullName.trim()
            );

            user.setProvider("GOOGLE");
            user.setGoogleSubject(googleSubject);
            user.addRole(userRole);

            user = userRepository.save(user);
        }

        // Check account status
        if (!user.isEnabled()) {
            throw new IllegalStateException("Account is disabled");
        }

        // Generate tokens
        String accessToken =
                jwtService.generateAccessToken(user);

        String refreshToken =
                refreshTokenService.createRefreshToken(user);

        // Record Google login after transaction commits
        auditAfterCommit(
                user.getId(),
                "GOOGLE_LOGIN_SUCCESS",
                "User authenticated using Google OpenID Connect"
        );

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenTtl()
        );
    }
}
