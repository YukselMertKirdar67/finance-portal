package com.financeportal.backend.User.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemoveRoleRequestDTO {

    @NotBlank(message = "Role name is required")
    private String roleName;
}
