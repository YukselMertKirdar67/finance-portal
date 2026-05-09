package com.financeportal.backend.Portfolio.Service;

import com.financeportal.backend.Portfolio.DTO.AssetAllocationDTO;
import com.financeportal.backend.Portfolio.DTO.HoldingDTO;

import java.math.BigDecimal;
import java.util.List;

public interface PortfolioHoldingService {

    List<HoldingDTO> getHoldingsByPortfolioId(Long portfolioId);

    HoldingDTO getHoldingById(Long holdingId);

    HoldingDTO getHoldingByPortfolioAndInstrument(Long portfolioId, Long instrumentId);

    List<HoldingDTO> getActiveHoldings(Long portfolioId);

    List<HoldingDTO> getTopHoldingsByValue(Long portfolioId, int limit);

    List<AssetAllocationDTO> getAssetAllocation(Long portfolioId);

    BigDecimal calculateTotalInvestment(Long portfolioId);

    BigDecimal calculateCurrentValue(Long portfolioId);

    BigDecimal calculateUnrealizedPnL(Long portfolioId);

    BigDecimal calculateHoldingUnrealizedPnL(Long holdingId);

    boolean holdingExists(Long portfolioId, Long instrumentId);

    void deleteHolding(Long holdingId);

    int deleteZeroQuantityHoldings(Long portfolioId);

    List<HoldingDTO> getHoldingsByPortfolioId(Long portfolioId, String portfolioCurrency);
    BigDecimal calculateTotalInvestment(Long portfolioId, String portfolioCurrency);
    BigDecimal calculateCurrentValue(Long portfolioId, String portfolioCurrency);
    List<AssetAllocationDTO> getAssetAllocation(Long portfolioId, String portfolioCurrency);
}
