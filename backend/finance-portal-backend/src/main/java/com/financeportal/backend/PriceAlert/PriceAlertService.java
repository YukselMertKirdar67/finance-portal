package com.financeportal.backend.PriceAlert;

import java.util.List;

public interface PriceAlertService {

    PriceAlertDTO createAlert(CreatePriceAlertRequestDTO request);

    List<PriceAlertDTO> getUserAlerts();

    List<PriceAlertDTO> getActiveUserAlerts();

    void deleteAlert(Long alertId);

    void checkAlerts();
}
