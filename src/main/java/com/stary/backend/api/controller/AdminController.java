package com.stary.backend.api.controller;

import com.stary.backend.api.users.Role;
import com.stary.backend.api.users.User;
import com.stary.backend.api.users.repositories.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/{id}/role")
    public ResponseEntity<?> setRole(@PathVariable Long id, @RequestParam Role role) {
        Optional<User> u = userRepository.findById(id);
        if (u.isEmpty()) return ResponseEntity.notFound().build();
        User user = u.get();
        user.setRole(role);
        userRepository.save(user);
        return ResponseEntity.ok(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }
}
