package com.financeportal.backend.User.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeResponseDTO {

    private Long id;
    private String username;
    private String email;
    private List<String> roles;
}
