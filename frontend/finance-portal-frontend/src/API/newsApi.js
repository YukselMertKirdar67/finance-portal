import api from './axiosConfig.js';



/**
 * Tüm haberleri getirir
 */
export const getAllNews = async (page = 0, size = 20) => {
    try {
        const response = await api.get('/news', {
            params: { page, size }
        });
        return response.data;
    } catch (error) {
        console.error('Error fetching news:', error);
        throw error;
    }
};

/**
 * Kategoriye göre haberleri getirir
 */
export const getNewsByCategory = async (category, page = 0, size = 20) => {
    try {
        const response = await api.get(`/news/category/${category}`, {
            params: { page, size }
        });
        return response.data;
    } catch (error) {
        console.error('Error fetching news by category:', error);
        throw error;
    }
};

/**
 * Id ye göre haber getirir
 */
export const getNewsById = async (id) => {
    try {
        const response = await api.get(`/news/${id}`);
        return response.data;
    } catch (error) {
        console.error('Error fetching news by ID:', error);
        throw error;
    }
};