import api from './instrumentsApi';

/**
 * Login
 */
export const login = async (credentials) => {
    try {
        const response = await api.post('/auth/login', credentials);
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
        const response = await api.post('/auth/refresh', { refreshToken });
        return response.data;
    } catch (error) {
        console.error('Refresh token error:', error);
        throw error;
    }
};

/**
 * Logout
 */
export const logout = async () => {
    try {
        const response = await api.post('/auth/logout');
        return response.data;
    } catch (error) {
        console.error('Logout error:', error);
        throw error;
    }
};