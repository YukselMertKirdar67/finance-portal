import api from './axiosConfig.js';


/**
 * Kullanıcı profilini getirir
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
 * Şifreyi değiştirir
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

/**
 * Kullanıcı adını değiştirir
 */
export const updateUsername = async (newUsername) => {
    const response = await api.put('/me/username', { newUsername });
    return response.data;
};

/**
 * E-maili değiştirir
 */
export const updateEmail = async (newEmail, password) => {
    const response = await api.put('/me/email', { newEmail, password });
    return response.data;
};

/**
 * En son değişen şifrenin tarihini getirir
 */
export const getPasswordLastChanged = async () => {
    const response = await api.get('/me/password-last-changed');
    return response.data;
};

/**
 * Tercihleri günceller
 */
export const updatePreferences = async (theme, notifyTransaction, notifyPortfolioChange, notifyPriceAlert, notifyNews) => {
    const response = await api.put('/me/preferences', {
        theme,
        notifyTransaction,
        notifyPortfolioChange,
        notifyPriceAlert,
        notifyNews
    });
    return response.data;
};

/**
 * Kullanıcı verilerini dışarı aktarır
 */
export const exportUserData = async () => {
    const response = await api.get('/me/export', { responseType: 'blob' });
    return response.data;
};

/**
 * Hesabı siler
 */
export const deleteAccount = async () => {
    const response = await api.delete('/me/account');
    return response.data;
};