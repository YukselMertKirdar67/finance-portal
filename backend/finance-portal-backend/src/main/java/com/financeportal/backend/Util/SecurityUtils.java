package com.financeportal.backend.Util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    /**
     * Get current authenticated user's Keycloak ID (sub claim from JWT)
     * @return Keycloak user ID
     */
    public static String getCurrentUserKeycloakId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject(); // Keycloak user ID (sub claim)
        }

        throw new RuntimeException("Invalid authentication principal");
    }

    /**
     * Get current authenticated user's username
     * @return Username
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("preferred_username");
        }

        throw new RuntimeException("Invalid authentication principal");
    }

    /**
     * Get current authenticated user's email
     * @return Email
     */
    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("email");
        }

        throw new RuntimeException("Invalid authentication principal");
    }

    /**
     * Get full JWT token
     * @return Jwt object
     */
    public static Jwt getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }

        throw new RuntimeException("Invalid authentication principal");
    }

    /**
     * Check if current user has a specific role
     * @param role Role name (without ROLE_ prefix)
     * @return true if user has role
     */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + role));
    }

    /**
     * Check if current user is admin
     * @return true if admin
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }
}