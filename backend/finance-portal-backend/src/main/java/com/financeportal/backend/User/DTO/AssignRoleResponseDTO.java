package com.financeportal.backend.User.DTO;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignRoleResponseDTO {

    private boolean success;
    private String message;
}
