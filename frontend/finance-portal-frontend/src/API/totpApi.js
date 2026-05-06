import api from './instrumentsApi';

export const setupTotp = async () => {
    try {
        const response = await api.post('/totp/setup');
        return response.data;
    } catch (error) {
        console.error('Error setting up TOTP:', error);
        throw error;
    }
};

export const verifyTotpSetup = async (code) => {
    try {
        const response = await api.post('/totp/verify-setup', { code });
        return response.data;
    } catch (error) {
        console.error('Error verifying TOTP setup:', error);
        throw error;
    }
};

export const verifyTotpLogin = async (keycloakId, code) => {
    try {
        const response = await api.post('/totp/verify-login', { keycloakId, code });
        return response.data;
    } catch (error) {
        console.error('Error verifying TOTP login:', error);
        throw error;
    }
};

export const getTotpStatus = async () => {
    try {
        const response = await api.get('/totp/status');
        return response.data;
    } catch (error) {
        console.error('Error getting TOTP status:', error);
        throw error;
    }
};

export const disableTotp = async () => {
    try {
        const response = await api.delete('/totp/disable');
        return response.data;
    } catch (error) {
        console.error('Error disabling TOTP:', error);
        throw error;
    }
};