package com.financeportal.backend.Portfolio;

import com.financeportal.backend.Instrument.Entity.ForexInstrument;
import com.financeportal.backend.Instrument.Enum.InstrumentType;
import com.financeportal.backend.Portfolio.DTO.*;
import com.financeportal.backend.Portfolio.Entity.Portfolio;
import com.financeportal.backend.Portfolio.Entity.PortfolioHolding;
import com.financeportal.backend.Portfolio.Entity.PortfolioTransaction;
import com.financeportal.backend.Portfolio.Enum.PortfolioType;
import com.financeportal.backend.Portfolio.Enum.TransactionType;
import com.financeportal.backend.Portfolio.Mapper.PortfolioMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PortfolioMapper Unit Testleri")
class PortfolioMapperTest {

    private PortfolioMapperImpl portfolioMapper;

    private Portfolio testPortfolio;
    private ForexInstrument testInstrument;
    private PortfolioHolding testHolding;
    private PortfolioTransaction testTransaction;

    @BeforeEach
    void setUp() {
        portfolioMapper = new PortfolioMapperImpl();

        testPortfolio = new Portfolio();
        testPortfolio.setId(1L);
        testPortfolio.setName("Test Portföy");
        testPortfolio.setUserId("test-user-id");
        testPortfolio.setPortfolioType(PortfolioType.PERSONAL);
        testPortfolio.setCurrency("TRY");
        testPortfolio.setActive(true);
        testPortfolio.setCreatedAt(LocalDateTime.now());

        testInstrument = ForexInstrument.builder()
                .symbol("USD/TRY")
                .name("Amerikan Doları")
                .currency("TRY")
                .exchange("TCMB")
                .build();
        testInstrument.setId(1L);

        testHolding = new PortfolioHolding();
        testHolding.setId(1L);
        testHolding.setPortfolio(testPortfolio);
        testHolding.setInstrument(testInstrument);
        testHolding.setQuantity(new BigDecimal("10"));
        testHolding.setAverageBuyPrice(new BigDecimal("38.00"));
        testHolding.setCurrency("TRY");
        testHolding.setExchangeRate(new BigDecimal("1.00"));

        testTransaction = new PortfolioTransaction();
        testTransaction.setId(1L);
        testTransaction.setPortfolio(testPortfolio);
        testTransaction.setInstrument(testInstrument);
        testTransaction.setTransactionType(TransactionType.BUY);
        testTransaction.setQuantity(new BigDecimal("10"));
        testTransaction.setPrice(new BigDecimal("38.50"));
        testTransaction.setTotalAmount(new BigDecimal("385.00"));
        testTransaction.setTransactionDate(LocalDateTime.now());
    }

    @Test
    @DisplayName("Portfolio → PortfolioDTO dönüşümü başarılı olmalı")
    void toDTO_Success() {
        PortfolioDTO result = portfolioMapper.toDTO(testPortfolio);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Portföy");
        assertThat(result.getUserId()).isEqualTo("test-user-id");
        assertThat(result.getPortfolioType()).isEqualTo(PortfolioType.PERSONAL);
        assertThat(result.getCurrency()).isEqualTo("TRY");
        assertThat(result.getActive()).isTrue();
    }

