import api from './axiosConfig.js';


/**
 * Tüm kullanıcıların portföylerini getirir (admin)
 */
export const getAllPortfoliosAdmin = async (userId = null) => {
    try {
        const params = userId ? { userId } : {};
        const response = await api.get('/admin/portfolios', { params });
        return response.data;
    } catch (error) {
        console.error('Error fetching all portfolios:', error);
        throw error;
    }
};

/**
 * Sistem geneli portföy istatistiklerini getirir (admin)
 */
export const getSystemStatistics = async () => {
    try {
        const response = await api.get('/admin/portfolios/statistics');
        return response.data;
    } catch (error) {
        console.error('Error fetching system statistics:', error);
        throw error;
    }
};

/**
 * Herhangi bir portföyü kalıcı olarak siler (admin)
 */
export const forceDeletePortfolio = async (id) => {
    try {
        const response = await api.delete(`/admin/portfolios/${id}`);
        return response.data;
    } catch (error) {
        console.error('Error deleting portfolio:', error);
        throw error;
    }
};