package com.stary.backend.api.controller;

import com.stary.backend.api.model.EditRequest;
import com.stary.backend.api.service.UserService;
import com.stary.backend.api.users.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import com.stary.backend.api.users.repositories.UserRepository;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
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

    @GetMapping("/user/edit")
    public void editUserData(@RequestBody EditRequest req) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        userService.edit(authentication.getName(), req);
    }

}