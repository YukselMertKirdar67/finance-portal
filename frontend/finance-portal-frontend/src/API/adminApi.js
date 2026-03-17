import api from './instrumentsApi';

/**
 * Admin dashboard istatistikleri
 */
export const getAdminStats = async () => {
    try {
        const response = await api.get('/admin/stats');
        return response.data;
    } catch (error) {
        console.error('Error fetching admin stats:', error);
        throw error;
    }
};

/**
 * Tüm kullanıcıları getir
 */
export const getAllUsers = async () => {
    try {
        const response = await api.get('/admin/users');
        return response.data;
    } catch (error) {
        console.error('Error fetching users:', error);
        throw error;
    }
};

/**
 * Kullanıcı ara
 */
export const searchUsers = async (query) => {
    try {
        const response = await api.get('/admin/users/search', {
            params: { query }
        });
        return response.data;
    } catch (error) {
        console.error('Error searching users:', error);
        throw error;
    }
};

/**
 * Kullanıcıyı devre dışı bırak
 */
export const disableUser = async (userId) => {
    try {
        const response = await api.put(`/admin/users/${userId}/disable`);
        return response.data;
    } catch (error) {
        console.error('Error disabling user:', error);
        throw error;
    }
};

/**
 * Kullanıcıyı aktif et
 */
export const enableUser = async (userId) => {
    try {
        const response = await api.put(`/admin/users/${userId}/enable`);
        return response.data;
    } catch (error) {
        console.error('Error enabling user:', error);
        throw error;
    }
};

/**
 * Kullanıcıyı ID ye göre getir
 */
export const getUserDetail = async (userId) => {
    try {
        const response = await api.get(`/admin/users/${userId}`);
        return response.data;
    } catch (error) {
        console.error('Error fetching user detail:', error);
        throw error;
    }
};

/**
 * Kullanıcıya rol ata
 */
export const assignRole = async (userId, roleName) => {
    try {
        const response = await api.post(`/admin/users/${userId}/assign-role`, {
            roleName
        });
        return response.data;
    } catch (error) {
        console.error('Error assigning role:', error);
        throw error;
    }
};

/**
 * Kullanıcının rolünü kaldır
 */
export const removeRole = async (userId, roleName) => {
    try {
        const response = await api.post(`/admin/users/${userId}/remove-role`, {
            roleName
        });
        return response.data;
    } catch (error) {
        console.error('Error removing role:', error);
        throw error;
    }
};

