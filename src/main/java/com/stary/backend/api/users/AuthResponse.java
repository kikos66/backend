package com.stary.backend.api.users;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private Long expiresInMs;

    public AuthResponse(String accessToken, Long expiresInMs) {
        this.accessToken = accessToken;
        this.expiresInMs = expiresInMs;
    }
}
