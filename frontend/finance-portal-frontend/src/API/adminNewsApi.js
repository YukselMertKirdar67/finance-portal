import api from './instrumentsApi';

export const getNewsStats = async () => {
    try {
        const response = await api.get('/admin/news/stats');
        return response.data;
    } catch (error) {
        console.error('Error fetching news stats:', error);
        throw error;
    }
};

export const getNewsCategories = async () => {
    try {
        const response = await api.get('/admin/news/categories');
        return response.data;
    } catch (error) {
        console.error('Error fetching news categories:', error);
        throw error;
    }
};

export const getAllNewsAdmin = async () => {
    try {
        const response = await api.get('/admin/news/all');
        return response.data;
    } catch (error) {
        console.error('Error fetching all news:', error);
        throw error;
    }
};

export const fetchNewsFromApi = async () => {
    try {
        const response = await api.post('/admin/news/fetch');
        return response.data;
    } catch (error) {
        console.error('Error fetching news from API:', error);
        throw error;
    }
};

export const refreshAllNews = async () => {
    try {
        const response = await api.post('/admin/news/refresh');
        return response.data;
    } catch (error) {
        console.error('Error refreshing news:', error);
        throw error;
    }
};

export const deleteAllNews = async () => {
    try {
        const response = await api.delete('/admin/news/all');
        return response.data;
    } catch (error) {
        console.error('Error deleting all news:', error);
        throw error;
    }
};

export const deleteNewsByCategory = async (category) => {
    try {
        const response = await api.delete(`/admin/news/category/${category}`);
        return response.data;
    } catch (error) {
        console.error('Error deleting news by category:', error);
        throw error;
    }
};