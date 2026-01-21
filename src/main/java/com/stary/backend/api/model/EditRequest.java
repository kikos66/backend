package com.stary.backend.api.model;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Setter
@Getter
public class EditRequest {
    private String email;
    private String username;
    private String password;
}
