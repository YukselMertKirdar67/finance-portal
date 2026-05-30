import api from './axiosConfig.js';


/**
 * Anasayfaya gider ve veriler döner
 */
export const getHomePageData = async () => {
    const response = await api.get('/home');
    return response.data;
};