package com.financeportal.backend.Watchlist;



public interface WatchlistService {

    WatchlistDTO.WatchlistResponse addToWatchlist(Long instrumentId);

    WatchlistDTO.WatchlistResponse removeFromWatchlist(Long instrumentId);

    WatchlistPageDTO getWatchlist(int page, int size);

    boolean isInWatchlist(Long instrumentId);
}
