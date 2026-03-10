import api from './instrumentsApi';

export const getHomePageData = async () => {
    const response = await api.get('/home');
    return response.data;
};