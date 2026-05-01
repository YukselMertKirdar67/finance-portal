package com.financeportal.backend.User.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDTO {

    private String id;  // Keycloak UUID
    private String username;
    private String email;
    private Boolean emailVerified;
    private List<String> roles;
    private LocalDateTime createdAt;
    private String theme;      // "light", "dark", "system"

    private boolean notifyTransaction;
    private boolean notifyPortfolioChange;
    private boolean notifyPriceAlert;
    private boolean notifyNews;
}
