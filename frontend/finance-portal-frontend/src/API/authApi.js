import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

/**
 * Refresh token kullanarak yeni access token alır
 */
export const refreshToken = async (refreshToken) => {
    try {
        const response = await axios.post(`${API_BASE_URL}/auth/refresh`, {
            refreshToken
        });
        return response.data;
    } catch (error) {
        console.error('Refresh token error:', error);
        throw error;
    }
};

/**
 * Keycloak oturumunu sonlandırır
 */
export const logoutUser = async (refreshToken) => {
    try {
        const response = await axios.post(`${API_BASE_URL}/auth/logout`, {
            refreshToken
        });
        return response.data;
    } catch (error) {
        console.error('Logout error:', error);
        // Hata olsa bile devam et (backend session cleanup başarısız olabilir)
        return { success: true, message: 'Logout completed' };
    }
};