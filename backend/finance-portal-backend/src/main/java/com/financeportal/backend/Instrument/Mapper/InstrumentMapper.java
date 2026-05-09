package com.financeportal.backend.Instrument.Mapper;

import com.financeportal.backend.Instrument.DTO.HistoricalPriceDTO;
import com.financeportal.backend.Instrument.DTO.InstrumentRequestDTO;
import com.financeportal.backend.Instrument.DTO.InstrumentResponseDTO;
import com.financeportal.backend.Instrument.DTO.PriceDataDTO;
import com.financeportal.backend.Instrument.Entity.*;
import org.springframework.stereotype.Component;

@Component
public class InstrumentMapper {

    // Entity → InstrumentResponseDTO
    public InstrumentResponseDTO toResponseDTO(BaseInstrument instrument) {
        InstrumentResponseDTO dto = InstrumentResponseDTO.builder()
                .id(instrument.getId())
                .symbol(instrument.getSymbol())
                .name(instrument.getName())
                .type(instrument.getInstrumentType())
                .exchange(instrument.getExchange())
                .description(instrument.getDescription())
                .currency(instrument.getCurrency())
                .active(instrument.isActive())
                .build();

        // Tip kontrolü ile özel alanları ekle
        if (instrument instanceof StockInstrument stock) {
            dto.setSector(stock.getSector());
            dto.setMarketCap(stock.getMarketCap());
        } else if (instrument instanceof BondInstrument bond) {
            dto.setMaturityDate(bond.getMaturityDate());
            dto.setCouponRate(bond.getCouponRate());
            dto.setFaceValue(bond.getFaceValue());
            dto.setIssuer(bond.getIssuer());
        }
          else if (instrument instanceof ForexInstrument forex) {
            dto.setBaseCurrency(forex.getBaseCurrency());
            dto.setQuoteCurrency(forex.getQuoteCurrency());
        } else if (instrument instanceof CryptoInstrument crypto) {
            dto.setBlockchain(crypto.getBlockchain());
            dto.setTotalSupply(crypto.getTotalSupply());
            dto.setCirculatingSupply(crypto.getCirculatingSupply());
        } else if (instrument instanceof PreciousInstrument precious) {
            dto.setMetalType(precious.getMetalType());
            dto.setUnit(precious.getUnit());
        }
        else if (instrument instanceof FundInstrument fund) {
            dto.setFundCode(fund.getFundCode());
            dto.setFundType(fund.getFundType());
            dto.setUmbrella(fund.getUmbrella());
            dto.setTotalValue(fund.getTotalValue());
            dto.setInvestorCount(fund.getInvestorCount());
        }

        return dto;
    }

    // Entity + Price → InstrumentResponseDTO (with current price)
    public InstrumentResponseDTO toResponseDTO(BaseInstrument instrument, InstrumentPrice price) {
        InstrumentResponseDTO dto = toResponseDTO(instrument);
        if (price != null) {
            dto.setCurrentPrice(toPriceDataDTO(price));
        }
        return dto;
    }

    // InstrumentRequestDTO → Entity
    public BaseInstrument toEntity(InstrumentRequestDTO dto) {
        // Tip bazlı entity oluştur
        return switch (dto.getType()) {
            case STOCK -> StockInstrument.builder()
                    .symbol(dto.getSymbol())
                    .name(dto.getName())
                    .exchange(dto.getExchange())
                    .description(dto.getDescription())
                    .currency(dto.getCurrency())
                    .sector(dto.getSector())
                    .marketCap(dto.getMarketCap())
                    .active(true)
                    .build();

            case BOND -> BondInstrument.builder()
                    .symbol(dto.getSymbol())
                    .name(dto.getName())
                    .exchange(dto.getExchange())
                    .description(dto.getDescription())
                    .currency(dto.getCurrency())
                    .maturityDate(dto.getMaturityDate())
                    .couponRate(dto.getCouponRate())
                    .faceValue(dto.getFaceValue())
                    .issuer(dto.getIssuer())
                    .active(true)
                    .build();

            case FOREX -> ForexInstrument.builder()
                    .symbol(dto.getSymbol())
                    .name(dto.getName())
                    .exchange(dto.getExchange())
                    .description(dto.getDescription())
                    .currency(dto.getCurrency())
                    .baseCurrency(dto.getBaseCurrency())
                    .quoteCurrency(dto.getQuoteCurrency())
                    .active(true)
                    .build();

            case CRYPTO -> CryptoInstrument.builder()
                    .symbol(dto.getSymbol())
                    .name(dto.getName())
                    .exchange(dto.getExchange())
                    .description(dto.getDescription())
                    .currency(dto.getCurrency())
                    .blockchain(dto.getBlockchain())
                    .totalSupply(dto.getTotalSupply())
                    .circulatingSupply(dto.getCirculatingSupply())
                    .active(true)
                    .build();

            case PRECIOUS -> PreciousInstrument.builder()
                    .symbol(dto.getSymbol())
                    .name(dto.getName())
                    .exchange(dto.getExchange())
                    .description(dto.getDescription())
                    .currency(dto.getCurrency())
                    .metalType(dto.getMetalType())
                    .unit(dto.getUnit())
                    .active(true)
                    .build();

            case FUND -> FundInstrument.builder()
                    .symbol(dto.getSymbol())
                    .name(dto.getName())
                    .exchange(dto.getExchange())
                    .description(dto.getDescription())
                    .currency(dto.getCurrency())
                    .fundCode(dto.getFundCode())
                    .fundType(dto.getFundType())
                    .umbrella(dto.getUmbrella())
                    .totalValue(dto.getTotalValue())
                    .investorCount(dto.getInvestorCount())
                    .active(true)
                    .build();
        };
    }

    // InstrumentPrice → PriceDataDTO
    public PriceDataDTO toPriceDataDTO(InstrumentPrice price) {
        if (price == null) return null;

        return PriceDataDTO.builder()
                .current(price.getCurrentPrice())
                .open(price.getOpenPrice())
                .high(price.getHighPrice())
                .low(price.getLowPrice())
                .previousClose(price.getPreviousClose())
                .changeAmount(price.getChangeAmount())
                .changePercent(price.getChangePercent())
                .volume(price.getVolume())
                .yieldRate(price.getYieldRate())
                .timestamp(price.getTimestamp())
                .build();
    }

    // PriceHistory → HistoricalPriceDTO
    public HistoricalPriceDTO toHistoricalPriceDTO(PriceHistory history) {
        return HistoricalPriceDTO.builder()
                .date(history.getDate())
                .open(history.getOpen())
                .high(history.getHigh())
                .low(history.getLow())
                .close(history.getClose())
                .volume(history.getVolume())
                .yieldRate(history.getYieldRate())
                .build();
    }
}