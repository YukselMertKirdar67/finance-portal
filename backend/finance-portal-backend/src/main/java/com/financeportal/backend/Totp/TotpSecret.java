package com.financeportal.backend.Totp;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "totp_secrets")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class TotpSecret {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String keycloakId;

    @Column(nullable = false)
    private String secret;

    @Column(nullable = false)
    private boolean verified;
}
