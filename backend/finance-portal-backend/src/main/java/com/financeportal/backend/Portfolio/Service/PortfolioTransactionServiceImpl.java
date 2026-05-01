package com.financeportal.backend.Portfolio.Service;

import com.financeportal.backend.Exception.BusinessRuleException;
import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.Instrument.Entity.BaseInstrument;
import com.financeportal.backend.Instrument.Repository.InstrumentRepository;
import com.financeportal.backend.Instrument.Service.TcmbService;
import com.financeportal.backend.Notification.NotificationService;
import com.financeportal.backend.Portfolio.DTO.CreateTransactionRequestDTO;
import com.financeportal.backend.Portfolio.DTO.TransactionDTO;
import com.financeportal.backend.Portfolio.DTO.TransactionSummaryDTO;
import com.financeportal.backend.Portfolio.Entity.Portfolio;
import com.financeportal.backend.Portfolio.Entity.PortfolioHolding;
import com.financeportal.backend.Portfolio.Entity.PortfolioTransaction;
import com.financeportal.backend.Portfolio.Enum.TransactionType;
import com.financeportal.backend.Portfolio.Mapper.PortfolioMapper;
import com.financeportal.backend.Portfolio.Repository.PortfolioHoldingRepository;
import com.financeportal.backend.Portfolio.Repository.PortfolioRepository;
import com.financeportal.backend.Portfolio.Repository.PortfolioTransactionRepository;
import com.financeportal.backend.Util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioTransactionServiceImpl implements PortfolioTransactionService {

    private final PortfolioTransactionRepository transactionRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioHoldingRepository holdingRepository;
    private final InstrumentRepository instrumentRepository;
    private final PortfolioCalculationService calculationService;
    private final PortfolioMapper portfolioMapper;
    private final TcmbService tcmbService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public TransactionDTO createTransaction(Long portfolioId, CreateTransactionRequestDTO request) {
        log.info("Creating {} transaction for portfolio ID: {}, instrument: {}, quantity: {}",
                request.getTransactionType(), portfolioId, request.getInstrumentId(), request.getQuantity());

        if (request.getTransactionType() == TransactionType.BUY) {
            return createBuyTransaction(portfolioId, request);
        } else if (request.getTransactionType() == TransactionType.SELL) {
            return createSellTransaction(portfolioId, request);
        } else {
            throw new IllegalArgumentException("Invalid transaction type: " + request.getTransactionType());
        }
    }

    @Override
    @Transactional
    public TransactionDTO createBuyTransaction(Long portfolioId, CreateTransactionRequestDTO request) {
        log.info("Processing BUY transaction for portfolio: {}, instrument: {}", portfolioId, request.getInstrumentId());

        // 1. Validate portfolio ownership
        Portfolio portfolio = getPortfolioWithOwnershipCheck(portfolioId);

        // 2. Get instrument
        BaseInstrument instrument = instrumentRepository.findById(request.getInstrumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Instrument not found with id: " + request.getInstrumentId()));

        // 3. Döviz kuru al
        String currency = instrument.getCurrency();
        BigDecimal exchangeRate = tcmbService.getExchangeRate(currency);

        // 4. Create transaction entity
        PortfolioTransaction transaction = portfolioMapper.toTransactionEntity(request);
        transaction.setPortfolio(portfolio);
        transaction.setInstrument(instrument);
        transaction.setCurrency(currency);
        transaction.setExchangeRate(exchangeRate);

        if (request.getTransactionDate() == null) {
            transaction.setTransactionDate(LocalDateTime.now());
        }

        // 5. Save transaction
        PortfolioTransaction savedTransaction = transactionRepository.save(transaction);
        log.info("BUY transaction saved with ID: {}", savedTransaction.getId());

        // 6. Update or create holding
        updateHoldingForBuy(portfolio, instrument, request.getQuantity(), request.getPrice(),
                transaction.getTransactionDate(), currency, exchangeRate);

        // 7. İşlem bildirimi
        notificationService.notifyTransaction(
                portfolio.getUserId(),
                instrument.getSymbol(),
                "BUY",
                request.getQuantity().doubleValue(),
                portfolioId
        );

        // 8. Map to DTO and return
        return portfolioMapper.toTransactionDTO(savedTransaction);
    }

    @Override
    @Transactional
    public TransactionDTO createSellTransaction(Long portfolioId, CreateTransactionRequestDTO request) {
        log.info("Processing SELL transaction for portfolio: {}, instrument: {}", portfolioId, request.getInstrumentId());

        // 1. Validate portfolio ownership
        Portfolio portfolio = getPortfolioWithOwnershipCheck(portfolioId);

        // 2. Get instrument
        BaseInstrument instrument = instrumentRepository.findById(request.getInstrumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Instrument not found with id: " + request.getInstrumentId()));

        // 3. Check if holding exists
        PortfolioHolding holding = holdingRepository.findByPortfolioIdAndInstrumentId(portfolioId, request.getInstrumentId())
                .orElseThrow(() -> new BusinessRuleException("You don't own this instrument in this portfolio"));

        // 4. Validate sell quantity
        if (!calculationService.validateSellQuantity(holding.getQuantity(), request.getQuantity())) {
            throw new BusinessRuleException(String.format(
                    "Insufficient quantity. Available: %s, Requested: %s",
                    holding.getQuantity(), request.getQuantity()
            ));
        }

        // 5. Döviz kuru al
        String currency = instrument.getCurrency();
        BigDecimal exchangeRate = tcmbService.getExchangeRate(currency);

        // 6. Create transaction entity
        PortfolioTransaction transaction = portfolioMapper.toTransactionEntity(request);
        transaction.setPortfolio(portfolio);
        transaction.setInstrument(instrument);
        transaction.setCurrency(currency);
        transaction.setExchangeRate(exchangeRate);

        if (request.getTransactionDate() == null) {
            transaction.setTransactionDate(LocalDateTime.now());
        }

        // 7. Save transaction
        PortfolioTransaction savedTransaction = transactionRepository.save(transaction);
        log.info("SELL transaction saved with ID: {}", savedTransaction.getId());

        // 8. Update holding (reduce quantity)
        updateHoldingForSell(holding, request.getQuantity());


        //  9. İşlem bildirimi
        notificationService.notifyTransaction(
                portfolio.getUserId(),
                instrument.getSymbol(),
                "SELL",
                request.getQuantity().doubleValue(),
                portfolioId
        );

        // 10. Map to DTO and return
        return portfolioMapper.toTransactionDTO(savedTransaction);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionDTO getTransactionById(Long transactionId) {
        log.info("Fetching transaction by ID: {}", transactionId);

        PortfolioTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        return portfolioMapper.toTransactionDTO(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionHistory(Long portfolioId) {
        getPortfolioWithOwnershipCheck(portfolioId);

        List<PortfolioTransaction> transactions = transactionRepository
                .findByPortfolioIdAndDeletedFalseOrderByTransactionDateDesc(portfolioId);

        return transactions.stream()
                .map(portfolioMapper::toTransactionDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionDTO> getTransactionHistory(Long portfolioId, Pageable pageable) {
        getPortfolioWithOwnershipCheck(portfolioId);

        Page<PortfolioTransaction> transactionPage = transactionRepository
                .findByPortfolioIdAndDeletedFalseOrderByTransactionDateDesc(portfolioId, pageable);

        return portfolioMapper.toTransactionDTOPage(transactionPage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsByType(Long portfolioId, TransactionType transactionType) {
        log.info("Fetching {} transactions for portfolio ID: {}", transactionType, portfolioId);

        getPortfolioWithOwnershipCheck(portfolioId);

        List<PortfolioTransaction> transactions = transactionRepository
                .findByPortfolioIdAndTransactionTypeAndDeletedFalse(portfolioId, transactionType);

        return transactions.stream()
                .map(portfolioMapper::toTransactionDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsByInstrument(Long portfolioId, Long instrumentId) {
        log.info("Fetching transactions for portfolio: {} and instrument: {}", portfolioId, instrumentId);

        getPortfolioWithOwnershipCheck(portfolioId);

        List<PortfolioTransaction> transactions = transactionRepository
                .findByPortfolioIdAndInstrumentIdAndDeletedFalseOrderByTransactionDateDesc(portfolioId, instrumentId);

        return transactions.stream()
                .map(portfolioMapper::toTransactionDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsByDateRange(Long portfolioId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching transactions for portfolio: {} between {} and {}", portfolioId, startDate, endDate);

        getPortfolioWithOwnershipCheck(portfolioId);

        List<PortfolioTransaction> transactions = transactionRepository
                .findByPortfolioIdAndDateBetween(portfolioId, startDate, endDate);

        return transactions.stream()
                .map(portfolioMapper::toTransactionDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDTO> getRecentTransactions(Long portfolioId, int days) {
        log.info("Fetching transactions for last {} days for portfolio ID: {}", days, portfolioId);

        getPortfolioWithOwnershipCheck(portfolioId);

        LocalDateTime sinceDate = LocalDateTime.now().minusDays(days);

        List<PortfolioTransaction> transactions = transactionRepository
                .findRecentTransactions(portfolioId, sinceDate);

        return transactions.stream()
                .map(portfolioMapper::toTransactionDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionSummaryDTO getTransactionSummary(Long portfolioId) {
        log.info("Calculating transaction summary for portfolio ID: {}", portfolioId);

        getPortfolioWithOwnershipCheck(portfolioId);

        long totalTransactions = transactionRepository.countByPortfolioIdAndDeletedFalse(portfolioId);
        long buyTransactions = transactionRepository.countByPortfolioIdAndTransactionTypeAndDeletedFalse(portfolioId, TransactionType.BUY);
        long sellTransactions = transactionRepository.countByPortfolioIdAndTransactionTypeAndDeletedFalse(portfolioId, TransactionType.SELL);

        BigDecimal totalBuyAmount = transactionRepository.sumBuyAmountByPortfolioId(portfolioId);
        BigDecimal totalSellAmount = transactionRepository.sumSellAmountByPortfolioId(portfolioId);
        BigDecimal totalCommission = transactionRepository.sumCommissionByPortfolioId(portfolioId);
        BigDecimal totalTax = transactionRepository.sumTaxByPortfolioId(portfolioId);

        BigDecimal realizedPnL = totalSellAmount.subtract(totalBuyAmount);

        return TransactionSummaryDTO.builder()
                .totalTransactions((int) totalTransactions)
                .buyTransactions((int) buyTransactions)
                .sellTransactions((int) sellTransactions)
                .totalBuyAmount(totalBuyAmount)
                .totalSellAmount(totalSellAmount)
                .totalCommission(totalCommission)
                .totalTax(totalTax)
                .realizedPnL(realizedPnL)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalBuyAmount(Long portfolioId) {
        return transactionRepository.sumBuyAmountByPortfolioId(portfolioId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalSellAmount(Long portfolioId) {
        return transactionRepository.sumSellAmountByPortfolioId(portfolioId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalCommission(Long portfolioId) {
        return transactionRepository.sumCommissionByPortfolioId(portfolioId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalTax(Long portfolioId) {
        return transactionRepository.sumTaxByPortfolioId(portfolioId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateRealizedPnL(Long portfolioId) {
        return transactionRepository.calculateRealizedPnL(portfolioId);
    }

    @Override
    @Transactional
    public void deleteTransaction(Long transactionId) {
        log.warn("Soft deleting transaction ID: {}", transactionId);

        PortfolioTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found with id: " + transactionId
                ));

        getPortfolioWithOwnershipCheck(transaction.getPortfolio().getId());

        transaction.setDeleted(true);
        transactionRepository.save(transaction);

        log.warn("Transaction soft deleted: {}", transactionId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countTransactions(Long portfolioId) {
        return transactionRepository.countByPortfolioIdAndDeletedFalse(portfolioId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countBuyTransactions(Long portfolioId) {
        return transactionRepository.countByPortfolioIdAndTransactionTypeAndDeletedFalse(portfolioId, TransactionType.BUY);
    }

    @Override
    @Transactional(readOnly = true)
    public long countSellTransactions(Long portfolioId) {
        return transactionRepository.countByPortfolioIdAndTransactionTypeAndDeletedFalse(portfolioId, TransactionType.SELL);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private Portfolio getPortfolioWithOwnershipCheck(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + portfolioId));

        String currentUserId = SecurityUtils.getCurrentUserKeycloakId();
        if (!portfolio.getUserId().equals(currentUserId)) {
            throw new BusinessRuleException("You don't have permission to access this portfolio");
        }

        return portfolio;
    }

    private void updateHoldingForBuy(Portfolio portfolio, BaseInstrument instrument,
                                     BigDecimal quantity, BigDecimal price,
                                     LocalDateTime transactionDate,
                                     String currency, BigDecimal exchangeRate) {
        Optional<PortfolioHolding> existingHolding = holdingRepository
                .findByPortfolioIdAndInstrumentId(portfolio.getId(), instrument.getId());

        if (existingHolding.isPresent()) {
            PortfolioHolding holding = existingHolding.get();

            log.info("Updating existing holding ID: {} (current quantity: {}, new quantity: {})",
                    holding.getId(), holding.getQuantity(), quantity);

            BigDecimal newAverageBuyPrice = calculationService.calculateNewAverageBuyPrice(
                    holding.getQuantity(),
                    holding.getAverageBuyPrice(),
                    quantity,
                    price
            );

            holding.setQuantity(holding.getQuantity().add(quantity));
            holding.setAverageBuyPrice(newAverageBuyPrice);
            holding.setLastPurchaseDate(transactionDate);
            holding.setCurrency(currency);
            holding.setExchangeRate(exchangeRate);

            holdingRepository.save(holding);

            log.info("Holding updated: new quantity: {}, new avg price: {}, currency: {}, rate: {}",
                    holding.getQuantity(), holding.getAverageBuyPrice(), currency, exchangeRate);

        } else {
            log.info("Creating new holding for instrument: {} in portfolio: {}", instrument.getSymbol(), portfolio.getId());

            PortfolioHolding newHolding = new PortfolioHolding();
            newHolding.setPortfolio(portfolio);
            newHolding.setInstrument(instrument);
            newHolding.setQuantity(quantity);
            newHolding.setAverageBuyPrice(price);
            newHolding.setFirstPurchaseDate(transactionDate);
            newHolding.setLastPurchaseDate(transactionDate);
            newHolding.setCurrency(currency);
            newHolding.setExchangeRate(exchangeRate);

            holdingRepository.save(newHolding);

            log.info("New holding created with quantity: {}, avg price: {}, currency: {}, rate: {}",
                    quantity, price, currency, exchangeRate);
        }
    }

    private void updateHoldingForSell(PortfolioHolding holding, BigDecimal sellQuantity) {
        log.info("Updating holding ID: {} for SELL (current quantity: {}, sell quantity: {})",
                holding.getId(), holding.getQuantity(), sellQuantity);

        BigDecimal newQuantity = holding.getQuantity().subtract(sellQuantity);

        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            log.info("Quantity became zero, deleting holding ID: {}", holding.getId());
            holdingRepository.delete(holding);
        } else {
            holding.setQuantity(newQuantity);
            holdingRepository.save(holding);
            log.info("Holding updated: new quantity: {}", newQuantity);
        }
    }
}