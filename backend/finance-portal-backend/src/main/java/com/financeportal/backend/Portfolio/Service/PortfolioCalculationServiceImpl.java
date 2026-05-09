package com.financeportal.backend.Portfolio.Service;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Log4j2
public class PortfolioCalculationServiceImpl implements PortfolioCalculationService {

    private static final int PRICE_SCALE = 6;       // 6 decimal places for prices
    private static final int AMOUNT_SCALE = 2;      // 2 decimal places for amounts
    private static final int PERCENT_SCALE = 2;     // 2 decimal places for percentages
    private static final int CALCULATION_SCALE = 4; // 4 decimal places for intermediate calculations

    /**
     * Mevcut pozisyona yeni alış eklendikten sonra yeni ortalama alış fiyatını hesaplar.
     * Formül: ((mevcut_miktar × mevcut_ort_fiyat) + (yeni_miktar × yeni_fiyat)) / toplam_miktar
     */
    @Override
    public BigDecimal calculateNewAverageBuyPrice(BigDecimal existingQuantity, BigDecimal existingAvgPrice,
                                                  BigDecimal newQuantity, BigDecimal newPrice) {
        log.debug("Calculating new average buy price. Existing: qty={}, avg={}, New: qty={}, price={}",
                existingQuantity, existingAvgPrice, newQuantity, newPrice);


        if (existingQuantity == null || existingQuantity.compareTo(BigDecimal.ZERO) == 0) {
            return newPrice.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal existingValue = existingQuantity.multiply(existingAvgPrice);
        BigDecimal newValue = newQuantity.multiply(newPrice);
        BigDecimal totalValue = existingValue.add(newValue);
        BigDecimal totalQuantity = existingQuantity.add(newQuantity);

        BigDecimal newAvgPrice = totalValue.divide(totalQuantity, PRICE_SCALE, RoundingMode.HALF_UP);

        log.debug("New average buy price calculated: {}", newAvgPrice);

        return newAvgPrice;
    }

    /**
     * Gerçekleşmemiş kâr/zararı hesaplar.
     * Formül: (güncel_fiyat - ortalama_alış_fiyatı) × miktar
     */
    @Override
    public BigDecimal calculateUnrealizedPnL(BigDecimal quantity, BigDecimal avgBuyPrice, BigDecimal currentPrice) {
        log.debug("Calculating unrealized P&L. Qty: {}, Avg Buy: {}, Current: {}",
                quantity, avgBuyPrice, currentPrice);


        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal priceDiff = currentPrice.subtract(avgBuyPrice);
        BigDecimal unrealizedPnL = priceDiff.multiply(quantity)
                .setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);

        log.debug("Unrealized P&L calculated: {}", unrealizedPnL);

        return unrealizedPnL;
    }

    /**
     * Kâr/zarar yüzdesini hesaplar.
     * Formül: ((güncel_fiyat - ortalama_alış_fiyatı) / ortalama_alış_fiyatı) × 100
     */
    @Override
    public BigDecimal calculatePnLPercent(BigDecimal avgBuyPrice, BigDecimal currentPrice) {
        log.debug("Calculating P&L percent. Avg Buy: {}, Current: {}", avgBuyPrice, currentPrice);


        if (avgBuyPrice == null || avgBuyPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal priceDiff = currentPrice.subtract(avgBuyPrice);
        BigDecimal pnlPercent = priceDiff
                .divide(avgBuyPrice, CALCULATION_SCALE, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(PERCENT_SCALE, RoundingMode.HALF_UP);

        log.debug("P&L percent calculated: {}%", pnlPercent);

        return pnlPercent;
    }

    /**
     * Toplam yatırım tutarını hesaplar.
     * Formül: miktar × ortalama_alış_fiyatı
     */
    @Override
    public BigDecimal calculateTotalInvestment(BigDecimal quantity, BigDecimal avgBuyPrice) {
        log.debug("Calculating total investment. Qty: {}, Avg Buy: {}", quantity, avgBuyPrice);


        if (quantity == null || avgBuyPrice == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalInvestment = quantity.multiply(avgBuyPrice)
                .setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);

        log.debug("Total investment calculated: {}", totalInvestment);

        return totalInvestment;
    }

    /**
     * Güncel portföy değerini hesaplar.
     * Formül: miktar × güncel_fiyat
     */
    @Override
    public BigDecimal calculateCurrentValue(BigDecimal quantity, BigDecimal currentPrice) {
        log.debug("Calculating current value. Qty: {}, Current Price: {}", quantity, currentPrice);


        if (quantity == null || currentPrice == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal currentValue = quantity.multiply(currentPrice)
                .setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);

        log.debug("Current value calculated: {}", currentValue);

        return currentValue;
    }

    /**
     * Komisyon ve vergi dahil net tutarı hesaplar.
     * Formül: toplam_tutar + komisyon + vergi
     */
    @Override
    public BigDecimal calculateNetAmount(BigDecimal totalAmount, BigDecimal commission, BigDecimal tax) {
        log.debug("Calculating net amount. Total: {}, Commission: {}, Tax: {}", totalAmount, commission, tax);


        BigDecimal net = totalAmount != null ? totalAmount : BigDecimal.ZERO;

        if (commission != null) {
            net = net.add(commission);
        }
        if (tax != null) {
            net = net.add(tax);
        }

        BigDecimal netAmount = net.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);

        log.debug("Net amount calculated: {}", netAmount);

        return netAmount;
    }

    /**
     * Satış miktarının geçerli olup olmadığını doğrular.
     * Sıfır veya negatif miktar ve mevcut miktarı aşan satış reddedilir.
     */
    @Override
    public boolean validateSellQuantity(BigDecimal availableQuantity, BigDecimal sellQuantity) {
        log.debug("Validating sell quantity. Available: {}, Sell: {}", availableQuantity, sellQuantity);

        if (availableQuantity == null || sellQuantity == null) {
            return false;
        }

        if (sellQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Sell quantity must be greater than zero");
            return false;
        }

        if (sellQuantity.compareTo(availableQuantity) > 0) {
            log.warn("Sell quantity ({}) exceeds available quantity ({})", sellQuantity, availableQuantity);
            return false;
        }

        log.debug("Sell quantity validation passed");
        return true;
    }

    /**
     * Portföy getirisini yüzde olarak hesaplar.
     * Formül: ((güncel_değer - başlangıç_bakiye) / başlangıç_bakiye) × 100
     */
    @Override
    public BigDecimal calculatePortfolioReturn(BigDecimal initialBalance, BigDecimal currentValue) {
        log.debug("Calculating portfolio return. Initial: {}, Current: {}", initialBalance, currentValue);


        if (initialBalance == null || initialBalance.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal gain = currentValue.subtract(initialBalance);
        BigDecimal returnPercent = gain
                .divide(initialBalance, CALCULATION_SCALE, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(PERCENT_SCALE, RoundingMode.HALF_UP);

        log.debug("Portfolio return calculated: {}%", returnPercent);

        return returnPercent;
    }

    private static class Lot {
        private BigDecimal quantity;
        private BigDecimal buyPrice;
        private LocalDateTime purchaseDate;

        // Constructor, getters, setters...
    }
}
