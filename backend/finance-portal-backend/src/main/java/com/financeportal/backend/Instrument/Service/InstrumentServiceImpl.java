package com.financeportal.backend.Instrument.Service;

import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.Instrument.DTO.*;
import com.financeportal.backend.Instrument.Entity.*;
import com.financeportal.backend.Instrument.Enum.InstrumentType;
import com.financeportal.backend.Instrument.Mapper.InstrumentMapper;
import com.financeportal.backend.Instrument.Repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Transactional
public class InstrumentServiceImpl implements InstrumentService {

    private final InstrumentRepository instrumentRepository;
    private final InstrumentPriceRepository priceRepository;
    private final PriceHistoryRepository historyRepository;
    private final InstrumentMapper instrumentMapper;

    @Override
    @CacheEvict(value = {"instrumentDetails", "instrumentPrices"}, allEntries = true)
    public InstrumentResponseDTO createInstrument(InstrumentRequestDTO requestDTO) {
        log.info("🗑️ Cache EVICT - Clearing cache");

        if (instrumentRepository.existsBySymbol(requestDTO.getSymbol())) {
            throw new IllegalArgumentException(
                    "Instrument with symbol " + requestDTO.getSymbol() + " already exists"
            );
        }

        BaseInstrument instrument = instrumentMapper.toEntity(requestDTO);
        BaseInstrument saved = instrumentRepository.save(instrument);

        log.info("Created instrument: {} (Type: {})", saved.getSymbol(), saved.getInstrumentType());

        return instrumentMapper.toResponseDTO(saved);
    }

    @Override
    @CacheEvict(value = {"instrumentDetails", "instrumentPrices"}, allEntries = true)
    public InstrumentResponseDTO updateInstrument(Long id, InstrumentRequestDTO requestDTO) {
        log.info("🗑️ Cache EVICT - Clearing cache for instrument: {}", id);

        BaseInstrument existing = instrumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Instrument not found with id: " + id
                ));

        if (!existing.getSymbol().equals(requestDTO.getSymbol()) &&
                instrumentRepository.existsBySymbol(requestDTO.getSymbol())) {
            throw new IllegalArgumentException(
                    "Instrument with symbol " + requestDTO.getSymbol() + " already exists"
            );
        }

        existing.setSymbol(requestDTO.getSymbol());
        existing.setName(requestDTO.getName());
        existing.setExchange(requestDTO.getExchange());
        existing.setDescription(requestDTO.getDescription());
        existing.setCurrency(requestDTO.getCurrency());

        if (existing instanceof StockInstrument stock) {
            stock.setSector(requestDTO.getSector());
            stock.setMarketCap(requestDTO.getMarketCap());
        } else if (existing instanceof BondInstrument bond) {
            bond.setMaturityDate(requestDTO.getMaturityDate());
            bond.setCouponRate(requestDTO.getCouponRate());
            bond.setFaceValue(requestDTO.getFaceValue());
            bond.setIssuer(requestDTO.getIssuer());
        } else if (existing instanceof EurobondInstrument eurobond) {
            eurobond.setMaturityDate(requestDTO.getMaturityDate());
            eurobond.setCouponRate(requestDTO.getCouponRate());
            eurobond.setFaceValue(requestDTO.getFaceValue());
            eurobond.setIssueCurrency(requestDTO.getIssueCurrency());
        } else if (existing instanceof ForexInstrument forex) {
            forex.setBaseCurrency(requestDTO.getBaseCurrency());
            forex.setQuoteCurrency(requestDTO.getQuoteCurrency());
        } else if (existing instanceof CryptoInstrument crypto) {
            crypto.setBlockchain(requestDTO.getBlockchain());
            crypto.setTotalSupply(requestDTO.getTotalSupply());
            crypto.setCirculatingSupply(requestDTO.getCirculatingSupply());
        } else if (existing instanceof PreciousInstrument precious) {
            precious.setMetalType(requestDTO.getMetalType());
            precious.setUnit(requestDTO.getUnit());
        }

        BaseInstrument updated = instrumentRepository.save(existing);

        log.info("Updated instrument: {} (Type: {})", updated.getSymbol(), updated.getInstrumentType());

        return instrumentMapper.toResponseDTO(updated);
    }

    @Override
    @CacheEvict(value = {"instrumentDetails", "instrumentPrices"}, allEntries = true)
    public void deleteInstrument(Long id) {
        log.info("🗑️ Cache EVICT - Clearing cache for deleted instrument: {}", id);

        BaseInstrument instrument = instrumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Instrument not found with id: " + id
                ));

        instrument.setActive(false);
        instrumentRepository.save(instrument);

        log.info("Deleted (deactivated) instrument: {} (Type: {})",
                instrument.getSymbol(), instrument.getInstrumentType());
    }

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
            case EUROBOND -> instrumentRepository.findAllEurobonds(pageable);
            case PRECIOUS -> instrumentRepository.findAllPrecious(pageable);
            default -> Page.empty(pageable);
        };

        return instruments.map(instrument -> {
            InstrumentPrice price = priceRepository
                    .findTopByInstrumentOrderByTimestampDesc(instrument)
                    .orElse(null);
            return instrumentMapper.toResponseDTO(instrument, price);
        });
    }

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