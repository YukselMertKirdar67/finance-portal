import api from './instrumentsApi';

export const compareInstruments = async (id1, id2, startDate, endDate) => {
    const response = await api.get('/comparison', {
        params: { id1, id2, startDate, endDate }
    });
    return response.data;
};

export const searchInstruments = async (query) => {
    const response = await api.get('/instruments/search', {
        params: { query, page: 0, size: 20 }
    });
    return response.data;
};