package com.stary.backend;

import lombok.Data;

// This DTO mirrors the fields needed from the frontend register form.
@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String password;
}