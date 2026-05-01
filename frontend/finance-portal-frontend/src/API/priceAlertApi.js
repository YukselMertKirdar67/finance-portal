import api from './instrumentsApi';

export const createPriceAlert = async (instrumentId, targetPrice, condition) => {
    const response = await api.post('/price-alerts', { instrumentId, targetPrice, condition });
    return response.data;
};

export const getUserAlerts = async () => {
    const response = await api.get('/price-alerts');
    return response.data;
};

export const getActiveUserAlerts = async () => {
    const response = await api.get('/price-alerts/active');
    return response.data;
};

export const deletePriceAlert = async (id) => {
    const response = await api.delete(`/price-alerts/${id}`);
    return response.data;
};