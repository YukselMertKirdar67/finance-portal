package com.financeportal.backend.Util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("SecurityUtils Unit Testleri")
class SecurityUtilsTest {

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("JWT token varsa Keycloak ID başarıyla dönmeli")
    void getCurrentUserKeycloakId_WithJwt_ReturnsSubject() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("test-keycloak-id");

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(jwt, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        String result = SecurityUtils.getCurrentUserKeycloakId();

        assertThat(result).isEqualTo("test-keycloak-id");
    }

    @Test
    @DisplayName("Authentication yoksa exception fırlatılmalı")
    void getCurrentUserKeycloakId_NoAuth_ThrowsException() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(SecurityUtils::getCurrentUserKeycloakId)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No authenticated user found");
    }

    @Test
    @DisplayName("Principal JWT değilse exception fırlatılmalı")
    void getCurrentUserKeycloakId_NonJwtPrincipal_ThrowsException() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("string-principal", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(SecurityUtils::getCurrentUserKeycloakId)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid authentication principal");
    }

    @Test
    @DisplayName("ADMIN rolü varsa hasRole true dönmeli")
    void hasRole_WithAdminRole_ReturnsTrue() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        "user", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(SecurityUtils.hasRole("ADMIN")).isTrue();
    }

    @Test
    @DisplayName("ADMIN rolü yoksa hasRole false dönmeli")
    void hasRole_WithoutAdminRole_ReturnsFalse() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        "user", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(SecurityUtils.hasRole("ADMIN")).isFalse();
    }

    @Test
    @DisplayName("Authentication yoksa hasRole false dönmeli")
    void hasRole_NoAuth_ReturnsFalse() {
        SecurityContextHolder.clearContext();

        assertThat(SecurityUtils.hasRole("ADMIN")).isFalse();
    }

    @Test
    @DisplayName("ADMIN rolü varsa isAdmin true dönmeli")
    void isAdmin_WithAdminRole_ReturnsTrue() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        "user", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(SecurityUtils.isAdmin()).isTrue();
    }

    @Test
    @DisplayName("ADMIN rolü yoksa isAdmin false dönmeli")
    void isAdmin_WithoutAdminRole_ReturnsFalse() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        "user", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(SecurityUtils.isAdmin()).isFalse();
    }
}