    @Test
    @DisplayName("Null portfolio için null dönmeli")
    void toDTO_NullPortfolio_ReturnsNull() {
        PortfolioDTO result = portfolioMapper.toDTO(null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Portfolio → PortfolioDetailDTO dönüşümü başarılı olmalı")
    void toDetailDTO_Success() {
        PortfolioDetailDTO result = portfolioMapper.toDetailDTO(testPortfolio);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Portföy");
    }

    @Test
    @DisplayName("CreatePortfolioRequestDTO → Portfolio entity dönüşümü başarılı olmalı")
    void toEntity_Success() {
        CreatePortfolioRequestDTO request = new CreatePortfolioRequestDTO();
        request.setName("Yeni Portföy");
        request.setPortfolioType(PortfolioType.SAVINGS);
        request.setCurrency("USD");

        Portfolio result = portfolioMapper.toEntity(request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Yeni Portföy");
        assertThat(result.getPortfolioType()).isEqualTo(PortfolioType.SAVINGS);
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    @DisplayName("Portfolio listesi → PortfolioDTO listesi dönüşümü başarılı olmalı")
    void toDTOList_Success() {
        List<PortfolioDTO> result = portfolioMapper.toDTOList(List.of(testPortfolio));

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test Portföy");
    }

    @Test
    @DisplayName("PortfolioHolding → HoldingDTO dönüşümü başarılı olmalı")
    void toHoldingDTO_Success() {
        BigDecimal currentPrice = new BigDecimal("38.50");

        HoldingDTO result = portfolioMapper.toHoldingDTO(testHolding, currentPrice);

        assertThat(result).isNotNull();
        assertThat(result.getHoldingId()).isEqualTo(1L);
        assertThat(result.getInstrumentId()).isEqualTo(1L);
        assertThat(result.getInstrumentSymbol()).isEqualTo("USD/TRY");
        assertThat(result.getInstrumentName()).isEqualTo("Amerikan Doları");
        assertThat(result.getQuantity()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(result.getAverageBuyPrice()).isEqualByComparingTo(new BigDecimal("38.00"));
        assertThat(result.getCurrentPrice()).isEqualByComparingTo(currentPrice);
        assertThat(result.getInstrumentType()).isEqualTo(InstrumentType.FOREX.name());
    }

    @Test
    @DisplayName("PortfolioTransaction → TransactionDTO dönüşümü başarılı olmalı")
    void toTransactionDTO_Success() {
        TransactionDTO result = portfolioMapper.toTransactionDTO(testTransaction);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getPortfolioId()).isEqualTo(1L);
        assertThat(result.getPortfolioName()).isEqualTo("Test Portföy");
        assertThat(result.getInstrumentId()).isEqualTo(1L);
        assertThat(result.getInstrumentSymbol()).isEqualTo("USD/TRY");
        assertThat(result.getTransactionType()).isEqualTo(TransactionType.BUY);
        assertThat(result.getQuantity()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("38.50"));
    }

    @Test
    @DisplayName("CreateTransactionRequestDTO → PortfolioTransaction entity dönüşümü başarılı olmalı")
    void toTransactionEntity_Success() {
        CreateTransactionRequestDTO request = new CreateTransactionRequestDTO();
        request.setTransactionType(TransactionType.BUY);
        request.setQuantity(new BigDecimal("5"));
        request.setPrice(new BigDecimal("40.00"));

        PortfolioTransaction result = portfolioMapper.toTransactionEntity(request);

        assertThat(result).isNotNull();
        assertThat(result.getTransactionType()).isEqualTo(TransactionType.BUY);
        assertThat(result.getQuantity()).isEqualByComparingTo(new BigDecimal("5"));
        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("40.00"));
        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    @DisplayName("calculateTotalAmount - miktar ve fiyat çarpımı doğru hesaplanmalı")
    void calculateTotalAmount_Success() {
        CreateTransactionRequestDTO request = new CreateTransactionRequestDTO();
        request.setQuantity(new BigDecimal("10"));
        request.setPrice(new BigDecimal("38.50"));

        BigDecimal result = portfolioMapper.calculateTotalAmount(request);

        assertThat(result).isEqualByComparingTo(new BigDecimal("385.00"));
    }

    @Test
    @DisplayName("calculateTotalAmount - null miktar için sıfır dönmeli")
    void calculateTotalAmount_NullQuantity_ReturnsZero() {
        CreateTransactionRequestDTO request = new CreateTransactionRequestDTO();
        request.setQuantity(null);
        request.setPrice(new BigDecimal("38.50"));

        BigDecimal result = portfolioMapper.calculateTotalAmount(request);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("calculateNetAmount - komisyon ve vergi dahil net tutar doğru hesaplanmalı")
    void calculateNetAmount_WithCommissionAndTax() {
        testTransaction.setTotalAmount(new BigDecimal("385.00"));
        testTransaction.setCommission(new BigDecimal("5.00"));
        testTransaction.setTax(new BigDecimal("2.00"));

        BigDecimal result = portfolioMapper.calculateNetAmount(testTransaction);

        assertThat(result).isEqualByComparingTo(new BigDecimal("392.00"));
    }

    @Test
    @DisplayName("calculateNetAmount - komisyon yoksa totalAmount dönmeli")
    void calculateNetAmount_NoCommission_ReturnsTotalAmount() {
        testTransaction.setTotalAmount(new BigDecimal("385.00"));
        testTransaction.setCommission(null);
        testTransaction.setTax(null);

        BigDecimal result = portfolioMapper.calculateNetAmount(testTransaction);

        assertThat(result).isEqualByComparingTo(new BigDecimal("385.00"));
    }

    @Test
    @DisplayName("mapInstrumentType - null instrument için null dönmeli")
    void mapInstrumentType_NullInstrument_ReturnsNull() {
        String result = portfolioMapper.mapInstrumentType(null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("mapInstrumentType - FOREX type doğru dönmeli")
    void mapInstrumentType_ForexInstrument_ReturnsForex() {
        String result = portfolioMapper.mapInstrumentType(testInstrument);
        assertThat(result).isEqualTo("FOREX");
    }
}
