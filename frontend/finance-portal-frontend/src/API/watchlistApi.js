import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';
const api = axios.create({ baseURL: API_BASE_URL });

/**
 * Watchlist'i getir (Paginated)
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
 * Enstrüman ara (Modal için)
 */
export const searchInstruments = async (query) => {
    const response = await api.get('/instruments/search', {
        params: { query, page: 0, size: 20 }
    });
    return response.data;
};