package com.financeportal.backend.User.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationResponseDTO {

    private boolean success;
    private String message;
    private boolean emailVerified;
}
