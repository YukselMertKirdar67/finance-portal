package com.financeportal.backend.Instrument.Service;

import com.financeportal.backend.Instrument.DTO.*;
import com.financeportal.backend.Instrument.Enum.InstrumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface InstrumentService {

    InstrumentResponseDTO getInstrumentById(Long id);

    InstrumentResponseDTO getInstrumentBySymbol(String symbol);

    Page<InstrumentResponseDTO> getAllInstruments(Pageable pageable);

    Page<InstrumentResponseDTO> getInstrumentsByType(InstrumentType type, Pageable pageable);

    Page<InstrumentResponseDTO> searchInstruments(String search, Pageable pageable);

    PriceDataDTO getCurrentPrice(Long instrumentId);

    List<HistoricalPriceDTO> getHistoricalPrices(
            Long instrumentId,
            LocalDate startDate,
            LocalDate endDate
    );
}
