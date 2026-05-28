import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

/**
 * İstek interceptor'ı — her isteğe JWT token ekler.
 * /auth/refresh endpoint'i için token eklenmez.
 */
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

/**
 * Yanıt interceptor'ı — 401 hatası alındığında token'ı otomatik yeniler.
 * Yenileme başarısız olursa storage temizlenir ve login sayfasına yönlendirilir.
 */
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

/**
 * Tüm enstrümanları sayfalı olarak getirir
 */
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


/**
 * Belirtilen tipe göre enstrümanları getirir
 */
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

/**
 * ID'ye göre enstrüman detayını getirir
 */
export const getInstrumentById = async (id) => {
    try {
        const response = await api.get(`/instruments/${id}`);
        return response.data;
    } catch (error) {
        console.error('Error fetching instrument by ID:', error);
        throw error;
    }
};

/**
 * Enstrüman arama yapar
 */
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

/**
 * Enstrümanın anlık fiyatını getirir
 */
export const getInstrumentPrice = async (id) => {
    try {
        const response = await api.get(`/instruments/${id}/price`);
        return response.data;
    } catch (error) {
        console.error('Error fetching instrument price:', error);
        throw error;
    }
};

/**
 * Enstrümanın belirli tarih aralığındaki geçmiş fiyat verilerini getirir
 */
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