package com.financeportal.backend.User;

import com.financeportal.backend.User.DTO.MeResponseDTO;
import com.financeportal.backend.User.DTO.UserResponseDTO;
import com.financeportal.backend.User.Entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserMapper {

    // Entity → UserResponseDTO (Keycloak ID kullan)
    public UserResponseDTO toUserResponseDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getKeycloakId())
                .username(user.getUsername())
                .email(user.getEmail())
                .enabled(user.isEnabled())
                .emailVerified(false)
                .createdAt(user.getCreatedAt())
                .build();
    }

    // Entity + Roles → MeResponseDTO
    public MeResponseDTO toMeResponseDTO(User user, List<String> roles) {
        return new MeResponseDTO(
                user.getKeycloakId(),
                user.getUsername(),
                user.getEmail(),
                roles
        );
    }
}
