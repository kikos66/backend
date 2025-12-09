package com.stary.backend.api.controller;

import com.stary.backend.api.model.EditRequest;
import com.stary.backend.api.service.UserService;
import com.stary.backend.api.users.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import com.stary.backend.api.users.repositories.UserRepository;
import com.stary.backend.api.users.TokenManager;
import com.stary.backend.api.users.RefreshToken;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final TokenManager tokenManager;

    public UserController(UserRepository userRepository, UserService userService, TokenManager tokenManager) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.tokenManager = tokenManager;
    }

    @GetMapping("/me")
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName(); // The 'name' is the email from UserDetails
        System.out.println(email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found in database."));
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        Optional<User> user = userRepository.findById(id);

        return user.orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + id));
    }

    @PutMapping("/edit")
    public ResponseEntity<?> editUserData(@RequestBody EditRequest req, HttpServletResponse res) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String oldEmail = authentication.getName();

        User user = userRepository.findByEmail(oldEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));

        Long userId = user.getId();

        userService.edit(userId, req);

        User updatedUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Updated user not found."));

        String newEmail = updatedUser.getEmail();
        String newAccessToken = tokenManager.generateAccessToken(newEmail);

        RefreshToken newRefreshToken = userService.createRefreshToken(updatedUser);

        Cookie cookie = new Cookie("refreshToken", newRefreshToken.getToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge((int) (tokenManager.getJwtRefreshExpirationMs() / 1000));
        cookie.setSecure(false);
        res.addCookie(cookie);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Successfully edited and session refreshed.");
        response.put("accessToken", newAccessToken);
        response.put("user", updatedUser);

        return ResponseEntity.ok(response);
    }

}