package com.arul.authforge.service;

import com.arul.authforge.dto.RegisterRequest;
import com.arul.authforge.dto.RegisterResponse;
import com.arul.authforge.entity.Role;
import com.arul.authforge.entity.User;
import com.arul.authforge.repository.RoleRepository;
import com.arul.authforge.repository.UserRepository;

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

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        // 1. Normalize the email address
        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        // 2. Check whether the email already exists
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists"
            );
        }

        // 3. Hash the password
        String hashedPassword =
                passwordEncoder.encode(request.getPassword());

        // 4. Create the user
        User user = new User(
                email,
                hashedPassword,
                request.getFullName().trim()
        );

        // 5. Find and assign the default role
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException(
                        "Default ROLE_USER was not found in the database"
                ));

        user.addRole(userRole);

        // 6. Save the user
        User savedUser = userRepository.save(user);

        // 7. Convert entity into a safe response DTO
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
}