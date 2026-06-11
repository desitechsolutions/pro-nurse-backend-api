package com.pronurse.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;
    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String mobile = null;
        String jwt = null;
        String role = null;

        try {
            // 1. Intercept Authorization Header and validate format
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwt = authHeader.substring(7);
                logger.debug("Processing secure filter layer for URI: {}", request.getRequestURI());

                if (jwtUtil.validateToken(jwt)) {
                    mobile = jwtUtil.extractMobile(jwt);
                    role = jwtUtil.extractRole(jwt);
                } else {
                    logger.debug("Stale or compromised JWT signature signature rejected for: {}", request.getRequestURI());
                }
            }

            // 2. Hydrate SecurityContext state-fully from claims if no context exists
            if (mobile != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Format Spring authority string (e.g., ROLE_PATIENT or ROLE_NURSE)
                String authorityField = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(authorityField));

                // Create standard authentication principal wrapper using JWT properties directly
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        mobile, // Principal identification anchor
                        null,   // Credentials (set to null since token verification is complete)
                        authorities
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                logger.debug("Successfully injected security context for identifier: {}", mobile);
            }

            // 3. Forward request along the servlet pipeline execution tree
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            logger.error("Security Authentication Filter failure for target URI {}: {}", request.getRequestURI(), e.getMessage());
            SecurityContextHolder.clearContext();

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\": false, \"message\": \"Authentication layer verification failure: " + e.getMessage() + "\"}");
        }
    }
}