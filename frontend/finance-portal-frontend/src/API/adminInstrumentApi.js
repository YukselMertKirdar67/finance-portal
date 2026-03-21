import api from './instrumentsApi';

/**
 * Get update status
 */
export const getUpdateStatus = async () => {
    try {
        const response = await api.get('/admin/instruments/update-status');
        return response.data;
    } catch (error) {
        console.error('Error fetching update status:', error);
        throw error;
    }
};

/**
 * Update all instruments
 */
export const updateAllInstruments = async () => {
    try {
        const response = await api.post('/admin/instruments/update-all');
        return response.data;
    } catch (error) {
        console.error('Error updating all instruments:', error);
        throw error;
    }
};

/**
 * Update TCMB rates
 */
export const updateTcmb = async () => {
    try {
        const response = await api.post('/admin/instruments/update-tcmb');
        return response.data;
    } catch (error) {
        console.error('Error updating TCMB:', error);
        throw error;
    }
};

/**
 * Update Finnhub prices
 */
export const updateFinnhub = async () => {
    try {
        const response = await api.post('/admin/instruments/update-finnhub');
        return response.data;
    } catch (error) {
        console.error('Error updating Finnhub:', error);
        throw error;
    }
};

/**
 * Update BIST stocks
 */
export const updateBist = async () => {
    try {
        const response = await api.post('/admin/instruments/update-bist-twelvedata');
        return response.data;
    } catch (error) {
        console.error('Error updating BIST:', error);
        throw error;
    }
};

/**
 * Update precious metals
 */
export const updatePrecious = async () => {
    try {
        const response = await api.post('/admin/instruments/update-precious');
        return response.data;
    } catch (error) {
        console.error('Error updating precious metals:', error);
        throw error;
    }
};

/**
 * Update bonds
 */
export const updateBonds = async () => {
    try {
        const response = await api.post('/admin/instruments/update-bonds');
        return response.data;
    } catch (error) {
        console.error('Error updating bonds:', error);
        throw error;
    }
};

/**
 * Get API stats
 */
export const getApiStats = async () => {
    try {
        const response = await api.get('/admin/instruments/stats');
        return response.data;
    } catch (error) {
        console.error('Error fetching API stats:', error);
        throw error;
    }
};