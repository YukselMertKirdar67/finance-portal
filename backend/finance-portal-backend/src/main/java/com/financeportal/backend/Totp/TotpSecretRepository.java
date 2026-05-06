package com.financeportal.backend.Totp;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TotpSecretRepository extends JpaRepository<TotpSecret, Long> {
    Optional<TotpSecret> findByKeycloakId(String keycloakId);
    void deleteByKeycloakId(String keycloakId);
    boolean existsByKeycloakIdAndVerifiedTrue(String keycloakId);
}
