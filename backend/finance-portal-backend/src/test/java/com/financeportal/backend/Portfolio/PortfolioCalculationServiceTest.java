package com.financeportal.backend.Portfolio;

import com.financeportal.backend.Portfolio.Service.PortfolioCalculationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Portfolio Calculation Service Unit Tests")
class PortfolioCalculationServiceTest {

    private PortfolioCalculationServiceImpl calculationService;

    @BeforeEach
    void setUp() {
        calculationService = new PortfolioCalculationServiceImpl();
    }

    @Test
    @DisplayName("Yeni ortalama alış fiyatı doğru hesaplanmalı")
    void calculateNewAverageBuyPrice_Success() {
        BigDecimal result = calculationService.calculateNewAverageBuyPrice(
                new BigDecimal("10"),
                new BigDecimal("100.00"),
                new BigDecimal("5"),
                new BigDecimal("110.00")
        );
        // (10 * 100 + 5 * 110) / 15 = 1550 / 15 = 103.33
        assertThat(result).isEqualByComparingTo(new BigDecimal("103.333333"));
    }

    @Test
    @DisplayName("Mevcut miktar sıfırsa yeni fiyat dönmeli")
    void calculateNewAverageBuyPrice_ZeroExisting_ReturnsNewPrice() {
        BigDecimal result = calculationService.calculateNewAverageBuyPrice(
                BigDecimal.ZERO,
                new BigDecimal("100.00"),
                new BigDecimal("5"),
                new BigDecimal("110.00")
        );
        assertThat(result).isEqualByComparingTo(new BigDecimal("110.000000"));
    }

    @Test
    @DisplayName("Gerçekleşmemiş kâr/zarar doğru hesaplanmalı - kâr")
    void calculateUnrealizedPnL_Profit() {
        BigDecimal result = calculationService.calculateUnrealizedPnL(
                new BigDecimal("10"),
                new BigDecimal("100.00"),
                new BigDecimal("120.00")
        );
        // (120 - 100) * 10 = 200
        assertThat(result).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    @DisplayName("Gerçekleşmemiş kâr/zarar doğru hesaplanmalı - zarar")
    void calculateUnrealizedPnL_Loss() {
        BigDecimal result = calculationService.calculateUnrealizedPnL(
                new BigDecimal("10"),
                new BigDecimal("100.00"),
                new BigDecimal("80.00")
        );
        // (80 - 100) * 10 = -200
        assertThat(result).isEqualByComparingTo(new BigDecimal("-200.00"));
    }

    @Test
    @DisplayName("Kâr/zarar yüzdesi doğru hesaplanmalı")
    void calculatePnLPercent_Success() {
        BigDecimal result = calculationService.calculatePnLPercent(
                new BigDecimal("100.00"),
                new BigDecimal("120.00")
        );
        // ((120 - 100) / 100) * 100 = 20%
        assertThat(result).isEqualByComparingTo(new BigDecimal("20.00"));
    }

    @Test
    @DisplayName("Ortalama alış fiyatı sıfırsa kâr/zarar yüzdesi sıfır dönmeli")
    void calculatePnLPercent_ZeroAvgPrice_ReturnsZero() {
        BigDecimal result = calculationService.calculatePnLPercent(
                BigDecimal.ZERO,
                new BigDecimal("120.00")
        );
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Toplam yatırım doğru hesaplanmalı")
    void calculateTotalInvestment_Success() {
        BigDecimal result = calculationService.calculateTotalInvestment(
                new BigDecimal("10"),
                new BigDecimal("100.00")
        );
        assertThat(result).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("Güncel değer doğru hesaplanmalı")
    void calculateCurrentValue_Success() {
        BigDecimal result = calculationService.calculateCurrentValue(
                new BigDecimal("10"),
                new BigDecimal("120.00")
        );
        assertThat(result).isEqualByComparingTo(new BigDecimal("1200.00"));
    }

    @Test
    @DisplayName("Satış miktarı geçerli olmalı")
    void validateSellQuantity_ValidQuantity_ReturnsTrue() {
        boolean result = calculationService.validateSellQuantity(
                new BigDecimal("10"),
                new BigDecimal("5")
        );
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Mevcut miktardan fazla satış reddedilmeli")
    void validateSellQuantity_ExceedsAvailable_ReturnsFalse() {
        boolean result = calculationService.validateSellQuantity(
                new BigDecimal("10"),
                new BigDecimal("15")
        );
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Sıfır satış miktarı reddedilmeli")
    void validateSellQuantity_ZeroQuantity_ReturnsFalse() {
        boolean result = calculationService.validateSellQuantity(
                new BigDecimal("10"),
                BigDecimal.ZERO
        );
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Portföy getirisi doğru hesaplanmalı")
    void calculatePortfolioReturn_Success() {
        BigDecimal result = calculationService.calculatePortfolioReturn(
                new BigDecimal("1000.00"),
                new BigDecimal("1200.00")
        );
        // ((1200 - 1000) / 1000) * 100 = 20%
        assertThat(result).isEqualByComparingTo(new BigDecimal("20.00"));
    }

    @Test
    @DisplayName("Başlangıç bakiyesi sıfırsa getiri sıfır dönmeli")
    void calculatePortfolioReturn_ZeroInitial_ReturnsZero() {
        BigDecimal result = calculationService.calculatePortfolioReturn(
                BigDecimal.ZERO,
                new BigDecimal("1200.00")
        );
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
