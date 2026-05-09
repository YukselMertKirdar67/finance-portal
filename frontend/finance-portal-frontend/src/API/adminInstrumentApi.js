import api from './instrumentsApi';

export const getUpdateStatus = async () => {
    try {
        const response = await api.get('/admin/instruments/update-status');
        return response.data;
    } catch (error) {
        console.error('Error fetching update status:', error);
        throw error;
    }
};

export const updateAllInstruments = async () => {
    try {
        const response = await api.post('/admin/instruments/update-all');
        return response.data;
    } catch (error) {
        console.error('Error updating all instruments:', error);
        throw error;
    }
};

export const updateTcmb = async () => {
    try {
        const response = await api.post('/admin/instruments/update-tcmb');
        return response.data;
    } catch (error) {
        console.error('Error updating TCMB:', error);
        throw error;
    }
};

// Yahoo US Stocks
export const updateUsStocks = async () => {
    try {
        const response = await api.post('/admin/instruments/update-us-stocks');
        return response.data;
    } catch (error) {
        console.error('Error updating US stocks:', error);
        throw error;
    }
};

//  Yahoo BIST
export const updateBist = async () => {
    try {
        const response = await api.post('/admin/instruments/update-bist');
        return response.data;
    } catch (error) {
        console.error('Error updating BIST:', error);
        throw error;
    }
};

//  Yahoo Kripto
export const updateCrypto = async () => {
    try {
        const response = await api.post('/admin/instruments/update-crypto');
        return response.data;
    } catch (error) {
        console.error('Error updating crypto:', error);
        throw error;
    }
};

// Yahoo Precious
export const updatePrecious = async () => {
    try {
        const response = await api.post('/admin/instruments/update-precious');
        return response.data;
    } catch (error) {
        console.error('Error updating precious metals:', error);
        throw error;
    }
};

export const updateBonds = async () => {
    try {
        const response = await api.post('/admin/instruments/update-bonds');
        return response.data;
    } catch (error) {
        console.error('Error updating bonds:', error);
        throw error;
    }
};

export const updateEtfs = async () => {
    try {
        const response = await api.post('/admin/instruments/update-etfs');
        return response.data;
    } catch (error) {
        console.error('Error updating ETFs:', error);
        throw error;
    }
};

// Geçmiş veri
export const fetchAllHistoricalData = async () => {
    try {
        const response = await api.post('/admin/instruments/fetch-all-historical');
        return response.data;
    } catch (error) {
        console.error('Error fetching historical data:', error);
        throw error;
    }
};

export const fetchForexHistoricalData = async (days = 365) => {
    try {
        const response = await api.post(`/admin/instruments/fetch-forex-historical?days=${days}`);
        return response.data;
    } catch (error) {
        console.error('Error fetching forex historical data:', error);
        throw error;
    }
};

export const getApiStats = async () => {
    try {
        const response = await api.get('/admin/instruments/stats');
        return response.data;
    } catch (error) {
        console.error('Error fetching API stats:', error);
        throw error;
    }
};