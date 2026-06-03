package com.financeportal.backend.User;

import com.financeportal.backend.User.DTO.MeResponseDTO;
import com.financeportal.backend.User.DTO.UserResponseDTO;
import com.financeportal.backend.User.Entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("UserMapper Unit Testleri")
class UserMapperTest {

    private UserMapper userMapper;
    private User testUser;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();

        testUser = User.builder()
                .keycloakId("test-keycloak-id")
                .username("testuser")
                .email("test@test.com")
                .enabled(true)
                .build();
        testUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("User → UserResponseDTO dönüşümü başarılı olmalı")
    void toUserResponseDTO_Success() {
        UserResponseDTO result = userMapper.toUserResponseDTO(testUser);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("test-keycloak-id");
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@test.com");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.isEmailVerified()).isFalse();
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("User + Roles → MeResponseDTO dönüşümü başarılı olmalı")
    void toMeResponseDTO_Success() {
        List<String> roles = List.of("USER", "ADMIN");

        MeResponseDTO result = userMapper.toMeResponseDTO(testUser, roles);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("test-keycloak-id");
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@test.com");
        assertThat(result.getRoles()).containsExactly("USER", "ADMIN");
    }

    @Test
    @DisplayName("Boş roller listesi ile MeResponseDTO dönüşümü başarılı olmalı")
    void toMeResponseDTO_EmptyRoles_Success() {
        MeResponseDTO result = userMapper.toMeResponseDTO(testUser, List.of());

        assertThat(result).isNotNull();
        assertThat(result.getRoles()).isEmpty();
    }

    @Test
    @DisplayName("USER rolü ile MeResponseDTO dönüşümü başarılı olmalı")
    void toMeResponseDTO_SingleRole_Success() {
        MeResponseDTO result = userMapper.toMeResponseDTO(testUser, List.of("USER"));

        assertThat(result).isNotNull();
        assertThat(result.getRoles()).hasSize(1);
        assertThat(result.getRoles()).contains("USER");
    }
}
