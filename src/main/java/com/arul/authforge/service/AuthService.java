
package com.arul.authforge.service;

import com.arul.authforge.dto.LoginRequest;
import com.arul.authforge.dto.LoginResponse;
import com.arul.authforge.dto.RegisterRequest;
import com.arul.authforge.dto.RegisterResponse;
import com.arul.authforge.entity.Role;
import com.arul.authforge.entity.User;
import com.arul.authforge.repository.RoleRepository;
import com.arul.authforge.repository.UserRepository;
import com.arul.authforge.dto.RefreshRequest;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // Constructor injection
    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    // User registration
    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Check whether the email already exists
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists"
            );
        }

        // Hash the password before storing it
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
                .orElseThrow(() -> new BadCredentialsException(
                        "Invalid email or password"
                ));

        // Check account status
        if (!user.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }

        // Verify password against BCrypt hash
        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash())) {

            throw new BadCredentialsException(
                    "Invalid email or password"
            );
        }

        // Generate JWT access token
        String accessToken =
                jwtService.generateAccessToken(user);

        // Generate and persist refresh token hash
        String refreshToken =
                refreshTokenService.createRefreshToken(user);

        // Return both tokens
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

        // Validate the old refresh token and rotate it
        RefreshTokenService.RotationResult result =
                refreshTokenService.rotateRefreshToken(
                        request.getRefreshToken()
                );

        User user = result.user();

        // Generate a fresh JWT access token
        String accessToken =
                jwtService.generateAccessToken(user);

        // Return the new access token and replacement refresh token
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

        refreshTokenService.revokeRefreshToken(
                request.getRefreshToken()
        );
    }

    @Transactional
    public LoginResponse loginWithGoogle(
            String googleSubject,
            String email,
            String fullName,
            boolean emailVerified) {

        if (!emailVerified) {
            throw new IllegalArgumentException(
                    "Google email must be verified");
        }

        if (googleSubject == null || googleSubject.isBlank()
                || email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Required Google account information is missing");
        }

        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        User user = userRepository
                .findByGoogleSubject(googleSubject)
                .orElse(null);

        if (user == null) {

            if (userRepository.existsByEmail(normalizedEmail)) {
                throw new IllegalArgumentException(
                        "An account with this email already exists. "
                                + "Sign in using the existing login method first.");
            }

            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new IllegalStateException(
                            "Default ROLE_USER was not found"));

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

        if (!user.isEnabled()) {
            throw new IllegalStateException("Account is disabled");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken =
                refreshTokenService.createRefreshToken(user);

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenTtl()
        );
    }

}
