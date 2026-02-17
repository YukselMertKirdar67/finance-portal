package com.financeportal.backend.Instrument.Repository;

import com.financeportal.backend.Instrument.Entity.BaseInstrument;
import com.financeportal.backend.Instrument.Entity.InstrumentPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InstrumentPriceRepository extends JpaRepository<InstrumentPrice, Long> {

    Optional<InstrumentPrice> findTopByInstrumentOrderByTimestampDesc(BaseInstrument instrument);

    List<InstrumentPrice> findByInstrumentAndTimestampAfter(
            BaseInstrument instrument, LocalDateTime after
    );

    @Query("SELECT ip FROM InstrumentPrice ip WHERE ip.instrument.id IN :ids " +
            "AND ip.timestamp IN (SELECT MAX(ip2.timestamp) FROM InstrumentPrice ip2 " +
            "WHERE ip2.instrument.id = ip.instrument.id)")
    List<InstrumentPrice> findLatestPricesByInstrumentIds(List<Long> ids);
}