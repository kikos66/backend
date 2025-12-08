package com.stary.backend.api.users;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "password")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @NotBlank
    @Size(min = 3, max = 25)
    private String username;

    @Column(unique = true, nullable = false)
    @NotBlank
    @Email
    private String email;

    @Column(nullable = false)
    @NotBlank
    @Size(min = 8)
    private String password;
}
