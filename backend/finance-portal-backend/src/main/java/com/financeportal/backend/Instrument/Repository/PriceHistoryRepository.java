package com.financeportal.backend.Instrument.Repository;

import com.financeportal.backend.Instrument.Entity.BaseInstrument;
import com.financeportal.backend.Instrument.Entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findByInstrumentAndDateBetweenOrderByDateAsc(
            BaseInstrument instrument, LocalDate startDate, LocalDate endDate
    );

    Optional<PriceHistory> findByInstrumentAndDate(BaseInstrument instrument, LocalDate date);

    List<PriceHistory> findByInstrumentOrderByDateDesc(BaseInstrument instrument);
}
