package com.financeportal.backend.User.Service;

import com.financeportal.backend.Portfolio.DTO.PortfolioDTO;
import com.financeportal.backend.User.DTO.AdminStatsDTO;
import com.financeportal.backend.User.DTO.UserResponseDTO;

import java.util.List;

public interface AdminService {

    List<UserResponseDTO> getAllUsers();

    void disableUser(String userId);

    AdminStatsDTO getAdminStats();

    List<UserResponseDTO> searchUsers(String query);

    void enableUser(String userId);

}
