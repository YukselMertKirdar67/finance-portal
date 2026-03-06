package com.financeportal.backend.Portfolio.Entity;

import com.financeportal.backend.Portfolio.Enum.PortfolioType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "portfolios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId; //Mock user

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "portfolio_type", nullable = false, length = 20)
    private PortfolioType portfolioType;

    @Column(name = "initial_balance", precision = 18, scale = 2, nullable = false)
    private BigDecimal initialBalance;

    @Column(name = "currency", length = 10, nullable = false)
    private String currency; // TRY, USD, EUR

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PortfolioHolding> holdings = new ArrayList<>();

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PortfolioTransaction> transactions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public void addHolding(PortfolioHolding holding) {
        holdings.add(holding);
        holding.setPortfolio(this);
    }

    public void removeHolding(PortfolioHolding holding) {
        holdings.remove(holding);
        holding.setPortfolio(null);
    }

    public void addTransaction(PortfolioTransaction transaction) {
        transactions.add(transaction);
        transaction.setPortfolio(this);
    }
}

