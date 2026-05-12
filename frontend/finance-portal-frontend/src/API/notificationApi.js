import api from './instrumentsApi';

/**
 * Bildirimleri getirir
 */
export const getNotifications = async (page = 0, size = 20) => {
    const response = await api.get('/notifications', { params: { page, size } });
    return response.data;
};

/**
 * Okunmamaış bildirimleri getirir
 */
export const getUnreadNotifications = async () => {
    const response = await api.get('/notifications/unread');
    return response.data;
};

/**
 * Okunmamış bildirim sayısını getirir
 */
export const getUnreadCount = async () => {
    const response = await api.get('/notifications/unread-count');
    return response.data;
};

/**
 * Bütün bildirimleri okunmuş yapar
 */
export const markAllAsRead = async () => {
    const response = await api.put('/notifications/mark-all-read');
    return response.data;
};

/**
 * Bildirimi okunmuş yapar
 */
export const markAsRead = async (id) => {
    const response = await api.put(`/notifications/${id}/read`);
    return response.data;
};