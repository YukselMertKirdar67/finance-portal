import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Tüm haberleri getir (SAYFALAMA EKLENDİ)
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

// Kategoriye göre haberleri getir (sayfalı)
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

// ID'ye göre haber getir
export const getNewsById = async (id) => {
    try {
        const response = await api.get(`/news/${id}`);
        return response.data;
    } catch (error) {
        console.error('Error fetching news by ID:', error);
        throw error;
    }
};

// Kategorileri getir
export const getCategories = async () => {
    try {
        const response = await api.get('/admin/news/categories');
        return response.data.categories;
    } catch (error) {
        console.error('Error fetching categories:', error);
        throw error;
    }
};

// Admin: Haberleri çek
export const fetchNewsFromAPI = async () => {
    try {
        const response = await api.post('/admin/news/fetch');
        return response.data;
    } catch (error) {
        console.error('Error fetching news from API:', error);
        throw error;
    }
};

export default api;