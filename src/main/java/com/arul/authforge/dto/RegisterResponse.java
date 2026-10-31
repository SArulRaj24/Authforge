package com.arul.authforge.dto;

import java.util.Set;

public class RegisterResponse {

    private Long id;
    private String fullName;
    private String email;
    private Set<String> roles;

    public RegisterResponse(Long id, String fullName,
                            String email, Set<String> roles) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.roles = roles;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public Set<String> getRoles() {
        return roles;
    }
}