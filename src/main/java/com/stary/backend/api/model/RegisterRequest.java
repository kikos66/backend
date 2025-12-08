package com.stary.backend.api.model;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Setter
@Getter
public class RegisterRequest {
    private String username;
    private String email;
    private String password;
}