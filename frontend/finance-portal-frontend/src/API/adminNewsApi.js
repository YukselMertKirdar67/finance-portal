import api from './instrumentsApi';

/**
 * Haber istatistiklerini getirir
 */
export const getNewsStats = async () => {
    try {
        const response = await api.get('/admin/news/stats');
        return response.data;
    } catch (error) {
        console.error('Error fetching news stats:', error);
        throw error;
    }
};

/**
 * Mevcut haber kategorilerini getirir
 */
export const getNewsCategories = async () => {
    try {
        const response = await api.get('/admin/news/categories');
        return response.data;
    } catch (error) {
        console.error('Error fetching news categories:', error);
        throw error;
    }
};

/**
 * NewsAPI'den güncel haberleri çeker ve veritabanına kaydeder
 */
export const fetchNewsFromApi = async () => {
    try {
        const response = await api.post('/admin/news/fetch');
        return response.data;
    } catch (error) {
        console.error('Error fetching news from API:', error);
        throw error;
    }
};

/**
 * Tüm haberleri siler ve yeniden çeker
 */
export const refreshAllNews = async () => {
    try {
        const response = await api.post('/admin/news/refresh');
        return response.data;
    } catch (error) {
        console.error('Error refreshing news:', error);
        throw error;
    }
};

/**
 * Veritabanındaki tüm haberleri siler
 */
export const deleteAllNews = async () => {
    try {
        const response = await api.delete('/admin/news/all');
        return response.data;
    } catch (error) {
        console.error('Error deleting all news:', error);
        throw error;
    }
};

/**
 * Belirtilen kategoriye ait tüm haberleri siler
 */
export const deleteNewsByCategory = async (category) => {
    try {
        const response = await api.delete(`/admin/news/category/${category}`);
        return response.data;
    } catch (error) {
        console.error('Error deleting news by category:', error);
        throw error;
    }
};