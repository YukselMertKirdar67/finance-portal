package com.financeportal.backend.Instrument.Repository;

import com.financeportal.backend.Instrument.Entity.*;
import com.financeportal.backend.Instrument.Enum.InstrumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InstrumentRepository extends JpaRepository<BaseInstrument, Long> {

    Optional<BaseInstrument> findBySymbol(String symbol);

    List<BaseInstrument> findByActiveTrue();

    Page<BaseInstrument> findByActiveTrue(Pageable pageable);

    @Query("SELECT COUNT(i) FROM BaseInstrument i WHERE TYPE(i) = :type AND i.active = true")
    long countByType(@Param("type") Class<? extends BaseInstrument> type);

    @Query("SELECT i FROM BaseInstrument i WHERE " +
            "LOWER(i.symbol) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<BaseInstrument> searchInstruments(@Param("search") String search, Pageable pageable);

    boolean existsBySymbol(String symbol);

    //Tip bazlı sorgular
    @Query("SELECT i FROM StockInstrument i WHERE i.active = true")
    Page<BaseInstrument> findAllStocks(Pageable pageable);

    @Query("SELECT i FROM ForexInstrument i WHERE i.active = true")
    Page<BaseInstrument> findAllForex(Pageable pageable);

    @Query("SELECT i FROM CryptoInstrument i WHERE i.active = true")
    Page<BaseInstrument> findAllCryptos(Pageable pageable);

    @Query("SELECT i FROM BondInstrument i WHERE i.active = true")
    Page<BaseInstrument> findAllBonds(Pageable pageable);

    @Query("SELECT i FROM EurobondInstrument i WHERE i.active = true")
    Page<BaseInstrument> findAllEurobonds(Pageable pageable);

    @Query("SELECT i FROM PreciousInstrument i WHERE i.active = true")
    Page<BaseInstrument> findAllPrecious(Pageable pageable);
}
