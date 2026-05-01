import api from './instrumentsApi';

export const getNotifications = async (page = 0, size = 20) => {
    const response = await api.get('/notifications', { params: { page, size } });
    return response.data;
};

export const getUnreadNotifications = async () => {
    const response = await api.get('/notifications/unread');
    return response.data;
};

export const getUnreadCount = async () => {
    const response = await api.get('/notifications/unread-count');
    return response.data;
};

export const markAllAsRead = async () => {
    const response = await api.put('/notifications/mark-all-read');
    return response.data;
};

export const markAsRead = async (id) => {
    const response = await api.put(`/notifications/${id}/read`);
    return response.data;
};