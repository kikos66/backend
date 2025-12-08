package com.stary.backend.api.users;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique=true)
    private String token;

    private Instant expiryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;
}
