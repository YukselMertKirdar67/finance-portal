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
        if (config.url && config.url.includes('/auth/refresh')) {
            return config;
        }
        const token = sessionStorage.getItem('token') || localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
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

            const refreshTokenValue = sessionStorage.getItem('refreshToken') ||
                localStorage.getItem('refreshToken');

            if (!refreshTokenValue) {
                localStorage.clear();
                sessionStorage.clear();
                window.location.href = '/login';
                return Promise.reject(error);
            }

            try {
                const response = await axios.post(`${API_BASE_URL}/auth/refresh`, {
                    refreshToken: refreshTokenValue
                }, {
                    headers: { 'Content-Type': 'application/json' }
                });

                if (response.data && response.data.success) {
                    const { accessToken, refreshToken: newRefreshToken } = response.data;

                    const storage = sessionStorage.getItem('refreshToken')
                        ? sessionStorage
                        : localStorage;

                    storage.setItem('token', accessToken);
                    if (newRefreshToken) {
                        storage.setItem('refreshToken', newRefreshToken);
                    }

                    originalRequest.headers.Authorization = `Bearer ${accessToken}`;
                    return api(originalRequest);
                } else {
                    throw new Error('Refresh token response invalid');
                }
            } catch (refreshError) {
                localStorage.clear();
                sessionStorage.clear();
                window.location.href = '/login';
                return Promise.reject(refreshError);
            }
        }

        return Promise.reject(error);
    }
);

export default api;