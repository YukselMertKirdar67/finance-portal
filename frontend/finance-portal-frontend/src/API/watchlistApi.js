import api from './axiosConfig.js';



/**
 * Watchlist'i getir
 */
export const getWatchlist = async (page = 0, size = 100) => {
    const response = await api.get('/watchlist', {
        params: { page, size }
    });
    return response.data;
};

/**
 * Watchlist'e ekle
 */
export const addToWatchlist = async (instrumentId) => {
    const response = await api.post(`/watchlist/${instrumentId}`);
    return response.data;
};

/**
 * Watchlist'ten çıkar
 */
export const removeFromWatchlist = async (instrumentId) => {
    const response = await api.delete(`/watchlist/${instrumentId}`);
    return response.data;
};

/**
 * Enstrüman watchlist'te mi?
 */
export const isInWatchlist = async (instrumentId) => {
    const response = await api.get(`/watchlist/check/${instrumentId}`);
    return response.data;
};

/**
 * Enstrüman ara
 */
export const searchInstruments = async (query) => {
    const response = await api.get('/instruments/search', {
        params: { query, page: 0, size: 20 }
    });
    return response.data;
};