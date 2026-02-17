/*package com.financeportal.backend.Mapper;

import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    // Entity → UserResponseDTO
    public UserResponseDTO toUserResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isEnabled()
        );
    }

    // Entity + Roles → MeResponseDTO
    public MeResponseDTO toMeResponseDTO(User user, List<String> roles) {
        return new MeResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                roles
        );
    }
}*/
