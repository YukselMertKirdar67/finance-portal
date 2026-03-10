import axios from 'axios';
import { getToken, updateToken, doLogout } from '../keycloak';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// ✅ REQUEST INTERCEPTOR - Her isteğe token ekle
api.interceptors.request.use(
    (config) => {
        const token = getToken();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// ✅ RESPONSE INTERCEPTOR - 401 Unauthorized handling
api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        // Token expired, try refresh
        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            return new Promise((resolve, reject) => {
                updateToken((newToken) => {
                    originalRequest.headers.Authorization = `Bearer ${newToken}`;
                    resolve(api(originalRequest));
                });
            }).catch((err) => {
                doLogout();
                return Promise.reject(err);
            });
        }

        return Promise.reject(error);
    }
);

// Tüm enstrümanları getir (sayfalı)
export const getAllInstruments = async (page = 0, size = 20) => {
    try {
        const response = await api.get('/instruments', {
            params: { page, size, sortBy: 'name' }
        });
        return response.data;
    } catch (error) {
        console.error('Error fetching instruments:', error);
        throw error;
    }
};

// Tipe göre enstrümanları getir
export const getInstrumentsByType = async (type, page = 0, size = 20) => {
    try {
        const response = await api.get(`/instruments/type/${type}`, {
            params: { page, size, sortBy: 'name' }
        });
        return response.data;
    } catch (error) {
        console.error('Error fetching instruments by type:', error);
        throw error;
    }
};

// ID ile enstrüman detayı
export const getInstrumentById = async (id) => {
    try {
        const response = await api.get(`/instruments/${id}`);
        return response.data;
    } catch (error) {
        console.error('Error fetching instrument by ID:', error);
        throw error;
    }
};

// Sembol ile enstrüman detayı (slash sorunu için query param)
export const getInstrumentBySymbol = async (symbol) => {
    try {
        const response = await api.get('/instruments/symbol', {
            params: { symbol }
        });
        return response.data;
    } catch (error) {
        console.error('Error fetching instrument by symbol:', error);
        throw error;
    }
};

// Enstrüman ara
export const searchInstruments = async (query, page = 0, size = 20) => {
    try {
        const response = await api.get('/instruments/search', {
            params: { query, page, size }
        });
        return response.data;
    } catch (error) {
        console.error('Error searching instruments:', error);
        throw error;
    }
};

// Anlık fiyat
export const getInstrumentPrice = async (id) => {
    try {
        const response = await api.get(`/instruments/${id}/price`);
        return response.data;
    } catch (error) {
        console.error('Error fetching instrument price:', error);
        throw error;
    }
};

// Geçmiş fiyatlar
export const getHistoricalPrices = async (id, startDate, endDate) => {
    try {
        const response = await api.get(`/instruments/${id}/history`, {
            params: { startDate, endDate }
        });
        return response.data;
    } catch (error) {
        console.error('Error fetching historical prices:', error);
        throw error;
    }
};

export default api;