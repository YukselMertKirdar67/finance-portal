package com.financeportal.backend.Portfolio.DTO;


import com.financeportal.backend.Portfolio.Enum.TransactionType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransactionRequestDTO {

    @NotNull(message = "Instrument ID is required")
    @Positive(message = "Instrument ID must be positive")
    private Long instrumentId;

    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType; // BUY, SELL

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.00000001", message = "Quantity must be greater than 0")
    @Digits(integer = 16, fraction = 8, message = "Invalid quantity format")
    private BigDecimal quantity;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 16, fraction = 6, message = "Invalid price format")
    private BigDecimal price;

    @DecimalMin(value = "0.00", message = "Commission cannot be negative")
    @Digits(integer = 16, fraction = 2, message = "Invalid commission format")
    private BigDecimal commission;

    @DecimalMin(value = "0.00", message = "Tax cannot be negative")
    @Digits(integer = 16, fraction = 2, message = "Invalid tax format")
    private BigDecimal tax;

    private LocalDateTime transactionDate; // Opsiyonel, null ise şimdiki zaman kullanılır

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;
}
