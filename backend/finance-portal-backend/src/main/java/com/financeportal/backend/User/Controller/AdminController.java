package com.financeportal.backend.User.Controller;


import com.financeportal.backend.Portfolio.DTO.PortfolioDTO;
import com.financeportal.backend.User.DTO.AdminStatsDTO;
import com.financeportal.backend.User.Service.AdminService;
import com.financeportal.backend.User.DTO.UserResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // ===== HEALTH CHECK =====

    @GetMapping("/ping")
    public String ping() {
        return "Admin authenticated";
    }

    // ===== USER MANAGEMENT =====

    @GetMapping("/users")
    public List<UserResponseDTO> getAllUsers() {
        return adminService.getAllUsers();
    }

    @GetMapping("/users/search")
    public List<UserResponseDTO> searchUsers(@RequestParam String query) {
        return adminService.searchUsers(query);
    }

    @PutMapping("/users/{id}/disable")
    public void disableUser(@PathVariable String id) {
        adminService.disableUser(id);
    }

    @PutMapping("/users/{id}/enable")
    public void enableUser(@PathVariable String id) {
        adminService.enableUser(id);
    }

    // ===== DASHBOARD STATS =====

    @GetMapping("/stats")
    public AdminStatsDTO getAdminStats() {
        return adminService.getAdminStats();
    }

}
