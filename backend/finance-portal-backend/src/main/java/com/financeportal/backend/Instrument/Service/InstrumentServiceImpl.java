package com.financeportal.backend.Instrument.Service;

import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.Instrument.DTO.*;
import com.financeportal.backend.Instrument.Entity.*;
import com.financeportal.backend.Instrument.Enum.InstrumentType;
import com.financeportal.backend.Instrument.Mapper.InstrumentMapper;
import com.financeportal.backend.Instrument.Repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
@Transactional
public class InstrumentServiceImpl implements InstrumentService {

    private final InstrumentRepository instrumentRepository;
    private final InstrumentPriceRepository priceRepository;
    private final PriceHistoryRepository historyRepository;
    private final InstrumentMapper instrumentMapper;


    /**
     * ID ile enstrüman detayını döner.
     * Sonuç cache'lenir, tekrar istekte DB'ye gidilmez.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "instrumentDetails", key = "#id")
    public InstrumentResponseDTO getInstrumentById(Long id) {
        log.info("🔍 Cache MISS - Fetching instrument from DB: {}", id);

        BaseInstrument instrument = instrumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Instrument not found with id: " + id
                ));

        if (instrument instanceof StockInstrument stock) {
            log.debug("Retrieved StockInstrument: {} - Sector: {}",
                    stock.getSymbol(), stock.getSector());
        } else if (instrument instanceof ForexInstrument forex) {
            log.debug("Retrieved ForexInstrument: {}/{}",
                    forex.getBaseCurrency(), forex.getQuoteCurrency());
        } else if (instrument instanceof CryptoInstrument crypto) {
            log.debug("Retrieved CryptoInstrument: {} - Blockchain: {}",
                    crypto.getSymbol(), crypto.getBlockchain());
        }

        InstrumentPrice price = priceRepository
                .findTopByInstrumentOrderByTimestampDesc(instrument)
                .orElse(null);

        return instrumentMapper.toResponseDTO(instrument, price);
    }

    /**
     * Sembol ile enstrüman detayını döner.
     * Sonuç cache'lenir, tekrar istekte DB'ye gidilmez.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "instrumentDetails", key = "'symbol:' + #symbol")
    public InstrumentResponseDTO getInstrumentBySymbol(String symbol) {
        log.info("🔍 Cache MISS - Fetching instrument from DB: {}", symbol);

        BaseInstrument instrument = instrumentRepository.findBySymbol(symbol)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Instrument not found with symbol: " + symbol
                ));

        InstrumentPrice price = priceRepository
                .findTopByInstrumentOrderByTimestampDesc(instrument)
                .orElse(null);

        return instrumentMapper.toResponseDTO(instrument, price);
    }

    /**
     * Tüm aktif enstrümanları sayfalı olarak döner.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<InstrumentResponseDTO> getAllInstruments(Pageable pageable) {
        log.info("Fetching all instruments - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<BaseInstrument> instruments = instrumentRepository.findByActiveTrue(pageable);

        return instruments.map(instrument -> {
            InstrumentPrice price = priceRepository
                    .findTopByInstrumentOrderByTimestampDesc(instrument)
                    .orElse(null);
            return instrumentMapper.toResponseDTO(instrument, price);
        });
    }

    /**
     * Belirtilen tipe göre enstrümanları sayfalı olarak döner.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<InstrumentResponseDTO> getInstrumentsByType(
            InstrumentType type,
            Pageable pageable
    ) {
        log.info("Fetching {} instruments - page: {}", type, pageable.getPageNumber());

        Page<BaseInstrument> instruments;

        // Tip bazlı doğrudan sorgu
        instruments = switch (type) {
            case STOCK -> instrumentRepository.findAllStocks(pageable);
            case FOREX -> instrumentRepository.findAllForex(pageable);
            case CRYPTO -> instrumentRepository.findAllCryptos(pageable);
            case BOND -> instrumentRepository.findAllBonds(pageable);
            case PRECIOUS -> instrumentRepository.findAllPrecious(pageable);
            case FUND -> instrumentRepository.findAllFunds(pageable);
            case VIOP -> instrumentRepository.findAllViop(pageable);
        };

        return instruments.map(instrument -> {
            InstrumentPrice price = priceRepository
                    .findTopByInstrumentOrderByTimestampDesc(instrument)
                    .orElse(null);
            return instrumentMapper.toResponseDTO(instrument, price);
        });
    }

    /**
     * Sembol veya isim bazlı enstrüman araması yapar.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<InstrumentResponseDTO> searchInstruments(String search, Pageable pageable) {
        log.info("Searching instruments: {}", search);

        Page<BaseInstrument> instruments = instrumentRepository
                .searchInstruments(search, pageable);

        return instruments.map(instrument -> {
            InstrumentPrice price = priceRepository
                    .findTopByInstrumentOrderByTimestampDesc(instrument)
                    .orElse(null);
            return instrumentMapper.toResponseDTO(instrument, price);
        });
    }

    /**
     * Enstrümanın en güncel fiyat verisini döner.
     * Sonuç cache'lenir.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "instrumentPrices", key = "#instrumentId")
    public PriceDataDTO getCurrentPrice(Long instrumentId) {
        BaseInstrument instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Instrument not found with id: " + instrumentId
                ));

        InstrumentPrice price = priceRepository
                .findTopByInstrumentOrderByTimestampDesc(instrument)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No price data found for instrument: " + instrument.getSymbol()
                ));

        return instrumentMapper.toPriceDataDTO(price);
    }

    /**
     * Enstrümanın belirtilen tarih aralığındaki geçmiş fiyat verilerini döner.
     */
    @Override
    @Transactional(readOnly = true)
    public List<HistoricalPriceDTO> getHistoricalPrices(
            Long instrumentId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        BaseInstrument instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Instrument not found with id: " + instrumentId
                ));

        List<PriceHistory> history = historyRepository
                .findByInstrumentAndDateBetweenOrderByDateAsc(
                        instrument, startDate, endDate
                );

        return history.stream()
                .map(instrumentMapper::toHistoricalPriceDTO)
                .collect(Collectors.toList());
    }
}