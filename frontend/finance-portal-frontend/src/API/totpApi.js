import api from './axiosConfig.js';



/**
 * TOTP kurulumunu başlatır ve QR kod ile secret key döner
 */
export const setupTotp = async () => {
    try {
        const response = await api.post('/totp/setup');
        return response.data;
    } catch (error) {
        console.error('Error setting up TOTP:', error);
        throw error;
    }
};


/**
 * TOTP kurulumunu doğrular
 */
export const verifyTotpSetup = async (code) => {
    try {
        const response = await api.post('/totp/verify-setup', { code });
        return response.data;
    } catch (error) {
        console.error('Error verifying TOTP setup:', error);
        throw error;
    }
};

/**
 * Giriş sırasında TOTP kodunu doğrular
 */
export const verifyTotpLogin = async (keycloakId, code) => {
    try {
        const response = await api.post('/totp/verify-login', { keycloakId, code });
        return response.data;
    } catch (error) {
        console.error('Error verifying TOTP login:', error);
        throw error;
    }
};

/**
 * Kullanıcının TOTP durumunu getirir (aktif/pasif)
 */
export const getTotpStatus = async () => {
    try {
        const response = await api.get('/totp/status');
        return response.data;
    } catch (error) {
        console.error('Error getting TOTP status:', error);
        throw error;
    }
};
