package com.financeportal.backend.Portfolio.Mapper;

import com.financeportal.backend.Instrument.Entity.BaseInstrument;
import com.financeportal.backend.Instrument.Enum.InstrumentType;
import com.financeportal.backend.Portfolio.DTO.*;
import com.financeportal.backend.Portfolio.Entity.Portfolio;
import com.financeportal.backend.Portfolio.Entity.PortfolioHolding;
import com.financeportal.backend.Portfolio.Entity.PortfolioTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PortfolioMapper {

    // ===================================================================
    // PORTFOLIO MAPPINGS
    // ===================================================================

    @Mapping(target = "totalValue", ignore = true)
    @Mapping(target = "totalInvested", ignore = true)
    @Mapping(target = "unrealizedPnL", ignore = true)
    @Mapping(target = "pnlPercent", ignore = true)
    @Mapping(target = "holdingCount", ignore = true)
    PortfolioDTO toDTO(Portfolio portfolio);

    @Mapping(target = "holdings", ignore = true)
    @Mapping(target = "totalInvested", ignore = true)
    @Mapping(target = "currentValue", ignore = true)
    @Mapping(target = "unrealizedPnL", ignore = true)
    @Mapping(target = "pnlPercent", ignore = true)
    @Mapping(target = "totalHoldings", ignore = true)
    @Mapping(target = "totalTransactions", ignore = true)
    PortfolioDetailDTO toDetailDTO(Portfolio portfolio);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "holdings", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    Portfolio toEntity(CreatePortfolioRequestDTO request);

    List<PortfolioDTO> toDTOList(List<Portfolio> portfolios);

    default Page<PortfolioDTO> toDTOPage(Page<Portfolio> portfolioPage) {
        List<PortfolioDTO> dtoList = portfolioPage.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(dtoList, portfolioPage.getPageable(), portfolioPage.getTotalElements());
    }

    // ===================================================================
    // HOLDING MAPPINGS
    // ===================================================================

    /**
     * instrument.instrumentType çağrısı (abstract method)
     */
    @Mapping(source = "holding.id", target = "holdingId")
    @Mapping(source = "holding.instrument.id", target = "instrumentId")
    @Mapping(source = "holding.instrument.symbol", target = "instrumentSymbol")
    @Mapping(source = "holding.instrument.name", target = "instrumentName")
    @Mapping(target = "instrumentType", expression = "java(mapInstrumentType(holding.getInstrument()))")
    @Mapping(source = "currentPrice", target = "currentPrice")
    @Mapping(source = "holding.currency", target = "currency")
    @Mapping(source = "holding.exchangeRate", target = "exchangeRate")
    @Mapping(source = "holding.quantity", target = "quantity")
    @Mapping(source = "holding.averageBuyPrice", target = "averageBuyPrice")
    @Mapping(target = "totalInvestment", ignore = true)
    @Mapping(target = "currentValue", ignore = true)
    @Mapping(target = "unrealizedPnL", ignore = true)
    @Mapping(target = "pnlPercent", ignore = true)
    HoldingDTO toHoldingDTO(PortfolioHolding holding, BigDecimal currentPrice);

    default List<HoldingDTO> toHoldingDTOList(List<PortfolioHolding> holdings,
                                              List<BigDecimal> currentPrices) {
        if (holdings == null || holdings.isEmpty()) {
            return List.of();
        }

        List<HoldingDTO> dtoList = new java.util.ArrayList<>();
        for (int i = 0; i < holdings.size(); i++) {
            PortfolioHolding holding = holdings.get(i);
            BigDecimal currentPrice = (currentPrices != null && i < currentPrices.size())
                    ? currentPrices.get(i)
                    : BigDecimal.ZERO;
            dtoList.add(toHoldingDTO(holding, currentPrice));
        }
        return dtoList;
    }

    // ===================================================================
    // TRANSACTION MAPPINGS
    // ===================================================================

    /**
     * instrument.instrumentType çağrısı (abstract method)
     */
    @Mapping(source = "portfolio.id", target = "portfolioId")
    @Mapping(source = "portfolio.name", target = "portfolioName")
    @Mapping(source = "instrument.id", target = "instrumentId")
    @Mapping(source = "instrument.symbol", target = "instrumentSymbol")
    @Mapping(source = "instrument.name", target = "instrumentName")
    @Mapping(target = "instrumentType", expression = "java(mapInstrumentType(transaction.getInstrument()))")
    @Mapping(target = "netAmount", expression = "java(calculateNetAmount(transaction))")
    TransactionDTO toTransactionDTO(PortfolioTransaction transaction);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "portfolio", ignore = true)
    @Mapping(target = "instrument", ignore = true)
    @Mapping(target = "totalAmount", expression = "java(calculateTotalAmount(request))")
    @Mapping(source = "transactionDate", target = "transactionDate")
    @Mapping(target = "createdAt", ignore = true)
    PortfolioTransaction toTransactionEntity(CreateTransactionRequestDTO request);

    List<TransactionDTO> toTransactionDTOList(List<PortfolioTransaction> transactions);

    default Page<TransactionDTO> toTransactionDTOPage(Page<PortfolioTransaction> transactionPage) {
        List<TransactionDTO> dtoList = transactionPage.getContent().stream()
                .map(this::toTransactionDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(dtoList, transactionPage.getPageable(), transactionPage.getTotalElements());
    }

    // ===================================================================
    // HELPER METHODS
    // ===================================================================

    /**
     * Map InstrumentType enum to String
     */
    default String mapInstrumentType(BaseInstrument instrument) {
        if (instrument == null) {
            return null;
        }

        InstrumentType type = instrument.getInstrumentType();
        return type != null ? type.name() : null;
    }

    /**
     * Calculate total amount (quantity × price)
     */
    default BigDecimal calculateTotalAmount(CreateTransactionRequestDTO request) {
        if (request.getQuantity() == null || request.getPrice() == null) {
            return BigDecimal.ZERO;
        }
        return request.getQuantity().multiply(request.getPrice());
    }

    /**
     * Calculate net amount (totalAmount + commission + tax)
     */
    default BigDecimal calculateNetAmount(PortfolioTransaction transaction) {
        BigDecimal net = transaction.getTotalAmount() != null
                ? transaction.getTotalAmount()
                : BigDecimal.ZERO;

        if (transaction.getCommission() != null) {
            net = net.add(transaction.getCommission());
        }
        if (transaction.getTax() != null) {
            net = net.add(transaction.getTax());
        }
        return net;
    }

    /**
     * Set transaction date (if null, use current time)
     */
    default LocalDateTime getTransactionDate(CreateTransactionRequestDTO request) {
        return request.getTransactionDate() != null
                ? request.getTransactionDate()
                : LocalDateTime.now();
    }
}
