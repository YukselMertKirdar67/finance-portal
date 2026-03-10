package com.financeportal.backend.User.Controller;

import com.financeportal.backend.User.DTO.MeResponseDTO;
import com.financeportal.backend.User.Entity.User;
import com.financeportal.backend.User.UserMapper;
import com.financeportal.backend.User.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping
    public MeResponseDTO me(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.getOrCreateUser(jwt);
        List<String> roles = extractRoles(jwt);
        return userMapper.toMeResponseDTO(user, roles);
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
