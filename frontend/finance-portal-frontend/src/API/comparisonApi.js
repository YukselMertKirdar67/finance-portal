import api from './axiosConfig.js';



/**
 * Finansal enstrümanları karşılaştırır
 */
export const compareInstruments = async (id1, id2, period = '1A') => {
    const response = await api.get('/comparison', {
        params: { id1, id2, period }
    });
    return response.data;
};

/**
 * Karşılaştırılacak finansal enstrümanları arar
 */
export const searchInstruments = async (query) => {
    const response = await api.get('/instruments/search', {
        params: { query, page: 0, size: 20 }
    });
    return response.data;
};