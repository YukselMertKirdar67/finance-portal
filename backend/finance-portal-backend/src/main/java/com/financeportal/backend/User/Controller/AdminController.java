package com.financeportal.backend.User.Controller;

import com.financeportal.backend.User.DTO.*;
import com.financeportal.backend.User.Service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Log4j2
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    /**
     * Admin servisinin çalışıp çalışmadığını kontrol eder.
     */
    @GetMapping("/ping")
    public String ping() {
        log.info("Admin ping request received");
        return "Admin authenticated";
    }

    /**
     * Sistemdeki tüm kullanıcıları Keycloak'tan getirir.
     */
    @GetMapping("/users")
    public List<UserResponseDTO> getAllUsers() {
        log.info("Admin: Fetching all users");
        return adminService.getAllUsers();
    }

    /**
     * Verilen sorguya göre Keycloak'ta kullanıcı arar.
     */
    @GetMapping("/users/search")
    public List<UserResponseDTO> searchUsers(@RequestParam String query) {
        log.info("Admin: Searching users with query: {}", query);
        return adminService.searchUsers(query);
    }

    /**
     * Belirtilen kullanıcıyı Keycloak'ta devre dışı bırakır.
     */
    @PutMapping("/users/{id}/disable")
    public void disableUser(@PathVariable String id) {
        log.info("Admin: Disabling user: {}", id);
        adminService.disableUser(id);
        log.info("✅ Admin: User disabled: {}", id);
    }

    /**
     * Belirtilen kullanıcıyı Keycloak'ta aktif eder.
     */
    @PutMapping("/users/{id}/enable")
    public void enableUser(@PathVariable String id) {
        log.info("Admin: Enabling user: {}", id);
        adminService.enableUser(id);
        log.info("✅ Admin: User enabled: {}", id);
    }

    /**
     * Belirtilen kullanıcının detay bilgilerini getirir.
     * Keycloak profil bilgileri ve DB istatistiklerini içerir.
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<UserDetailDTO> getUserDetail(@PathVariable String id) {
        log.info("Admin: Fetching user detail for: {}", id);
        UserDetailDTO userDetail = adminService.getUserDetail(id);
        return ResponseEntity.ok(userDetail);
    }

    /**
     * Belirtilen kullanıcıya Keycloak'ta rol atar.
     */
    @PostMapping("/users/{id}/assign-role")
    public ResponseEntity<AssignRoleResponseDTO> assignRole(
            @PathVariable String id,
            @Valid @RequestBody AssignRoleRequestDTO request) {

        log.info("Admin: Assigning role {} to user: {}", request.getRoleName(), id);

        AssignRoleResponseDTO response = adminService.assignRole(id, request);

        if (response.isSuccess()) {
            log.info("✅ Admin: Role {} assigned to user: {}", request.getRoleName(), id);
            return ResponseEntity.ok(response);
        } else {
            log.warn("❌ Admin: Failed to assign role {} to user: {}", request.getRoleName(), id);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Belirtilen kullanıcıdan Keycloak'ta rolü kaldırır.
     */
    @PostMapping("/users/{id}/remove-role")
    public ResponseEntity<RemoveRoleResponseDTO> removeRole(
            @PathVariable String id,
            @Valid @RequestBody RemoveRoleRequestDTO request) {

        log.info("Admin: Removing role {} from user: {}", request.getRoleName(), id);

        RemoveRoleResponseDTO response = adminService.removeRole(id, request);

        if (response.isSuccess()) {
            log.info("✅ Admin: Role {} removed from user: {}", request.getRoleName(), id);
            return ResponseEntity.ok(response);
        } else {
            log.warn("❌ Admin: Failed to remove role {} from user: {}", request.getRoleName(), id);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Admin dashboard için genel istatistikleri getirir.
     * Kullanıcı, portföy, işlem ve watchlist sayılarını içerir.
     */
    @GetMapping("/stats")
    public AdminStatsDTO getAdminStats() {
        log.info("Admin: Fetching dashboard statistics");
        return adminService.getAdminStats();
    }
}