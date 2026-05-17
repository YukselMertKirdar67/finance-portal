package com.financeportal.backend.Portfolio;

import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.Exception.UnauthorizedException;
import com.financeportal.backend.Instrument.Repository.InstrumentPriceRepository;
import com.financeportal.backend.Instrument.Repository.InstrumentRepository;
import com.financeportal.backend.Instrument.Service.TcmbService;
import com.financeportal.backend.Portfolio.DTO.CreatePortfolioRequestDTO;
import com.financeportal.backend.Portfolio.DTO.PortfolioDTO;
import com.financeportal.backend.Portfolio.Entity.Portfolio;
import com.financeportal.backend.Portfolio.Enum.PortfolioType;
import com.financeportal.backend.Portfolio.Mapper.PortfolioMapper;
import com.financeportal.backend.Portfolio.Repository.PortfolioHoldingRepository;
import com.financeportal.backend.Portfolio.Repository.PortfolioRepository;
import com.financeportal.backend.Portfolio.Service.PortfolioHoldingService;
import com.financeportal.backend.Portfolio.Service.PortfolioServiceImpl;
import com.financeportal.backend.Util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Portfolio Service Unit Tests")
class PortfolioServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PortfolioHoldingService holdingService;

    @Mock
    private PortfolioMapper portfolioMapper;

    @Mock
    private InstrumentPriceRepository instrumentPriceRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private PortfolioHoldingRepository holdingRepository;

    @Mock
    private TcmbService tcmbService;

    @InjectMocks
    private PortfolioServiceImpl portfolioService;

    private static final String TEST_USER_ID = "test-keycloak-id-123";

    private Portfolio testPortfolio;
    private PortfolioDTO testPortfolioDTO;
    private CreatePortfolioRequestDTO createRequest;

    @BeforeEach
    void setUp() {
        testPortfolio = new Portfolio();
        testPortfolio.setId(1L);
        testPortfolio.setUserId(TEST_USER_ID);
        testPortfolio.setName("Test Portföy");
        testPortfolio.setDescription("Test açıklama");
        testPortfolio.setPortfolioType(PortfolioType.PERSONAL);
        testPortfolio.setCurrency("TRY");
        testPortfolio.setActive(true);
        testPortfolio.setHoldings(new ArrayList<>());
        testPortfolio.setTransactions(new ArrayList<>());
        testPortfolio.setCreatedAt(LocalDateTime.now());
        testPortfolio.setUpdatedAt(LocalDateTime.now());

        testPortfolioDTO = PortfolioDTO.builder()
                .id(1L)
                .name("Test Portföy")
                .description("Test açıklama")
                .portfolioType(PortfolioType.PERSONAL)
                .currency("TRY")
                .active(true)
                .userId(TEST_USER_ID)
                .totalValue(BigDecimal.ZERO)
                .totalInvested(BigDecimal.ZERO)
                .unrealizedPnL(BigDecimal.ZERO)
                .pnlPercent(BigDecimal.ZERO)
                .holdingCount(0)
                .build();

        createRequest = new CreatePortfolioRequestDTO();
        createRequest.setName("Test Portföy");
        createRequest.setDescription("Test açıklama");
        createRequest.setPortfolioType(PortfolioType.PERSONAL);
        createRequest.setCurrency("TRY");
    }

    // ========== CREATE PORTFOLIO TESTS ==========

    @Test
    @DisplayName("Portföy başarıyla oluşturulmalı")
    void createPortfolio_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            when(portfolioMapper.toEntity(createRequest)).thenReturn(testPortfolio);
            when(portfolioRepository.save(testPortfolio)).thenReturn(testPortfolio);
            when(portfolioMapper.toDTO(testPortfolio)).thenReturn(testPortfolioDTO);

            PortfolioDTO result = portfolioService.createPortfolio(createRequest);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Test Portföy");
            assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);
            assertThat(result.getTotalValue()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getHoldingCount()).isEqualTo(0);

            verify(portfolioRepository, times(1)).save(testPortfolio);
        }
    }

    @Test
    @DisplayName("Portföy oluşturulurken userId set edilmeli")
    void createPortfolio_SetsUserId() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            Portfolio portfolioToSave = new Portfolio();
            portfolioToSave.setHoldings(new ArrayList<>());
            portfolioToSave.setTransactions(new ArrayList<>());

            when(portfolioMapper.toEntity(createRequest)).thenReturn(portfolioToSave);
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(testPortfolio);
            when(portfolioMapper.toDTO(testPortfolio)).thenReturn(testPortfolioDTO);

            portfolioService.createPortfolio(createRequest);

            assertThat(portfolioToSave.getUserId()).isEqualTo(TEST_USER_ID);
        }
    }

    // ========== DELETE PORTFOLIO TESTS ==========

    @Test
    @DisplayName("Portföy soft delete ile pasif hale getirilmeli")
    void deletePortfolio_SoftDelete_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(testPortfolio));
            when(portfolioRepository.save(testPortfolio)).thenReturn(testPortfolio);

            portfolioService.deletePortfolio(1L);

            assertThat(testPortfolio.isActive()).isFalse();
            verify(portfolioRepository, times(1)).save(testPortfolio);
        }
    }

    @Test
    @DisplayName("Portföy kalıcı olarak silinmeli")
    void hardDeletePortfolio_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(testPortfolio));

            portfolioService.hardDeletePortfolio(1L);

            verify(portfolioRepository, times(1)).delete(testPortfolio);
        }
    }

    @Test
    @DisplayName("Var olmayan portföy silinmeye çalışıldığında exception fırlatılmalı")
    void deletePortfolio_NotFound_ThrowsException() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            when(portfolioRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> portfolioService.deletePortfolio(999L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(portfolioRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("Başka kullanıcının portföyüne erişim reddedilmeli")
    void deletePortfolio_UnauthorizedAccess_ThrowsException() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("different-user-id");

            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(testPortfolio));

            assertThatThrownBy(() -> portfolioService.deletePortfolio(1L))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    // ========== GET PORTFOLIO TESTS ==========

    @Test
    @DisplayName("Kullanıcının portföyleri getirilmeli")
    void getUserPortfolios_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            when(portfolioRepository.findByUserId(TEST_USER_ID)).thenReturn(List.of(testPortfolio));
            when(portfolioMapper.toDTO(testPortfolio)).thenReturn(testPortfolioDTO);
            when(holdingService.calculateTotalInvestment(anyLong(), anyString())).thenReturn(BigDecimal.ZERO);
            when(holdingService.calculateCurrentValue(anyLong(), anyString())).thenReturn(BigDecimal.ZERO);

            List<PortfolioDTO> result = portfolioService.getUserPortfolios();

            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Test Portföy");
        }
    }

    @Test
    @DisplayName("Kullanıcının portföyü yoksa boş liste dönmeli")
    void getUserPortfolios_Empty() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            when(portfolioRepository.findByUserId(TEST_USER_ID)).thenReturn(List.of());

            List<PortfolioDTO> result = portfolioService.getUserPortfolios();

            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }
    }

    // ========== ACTIVATE / DEACTIVATE TESTS ==========

    @Test
    @DisplayName("Portföy aktif hale getirilmeli")
    void activatePortfolio_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            testPortfolio.setActive(false);
            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(testPortfolio));

            portfolioService.activatePortfolio(1L);

            assertThat(testPortfolio.isActive()).isTrue();
            verify(portfolioRepository, times(1)).save(testPortfolio);
        }
    }

    @Test
    @DisplayName("Portföy pasif hale getirilmeli")
    void deactivatePortfolio_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(testPortfolio));

            portfolioService.deactivatePortfolio(1L);

            assertThat(testPortfolio.isActive()).isFalse();
            verify(portfolioRepository, times(1)).save(testPortfolio);
        }
    }
}
