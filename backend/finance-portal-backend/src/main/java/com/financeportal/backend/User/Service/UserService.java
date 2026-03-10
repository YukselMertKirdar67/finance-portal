package com.financeportal.backend.User.Service;

import com.financeportal.backend.User.Entity.User;
import org.springframework.security.oauth2.jwt.Jwt;

public interface UserService {


    User getOrCreateUser(Jwt jwt);

    User getByKeycloakId(String keycloakId);
}