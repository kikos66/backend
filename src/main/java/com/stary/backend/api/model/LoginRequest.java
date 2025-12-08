package com.stary.backend.api.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Setter
@Getter
public class LoginRequest {
    private String email;
    private String password;
}
