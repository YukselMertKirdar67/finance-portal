import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

/**
 * Login
 */
export const login = async (credentials) => {
    try {
        const response = await axios.post(`${API_BASE_URL}/auth/login`, credentials);
        return response.data;
    } catch (error) {
        console.error('Login error:', error);
        throw error;
    }
};

/**
 * Refresh Token
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
 * Logout - Terminate Keycloak session
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