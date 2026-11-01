
package com.arul.authforge.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @GetMapping("/dashboard")
    public Map<String, String> getAdminDashboard() {

        return Map.of(
                "message", "Welcome to the AuthForge Admin Dashboard",
                "access", "ADMIN"
        );
    }
}
