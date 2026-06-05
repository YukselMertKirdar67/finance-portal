import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Briefcase, TrendingUp, TrendingDown, Trash2, RefreshCw, Search, CheckCircle, XCircle } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../UI/Card';
import { Button } from '../UI/Button';
import { getAllPortfoliosAdmin, getSystemStatistics, forceDeletePortfolio } from '../../API/adminPortfolioApi';

export default function AdminPortfolioPage() {
    const { t } = useTranslation();

    const [portfolios, setPortfolios] = useState([]);
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [userIdFilter, setUserIdFilter] = useState('');
    const [error, setError] = useState(null);
    const [deleteModal, setDeleteModal] = useState({ isOpen: false, id: null, name: null });

    useEffect(() => { loadData(); }, []);

    const loadData = async () => {
        setLoading(true);
        setError(null);
        try {
            const [portfoliosData, statsData] = await Promise.all([
                getAllPortfoliosAdmin(),
                getSystemStatistics()
            ]);
            setPortfolios(portfoliosData);
            setStats(statsData);
        } catch (err) {
            setError(t('admin.portfolio.loadError'));
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const handleRefresh = async () => {
        setRefreshing(true);
        await loadData();
        setRefreshing(false);
    };

    const handleSearch = async () => {
        setLoading(true);
        try {
            const data = await getAllPortfoliosAdmin(userIdFilter || null);
            setPortfolios(data);
        } catch {
            setError(t('admin.portfolio.searchError'));
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = (id, name) => {
        setDeleteModal({ isOpen: true, id, name });
    };

    const handleConfirmDelete = async () => {
        try {
            await forceDeletePortfolio(deleteModal.id);
            setPortfolios(prev => prev.filter(p => p.id !== deleteModal.id));
        } catch {
            alert(t('admin.portfolio.deleteError'));
        } finally {
            setDeleteModal({ isOpen: false, id: null, name: null });
        }
    };

    const formatCurrency = (value, currency = 'TRY') => {
        if (!value && value !== 0) return '-';
        const sym = { TRY: '₺', USD: '$', EUR: '€', GBP: '£' }[currency] || currency;
        return `${sym}${parseFloat(value).toLocaleString('tr-TR', { minimumFractionDigits: 2 })}`;
    };

    if (loading) return (
        <div className="p-8 flex items-center justify-center h-screen">
            <div className="text-center">
                <div className="animate-spin rounded-full h-16 w-16 border-b-4 border-blue-600 mx-auto mb-4"></div>
                <p className="text-gray-600">{t('common.loading')}</p>
            </div>
        </div>
    );

    return (
        <div className="p-8">

            {/* Header */}
            <div className="mb-8 flex items-center justify-between">
                <div>
                    <h1 className="text-3xl font-bold mb-2">{t('admin.portfolio.title')}</h1>
                    <p className="text-gray-600">{t('admin.portfolio.subtitle')}</p>
                </div>
                <Button variant="outline" onClick={handleRefresh} disabled={refreshing}>
                    <RefreshCw className={`w-5 h-5 mr-2 ${refreshing ? 'animate-spin' : ''}`} />
                    {t('common.refresh')}
                </Button>
            </div>

            {/* Error */}
            {error && (
                <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700">{error}</div>
            )}

            {/* Stats */}
            {stats && (
                <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
                    <Card>
                        <CardHeader className="pb-2">
                            <CardTitle className="text-sm text-gray-600 flex items-center gap-2">
                                <Briefcase className="w-4 h-4" />
                                {t('admin.portfolio.totalPortfolios')}
                            </CardTitle>
                        </CardHeader>
                        <CardContent>
                            <p className="text-3xl font-bold text-gray-900">{stats.totalPortfolios}</p>
                        </CardContent>
                    </Card>
                    <Card>
                        <CardHeader className="pb-2">
                            <CardTitle className="text-sm text-gray-600 flex items-center gap-2">
                                <CheckCircle className="w-4 h-4" />
                                {t('admin.portfolio.activePortfolios')}
                            </CardTitle>
                        </CardHeader>
                        <CardContent>
                            <p className="text-3xl font-bold text-green-600">{stats.activePortfolios}</p>
                        </CardContent>
                    </Card>
                    <Card>
                        <CardHeader className="pb-2">
                            <CardTitle className="text-sm text-gray-600 flex items-center gap-2">
                                <XCircle className="w-4 h-4" />
                                {t('admin.portfolio.inactivePortfolios')}
                            </CardTitle>
                        </CardHeader>
                        <CardContent>
                            <p className="text-3xl font-bold text-gray-500">{stats.inactivePortfolios}</p>
                        </CardContent>
                    </Card>
                    <Card>
                        <CardHeader className="pb-2">
                            <CardTitle className="text-sm text-gray-600 flex items-center gap-2">
                                <TrendingUp className="w-4 h-4" />
                                {t('admin.portfolio.totalValueTRY')}
                            </CardTitle>
                        </CardHeader>
                        <CardContent>
                            <p className="text-2xl font-bold text-gray-900">{formatCurrency(stats.totalValueTRY)}</p>
                        </CardContent>
                    </Card>
                </div>
            )}

            {/* Filter */}
            <div className="mb-6 flex gap-3 max-w-lg">
                <div className="relative flex-1">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                    <input
                        type="text"
                        placeholder={t('admin.portfolio.filterPlaceholder')}
                        value={userIdFilter}
                        onChange={(e) => setUserIdFilter(e.target.value)}
                        className="w-full pl-10 pr-4 py-2.5 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                        onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
                    />
                </div>
                <Button onClick={handleSearch}>{t('common.search')}</Button>
                {userIdFilter && (
                    <Button variant="outline" onClick={() => { setUserIdFilter(''); loadData(); }}>
                        {t('common.clear')}
                    </Button>
                )}
            </div>

            {/* Table */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('admin.portfolio.portfolios')} ({portfolios.length})</CardTitle>
                </CardHeader>
                <CardContent className="p-0">
                    <div className="overflow-x-auto">
                        <table className="w-full">
                            <thead className="bg-gray-50 border-b border-gray-200">
                            <tr>
                                <th className="text-left py-4 px-6 text-sm font-medium text-gray-600">{t('portfolio.title')}</th>
                                <th className="text-left py-4 px-6 text-sm font-medium text-gray-600">{t('admin.portfolio.userId')}</th>
                                <th className="text-left py-4 px-6 text-sm font-medium text-gray-600">{t('portfolioList.typeLabel')}</th>
                                <th className="text-right py-4 px-6 text-sm font-medium text-gray-600">{t('portfolio.totalValue')}</th>
                                <th className="text-right py-4 px-6 text-sm font-medium text-gray-600">{t('portfolio.unrealizedPnL')}</th>
                                <th className="text-center py-4 px-6 text-sm font-medium text-gray-600">{t('admin.users.colStatus')}</th>
                                <th className="text-center py-4 px-6 text-sm font-medium text-gray-600">{t('admin.users.colActions')}</th>
                            </tr>
                            </thead>
                            <tbody>
                            {portfolios.length === 0 ? (
                                <tr>
                                    <td colSpan={7} className="text-center py-12 text-gray-500">
                                        {t('common.noData')}
                                    </td>
                                </tr>
                            ) : (
                                portfolios.map((portfolio) => {
                                    const pnl = parseFloat(portfolio.unrealizedPnL || 0);
                                    const isPositive = pnl > 0;
                                    const isNeutral = pnl === 0;

                                    return (
                                        <tr key={portfolio.id} className="border-b border-gray-100 hover:bg-gray-50">
                                            <td className="py-4 px-6">
                                                <p className="font-medium text-gray-900">{portfolio.name}</p>
                                                <p className="text-xs text-gray-400">
                                                    {t('portfolioList.holdingCount', { count: portfolio.holdingCount || 0 })}
                                                </p>
                                            </td>
                                            <td className="py-4 px-6">
                                                    <span
                                                        className="text-xs font-mono bg-gray-100 text-gray-600 px-2 py-1 rounded cursor-pointer"
                                                        onClick={() => setUserIdFilter(portfolio.userId)}
                                                        title={portfolio.userId}
                                                    >
                                                        {portfolio.userId?.substring(0, 8)}...
                                                    </span>
                                            </td>
                                            <td className="py-4 px-6">
                                                <span className="text-sm text-gray-600">{portfolio.portfolioType}</span>
                                            </td>
                                            <td className="py-4 px-6 text-right">
                                                    <span className="font-semibold text-gray-900">
                                                        {formatCurrency(portfolio.totalValue, portfolio.currency)}
                                                    </span>
                                            </td>
                                            <td className="py-4 px-6 text-right">
                                                <div className={`flex items-center justify-end gap-1 ${
                                                    isNeutral ? 'text-gray-500' : isPositive ? 'text-green-600' : 'text-red-600'
                                                }`}>
                                                    {isNeutral ? <span>—</span> :
                                                        isPositive ? <TrendingUp className="w-4 h-4" /> : <TrendingDown className="w-4 h-4" />}
                                                    <span className="font-medium">
                                                            {isPositive ? '+' : ''}{formatCurrency(portfolio.unrealizedPnL, portfolio.currency)}
                                                        </span>
                                                </div>
                                            </td>
                                            <td className="py-4 px-6 text-center">
                                                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                                                        portfolio.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'
                                                    }`}>
                                                        {portfolio.active ? t('profile.active') : t('portfolio.passive')}
                                                    </span>
                                            </td>
                                            <td className="py-4 px-6 text-center">
                                                <Button
                                                    variant="ghost"
                                                    size="sm"
                                                    onClick={() => handleDelete(portfolio.id, portfolio.name)}
                                                    className="text-red-400 hover:text-red-600 hover:bg-red-50"
                                                >
                                                    <Trash2 className="w-4 h-4" />
                                                </Button>
                                            </td>
                                        </tr>
                                    );
                                })
                            )}
                            </tbody>
                        </table>
                    </div>
                </CardContent>
            </Card>
            {deleteModal.isOpen && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-xl shadow-xl max-w-md w-full p-6">
                        <div className="flex items-center gap-3 mb-4">
                            <div className="w-10 h-10 bg-red-100 rounded-full flex items-center justify-center flex-shrink-0">
                                <span className="text-red-600 text-xl">⚠️</span>
                            </div>
                            <h3 className="text-lg font-bold text-gray-900">{t('admin.portfolio.title')}</h3>
                        </div>
                        <p className="text-gray-600 mb-6">
                            {t('admin.portfolio.deleteConfirm', { name: deleteModal.name })}
                        </p>
                        <div className="flex gap-3">
                            <Button variant="outline" className="flex-1"
                                    onClick={() => setDeleteModal({ isOpen: false, id: null, name: null })}>
                                {t('common.cancel')}
                            </Button>
                            <Button className="flex-1 bg-red-600 hover:bg-red-700 text-white"
                                    onClick={handleConfirmDelete}>
                                {t('common.delete')}
                            </Button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}