import api from './instrumentsApi';

/**
 * Anasayfaya gider ve veriler döner
 */
export const getHomePageData = async () => {
    const response = await api.get('/home');
    return response.data;
};