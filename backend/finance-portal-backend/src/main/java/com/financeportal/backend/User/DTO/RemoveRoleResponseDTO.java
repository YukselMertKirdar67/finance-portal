package com.financeportal.backend.User.DTO;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemoveRoleResponseDTO {

    private boolean success;
    private String message;
}