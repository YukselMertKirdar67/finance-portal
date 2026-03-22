package com.financeportal.backend.User.Service;

import com.financeportal.backend.User.Entity.User;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;

public interface UserService {


    User getOrCreateUser(Jwt jwt);

    User getByKeycloakId(String keycloakId);

    void updateUsername(String userId, String newUsername);
    void updateEmail(String userId, String newEmail, String password);
    LocalDateTime getPasswordLastChanged(String userId);
}