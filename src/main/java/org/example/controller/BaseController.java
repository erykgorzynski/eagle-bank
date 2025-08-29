package org.example.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Base controller class providing common functionality for all controllers
 * Contains shared authentication and authorization utilities
 */
public abstract class BaseController {

    /**
     * Helper method to get current authenticated user ID from JWT token
     * @return the current authenticated user ID, or null if not authenticated
     */
    protected String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal();
        }
        return null;
    }
}
