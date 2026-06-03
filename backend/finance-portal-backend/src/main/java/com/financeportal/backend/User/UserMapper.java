package com.financeportal.backend.User;

import com.financeportal.backend.User.DTO.MeResponseDTO;
import com.financeportal.backend.User.DTO.UserResponseDTO;
import com.financeportal.backend.User.Entity.User;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    @Mapping(source = "keycloakId", target = "id")
    @Mapping(target = "emailVerified", constant = "false")
    UserResponseDTO toUserResponseDTO(User user);

    default MeResponseDTO toMeResponseDTO(User user, List<String> roles) {
        return new MeResponseDTO(
                user.getKeycloakId(),
                user.getUsername(),
                user.getEmail(),
                roles
        );
    }
}