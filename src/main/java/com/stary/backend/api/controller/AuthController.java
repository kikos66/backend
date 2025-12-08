package com.stary.backend.api.controller;

import jakarta.servlet.http.HttpSession;
import com.stary.backend.api.model.LoginRequest;
import com.stary.backend.api.model.RegisterRequest;
import com.stary.backend.api.users.TokenManager;
import com.stary.backend.api.users.User;
import com.stary.backend.api.service.UserService;
import org.aspectj.weaver.patterns.IToken;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.*;
import com.stary.backend.api.users.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final TokenManager tokenManager;

    public AuthController(UserService userService, TokenManager tokenManager) {
        this.userService = userService;
        this.tokenManager = tokenManager;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest) {
        try {
            User newUser = userService.registerNewUser(registerRequest);
            return new ResponseEntity<>("User registered successfully with ID: " + newUser.getId(), HttpStatus.CREATED);
        } catch (IllegalStateException e) {
            // Return a 409 Conflict if username/email is already taken
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        } catch (Exception e) {
            return new ResponseEntity<>("Registration failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletResponse res) {
        try {
            User user = userService.authenticate(loginRequest);

            String accessToken = tokenManager.generateAccessToken(user.getEmail());

            RefreshToken refreshToken = userService.createRefreshToken(user);

            Cookie cookie = new Cookie("refreshToken", refreshToken.getToken());
            cookie.setHttpOnly(true);
            cookie.setPath("/api/auth");
            cookie.setMaxAge((int) (tokenManager.getJwtRefreshExpirationMs() / 1000));
            cookie.setSecure(false);
            res.addCookie(cookie);

            AuthResponse authResp = new AuthResponse(accessToken, tokenManager.getJwtExpirationMs());

            return ResponseEntity.ok(authResp);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            return new ResponseEntity<>("Login failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return new ResponseEntity<>("No refresh token", HttpStatus.UNAUTHORIZED);
        }
        String rtToken = null;
        for (Cookie c : cookies) {
            if ("refreshToken".equals(c.getName())) {
                rtToken = c.getValue();
            }
        }
        if (rtToken == null) {
            return new ResponseEntity<>("No refresh token", HttpStatus.UNAUTHORIZED);
        }

        RefreshToken stored = userService.findByToken(rtToken);
        if (stored == null || stored.getExpiryDate().isBefore(java.time.Instant.now())) {
            return new ResponseEntity<>("Invalid refresh token", HttpStatus.UNAUTHORIZED);
        }

        String email = stored.getUser().getEmail();
        String newAccessToken = tokenManager.generateAccessToken(email);
        AuthResponse authResp = new AuthResponse(newAccessToken, tokenManager.getJwtExpirationMs());

        return ResponseEntity.ok(authResp);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse res) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("refreshToken".equals(c.getAttribute("email"))) {
                    String token = c.getValue();
                    RefreshToken rt = userService.findByToken(token);
                    if (rt != null) userService.deleteRefreshToken(rt);

                    Cookie cleared = new Cookie("refreshToken", null);
                    cleared.setPath("/api/auth");
                    cleared.setHttpOnly(true);
                    cleared.setMaxAge(0);
                    res.addCookie(cleared);
                }
            }
        }
        return ResponseEntity.ok("Logged out");
    }
}
