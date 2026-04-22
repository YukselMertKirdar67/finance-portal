package com.financeportal.backend.User.DTO;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordLastChangedDTO {
    private LocalDateTime lastChanged;
}
