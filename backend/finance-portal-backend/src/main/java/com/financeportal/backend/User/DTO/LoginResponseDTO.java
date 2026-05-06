package com.financeportal.backend.User.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {

    private boolean success;
    private String message;
    private String accessToken;
    private String refreshToken;
    private String username;
    private String email;
    private List<String> roles;
    private String keycloakId;
}
