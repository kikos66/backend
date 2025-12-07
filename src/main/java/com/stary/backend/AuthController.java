package com.stary.backend;

import com.stary.backend.RegisterRequest;
import com.stary.backend.User;
import com.stary.backend.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest) {
        try {
            User newUser = userService.registerNewUser(registerRequest);
            // Return a success status and the created user details (excluding password)
            return new ResponseEntity<>("User registered successfully with ID: " + newUser.getId(), HttpStatus.CREATED);
        } catch (IllegalStateException e) {
            // Return a 409 Conflict if username/email is already taken
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        } catch (Exception e) {
            // Generic error handling
            return new ResponseEntity<>("Registration failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
