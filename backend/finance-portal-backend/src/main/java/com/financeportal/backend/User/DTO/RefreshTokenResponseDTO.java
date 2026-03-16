package com.financeportal.backend.User.DTO;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenResponseDTO {

    private boolean success;
    private String message;
    private String accessToken;
    private String refreshToken;
}