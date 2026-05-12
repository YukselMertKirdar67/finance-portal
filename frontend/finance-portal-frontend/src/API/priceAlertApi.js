import api from './instrumentsApi';

/**
 * İşlem bildirimi oluşturur
 */
export const createPriceAlert = async (instrumentId, targetPrice, condition) => {
    const response = await api.post('/price-alerts', { instrumentId, targetPrice, condition });
    return response.data;
};

/**
 * İşlem bildirimlerini getirir
 */
export const getUserAlerts = async () => {
    const response = await api.get('/price-alerts');
    return response.data;
};

/**
 * Aktif işlem bildirimlerini getirir
 */
export const getActiveUserAlerts = async () => {
    const response = await api.get('/price-alerts/active');
    return response.data;
};

/**
 * İşlem bildirimini siler
 */
export const deletePriceAlert = async (id) => {
    const response = await api.delete(`/price-alerts/${id}`);
    return response.data;
};