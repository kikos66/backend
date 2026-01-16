package com.stary.backend.api.security;

import lombok.NonNull;
import com.stary.backend.api.users.TokenManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.userdetails.UserDetailsService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final TokenManager tokenManager;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(TokenManager tokenManager, UserDetailsService uds) {
        this.tokenManager = tokenManager;
        this.userDetailsService = uds;
    }



    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req, @NonNull HttpServletResponse res,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String header = req.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }

        String token = header.substring(7);

        try {
            if (tokenManager.validateToken(token)) {
                String email = tokenManager.getEmailFromToken(token);

                if(email != null) {
                    if (SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            }
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
        }
        chain.doFilter(req, res);
    }
}
