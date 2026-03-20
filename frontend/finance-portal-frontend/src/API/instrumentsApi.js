import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// REQUEST INTERCEPTOR - Token ekle
api.interceptors.request.use(
    (config) => {
        // /auth/refresh isteğine token ekleme
        if (config.url && config.url.includes('/auth/refresh')) {
            return config;
        }

        // Token'ı bul: Önce sessionStorage, sonra localStorage
        const token = sessionStorage.getItem('token') || localStorage.getItem('token');

        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// RESPONSE INTERCEPTOR - Auto Token Refresh
api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            // Refresh token'ı bul: Önce sessionStorage, sonra localStorage
            const refreshTokenValue = sessionStorage.getItem('refreshToken') ||
                localStorage.getItem('refreshToken');

            if (!refreshTokenValue) {
                console.log('❌ No refresh token found, logging out...');
                localStorage.clear();
                sessionStorage.clear();
                window.location.href = '/login';
                return Promise.reject(error);
            }

            try {
                console.log('🔄 Access token expired, refreshing...');

                // Direkt axios kullan (api instance değil!)
                const response = await axios.post(`${API_BASE_URL}/auth/refresh`, {
                    refreshToken: refreshTokenValue
                }, {
                    headers: {
                        'Content-Type': 'application/json'
                    }
                });

                if (response.data && response.data.success) {
                    const { accessToken, refreshToken: newRefreshToken } = response.data;

                    // Doğru storage'a kaydet
                    const storage = sessionStorage.getItem('refreshToken')
                        ? sessionStorage
                        : localStorage;

                    storage.setItem('token', accessToken);
                    if (newRefreshToken) {
                        storage.setItem('refreshToken', newRefreshToken);
                    }

                    originalRequest.headers.Authorization = `Bearer ${accessToken}`;

                    console.log('✅ Token refreshed successfully, retrying request...');
                    return api(originalRequest);
                } else {
                    throw new Error('Refresh token response invalid');
                }

            } catch (refreshError) {
                console.error('❌ Token refresh failed:', refreshError);
                localStorage.clear();
                sessionStorage.clear();
                window.location.href = '/login';
                return Promise.reject(refreshError);
            }
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