import api from './axiosConfig.js';



/**
 * Tüm enstrümanların güncelleme durumunu getirir
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
 * Tüm enstrümanları tüm kaynaklardan günceller
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
 * TCMB'den günlük döviz kurlarını günceller
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
 * Yahoo Finance'den ABD hisse senedi fiyatlarını günceller
 */
export const updateUsStocks = async () => {
    try {
        const response = await api.post('/admin/instruments/update-us-stocks');
        return response.data;
    } catch (error) {
        console.error('Error updating US stocks:', error);
        throw error;
    }
};

/**
 * Yahoo Finance'den BIST hisse senedi fiyatlarını günceller
 */
export const updateBist = async () => {
    try {
        const response = await api.post('/admin/instruments/update-bist');
        return response.data;
    } catch (error) {
        console.error('Error updating BIST:', error);
        throw error;
    }
};

/**
 * Yahoo Finance'den kripto para fiyatlarını günceller
 */
export const updateCrypto = async () => {
    try {
        const response = await api.post('/admin/instruments/update-crypto');
        return response.data;
    } catch (error) {
        console.error('Error updating crypto:', error);
        throw error;
    }
};

/**
 * Yahoo Finance'den kıymetli metal fiyatlarını günceller (altın, gümüş vb.)
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
 * Yahoo Finance'den ABD tahvil faiz oranlarını günceller
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
 * Yahoo Finance'den ABD tahvil faiz oranlarını günceller
 */
export const updateTrBonds = async () => {
    try {
        const response = await api.post('/admin/instruments/update-tr-bonds');
        return response.data;
    } catch (error) {
        console.error('Error updating TR bonds:', error);
        throw error;
    }
};

/**
 * Yahoo Finance'den ETF fiyatlarını günceller
 */
export const updateEtfs = async () => {
    try {
        const response = await api.post('/admin/instruments/update-etfs');
        return response.data;
    } catch (error) {
        console.error('Error updating ETFs:', error);
        throw error;
    }
};

/**
 * Yahoo Finance'den ETF fiyatlarını günceller
 */
export const fetchAllHistoricalData = async () => {
    try {
        const response = await api.post('/admin/instruments/fetch-all-historical');
        return response.data;
    } catch (error) {
        console.error('Error fetching historical data:', error);
        throw error;
    }
};

/**
 * TCMB arşivinden geçmiş döviz kurlarını çeker
 */
export const fetchForexHistoricalData = async (days = 365) => {
    try {
        const response = await api.post(`/admin/instruments/fetch-forex-historical?days=${days}`);
        return response.data;
    } catch (error) {
        console.error('Error fetching forex historical data:', error);
        throw error;
    }
};

/**
 * TCMB EVDS API'den geçmiş Türk tahvil faiz verilerini çeker
 */
export const fetchTrBondsHistorical = async (days = 365) => {
    try {
        const response = await api.post(`/admin/instruments/fetch-tr-bonds-historical?days=${days}`);
        return response.data;
    } catch (error) {
        console.error('Error fetching TR bonds historical:', error);
        throw error;
    }
};

/**
 * İş Yatırım'dan VİOP fiyatlarını günceller
 */
export const updateViop = async () => {
    try {
        const response = await api.post('/admin/instruments/update-viop');
        return response.data;
    } catch (error) {
        console.error('Error updating VIOP:', error);
        throw error;
    }
};

/**
 * İş Yatırım'dan geçmiş VİOP fiyatlarını çeker
 */
export const fetchViopHistorical = async () => {
    try {
        const response = await api.post('/admin/instruments/fetch-viop-historical');
        return response.data;
    } catch (error) {
        console.error('Error fetching VIOP historical:', error);
        throw error;
    }
};