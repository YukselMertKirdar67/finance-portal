package com.financeportal.backend.User;

import com.financeportal.backend.Portfolio.Repository.PortfolioRepository;
import com.financeportal.backend.Portfolio.Repository.PortfolioTransactionRepository;
import com.financeportal.backend.User.DTO.*;
import com.financeportal.backend.User.Service.AdminServiceImpl;
import com.financeportal.backend.Watchlist.WatchlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminService Unit Testleri")
class AdminServiceTest {

    @Mock
    private Keycloak keycloakAdminClient;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PortfolioTransactionRepository transactionRepository;

    @Mock
    private WatchlistRepository watchlistRepository;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    @Mock
    private RolesResource rolesResource;

    @Mock
    private RoleResource roleResource;

    @InjectMocks
    private AdminServiceImpl adminService;

    private UserRepresentation testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminService, "realm", "finance-portal");

        testUser = new UserRepresentation();
        testUser.setId("test-user-id");
        testUser.setUsername("testuser");
        testUser.setEmail("test@test.com");
        testUser.setEnabled(true);
        testUser.setEmailVerified(true);
        testUser.setCreatedTimestamp(System.currentTimeMillis());

        when(keycloakAdminClient.realm("finance-portal")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(anyString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(testUser);
    }

    @Test
    @DisplayName("Tüm kullanıcılar başarıyla getirilmeli")
    void getAllUsers_ReturnsUserList() {
        when(usersResource.list()).thenReturn(List.of(testUser));

        List<UserResponseDTO> result = adminService.getAllUsers();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("testuser");
        verify(usersResource, times(1)).list();
    }

    @Test
    @DisplayName("Keycloak hatası olduğunda exception fırlatılmalı")
    void getAllUsers_KeycloakError_ThrowsException() {
        when(usersResource.list()).thenThrow(new RuntimeException("Keycloak error"));

        assertThatThrownBy(() -> adminService.getAllUsers())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to fetch users from Keycloak");
    }

    @Test
    @DisplayName("Kullanıcı başarıyla devre dışı bırakılmalı")
    void disableUser_Success() {
        doNothing().when(userResource).update(any(UserRepresentation.class));

        assertThatNoException().isThrownBy(() -> adminService.disableUser("test-user-id"));

        verify(userResource, times(1)).update(any(UserRepresentation.class));
    }

    @Test
    @DisplayName("Kullanıcı başarıyla aktif edilmeli")
    void enableUser_Success() {
        doNothing().when(userResource).update(any(UserRepresentation.class));

        assertThatNoException().isThrownBy(() -> adminService.enableUser("test-user-id"));

        verify(userResource, times(1)).update(any(UserRepresentation.class));
    }

    @Test
    @DisplayName("Kullanıcı araması sonuç döndürmeli")
    void searchUsers_ReturnsResults() {
        when(usersResource.search("testuser")).thenReturn(List.of(testUser));

        List<UserResponseDTO> result = adminService.searchUsers("testuser");

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Admin istatistikleri hata durumunda sıfır değer döndürmeli")
    void getAdminStats_KeycloakError_ReturnsZeroStats() {
        when(usersResource.list()).thenThrow(new RuntimeException("Keycloak error"));

        AdminStatsDTO result = adminService.getAdminStats();

        assertThat(result).isNotNull();
        assertThat(result.getTotalUsers()).isEqualTo(0L);
        assertThat(result.getActiveUsers()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Admin istatistikleri başarıyla getirilmeli")
    void getAdminStats_Success() {
        when(usersResource.list()).thenReturn(List.of(testUser));
        when(portfolioRepository.count()).thenReturn(5L);
        when(portfolioRepository.findAll()).thenReturn(List.of());
        when(transactionRepository.count()).thenReturn(10L);
        when(transactionRepository.findAll()).thenReturn(List.of());
        when(watchlistRepository.count()).thenReturn(3L);

        AdminStatsDTO result = adminService.getAdminStats();

        assertThat(result).isNotNull();
        assertThat(result.getTotalUsers()).isEqualTo(1L);
        assertThat(result.getActiveUsers()).isEqualTo(1L);
        assertThat(result.getTotalPortfolios()).isEqualTo(5L);
        assertThat(result.getTotalWatchlistItems()).isEqualTo(3L);
    }
}