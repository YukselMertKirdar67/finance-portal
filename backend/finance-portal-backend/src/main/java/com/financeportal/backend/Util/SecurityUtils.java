package com.financeportal.backend.Util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    /**
     * Mevcut kimliği doğrulanmış kullanıcının Keycloak ID'sini döner (JWT'deki sub claim)
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
     * Mevcut kullanıcının belirtilen role sahip olup olmadığını kontrol eder
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
     * Mevcut kullanıcının admin olup olmadığını kontrol eder
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }
}