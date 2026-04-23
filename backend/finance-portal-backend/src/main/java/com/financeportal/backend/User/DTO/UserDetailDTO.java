package com.financeportal.backend.User.DTO;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetailDTO {

    private String id;
    private String username;
    private String email;
    private boolean enabled;
    private boolean emailVerified;
    private LocalDateTime createdAt;
    private List<String> roles;

    // Portfolio & Transaction stats
    private int portfolioCount;
    private int transactionCount;
    private int watchlistCount;
}
