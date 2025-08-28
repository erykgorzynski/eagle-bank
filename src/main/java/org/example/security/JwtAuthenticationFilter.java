package org.example.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT Authentication Filter that processes JWT tokens from requests
 * Extends OncePerRequestFilter to ensure it's executed once per request
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Skip JWT processing for authentication endpoints
        if (isAuthEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userId;

        // Check if Authorization header exists and starts with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract JWT token from Authorization header
        jwt = authHeader.substring(7);

        try {
            // Extract user ID from JWT token
            userId = jwtService.extractUserId(jwt);

            // If user ID exists and no authentication is set in SecurityContext
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Validate the token
                if (jwtService.isTokenValid(jwt, userId)) {
                    // Create authentication token
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            new ArrayList<>() // No authorities for now, can be extended later
                    );

                    // Set authentication details
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Set authentication in SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("Successfully authenticated user: {}", userId);
                } else {
                    log.warn("Invalid JWT token for user: {}", userId);
                }
            }
        } catch (Exception e) {
            log.error("Error processing JWT token: {}", e.getMessage());
            // Clear security context on error
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Check if the request is for an authentication endpoint that doesn't require JWT
     *
     * @param request HTTP request
     * @return true if it's an auth endpoint, false otherwise
     */
    private boolean isAuthEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Allow POST requests to user creation and login endpoints
        return ("POST".equals(method) && "/v1/users".equals(path)) ||
               ("POST".equals(method) && "/v1/auth/login".equals(path)) ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/api-docs") ||
               path.equals("/swagger-ui.html");
    }
}
