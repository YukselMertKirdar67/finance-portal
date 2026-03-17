package com.financeportal.backend.User.Service;

import com.financeportal.backend.Portfolio.DTO.PortfolioDTO;
import com.financeportal.backend.User.DTO.AdminStatsDTO;
import com.financeportal.backend.User.DTO.*;

import java.util.List;

public interface AdminService {

    List<UserResponseDTO> getAllUsers();

    void disableUser(String userId);

    AdminStatsDTO getAdminStats();

    List<UserResponseDTO> searchUsers(String query);

    void enableUser(String userId);

    UserDetailDTO getUserDetail(String userId);

    AssignRoleResponseDTO assignRole(String userId, AssignRoleRequestDTO request);

    RemoveRoleResponseDTO removeRole(String userId, RemoveRoleRequestDTO request);

}
