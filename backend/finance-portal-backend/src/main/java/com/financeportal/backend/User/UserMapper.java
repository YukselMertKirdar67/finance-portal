package com.financeportal.backend.User;

import com.financeportal.backend.User.DTO.MeResponseDTO;
import com.financeportal.backend.User.DTO.UserResponseDTO;
import com.financeportal.backend.User.Entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

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
}
