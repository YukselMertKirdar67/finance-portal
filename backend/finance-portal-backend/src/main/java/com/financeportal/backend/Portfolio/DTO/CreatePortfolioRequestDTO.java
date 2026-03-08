package com.financeportal.backend.Portfolio.DTO;

import com.financeportal.backend.Portfolio.Enum.PortfolioType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePortfolioRequestDTO {

    @NotBlank(message = "Portfolio name is required")
    @Size(min = 3, max = 100, message = "Portfolio name must be between 3 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Portfolio type is required")
    private PortfolioType portfolioType;

    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "0.00", message = "Initial balance must be greater than or equal to 0")
    @Digits(integer = 16, fraction = 2, message = "Invalid balance format")
    private BigDecimal initialBalance;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "TRY|USD|EUR|GBP", message = "Currency must be TRY, USD, EUR, or GBP")
    private String currency;
}