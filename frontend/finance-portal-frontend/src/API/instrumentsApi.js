import api from './axiosConfig.js'


/**
 * Tüm enstrümanları sayfalı olarak getirir
 */
export const getAllInstruments = async (page = 0, size = 20) => {
    try {
        const response = await api.get('/instruments', {
            params: { page, size, sortBy: 'name' }
        });
        return response.data;
    } catch (error) {
        console.error('Error fetching instruments:', error);
        throw error;
    }
};


/**
 * Belirtilen tipe göre enstrümanları getirir
 */
export const getInstrumentsByType = async (type, page = 0, size = 20) => {
    try {
        const response = await api.get(`/instruments/type/${type}`, {
            params: { page, size, sortBy: 'name' }
        });
        return response.data;
    } catch (error) {
        console.error('Error fetching instruments by type:', error);
        throw error;
    }
};

/**
 * ID'ye göre enstrüman detayını getirir
 */
export const getInstrumentById = async (id) => {
    try {
        const response = await api.get(`/instruments/${id}`);
        return response.data;
    } catch (error) {
        console.error('Error fetching instrument by ID:', error);
        throw error;
    }
};

/**
 * Enstrüman arama yapar
 */
export const searchInstruments = async (query, page = 0, size = 20) => {
    try {
        const response = await api.get('/instruments/search', {
            params: { query, page, size }
        });
        return response.data;
    } catch (error) {
        console.error('Error searching instruments:', error);
        throw error;
    }
};

/**
 * Enstrümanın anlık fiyatını getirir
 */
export const getInstrumentPrice = async (id) => {
    try {
        const response = await api.get(`/instruments/${id}/price`);
        return response.data;
    } catch (error) {
        console.error('Error fetching instrument price:', error);
        throw error;
    }
};

/**
 * Enstrümanın belirli tarih aralığındaki geçmiş fiyat verilerini getirir
 */
export const getHistoricalPrices = async (id, startDate, endDate) => {
    try {
        const response = await api.get(`/instruments/${id}/history`, {
            params: { startDate, endDate }
        });
        return response.data;
    } catch (error) {
        console.error('Error fetching historical prices:', error);
        throw error;
    }
};

export default api;