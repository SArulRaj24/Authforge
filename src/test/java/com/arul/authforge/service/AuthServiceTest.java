
package com.arul.authforge.service;

import com.arul.authforge.dto.LoginRequest;
import com.arul.authforge.dto.LoginResponse;
import com.arul.authforge.entity.User;
import com.arul.authforge.repository.RoleRepository;
import com.arul.authforge.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthService authService;

    private LoginRequest loginRequest;
    private User mockUser;

    @BeforeEach
    void setUp() {

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("StrongPass@123");

        mockUser = mock(User.class);
    }

    // Test 1: Successful login
    @Test
    void login_shouldReturnTokens_whenCredentialsAreValid() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(mockUser));

        when(mockUser.isEnabled()).thenReturn(true);
        when(mockUser.getPasswordHash()).thenReturn("bcrypt-hash");

        when(passwordEncoder.matches(
                "StrongPass@123",
                "bcrypt-hash"
        )).thenReturn(true);

        when(jwtService.generateAccessToken(mockUser))
                .thenReturn("test-access-token");

        when(refreshTokenService.createRefreshToken(mockUser))
                .thenReturn("test-refresh-token");

        when(jwtService.getAccessTokenTtl())
                .thenReturn(900L);

        LoginResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("test-access-token", response.getAccessToken());
        assertEquals("test-refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(900L, response.getExpiresIn());

        verify(auditService).logEvent(
                nullSafeUserId(),
                eq("LOGIN_SUCCESS"),
                eq("User logged in using email and password")
        );
    }

    // Test 2: Invalid password
    @Test
    void login_shouldThrowException_whenPasswordIsIncorrect() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(mockUser));

        when(mockUser.isEnabled()).thenReturn(true);
        when(mockUser.getPasswordHash()).thenReturn("bcrypt-hash");

        when(passwordEncoder.matches(
                "StrongPass@123",
                "bcrypt-hash"
        )).thenReturn(false);

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        verify(auditService).logEvent(
                nullSafeUserId(),
                eq("LOGIN_FAILURE"),
                eq("Login failed: invalid password")
        );

        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    // Test 3: Account does not exist
    @Test
    void login_shouldThrowException_whenUserDoesNotExist() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        verify(auditService).logEvent(
                isNull(),
                eq("LOGIN_FAILURE"),
                eq("Login failed: account not found")
        );

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateAccessToken(any());
    }

    // Helper for the mocked user's ID
    private Long nullSafeUserId() {
        return mockUser.getId();
    }
}
