package com.arul.authforge.repository;

import com.arul.authforge.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    // Find a role using its name
    Optional<Role> findByName(String name);
}