package com.financeportal.backend.Watchlist;

import com.financeportal.backend.Instrument.Entity.BaseInstrument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    List<Watchlist> findByUserIdOrderByAddedAtDesc(String userId);
    Page<Watchlist> findByUserId(String userId, Pageable pageable);

    Optional<Watchlist> findByUserIdAndInstrument(String userId, BaseInstrument instrument);

    boolean existsByUserIdAndInstrument(String userId, BaseInstrument instrument);

    void deleteByUserIdAndInstrument(String userId, BaseInstrument instrument);

    long countByUserId(String userId);

    void deleteAllByUserId(String userId);
}
