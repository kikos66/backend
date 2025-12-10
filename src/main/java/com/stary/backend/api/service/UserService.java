package com.stary.backend.api.service;

import com.stary.backend.api.model.EditRequest;
import jakarta.transaction.Transactional;
import com.stary.backend.api.model.LoginRequest;
import com.stary.backend.api.model.RegisterRequest;
import com.stary.backend.api.users.repositories.RefreshTokenRepository;
import com.stary.backend.api.users.repositories.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.stary.backend.api.users.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenManager tokenManager;
    private static final String EMAIL_REGEX =
            "^[a-zA-Z0-9+&-]+(?:.[a-zA-Z0-9_+&-]+)*@(?:[a-zA-Z0-9-]+.)+[a-zA-Z]{2,7}$";
    private static final Pattern PATTERN = Pattern.compile(EMAIL_REGEX);

    public UserService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder, TokenManager tokenManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenManager = tokenManager;
    }

    @Transactional
    public User registerNewUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalStateException("Username is already taken.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email is already registered.");
        }
        if(!PATTERN.matcher(request.getEmail()).matches()) {
            throw new IllegalStateException("Email is invalid.");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        user.setPassword(encodedPassword);

        System.out.println(user);
        return userRepository.save(user);
    }

    public User authenticate(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalStateException("Invalid username/password"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalStateException("Invalid username/password");
        }
        return user;
    }

    public RefreshToken createRefreshToken(User user) {
        RefreshToken rt = new RefreshToken();
        //refreshTokenRepository.deleteByUser(user);
        rt.setUser(user);
        rt.setToken(UUID.randomUUID().toString());
        rt.setExpiryDate(Instant.now().plusMillis(tokenManager.getJwtRefreshExpirationMs()));
        return refreshTokenRepository.save(rt);
    }

    public boolean edit(Long id, EditRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));

        if (!user.getEmail().equals(req.getEmail())) {
            if (userRepository.findByEmail(req.getEmail()).isPresent()) {
                //throw new BadCredentialsException("Email is already in use.");
                return false;
            }
            user.setEmail(req.getEmail());
        } else {
            return false;
        }
        userRepository.save(user);
        return true;
    }

    @Transactional
    public boolean deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));

        if(user == null)
            return false;

        deleteRefreshTokensForUser(user);
        userRepository.delete(user);

        return true;
    }

    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token).orElse(null);
    }

    public void deleteRefreshToken(RefreshToken token) {
        refreshTokenRepository.delete(token);
    }

    public void deleteRefreshTokensForUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
