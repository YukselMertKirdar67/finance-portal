/*package com.financeportal.backend.Controller;

import com.financeportal.backend.DTO.UserResponseDTO;
import com.financeportal.backend.Entity.User;
import com.financeportal.backend.Repository.UserRepository;
import com.financeportal.backend.Service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/ping")
    public String ping() {
        return "Admin authenticated";
    }

    @GetMapping("/users")
    public List<UserResponseDTO> getAllUsers() {
        return adminService.getAllUsers();
    }

    @PutMapping("/users/{id}/disable")
    public void disableUser(@PathVariable Long id) {
        adminService.disableUser(id);
    }
}*/
