package com.financeportal.backend.Portfolio.Service;

import com.financeportal.backend.Portfolio.DTO.CreateTransactionRequestDTO;
import com.financeportal.backend.Portfolio.DTO.TransactionDTO;
import com.financeportal.backend.Portfolio.DTO.TransactionSummaryDTO;
import com.financeportal.backend.Portfolio.Enum.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PortfolioTransactionService {

    TransactionDTO createTransaction(Long portfolioId, CreateTransactionRequestDTO request);

    TransactionDTO createBuyTransaction(Long portfolioId, CreateTransactionRequestDTO request);

    TransactionDTO createSellTransaction(Long portfolioId, CreateTransactionRequestDTO request);

    TransactionDTO getTransactionById(Long transactionId);

    List<TransactionDTO> getTransactionHistory(Long portfolioId);

    Page<TransactionDTO> getTransactionHistory(Long portfolioId, Pageable pageable);

    List<TransactionDTO> getTransactionsByType(Long portfolioId, TransactionType transactionType);

    List<TransactionDTO> getTransactionsByInstrument(Long portfolioId, Long instrumentId);

    List<TransactionDTO> getTransactionsByDateRange(
            Long portfolioId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    List<TransactionDTO> getRecentTransactions(Long portfolioId, int days);

    TransactionSummaryDTO getTransactionSummary(Long portfolioId);

    BigDecimal calculateTotalBuyAmount(Long portfolioId);

    BigDecimal calculateTotalSellAmount(Long portfolioId);

    BigDecimal calculateTotalCommission(Long portfolioId);

    BigDecimal calculateTotalTax(Long portfolioId);

    BigDecimal calculateRealizedPnL(Long portfolioId);

    void deleteTransaction(Long transactionId);

    long countTransactions(Long portfolioId);

    long countBuyTransactions(Long portfolioId);

    long countSellTransactions(Long portfolioId);
}
