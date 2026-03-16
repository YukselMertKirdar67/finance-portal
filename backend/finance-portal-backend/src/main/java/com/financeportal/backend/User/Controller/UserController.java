package com.financeportal.backend.User.Controller;

import com.financeportal.backend.User.DTO.ChangePasswordRequestDTO;
import com.financeportal.backend.User.DTO.ChangePasswordResponseDTO;
import com.financeportal.backend.User.DTO.MeResponseDTO;
import com.financeportal.backend.User.DTO.UserProfileDTO;
import com.financeportal.backend.User.Entity.User;
import com.financeportal.backend.User.Service.AuthService;
import com.financeportal.backend.User.UserMapper;
import com.financeportal.backend.User.Service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final AuthService authService;

    /**
     * Get current user profile
     */
    @GetMapping
    public MeResponseDTO me(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.getOrCreateUser(jwt);
        List<String> roles = extractRoles(jwt);
        return userMapper.toMeResponseDTO(user, roles);
    }

    /**
     * Get current user profile (detailed)
     */
    @GetMapping("/profile")
    public UserProfileDTO getCurrentUserProfile(@AuthenticationPrincipal Jwt jwt) {
        log.info("Fetching current user profile");

        String keycloakId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");
        Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");

        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        List<String> roles = realmAccess != null
                ? (List<String>) realmAccess.get("roles")
                : List.of();

        Long createdTimestamp = jwt.getClaim("auth_time");

        return UserProfileDTO.builder()
                .id(keycloakId)
                .username(username)
                .email(email)
                .emailVerified(emailVerified != null ? emailVerified : false)
                .roles(roles)
                .createdAt(createdTimestamp != null
                        ? java.time.Instant.ofEpochSecond(createdTimestamp)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime()
                        : null)
                .build();
    }

    /**
     * Change password for current user
     */
    @PostMapping("/change-password")
    public ResponseEntity<ChangePasswordResponseDTO> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequestDTO request) {

        log.info("Password change request received");

        String userId = jwt.getSubject();

        ChangePasswordResponseDTO response = authService.changePassword(userId, request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/ping")
    public String ping() {
        return "User authenticated";
    }

    @GetMapping("/admin-check")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminCheck() {
        return "Admin access granted";
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) return Collections.emptyList();
        List<String> roles = (List<String>) realmAccess.get("roles");
        return roles != null ? roles : Collections.emptyList();
    }
}
