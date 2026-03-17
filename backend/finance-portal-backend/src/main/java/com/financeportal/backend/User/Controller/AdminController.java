package com.financeportal.backend.User.Controller;


import com.financeportal.backend.Portfolio.DTO.PortfolioDTO;
import com.financeportal.backend.User.DTO.*;
import com.financeportal.backend.User.Service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/users/{id}")
    public ResponseEntity<UserDetailDTO> getUserDetail(@PathVariable String id) {
        UserDetailDTO userDetail = adminService.getUserDetail(id);
        return ResponseEntity.ok(userDetail);
    }

    @PostMapping("/users/{id}/assign-role")
    public ResponseEntity<AssignRoleResponseDTO> assignRole(
            @PathVariable String id,
            @Valid @RequestBody AssignRoleRequestDTO request) {

        AssignRoleResponseDTO response = adminService.assignRole(id, request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/users/{id}/remove-role")
    public ResponseEntity<RemoveRoleResponseDTO> removeRole(
            @PathVariable String id,
            @Valid @RequestBody RemoveRoleRequestDTO request) {

        RemoveRoleResponseDTO response = adminService.removeRole(id, request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // ===== DASHBOARD STATS =====

    @GetMapping("/stats")
    public AdminStatsDTO getAdminStats() {
        return adminService.getAdminStats();
    }

}
