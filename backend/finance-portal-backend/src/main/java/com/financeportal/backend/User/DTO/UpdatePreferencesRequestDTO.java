package com.financeportal.backend.User.DTO;

import jakarta.validation.constraints.Pattern;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePreferencesRequestDTO {

    @Pattern(regexp = "light|dark|system", message = "Theme must be light, dark, or system")
    private String theme;

    private Boolean notifyTransaction;
    private Boolean notifyPortfolioChange;
    private Boolean notifyPriceAlert;
    private Boolean notifyNews;
}
