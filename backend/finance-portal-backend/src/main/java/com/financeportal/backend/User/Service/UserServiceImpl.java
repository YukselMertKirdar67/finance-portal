package com.financeportal.backend.User.Service;


import com.financeportal.backend.User.Entity.User;
import com.financeportal.backend.User.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User getOrCreateUser(Jwt jwt) {

        String keycloakId = jwt.getSubject(); // sub
        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");

        return userRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> {
                    User user = User.builder()
                            .keycloakId(keycloakId)
                            .username(username)
                            .email(email)
                            .enabled(true)
                            .build();
                    return userRepository.save(user);
                });
    }

    @Override
    public User getByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() ->
                        new RuntimeException("User not found with keycloakId: " + keycloakId));
    }
}

