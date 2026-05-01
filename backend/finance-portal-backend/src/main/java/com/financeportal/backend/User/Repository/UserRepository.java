package com.financeportal.backend.User.Repository;

import com.financeportal.backend.User.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByKeycloakId(String keycloakId);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    boolean existsByKeycloakId(String keycloakId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT u.keycloakId FROM User u WHERE u.enabled = true")
    List<String> findAllKeycloakIds();
}
