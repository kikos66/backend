package com.stary.backend.configs;

import com.stary.backend.api.security.JwtAuthenticationFilter;
import com.stary.backend.api.service.MSUserDetailService;
import com.stary.backend.api.users.TokenManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    private final TokenManager tokenManager;
    private final MSUserDetailService userDetailService;

    public SecurityConfig(TokenManager tokenManager, MSUserDetailService userDetailService) {
        this.tokenManager = tokenManager;
        this.userDetailService = userDetailService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtaf = new JwtAuthenticationFilter(tokenManager, userDetailService);

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/logout",
                                "/api/auth/refresh"
                        ).permitAll()
                        // Publicly allow listing products and product details
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/products/**").permitAll()
                        // Allow public GET /api/users/{id}
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/users/*").permitAll()
                        // Users endpoints (PUT /api/users/edit, POST /api/users/delete) require auth
                        .requestMatchers("/api/users/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtaf, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("http://localhost:5173");  // React dev server
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
