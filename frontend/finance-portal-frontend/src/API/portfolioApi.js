import api from './instrumentsApi';

// Tüm portfolioları getir
export const getAllPortfolios = async () => {
    try {
        const response = await api.get('/portfolios');
        return response.data;
    } catch (error) {
        console.error('Error fetching portfolios:', error);
        throw error;
    }
};

// Portfolio detayı getir
export const getPortfolioDetail = async (id) => {
    try {
        const response = await api.get(`/portfolios/${id}/detail`);
        return response.data;
    } catch (error) {
        console.error('Error fetching portfolio detail:', error);
        throw error;
    }
};

// Portfolio oluştur
export const createPortfolio = async (data) => {
    try {
        const response = await api.post('/portfolios', data);
        return response.data;
    } catch (error) {
        console.error('Error creating portfolio:', error);
        throw error;
    }
};

// Portfolio güncelle
export const updatePortfolio = async (id, data) => {
    try {
        const response = await api.put(`/portfolios/${id}`, data);
        return response.data;
    } catch (error) {
        console.error('Error updating portfolio:', error);
        throw error;
    }
};


export const hardDeletePortfolio = async (id) => {
    try {
        await api.delete(`/portfolios/${id}/hard`);
    } catch (error) {
        console.error('Error hard deleting portfolio:', error);
        throw error;
    }
};

// Transaction oluştur
export const createTransaction = async (portfolioId, data) => {
    try {
        const response = await api.post(`/portfolios/${portfolioId}/transactions`, data);
        return response.data;
    } catch (error) {
        console.error('Error creating transaction:', error);
        throw error;
    }
};

// Portfolio özeti getir (Dashboard için)
export const getPortfolioSummary = async () => {
    try {
        const response = await api.get('/portfolios/summary');
        return response.data;
    } catch (error) {
        console.error('Error fetching portfolio summary:', error);
        throw error;
    }
};

// Transaction'ları getir (sayfalı)
export const getTransactions = async (portfolioId, page = 0, size = 50) => {
    try {
        const response = await api.get(`/portfolios/${portfolioId}/transactions`, {
            params: { page, size }
        });
        return response.data;
    } catch (error) {
        console.error('Error fetching transactions:', error);
        throw error;
    }
};

export const deleteTransaction = async (portfolioId, transactionId) => {
    try {
        await api.delete(`/portfolios/${portfolioId}/transactions/${transactionId}`);
    } catch (error) {
        console.error('Error deleting transaction:', error);
        throw error;
    }
};

// Holdings getir
export const getHoldings = async (portfolioId) => {
    try {
        const response = await api.get(`/portfolios/${portfolioId}/holdings`);
        return response.data;
    } catch (error) {
        console.error('Error fetching holdings:', error);
        throw error;
    }
};

// Asset allocation getir
export const getAssetAllocation = async (portfolioId) => {
    try {
        const response = await api.get(`/portfolios/${portfolioId}/holdings/asset-allocation`);
        return response.data;
    } catch (error) {
        console.error('Error fetching asset allocation:', error);
        throw error;
    }
};

export const getPortfolioPerformance = async (portfolioId, days = 30) => {
    try {
        const response = await api.get(`/portfolios/${portfolioId}/performance`, {
            params: { days }  // Query parameter: ?days=30
        });
        return response.data;
    } catch (error) {
        console.error('Error fetching portfolio performance:', error);
        throw error;
    }
};

export default api;