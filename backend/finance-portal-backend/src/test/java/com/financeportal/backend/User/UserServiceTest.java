package com.financeportal.backend.User;

import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.Portfolio.Repository.PortfolioHoldingRepository;
import com.financeportal.backend.Portfolio.Repository.PortfolioRepository;
import com.financeportal.backend.Portfolio.Repository.PortfolioTransactionRepository;
import com.financeportal.backend.User.Entity.User;
import com.financeportal.backend.User.Repository.UserRepository;
import com.financeportal.backend.User.Service.UserServiceImpl;
import com.financeportal.backend.Watchlist.WatchlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserService Unit Testleri")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private Keycloak keycloak;
    @Mock private PortfolioRepository portfolioRepository;
    @Mock private WatchlistRepository watchlistRepository;
    @Mock private PortfolioTransactionRepository transactionRepository;
    @Mock private PortfolioHoldingRepository holdingRepository;
    @Mock private RealmResource realmResource;
    @Mock private UsersResource usersResource;
    @Mock private UserResource userResource;
    @Mock private Jwt jwt;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "realm", "finance-portal");

        testUser = User.builder()
                .keycloakId("test-keycloak-id")
                .username("testuser")
                .email("test@test.com")
                .enabled(true)
                .build();

        when(keycloak.realm("finance-portal")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(anyString())).thenReturn(userResource);

        UserRepresentation keycloakUser = new UserRepresentation();
        keycloakUser.setUsername("testuser");
        keycloakUser.setEmail("test@test.com");
        when(userResource.toRepresentation()).thenReturn(keycloakUser);

        when(jwt.getSubject()).thenReturn("test-keycloak-id");
        when(jwt.getClaimAsString("preferred_username")).thenReturn("testuser");
        when(jwt.getClaimAsString("email")).thenReturn("test@test.com");
    }

    @Test
    @DisplayName("Mevcut kullanıcı JWT ile getirilmeli")
    void getOrCreateUser_ExistingUser_ReturnsUser() {
        when(userRepository.findByKeycloakId("test-keycloak-id"))
                .thenReturn(Optional.of(testUser));

        User result = userService.getOrCreateUser(jwt);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Yeni kullanıcı JWT ile oluşturulmalı")
    void getOrCreateUser_NewUser_CreatesAndReturns() {
        when(userRepository.findByKeycloakId("test-keycloak-id"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.getOrCreateUser(jwt);

        assertThat(result).isNotNull();
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Keycloak ID ile kullanıcı getirilmeli")
    void getByKeycloakId_Success() {
        when(userRepository.findByKeycloakId("test-keycloak-id"))
                .thenReturn(Optional.of(testUser));

        User result = userService.getByKeycloakId("test-keycloak-id");

        assertThat(result).isNotNull();
        assertThat(result.getKeycloakId()).isEqualTo("test-keycloak-id");
    }

    @Test
    @DisplayName("Var olmayan Keycloak ID için exception fırlatılmalı")
    void getByKeycloakId_NotFound_ThrowsException() {
        when(userRepository.findByKeycloakId("nonexistent"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByKeycloakId("nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Kullanıcı adı zaten kullanılıyorsa exception fırlatılmalı")
    void updateUsername_AlreadyExists_ThrowsException() {
        when(userRepository.findByKeycloakId("test-keycloak-id"))
                .thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUsername("test-keycloak-id", "existinguser"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("kullanıcı adı");
    }

    @Test
    @DisplayName("Kullanıcı bulunamadığında username güncellemesi exception fırlatmalı")
    void updateUsername_UserNotFound_ThrowsException() {
        when(userRepository.findByKeycloakId("nonexistent"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUsername("nonexistent", "newuser"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Email zaten kullanılıyorsa exception fırlatılmalı")
    void updateEmail_AlreadyExists_ThrowsException() {
        when(userRepository.findByKeycloakId("test-keycloak-id"))
                .thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateEmail(
                "test-keycloak-id", "existing@test.com", "password"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("e-posta");
    }

    @Test
    @DisplayName("Son şifre değişim tarihi getirilmeli")
    void getPasswordLastChanged_Success() {
        LocalDateTime lastChanged = LocalDateTime.now().minusDays(5);
        testUser.setPasswordLastChanged(lastChanged);
        when(userRepository.findByKeycloakId("test-keycloak-id"))
                .thenReturn(Optional.of(testUser));

        LocalDateTime result = userService.getPasswordLastChanged("test-keycloak-id");

        assertThat(result).isEqualTo(lastChanged);
    }

    @Test
    @DisplayName("Tercihler başarıyla güncellenmeli")
    void updatePreferences_Success() {
        when(userRepository.findByKeycloakId("test-keycloak-id"))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        assertThatNoException().isThrownBy(() ->
                userService.updatePreferences(
                        "test-keycloak-id", "dark",
                        true, true, true, true));

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Hesap silme başarıyla tamamlanmalı")
    void deleteAccount_Success() {
        when(userRepository.findByKeycloakId("test-keycloak-id"))
                .thenReturn(Optional.of(testUser));
        when(portfolioRepository.findByUserId("test-keycloak-id"))
                .thenReturn(List.of());
        doNothing().when(watchlistRepository).deleteAllByUserId("test-keycloak-id");
        doNothing().when(userRepository).delete(testUser);
        doNothing().when(userResource).remove();

        assertThatNoException().isThrownBy(() ->
                userService.deleteAccount("test-keycloak-id"));

        verify(userRepository, times(1)).delete(testUser);
    }
}
