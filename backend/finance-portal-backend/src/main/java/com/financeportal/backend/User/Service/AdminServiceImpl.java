package com.financeportal.backend.User.Service;


import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.Portfolio.DTO.PortfolioDTO;
import com.financeportal.backend.Portfolio.Entity.Portfolio;
import com.financeportal.backend.Portfolio.Enum.TransactionType;
import com.financeportal.backend.Portfolio.Mapper.PortfolioMapper;
import com.financeportal.backend.Portfolio.Repository.PortfolioRepository;
import com.financeportal.backend.Portfolio.Repository.PortfolioTransactionRepository;
import com.financeportal.backend.User.DTO.AdminStatsDTO;
import com.financeportal.backend.User.Entity.User;
import com.financeportal.backend.User.UserMapper;
import com.financeportal.backend.User.Repository.UserRepository;
import com.financeportal.backend.User.DTO.UserResponseDTO;
import com.financeportal.backend.Watchlist.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioMapper portfolioMapper;
    private final PortfolioTransactionRepository transactionRepository;
    private final WatchlistRepository watchlistRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        log.info("Admin: Fetching all users");

        return userRepository.findAll()
                .stream()
                .map(userMapper::toUserResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void disableUser(Long userId) {
        log.info("Admin: Disabling user with id: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with id: " + userId));

        user.setEnabled(false);
        userRepository.save(user);

        log.info("Admin: User disabled successfully: {}", userId);
    }

    @Override
    @Transactional
    public void enableUser(Long userId) {
        log.info("Admin: Enabling user with id: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with id: " + userId));

        user.setEnabled(true);
        userRepository.save(user);

        log.info("Admin: User enabled successfully: {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> searchUsers(String query) {
        log.info("Admin: Searching users with query: {}", query);

        return userRepository.findAll()
                .stream()
                .filter(user ->
                        user.getUsername().toLowerCase().contains(query.toLowerCase()) ||
                                user.getEmail().toLowerCase().contains(query.toLowerCase())
                )
                .map(userMapper::toUserResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminStatsDTO getAdminStats() {
        log.info("Admin: Fetching admin dashboard statistics");

        // Kullanıcı istatistikleri
        Long totalUsers = userRepository.count();
        Long activeUsers = userRepository.findAll().stream()
                .filter(User::isEnabled)
                .count();
        Long disabledUsers = totalUsers - activeUsers;

        // Portföy istatistikleri
        Long totalPortfolios = portfolioRepository.count();
        Long activePortfolios = portfolioRepository.findAll().stream()
                .filter(Portfolio::isActive)
                .count();

        // Toplam portföy değeri (basit hesaplama - initial balance toplamı)
        BigDecimal totalPortfolioValue = portfolioRepository.findAll().stream()
                .map(Portfolio::getInitialBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // İşlem istatistikleri
        Long totalTransactions = transactionRepository.count();
        Long buyTransactions = transactionRepository.findAll().stream()
                .filter(tx -> tx.getTransactionType() == TransactionType.BUY)
                .count();
        Long sellTransactions = totalTransactions - buyTransactions;

        // Watchlist istatistikleri
        Long totalWatchlistItems = watchlistRepository.count();

        return AdminStatsDTO.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .disabledUsers(disabledUsers)
                .totalPortfolios(totalPortfolios)
                .activePortfolios(activePortfolios)
                .totalPortfolioValue(totalPortfolioValue)
                .totalTransactions(totalTransactions)
                .buyTransactions(buyTransactions)
                .sellTransactions(sellTransactions)
                .totalWatchlistItems(totalWatchlistItems)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioDTO> getAllPortfolios() {
        log.info("Admin: Fetching all portfolios (all users)");

        return portfolioRepository.findAll()
                .stream()
                .map(portfolioMapper::toDTO)
                .collect(Collectors.toList());
    }
}
