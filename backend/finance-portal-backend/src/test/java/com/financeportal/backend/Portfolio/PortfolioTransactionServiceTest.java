package com.financeportal.backend.Portfolio;

import com.financeportal.backend.Exception.BusinessRuleException;
import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.Instrument.Entity.ForexInstrument;
import com.financeportal.backend.Instrument.Repository.InstrumentRepository;
import com.financeportal.backend.Instrument.Service.TcmbService;
import com.financeportal.backend.Notification.NotificationService;
import com.financeportal.backend.Portfolio.DTO.CreateTransactionRequestDTO;
import com.financeportal.backend.Portfolio.DTO.TransactionDTO;
import com.financeportal.backend.Portfolio.Entity.Portfolio;
import com.financeportal.backend.Portfolio.Entity.PortfolioHolding;
import com.financeportal.backend.Portfolio.Entity.PortfolioTransaction;
import com.financeportal.backend.Portfolio.Enum.PortfolioType;
import com.financeportal.backend.Portfolio.Enum.TransactionType;
import com.financeportal.backend.Portfolio.Mapper.PortfolioMapper;
import com.financeportal.backend.Portfolio.Repository.PortfolioHoldingRepository;
import com.financeportal.backend.Portfolio.Repository.PortfolioRepository;
import com.financeportal.backend.Portfolio.Repository.PortfolioTransactionRepository;
import com.financeportal.backend.Portfolio.Service.PortfolioCalculationService;
import com.financeportal.backend.Portfolio.Service.PortfolioTransactionServiceImpl;
import com.financeportal.backend.Util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Portfolio Transaction Service Unit Tests")
class PortfolioTransactionServiceTest {

    @Mock
    private PortfolioTransactionRepository transactionRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PortfolioHoldingRepository holdingRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private PortfolioCalculationService calculationService;

    @Mock
    private PortfolioMapper portfolioMapper;

    @Mock
    private TcmbService tcmbService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PortfolioTransactionServiceImpl transactionService;

    private static final String TEST_USER_ID = "test-keycloak-id-123";

    private Portfolio testPortfolio;
    private ForexInstrument testInstrument;
    private PortfolioHolding testHolding;
    private PortfolioTransaction testTransaction;
    private CreateTransactionRequestDTO buyRequest;
    private CreateTransactionRequestDTO sellRequest;
    private TransactionDTO testTransactionDTO;

    @BeforeEach
    void setUp() {
        testPortfolio = new Portfolio();
        testPortfolio.setId(1L);
        testPortfolio.setUserId(TEST_USER_ID);
        testPortfolio.setName("Test Portföy");
        testPortfolio.setPortfolioType(PortfolioType.PERSONAL);
        testPortfolio.setCurrency("TRY");
        testPortfolio.setActive(true);

        testInstrument = ForexInstrument.builder()
                .symbol("USD/TRY")
                .name("Amerikan Doları")
                .currency("USD")
                .exchange("TCMB")
                .build();
        testInstrument.setActive(true);
        testInstrument.setId(1L);

        testHolding = new PortfolioHolding();
        testHolding.setId(1L);
        testHolding.setPortfolio(testPortfolio);
        testHolding.setInstrument(testInstrument);
        testHolding.setQuantity(new BigDecimal("100"));
        testHolding.setAverageBuyPrice(new BigDecimal("38.00"));
        testHolding.setCurrency("USD");
        testHolding.setExchangeRate(new BigDecimal("1.00"));

        testTransaction = new PortfolioTransaction();
        testTransaction.setId(1L);
        testTransaction.setPortfolio(testPortfolio);
        testTransaction.setInstrument(testInstrument);
        testTransaction.setTransactionType(TransactionType.BUY);
        testTransaction.setQuantity(new BigDecimal("10"));
        testTransaction.setPrice(new BigDecimal("38.50"));
        testTransaction.setTransactionDate(LocalDateTime.now());

        buyRequest = new CreateTransactionRequestDTO();
        buyRequest.setInstrumentId(1L);
        buyRequest.setTransactionType(TransactionType.BUY);
        buyRequest.setQuantity(new BigDecimal("10"));
        buyRequest.setPrice(new BigDecimal("38.50"));

        sellRequest = new CreateTransactionRequestDTO();
        sellRequest.setInstrumentId(1L);
        sellRequest.setTransactionType(TransactionType.SELL);
        sellRequest.setQuantity(new BigDecimal("5"));
        sellRequest.setPrice(new BigDecimal("40.00"));

        testTransactionDTO = TransactionDTO.builder()
                .id(1L)
                .transactionType(TransactionType.BUY)
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("38.50"))
                .build();

