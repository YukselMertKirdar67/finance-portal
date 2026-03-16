import api from './instrumentsApi';

/**
 * Get current user profile
 */
export const getCurrentUser = async () => {
    try {
        const response = await api.get('/me/profile');
        return response.data;
    } catch (error) {
        console.error('Error fetching current user:', error);
        throw error;
    }
};

/**
 * Change password
 */
export const changePassword = async (passwordData) => {
    try {
        const response = await api.post('/me/change-password', passwordData);
        return response.data;
    } catch (error) {
        console.error('Error changing password:', error);
        throw error;
    }
};