package com.financeportal.backend.Instrument;

import com.financeportal.backend.Instrument.DTO.HistoricalPriceDTO;
import com.financeportal.backend.Instrument.DTO.InstrumentResponseDTO;
import com.financeportal.backend.Instrument.DTO.PriceDataDTO;
import com.financeportal.backend.Instrument.Entity.*;
import com.financeportal.backend.Instrument.Enum.InstrumentType;
import com.financeportal.backend.Instrument.Mapper.InstrumentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("InstrumentMapper Unit Testleri")
class InstrumentMapperTest {

    private InstrumentMapper instrumentMapper;

    @BeforeEach
    void setUp() {
        instrumentMapper = new InstrumentMapper();
    }

    // ========== toResponseDTO(BaseInstrument) ==========

    @Test
    @DisplayName("ForexInstrument → InstrumentResponseDTO dönüşümü başarılı olmalı")
    void toResponseDTO_Forex_Success() {
        ForexInstrument forex = ForexInstrument.builder()
                .symbol("USD/TRY")
                .name("Amerikan Doları")
                .currency("TRY")
                .exchange("TCMB")
                .baseCurrency("USD")
                .quoteCurrency("TRY")
                .build();
        forex.setId(1L);
        forex.setActive(true);

        InstrumentResponseDTO result = instrumentMapper.toResponseDTO(forex);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getSymbol()).isEqualTo("USD/TRY");
        assertThat(result.getName()).isEqualTo("Amerikan Doları");
        assertThat(result.getType()).isEqualTo(InstrumentType.FOREX);
        assertThat(result.getCurrency()).isEqualTo("TRY");
        assertThat(result.getExchange()).isEqualTo("TCMB");
        assertThat(result.isActive()).isTrue();
        assertThat(result.getBaseCurrency()).isEqualTo("USD");
        assertThat(result.getQuoteCurrency()).isEqualTo("TRY");
    }

    @Test
    @DisplayName("StockInstrument → InstrumentResponseDTO dönüşümü başarılı olmalı")
    void toResponseDTO_Stock_Success() {
        StockInstrument stock = StockInstrument.builder()
                .symbol("AAPL")
                .name("Apple Inc.")
                .currency("USD")
                .exchange("NASDAQ")
                .sector("Teknoloji")
                .marketCap(new BigDecimal("3000000000000"))
                .build();
        stock.setId(2L);
        stock.setActive(true);

        InstrumentResponseDTO result = instrumentMapper.toResponseDTO(stock);

        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo("AAPL");
        assertThat(result.getType()).isEqualTo(InstrumentType.STOCK);
        assertThat(result.getSector()).isEqualTo("Teknoloji");
        assertThat(result.getMarketCap()).isEqualByComparingTo(new BigDecimal("3000000000000"));
    }

    @Test
    @DisplayName("CryptoInstrument → InstrumentResponseDTO dönüşümü başarılı olmalı")
    void toResponseDTO_Crypto_Success() {
        CryptoInstrument crypto = CryptoInstrument.builder()
                .symbol("BTC-USD")
                .name("Bitcoin")
                .currency("USD")
                .exchange("CRYPTO")
                .blockchain("Bitcoin")
                .totalSupply(new BigDecimal("21000000"))
                .circulatingSupply(new BigDecimal("19700000"))
                .build();
        crypto.setId(3L);

        InstrumentResponseDTO result = instrumentMapper.toResponseDTO(crypto);

        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo("BTC-USD");
        assertThat(result.getType()).isEqualTo(InstrumentType.CRYPTO);
        assertThat(result.getBlockchain()).isEqualTo("Bitcoin");
        assertThat(result.getTotalSupply()).isEqualByComparingTo(new BigDecimal("21000000"));
        assertThat(result.getCirculatingSupply()).isEqualByComparingTo(new BigDecimal("19700000"));
    }

    @Test
    @DisplayName("BondInstrument → InstrumentResponseDTO dönüşümü başarılı olmalı")
    void toResponseDTO_Bond_Success() {
        BondInstrument bond = BondInstrument.builder()
                .symbol("US-10Y-BOND")
                .name("ABD 10 Yıllık Tahvil")
                .currency("USD")
                .exchange("CBOE")
                .issuer("US Treasury")
                .faceValue(new BigDecimal("1000"))
                .maturityDate(LocalDate.of(2034, 5, 15))
                .couponRate(new BigDecimal("4.25"))
                .build();
        bond.setId(4L);

        InstrumentResponseDTO result = instrumentMapper.toResponseDTO(bond);

        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo("US-10Y-BOND");
        assertThat(result.getType()).isEqualTo(InstrumentType.BOND);
        assertThat(result.getIssuer()).isEqualTo("US Treasury");
        assertThat(result.getFaceValue()).isEqualByComparingTo(new BigDecimal("1000"));
        assertThat(result.getMaturityDate()).isEqualTo(LocalDate.of(2034, 5, 15));
        assertThat(result.getCouponRate()).isEqualByComparingTo(new BigDecimal("4.25"));
    }

    @Test
    @DisplayName("PreciousInstrument → InstrumentResponseDTO dönüşümü başarılı olmalı")
    void toResponseDTO_Precious_Success() {
        PreciousInstrument precious = PreciousInstrument.builder()
                .symbol("XAU/USD")
                .name("Altın (Ons)")
                .currency("USD")
                .exchange("COMMODITY")
                .metalType("GOLD")
                .unit("oz")
                .build();
        precious.setId(5L);

        InstrumentResponseDTO result = instrumentMapper.toResponseDTO(precious);

        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo("XAU/USD");
        assertThat(result.getType()).isEqualTo(InstrumentType.PRECIOUS);
        assertThat(result.getMetalType()).isEqualTo("GOLD");
        assertThat(result.getUnit()).isEqualTo("oz");
    }

    @Test
    @DisplayName("FundInstrument → InstrumentResponseDTO dönüşümü başarılı olmalı")
    void toResponseDTO_Fund_Success() {
        FundInstrument fund = FundInstrument.builder()
                .symbol("SPY")
                .name("SPDR S&P 500 ETF")
                .currency("USD")
                .exchange("NYSE")
                .fundCode("SPY")
                .fundType("ETF")
                .build();
        fund.setId(6L);

        InstrumentResponseDTO result = instrumentMapper.toResponseDTO(fund);

        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo("SPY");
        assertThat(result.getType()).isEqualTo(InstrumentType.FUND);
        assertThat(result.getFundCode()).isEqualTo("SPY");
        assertThat(result.getFundType()).isEqualTo("ETF");
    }

    // ========== toResponseDTO(BaseInstrument, InstrumentPrice) ==========

    @Test
    @DisplayName("Enstrüman + Fiyat → InstrumentResponseDTO dönüşümü başarılı olmalı")
    void toResponseDTO_WithPrice_Success() {
        ForexInstrument forex = ForexInstrument.builder()
                .symbol("USD/TRY").name("Amerikan Doları")
                .currency("TRY").exchange("TCMB").build();
        forex.setId(1L);

        InstrumentPrice price = InstrumentPrice.builder()
                .instrument(forex)
                .currentPrice(new BigDecimal("38.50"))
                .openPrice(new BigDecimal("38.00"))
                .highPrice(new BigDecimal("39.00"))
                .lowPrice(new BigDecimal("37.50"))
                .previousClose(new BigDecimal("38.00"))
                .changeAmount(new BigDecimal("0.50"))
                .changePercent(new BigDecimal("1.32"))
                .volume(1000000L)
                .timestamp(LocalDateTime.now())
                .build();

        InstrumentResponseDTO result = instrumentMapper.toResponseDTO(forex, price);

        assertThat(result).isNotNull();
        assertThat(result.getCurrentPrice()).isNotNull();
        assertThat(result.getCurrentPrice().getCurrent())
                .isEqualByComparingTo(new BigDecimal("38.50"));
        assertThat(result.getCurrentPrice().getChangePercent())
                .isEqualByComparingTo(new BigDecimal("1.32"));
    }

    @Test
    @DisplayName("Fiyat null olduğunda currentPrice null dönmeli")
    void toResponseDTO_WithNullPrice_CurrentPriceIsNull() {
        ForexInstrument forex = ForexInstrument.builder()
                .symbol("USD/TRY").name("Amerikan Doları")
                .currency("TRY").exchange("TCMB").build();
        forex.setId(1L);

        InstrumentResponseDTO result = instrumentMapper.toResponseDTO(forex, null);

        assertThat(result).isNotNull();
        assertThat(result.getCurrentPrice()).isNull();
    }

    // ========== toPriceDataDTO ==========

    @Test
    @DisplayName("InstrumentPrice → PriceDataDTO dönüşümü başarılı olmalı")
    void toPriceDataDTO_Success() {
        ForexInstrument forex = ForexInstrument.builder()
                .symbol("USD/TRY").name("Amerikan Doları")
                .currency("TRY").exchange("TCMB").build();

        InstrumentPrice price = InstrumentPrice.builder()
                .instrument(forex)
                .currentPrice(new BigDecimal("38.50"))
                .openPrice(new BigDecimal("38.00"))
                .highPrice(new BigDecimal("39.00"))
                .lowPrice(new BigDecimal("37.50"))
                .previousClose(new BigDecimal("38.00"))
                .changeAmount(new BigDecimal("0.50"))
                .changePercent(new BigDecimal("1.32"))
                .volume(1000000L)
                .yieldRate(null)
                .timestamp(LocalDateTime.now())
                .build();

        PriceDataDTO result = instrumentMapper.toPriceDataDTO(price);

        assertThat(result).isNotNull();
        assertThat(result.getCurrent()).isEqualByComparingTo(new BigDecimal("38.50"));
        assertThat(result.getOpen()).isEqualByComparingTo(new BigDecimal("38.00"));
        assertThat(result.getHigh()).isEqualByComparingTo(new BigDecimal("39.00"));
        assertThat(result.getLow()).isEqualByComparingTo(new BigDecimal("37.50"));
        assertThat(result.getPreviousClose()).isEqualByComparingTo(new BigDecimal("38.00"));
        assertThat(result.getChangeAmount()).isEqualByComparingTo(new BigDecimal("0.50"));
        assertThat(result.getChangePercent()).isEqualByComparingTo(new BigDecimal("1.32"));
        assertThat(result.getVolume()).isEqualTo(1000000L);
        assertThat(result.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Null price için null dönmeli")
    void toPriceDataDTO_NullPrice_ReturnsNull() {
        PriceDataDTO result = instrumentMapper.toPriceDataDTO(null);
        assertThat(result).isNull();
    }

    // ========== toHistoricalPriceDTO ==========

    @Test
    @DisplayName("PriceHistory → HistoricalPriceDTO dönüşümü başarılı olmalı")
    void toHistoricalPriceDTO_Success() {
        ForexInstrument forex = ForexInstrument.builder()
                .symbol("USD/TRY").name("Amerikan Doları")
                .currency("TRY").exchange("TCMB").build();

        PriceHistory history = PriceHistory.builder()
                .instrument(forex)
                .date(LocalDate.of(2024, 1, 15))
                .open(new BigDecimal("38.00"))
                .high(new BigDecimal("39.00"))
                .low(new BigDecimal("37.50"))
                .close(new BigDecimal("38.50"))
                .volume(1000000L)
                .yieldRate(null)
                .build();

        HistoricalPriceDTO result = instrumentMapper.toHistoricalPriceDTO(history);

        assertThat(result).isNotNull();
        assertThat(result.getDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(result.getOpen()).isEqualByComparingTo(new BigDecimal("38.00"));
        assertThat(result.getHigh()).isEqualByComparingTo(new BigDecimal("39.00"));
        assertThat(result.getLow()).isEqualByComparingTo(new BigDecimal("37.50"));
        assertThat(result.getClose()).isEqualByComparingTo(new BigDecimal("38.50"));
        assertThat(result.getVolume()).isEqualTo(1000000L);
    }

    @Test
    @DisplayName("Tahvil için yieldRate alanı dönmeli")
    void toHistoricalPriceDTO_WithYieldRate_Success() {
        BondInstrument bond = BondInstrument.builder()
                .symbol("US-10Y-BOND").name("ABD 10 Yıllık Tahvil")
                .currency("USD").exchange("CBOE").issuer("US Treasury").build();

        PriceHistory history = PriceHistory.builder()
                .instrument(bond)
                .date(LocalDate.of(2024, 1, 15))
                .open(new BigDecimal("4.25"))
                .high(new BigDecimal("4.30"))
                .low(new BigDecimal("4.20"))
                .close(new BigDecimal("4.27"))
                .yieldRate(new BigDecimal("4.27"))
                .build();

        HistoricalPriceDTO result = instrumentMapper.toHistoricalPriceDTO(history);

        assertThat(result).isNotNull();
        assertThat(result.getYieldRate()).isEqualByComparingTo(new BigDecimal("4.27"));
    }
}
