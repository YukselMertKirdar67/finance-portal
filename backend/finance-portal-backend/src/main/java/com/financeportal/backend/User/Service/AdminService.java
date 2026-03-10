package com.financeportal.backend.User.Service;

import com.financeportal.backend.User.DTO.UserResponseDTO;

import java.util.List;

public interface AdminService {

    List<UserResponseDTO> getAllUsers();

    void disableUser(Long userId);
}
