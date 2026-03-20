package com.financeportal.backend.User.DTO;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreAuthResponseDTO {
    private boolean success;
    private String message;
    private boolean requiresOTP;        // OTP gerekli mi?
    private String keycloakAuthUrl;     // Keycloak OTP URL (popup için)
}
