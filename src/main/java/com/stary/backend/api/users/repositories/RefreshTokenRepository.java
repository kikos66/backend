package com.stary.backend.api.users.repositories;

import com.stary.backend.api.users.RefreshToken;
import com.stary.backend.api.users.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUser(User user);
}
