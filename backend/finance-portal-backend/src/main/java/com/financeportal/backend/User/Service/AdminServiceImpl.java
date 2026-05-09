package com.financeportal.backend.User.Service;

import com.financeportal.backend.Portfolio.Entity.Portfolio;
import com.financeportal.backend.Portfolio.Enum.TransactionType;
import com.financeportal.backend.Portfolio.Repository.PortfolioRepository;
import com.financeportal.backend.Portfolio.Repository.PortfolioTransactionRepository;
import com.financeportal.backend.User.DTO.*;
import com.financeportal.backend.Watchlist.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class AdminServiceImpl implements AdminService {

    private final Keycloak keycloakAdminClient;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioTransactionRepository transactionRepository;
    private final WatchlistRepository watchlistRepository;

    @Value("${keycloak.admin.realm}")
    private String realm;

    /**
     * Keycloak'tan tüm kullanıcıları getirir ve DTO'ya dönüştürür.
     */
    @Override
    public List<UserResponseDTO> getAllUsers() {
        log.info("Admin: Fetching all users from Keycloak");

        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            List<UserRepresentation> users = realmResource.users().list();

            return users.stream()
                    .map(this::mapToUserResponseDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching users from Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch users from Keycloak", e);
        }
    }

    /**
     * Keycloak'ta kullanıcıyı devre dışı bırakır (enabled = false).
     */
    @Override
    public void disableUser(String userId) {
        log.info("Admin: Disabling user with Keycloak ID: {}", userId);

        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UsersResource usersResource = realmResource.users();

            UserRepresentation user = usersResource.get(userId).toRepresentation();
            user.setEnabled(false);
            usersResource.get(userId).update(user);

            log.info("Admin: User disabled successfully: {}", userId);

        } catch (Exception e) {
            log.error("Error disabling user: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to disable user", e);
        }
    }

    /**
     * Keycloak'ta kullanıcıyı aktif eder (enabled = true).
     */
    @Override
    public void enableUser(String userId) {
        log.info("Admin: Enabling user with Keycloak ID: {}", userId);

        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UsersResource usersResource = realmResource.users();

            UserRepresentation user = usersResource.get(userId).toRepresentation();
            user.setEnabled(true);
            usersResource.get(userId).update(user);

            log.info("Admin: User enabled successfully: {}", userId);

        } catch (Exception e) {
            log.error("Error enabling user: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to enable user", e);
        }
    }

    /**
     * Keycloak'ta kullanıcı arar. Query ile username, email veya ad soyad eşleşmesi yapar.
     */
    @Override
    public List<UserResponseDTO> searchUsers(String query) {
        log.info("Admin: Searching users with query: {}", query);

        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            List<UserRepresentation> users = realmResource.users().search(query);

            return users.stream()
                    .map(this::mapToUserResponseDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error searching users: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search users", e);
        }
    }

    /**
     * Kullanıcının detay bilgilerini getirir.
     * Keycloak'tan profil ve roller, DB'den portföy/işlem/watchlist istatistikleri alınır.
     */
    @Override
    public UserDetailDTO getUserDetail(String userId) {
        log.info("Fetching user detail for userId: {}", userId);

        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UsersResource usersResource = realmResource.users();

            UserRepresentation keycloakUser = usersResource.get(userId).toRepresentation();

            List<String> roles = usersResource.get(userId)
                    .roles()
                    .realmLevel()
                    .listAll()
                    .stream()
                    .map(org.keycloak.representations.idm.RoleRepresentation::getName)
                    .collect(Collectors.toList());

            int portfolioCount = 0;
            int transactionCount = 0;
            int watchlistCount = 0;

            try {
                portfolioCount = (int) portfolioRepository.findAll().stream()
                        .filter(p -> userId.equals(p.getUserId()))
                        .count();

                transactionCount = (int) transactionRepository.findAll().stream()
                        .filter(t -> t.getPortfolio() != null &&
                                userId.equals(t.getPortfolio().getUserId()))
                        .count();

                watchlistCount = (int) watchlistRepository.findAll().stream()
                        .filter(w -> userId.equals(w.getUserId()))
                        .count();

            } catch (Exception e) {
                log.warn("Could not fetch user statistics from database: {}", e.getMessage());
            }

            LocalDateTime createdAt = keycloakUser.getCreatedTimestamp() != null
                    ? LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(keycloakUser.getCreatedTimestamp()),
                    ZoneId.systemDefault())
                    : null;

            return UserDetailDTO.builder()
                    .id(keycloakUser.getId())
                    .username(keycloakUser.getUsername())
                    .email(keycloakUser.getEmail())
                    .enabled(keycloakUser.isEnabled())
                    .emailVerified(keycloakUser.isEmailVerified())
                    .createdAt(createdAt)
                    .roles(roles)
                    .portfolioCount(portfolioCount)
                    .transactionCount(transactionCount)
                    .watchlistCount(watchlistCount)
                    .build();

        } catch (Exception e) {
            log.error("Error fetching user detail: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch user detail");
        }
    }

    /**
     * Kullanıcıya Keycloak'ta belirtilen rolü atar.
     */
    @Override
    public AssignRoleResponseDTO assignRole(String userId, AssignRoleRequestDTO request) {
        log.info("Assigning role {} to user {}", request.getRoleName(), userId);

        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UsersResource usersResource = realmResource.users();

            org.keycloak.representations.idm.RoleRepresentation roleRepresentation =
                    realmResource.roles().get(request.getRoleName()).toRepresentation();

            usersResource.get(userId).roles().realmLevel().add(List.of(roleRepresentation));

            log.info("✅ Role {} assigned to user {}", request.getRoleName(), userId);

            return AssignRoleResponseDTO.builder()
                    .success(true)
                    .message("Role assigned successfully")
                    .build();

        } catch (Exception e) {
            log.error("❌ Error assigning role: {}", e.getMessage(), e);
            return AssignRoleResponseDTO.builder()
                    .success(false)
                    .message("Failed to assign role: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Kullanıcıdan Keycloak'ta belirtilen rolü kaldırır.
     */
    @Override
    public RemoveRoleResponseDTO removeRole(String userId, RemoveRoleRequestDTO request) {
        log.info("Removing role {} from user {}", request.getRoleName(), userId);

        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UsersResource usersResource = realmResource.users();

            org.keycloak.representations.idm.RoleRepresentation roleRepresentation =
                    realmResource.roles().get(request.getRoleName()).toRepresentation();

            usersResource.get(userId).roles().realmLevel().remove(List.of(roleRepresentation));

            log.info("✅ Role {} removed from user {}", request.getRoleName(), userId);

            return RemoveRoleResponseDTO.builder()
                    .success(true)
                    .message("Role removed successfully")
                    .build();

        } catch (Exception e) {
            log.error("❌ Error removing role: {}", e.getMessage(), e);
            return RemoveRoleResponseDTO.builder()
                    .success(false)
                    .message("Failed to remove role: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Admin dashboard için genel istatistikleri getirir.
     * Keycloak'tan kullanıcı sayıları, DB'den portföy, işlem ve watchlist istatistikleri alınır.
     */
    @Override
    @Transactional(readOnly = true)
    public AdminStatsDTO getAdminStats() {
        log.info("Admin: Fetching admin dashboard statistics");

        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            List<UserRepresentation> allUsers = realmResource.users().list();

            Long totalUsers = (long) allUsers.size();
            Long activeUsers = allUsers.stream().filter(UserRepresentation::isEnabled).count();
            Long disabledUsers = totalUsers - activeUsers;

            log.info("✅ Keycloak stats - Total: {}, Active: {}, Disabled: {}",
                    totalUsers, activeUsers, disabledUsers);

            Long totalPortfolios = portfolioRepository.count();
            Long activePortfolios = portfolioRepository.findAll().stream()
                    .filter(Portfolio::isActive)
                    .count();

            log.info("✅ Portfolio stats - Total: {}, Active: {}", totalPortfolios, activePortfolios);

            BigDecimal totalPortfolioValue = transactionRepository.findAll().stream()
                    .filter(tx -> tx.getTransactionType() == TransactionType.BUY && !tx.isDeleted())
                    .map(tx -> tx.getTotalAmount() != null ? tx.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Long totalTransactions = transactionRepository.count();
            Long buyTransactions = transactionRepository.findAll().stream()
                    .filter(tx -> tx.getTransactionType() == TransactionType.BUY)
                    .count();
            Long sellTransactions = totalTransactions - buyTransactions;

            log.info("✅ Transaction stats - Total: {}, Buy: {}, Sell: {}",
                    totalTransactions, buyTransactions, sellTransactions);

            Long totalWatchlistItems = watchlistRepository.count();
            log.info("✅ Watchlist stats - Total: {}", totalWatchlistItems);

            return AdminStatsDTO.builder()
                    .totalUsers(totalUsers)
                    .activeUsers(activeUsers)
                    .disabledUsers(disabledUsers)
                    .totalPortfolios(totalPortfolios)
                    .activePortfolios(activePortfolios)
                    .totalPortfolioValue(totalPortfolioValue)
                    .totalTransactions(totalTransactions)
                    .buyTransactions(buyTransactions)
                    .sellTransactions(sellTransactions)
                    .totalWatchlistItems(totalWatchlistItems)
                    .build();

        } catch (Exception e) {
            log.error("❌ Error fetching admin stats: {}", e.getMessage(), e);
            return AdminStatsDTO.builder()
                    .totalUsers(0L).activeUsers(0L).disabledUsers(0L)
                    .totalPortfolios(0L).activePortfolios(0L)
                    .totalPortfolioValue(BigDecimal.ZERO)
                    .totalTransactions(0L).buyTransactions(0L).sellTransactions(0L)
                    .totalWatchlistItems(0L)
                    .build();
        }
    }

    /**
     * Keycloak UserRepresentation'ı UserResponseDTO'ya dönüştürür.
     */
    private UserResponseDTO mapToUserResponseDTO(UserRepresentation keycloakUser) {
        return UserResponseDTO.builder()
                .id(keycloakUser.getId())
                .username(keycloakUser.getUsername())
                .email(keycloakUser.getEmail())
                .enabled(keycloakUser.isEnabled())
                .emailVerified(keycloakUser.isEmailVerified())
                .createdAt(keycloakUser.getCreatedTimestamp() != null
                        ? new java.util.Date(keycloakUser.getCreatedTimestamp()).toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime()
                        : null)
                .build();
    }
}