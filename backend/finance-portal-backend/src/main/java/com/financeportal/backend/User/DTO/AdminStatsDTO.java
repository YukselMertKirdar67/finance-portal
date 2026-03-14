package com.financeportal.backend.User.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStatsDTO {


    private Long totalUsers;
    private Long activeUsers;
    private Long disabledUsers;


    private Long totalPortfolios;
    private Long activePortfolios;
    private BigDecimal totalPortfolioValue;


    private Long totalTransactions;
    private Long buyTransactions;
    private Long sellTransactions;


    private Long totalWatchlistItems;
}
