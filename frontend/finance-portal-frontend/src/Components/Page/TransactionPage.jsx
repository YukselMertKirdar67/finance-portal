import React, { useState, useEffect } from 'react';
import { ArrowUpRight, ArrowDownLeft, Calendar, Download, Search, Trash2 } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../UI/Card';
import { Button } from '../UI/Button';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { getTransactions, getPortfolioDetail, deleteTransaction } from '../../API/portfolioApi';

export default function TransactionPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const { t, i18n } = useTranslation();
    const PORTFOLIO_ID = parseInt(id);

    const [portfolio, setPortfolio] = useState(null);
    const [transactions, setTransactions] = useState([]);
    const [filteredTransactions, setFilteredTransactions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [showDeleteModal, setShowDeleteModal] = useState(false);
    const [deletingTransactionId, setDeletingTransactionId] = useState(null);
    const [deleting, setDeleting] = useState(false);

    const [typeFilter, setTypeFilter] = useState('ALL');
    const [searchTerm, setSearchTerm] = useState('');
    const [dateFilter, setDateFilter] = useState('ALL');

    useEffect(() => { loadData(); }, []);
    useEffect(() => { applyFilters(); }, [transactions, typeFilter, searchTerm, dateFilter]);

    const loadData = async () => {
        try {
            setLoading(true);
            setError(null);
            const [portfolioData, transactionsResponse] = await Promise.all([
                getPortfolioDetail(PORTFOLIO_ID),
                getTransactions(PORTFOLIO_ID, 0, 100)
            ]);
            setPortfolio(portfolioData);
            const transactionsList = transactionsResponse.content || transactionsResponse || [];
            setTransactions(transactionsList);
        } catch (err) {
            console.error('Error loading data:', err);
            setError(err.response?.data?.message || t('transaction.loadError'));
        } finally {
            setLoading(false);
        }
    };

    const applyFilters = () => {
        let filtered = [...transactions];
        if (typeFilter !== 'ALL') filtered = filtered.filter(tx => tx.transactionType === typeFilter);
        if (searchTerm) {
            filtered = filtered.filter(tx =>
                tx.instrumentSymbol?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                tx.instrumentName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                tx.notes?.toLowerCase().includes(searchTerm.toLowerCase())
            );
        }
        if (dateFilter !== 'ALL') {
            const now = new Date();
            filtered = filtered.filter(tx => {
                const txDate = new Date(tx.transactionDate);
                if (dateFilter === 'TODAY') return txDate.toDateString() === now.toDateString();
                if (dateFilter === 'WEEK') return txDate >= new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
                if (dateFilter === 'MONTH') return txDate >= new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
                return true;
            });
        }
        setFilteredTransactions(filtered);
    };

    const calculateStats = () => {
        const buyTransactions = transactions.filter(tx => tx.transactionType === 'BUY');
        const sellTransactions = transactions.filter(tx => tx.transactionType === 'SELL');
        return {
            totalBuy: buyTransactions.reduce((sum, tx) => sum + (tx.totalAmount || tx.netAmount || 0), 0),
            totalSell: sellTransactions.reduce((sum, tx) => sum + (tx.totalAmount || tx.netAmount || 0), 0),
            totalCommission: transactions.reduce((sum, tx) => sum + (tx.commission || 0), 0),
            totalTax: transactions.reduce((sum, tx) => sum + (tx.tax || 0), 0),
            buyCount: buyTransactions.length,
            sellCount: sellTransactions.length
        };
    };

    const formatDate = (dateString) => {
        const locale = i18n.language === 'en' ? 'en-US' : 'tr-TR';
        return new Date(dateString).toLocaleDateString(locale, {
            year: 'numeric', month: 'short', day: 'numeric',
            hour: '2-digit', minute: '2-digit'
        });
    };

    const handleDelete = async () => {
        try {
            setDeleting(true);
            await deleteTransaction(PORTFOLIO_ID, deletingTransactionId);
            setTransactions(prev => prev.filter(tx => tx.id !== deletingTransactionId));
            setShowDeleteModal(false);
            setDeletingTransactionId(null);
        } catch {
            alert(t('transaction.deleteError'));
        } finally { setDeleting(false); }
    };

    if (loading) {
        return (
            <div className="p-8 flex items-center justify-center h-screen">
                <div className="text-center">
                    <div className="animate-spin rounded-full h-16 w-16 border-b-4 border-blue-600 mx-auto mb-4"></div>
                    <p className="text-lg text-gray-700 font-medium">{t('transaction.loading')}</p>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="p-8">
                <div className="bg-red-50 border-2 border-red-200 rounded-lg p-8 text-center max-w-md mx-auto">
                    <div className="text-red-600 text-5xl mb-4">⚠️</div>
                    <p className="text-red-800 font-semibold text-xl mb-2">{t('common.error')}</p>
                    <p className="text-red-600 mb-6">{error}</p>
                    <Button className="bg-red-600 hover:bg-red-700" onClick={loadData}>
                        {t('common.refresh')}
                    </Button>
                </div>
            </div>
        );
    }

    const stats = calculateStats();

    return (
        <div className="p-8">

            {/* Header */}
            <div className="mb-6 flex items-center justify-between">
                <div>
                    <h1 className="text-3xl font-bold mb-2">{t('transaction.history')}</h1>
                    <p className="text-gray-600">{portfolio?.name || t('portfolio.title')}</p>
                </div>
                <Button variant="outline" onClick={() => navigate(`/portfolios/${PORTFOLIO_ID}`)}>
                    ← {t('transaction.backToPortfolio')}
                </Button>
            </div>

            {/* Stats Cards */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-6">
                <Card>
                    <CardHeader className="pb-2">
                        <CardTitle className="text-sm text-gray-600">{t('transaction.totalBuy')}</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <p className="text-2xl font-bold text-green-600">
                            ₺{stats.totalBuy.toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                        </p>
                        <p className="text-xs text-gray-500 mt-1">
                            {t('transaction.transactionCount', { count: stats.buyCount })}
                        </p>
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader className="pb-2">
                        <CardTitle className="text-sm text-gray-600">{t('transaction.totalSell')}</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <p className="text-2xl font-bold text-red-600">
                            ₺{stats.totalSell.toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                        </p>
                        <p className="text-xs text-gray-500 mt-1">
                            {t('transaction.transactionCount', { count: stats.sellCount })}
                        </p>
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader className="pb-2">
                        <CardTitle className="text-sm text-gray-600">{t('transaction.totalCommission')}</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <p className="text-2xl font-bold text-gray-900">
                            ₺{stats.totalCommission.toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                        </p>
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader className="pb-2">
                        <CardTitle className="text-sm text-gray-600">{t('transaction.totalTax')}</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <p className="text-2xl font-bold text-gray-900">
                            ₺{stats.totalTax.toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                        </p>
                    </CardContent>
                </Card>
            </div>

            {/* Filters */}
            <Card className="mb-6">
                <CardContent className="pt-6">
                    <div className="flex flex-wrap gap-4">

                        {/* Search */}
                        <div className="flex-1 min-w-[250px]">
                            <div className="relative">
                                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
                                <input
                                    type="text"
                                    className="w-full pl-10 pr-4 py-2 border-2 border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#0066FF] focus:border-transparent"
                                    placeholder={t('transaction.searchPlaceholder')}
                                    value={searchTerm}
                                    onChange={(e) => setSearchTerm(e.target.value)}
                                />
                            </div>
                        </div>

                        {/* Type Filter */}
                        <div className="flex gap-2">
                            {[
                                { value: 'ALL', label: t('transaction.all') },
                                { value: 'BUY', label: t('transaction.buy') },
                                { value: 'SELL', label: t('transaction.sell') }
                            ].map((type) => (
                                <Button
                                    key={type.value}
                                    variant={typeFilter === type.value ? 'default' : 'outline'}
                                    onClick={() => setTypeFilter(type.value)}
                                    className={typeFilter === type.value
                                        ? type.value === 'BUY' ? 'bg-green-600 hover:bg-green-700'
                                            : type.value === 'SELL' ? 'bg-red-600 hover:bg-red-700'
                                                : 'bg-[#0066FF] hover:bg-[#0052CC]'
                                        : ''}
                                >
                                    {type.label}
                                </Button>
                            ))}
                        </div>

                        {/* Date Filter */}
                        <div className="flex gap-2">
                            {[
                                { value: 'ALL', label: t('transaction.allTime') },
                                { value: 'TODAY', label: t('transaction.today') },
                                { value: 'WEEK', label: t('transaction.thisWeek') },
                                { value: 'MONTH', label: t('transaction.thisMonth') }
                            ].map((filter) => (
                                <Button
                                    key={filter.value}
                                    variant={dateFilter === filter.value ? 'default' : 'outline'}
                                    size="sm"
                                    onClick={() => setDateFilter(filter.value)}
                                    className={dateFilter === filter.value ? 'bg-[#0066FF] hover:bg-[#0052CC]' : ''}
                                >
                                    <Calendar className="w-4 h-4 mr-2" />
                                    {filter.label}
                                </Button>
                            ))}
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Transactions Table */}
            <Card>
                <CardHeader>
                    <div className="flex items-center justify-between">
                        <CardTitle>
                            {t('transaction.transactions')} ({filteredTransactions.length})
                        </CardTitle>
                        <Button variant="outline" size="sm">
                            <Download className="w-4 h-4 mr-2" />
                            {t('profile.exportData')}
                        </Button>
                    </div>
                </CardHeader>
                <CardContent>
                    <div className="overflow-x-auto">
                        <table className="w-full">
                            <thead>
                            <tr className="border-b border-gray-200">
                                <th className="text-left py-3 px-4 text-sm font-semibold text-gray-600">{t('transaction.date')}</th>
                                <th className="text-left py-3 px-4 text-sm font-semibold text-gray-600">{t('transaction.type')}</th>
                                <th className="text-left py-3 px-4 text-sm font-semibold text-gray-600">{t('holding.name')}</th>
                                <th className="text-right py-3 px-4 text-sm font-semibold text-gray-600">{t('transaction.quantity')}</th>
                                <th className="text-right py-3 px-4 text-sm font-semibold text-gray-600">{t('transaction.price')}</th>
                                <th className="text-right py-3 px-4 text-sm font-semibold text-gray-600">{t('transaction.commission')}</th>
                                <th className="text-right py-3 px-4 text-sm font-semibold text-gray-600">{t('transaction.tax')}</th>
                                <th className="text-right py-3 px-4 text-sm font-semibold text-gray-600">{t('transaction.total')}</th>
                                <th className="text-left py-3 px-4 text-sm font-semibold text-gray-600">{t('transaction.note')}</th>
                                <th className="text-center py-3 px-4 text-sm font-semibold text-gray-600">{t('common.edit')}</th>
                            </tr>
                            </thead>
                            <tbody>
                            {filteredTransactions.length === 0 ? (
                                <tr>
                                    <td colSpan={10} className="text-center py-12">
                                        <div className="text-gray-400 text-6xl mb-4">📋</div>
                                        <p className="text-gray-500 text-lg font-medium">{t('transaction.notFound')}</p>
                                        <p className="text-gray-400 text-sm mt-2">{t('transaction.changeFilters')}</p>
                                    </td>
                                </tr>
                            ) : (
                                filteredTransactions.map((tx) => (
                                    <tr key={tx.id} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                                        <td className="py-4 px-4">
                                            <p className="text-sm text-gray-900">{formatDate(tx.transactionDate)}</p>
                                        </td>
                                        <td className="py-4 px-4">
                                            {tx.transactionType === 'BUY' ? (
                                                <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-medium bg-green-100 text-green-700">
                                                        <ArrowDownLeft className="w-3 h-3" />
                                                    {t('transaction.buy').toUpperCase()}
                                                    </span>
                                            ) : (
                                                <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-medium bg-red-100 text-red-700">
                                                        <ArrowUpRight className="w-3 h-3" />
                                                    {t('transaction.sell').toUpperCase()}
                                                    </span>
                                            )}
                                        </td>
                                        <td className="py-4 px-4">
                                            <p className="font-semibold text-gray-900">{tx.instrumentSymbol}</p>
                                            <p className="text-xs text-gray-500 mt-0.5">{tx.instrumentName}</p>
                                        </td>
                                        <td className="py-4 px-4 text-right">
                                                <span className="text-sm font-medium text-gray-900">
                                                    {tx.quantity?.toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                                                </span>
                                        </td>
                                        <td className="py-4 px-4 text-right">
                                                <span className="text-sm text-gray-700">
                                                    ₺{tx.price?.toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                                                </span>
                                        </td>
                                        <td className="py-4 px-4 text-right">
                                                <span className="text-sm text-gray-700">
                                                    {tx.commission ? `₺${tx.commission.toLocaleString('tr-TR', { minimumFractionDigits: 2 })}` : '-'}
                                                </span>
                                        </td>
                                        <td className="py-4 px-4 text-right">
                                                <span className="text-sm text-gray-700">
                                                    {tx.tax ? `₺${tx.tax.toLocaleString('tr-TR', { minimumFractionDigits: 2 })}` : '-'}
                                                </span>
                                        </td>
                                        <td className="py-4 px-4 text-right">
                                                <span className={`font-semibold ${tx.transactionType === 'BUY' ? 'text-green-600' : 'text-red-600'}`}>
                                                    ₺{(tx.netAmount || tx.totalAmount || 0).toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                                                </span>
                                        </td>
                                        <td className="py-4 px-4">
                                            <p className="text-xs text-gray-500 max-w-[150px] truncate" title={tx.notes}>
                                                {tx.notes || '-'}
                                            </p>
                                        </td>
                                        <td className="py-4 px-4 text-center">
                                            <button
                                                onClick={() => { setDeletingTransactionId(tx.id); setShowDeleteModal(true); }}
                                                className="p-2 text-red-500 hover:text-red-700 hover:bg-red-50 rounded-lg transition-colors"
                                                title={t('transaction.delete')}
                                            >
                                                <Trash2 className="w-4 h-4" />
                                            </button>
                                        </td>
                                    </tr>
                                ))
                            )}
                            </tbody>
                        </table>
                    </div>
                </CardContent>
            </Card>

            {/* Delete Modal */}
            {showDeleteModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-lg shadow-2xl w-full max-w-md">
                        <div className="p-6 border-b border-gray-200">
                            <h2 className="text-2xl font-bold text-gray-900">{t('transaction.deleteTitle')}</h2>
                        </div>
                        <div className="p-6">
                            <div className="flex items-center gap-4 p-4 bg-red-50 border-2 border-red-200 rounded-lg">
                                <div className="text-red-500 text-4xl">⚠️</div>
                                <div>
                                    <p className="font-semibold text-red-800">{t('transaction.deleteConfirm')}</p>
                                    <p className="text-sm text-red-600 mt-1">{t('transaction.deleteDesc')}</p>
                                </div>
                            </div>
                        </div>
                        <div className="p-6 border-t border-gray-200 flex gap-3">
                            <Button
                                variant="outline"
                                className="flex-1 h-12 font-semibold border-2"
                                onClick={() => { setShowDeleteModal(false); setDeletingTransactionId(null); }}
                                disabled={deleting}
                            >
                                {t('common.cancel')}
                            </Button>
                            <Button
                                className="flex-1 h-12 font-semibold bg-red-600 hover:bg-red-700 text-white"
                                onClick={handleDelete}
                                disabled={deleting}
                            >
                                {deleting
                                    ? <><span className="animate-spin mr-2">⟳</span>{t('transaction.deleting')}</>
                                    : t('transaction.deleteConfirmBtn')}
                            </Button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}