        when(portfolioMapper.toTransactionEntity(any())).thenReturn(testTransaction);
        when(portfolioMapper.toTransactionDTO(any())).thenReturn(testTransactionDTO);
        when(tcmbService.getExchangeRate(anyString())).thenReturn(new BigDecimal("38.50"));
        when(calculationService.calculateNewAverageBuyPrice(any(), any(), any(), any()))
                .thenReturn(new BigDecimal("38.50"));
        when(calculationService.validateSellQuantity(any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("Alış işlemi başarıyla oluşturulmalı")
    void createBuyTransaction_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(testPortfolio));
            when(instrumentRepository.findById(1L)).thenReturn(Optional.of(testInstrument));
            when(transactionRepository.save(any())).thenReturn(testTransaction);
            when(holdingRepository.findByPortfolioIdAndInstrumentId(1L, 1L))
                    .thenReturn(Optional.empty());
            when(holdingRepository.save(any())).thenReturn(testHolding);

            TransactionDTO result = transactionService.createBuyTransaction(1L, buyRequest);

            assertThat(result).isNotNull();
            assertThat(result.getTransactionType()).isEqualTo(TransactionType.BUY);
            verify(transactionRepository, times(1)).save(any());
            verify(notificationService, times(1)).notifyTransaction(
                    eq(TEST_USER_ID), anyString(), eq("BUY"), anyDouble(), eq(1L));
        }
    }

    @Test
    @DisplayName("Var olmayan portföy için exception fırlatılmalı")
    void createBuyTransaction_PortfolioNotFound_ThrowsException() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            when(portfolioRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.createBuyTransaction(999L, buyRequest))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(transactionRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("Başka kullanıcının portföyüne işlem yapılamaz")
    void createBuyTransaction_UnauthorizedPortfolio_ThrowsException() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("different-user");

            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(testPortfolio));

            assertThatThrownBy(() -> transactionService.createBuyTransaction(1L, buyRequest))
                    .isInstanceOf(BusinessRuleException.class);

            verify(transactionRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("Var olmayan enstrüman için exception fırlatılmalı")
    void createBuyTransaction_InstrumentNotFound_ThrowsException() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(testPortfolio));
            when(instrumentRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.createBuyTransaction(1L, buyRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Test
    @DisplayName("Satış işlemi başarıyla oluşturulmalı")
    void createSellTransaction_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(testPortfolio));
            when(instrumentRepository.findById(1L)).thenReturn(Optional.of(testInstrument));
            when(holdingRepository.findByPortfolioIdAndInstrumentId(1L, 1L))
                    .thenReturn(Optional.of(testHolding));
            when(transactionRepository.save(any())).thenReturn(testTransaction);
            when(holdingRepository.save(any())).thenReturn(testHolding);

            TransactionDTO result = transactionService.createSellTransaction(1L, sellRequest);

            assertThat(result).isNotNull();
            verify(transactionRepository, times(1)).save(any());
            verify(notificationService, times(1)).notifyTransaction(
                    eq(TEST_USER_ID), anyString(), eq("SELL"), anyDouble(), eq(1L));
        }
    }

    @Test
    @DisplayName("Holding yoksa satış yapılamaz")
    void createSellTransaction_NoHolding_ThrowsException() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(testPortfolio));
            when(instrumentRepository.findById(1L)).thenReturn(Optional.of(testInstrument));
            when(holdingRepository.findByPortfolioIdAndInstrumentId(1L, null))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.createSellTransaction(1L, sellRequest))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Test
    @DisplayName("Yetersiz miktar ile satış yapılamaz")
    void createSellTransaction_InsufficientQuantity_ThrowsException() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(testPortfolio));
            when(instrumentRepository.findById(1L)).thenReturn(Optional.of(testInstrument));
            when(holdingRepository.findByPortfolioIdAndInstrumentId(1L, null))
                    .thenReturn(Optional.of(testHolding));
            when(calculationService.validateSellQuantity(any(), any())).thenReturn(false);

            assertThatThrownBy(() -> transactionService.createSellTransaction(1L, sellRequest))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Test
    @DisplayName("İşlem geçmişi başarıyla getirilmeli")
    void getTransactionHistory_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(testPortfolio));
            when(transactionRepository.findByPortfolioIdAndDeletedFalseOrderByTransactionDateDesc(1L))
                    .thenReturn(List.of(testTransaction));

            List<TransactionDTO> result = transactionService.getTransactionHistory(1L);

            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
        }
    }

    @Test
    @DisplayName("İşlem soft delete ile silinmeli")
    void deleteTransaction_SoftDelete_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn(TEST_USER_ID);

            when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));
            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(testPortfolio));
            when(transactionRepository.save(any())).thenReturn(testTransaction);

            transactionService.deleteTransaction(1L);

            assertThat(testTransaction.isDeleted()).isTrue();
            verify(transactionRepository, times(1)).save(testTransaction);
        }
    }
}
