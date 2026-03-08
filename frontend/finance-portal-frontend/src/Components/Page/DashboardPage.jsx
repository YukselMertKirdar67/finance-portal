import React, { useState, useEffect } from 'react';
import { TrendingUp, TrendingDown, Wallet, ArrowRight, Plus, RefreshCw, DollarSign, PieChart as PieChartIcon, Activity } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../UI/Card';
import { Button } from '../UI/Button';
import { useNavigate } from 'react-router-dom';
import {
    PieChart,
    Pie,
    Cell,
    ResponsiveContainer,
    Tooltip as RechartsTooltip,
    BarChart,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid
} from 'recharts';

import { getAllPortfolios, getTransactions } from '../../API/portfolioApi';

export default function DashboardPage() {
    const navigate = useNavigate();

    const [portfolios, setPortfolios] = useState([]);
    const [recentTransactions, setRecentTransactions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        loadDashboardData();
    }, []);

    const loadDashboardData = async () => {
        try {
            setLoading(true);
            setError(null);

            // Get all portfolios
            const portfoliosData = await getAllPortfolios();
            setPortfolios(portfoliosData || []);

            // Get recent transactions from all portfolios (first portfolio only for demo)
            if (portfoliosData && portfoliosData.length > 0) {
                try {
                    const txResponse = await getTransactions(portfoliosData[0].id, 0, 10);
                    const txList = txResponse.content || txResponse || [];
                    setRecentTransactions(txList.slice(0, 5)); // Son 5 işlem
                } catch (err) {
                    console.warn('Could not load transactions:', err);
                    setRecentTransactions([]);
                }
            }

        } catch (err) {
            console.error('Error loading dashboard:', err);
            setError(err.response?.data?.message || 'Dashboard yüklenirken hata oluştu');
        } finally {
            setLoading(false);
        }
    };

    const handleRefresh = async () => {
        setRefreshing(true);
        await loadDashboardData();
        setRefreshing(false);
    };

    // Calculate overall stats
    const calculateOverallStats = () => {
        const totalValue = portfolios.reduce((sum, p) => sum + (p.totalValue || 0), 0);
        const totalInvested = portfolios.reduce((sum, p) => sum + (p.totalInvested || 0), 0);
        const totalPnL = portfolios.reduce((sum, p) => sum + (p.unrealizedPnL || 0), 0);
        const totalHoldings = portfolios.reduce((sum, p) => sum + (p.holdingCount || 0), 0);

        const pnlPercent = totalInvested > 0 ? ((totalPnL / totalInvested) * 100) : 0;

        return {
            totalValue,
            totalInvested,
            totalPnL,
            pnlPercent,
            totalHoldings,
            portfolioCount: portfolios.length,
            activePortfolios: portfolios.filter(p => p.active).length
        };
    };

    // Get top/bottom performers
    const getPerformers = () => {
        const sorted = [...portfolios].sort((a, b) => (b.pnlPercent || 0) - (a.pnlPercent || 0));
        return {
            topPerformer: sorted[0] || null,
            bottomPerformer: sorted[sorted.length - 1] || null
        };
    };

    // Asset allocation across all portfolios
    const getAssetAllocation = () => {
        const allocation = {};

        portfolios.forEach(portfolio => {
            const type = portfolio.portfolioType || 'PERSONAL';
            if (!allocation[type]) {
                allocation[type] = {
                    type,
                    value: 0,
                    count: 0
                };
            }
            allocation[type].value += portfolio.totalValue || 0;
            allocation[type].count += 1;
        });

        return Object.values(allocation);
    };

    const formatDate = (dateString) => {
        const date = new Date(dateString);
        return date.toLocaleDateString('tr-TR', {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    const typeColors = {
        'PERSONAL': '#3B82F6',
        'BUSINESS': '#8B5CF6',
        'RETIREMENT': '#10B981',
        'SAVINGS': '#F59E0B'
    };

    const typeLabels = {
        'PERSONAL': 'Bireysel',
        'BUSINESS': 'İş',
        'RETIREMENT': 'Emeklilik',
        'SAVINGS': 'Tasarruf'
    };

    if (loading) {
        return (
            <div className="p-8 flex items-center justify-center h-screen">
                <div className="text-center">
                    <div className="animate-spin rounded-full h-16 w-16 border-b-4 border-blue-600 mx-auto mb-4"></div>
                    <p className="text-lg text-gray-700 font-medium">Dashboard yükleniyor...</p>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="p-8">
                <div className="bg-red-50 border-2 border-red-200 rounded-lg p-8 text-center max-w-md mx-auto">
                    <div className="text-red-600 text-5xl mb-4">⚠️</div>
                    <p className="text-red-800 font-semibold text-xl mb-2">Hata Oluştu</p>
                    <p className="text-red-600 mb-6">{error}</p>
                    <Button className="bg-red-600 hover:bg-red-700" onClick={loadDashboardData}>
                        Tekrar Dene
                    </Button>
                </div>
            </div>
        );
    }

    const stats = calculateOverallStats();
    const performers = getPerformers();
    const assetAllocation = getAssetAllocation();

    const pieChartData = assetAllocation.map(item => ({
        name: typeLabels[item.type] || item.type,
        value: item.value,
        color: typeColors[item.type] || '#6B7280'
    }));

    const barChartData = portfolios.map(p => ({
        name: p.name.substring(0, 15) + (p.name.length > 15 ? '...' : ''),
        value: p.totalValue || 0,
        pnl: p.unrealizedPnL || 0
    }));

    return (
        <div className="p-8">
            {/* Header */}
            <div className="mb-8 flex items-center justify-between">
                <div>
                    <h1 className="text-3xl font-bold mb-2">Dashboard</h1>
                    <p className="text-gray-600">Portföylerinizin genel görünümü</p>
                </div>
                <div className="flex gap-3">
                    <Button
                        variant="outline"
                        onClick={handleRefresh}
                        disabled={refreshing}
                    >
                        <RefreshCw className={`w-5 h-5 mr-2 ${refreshing ? 'animate-spin' : ''}`} />
                        Yenile
                    </Button>
                    <Button
                        onClick={() => navigate('/portfolios')}
                        className="bg-[#0066FF] hover:bg-[#0052CC]"
                    >
                        <Plus className="w-5 h-5 mr-2" />
                        Yeni Portföy
                    </Button>
                </div>
            </div>

            {/* Empty State */}
            {portfolios.length === 0 ? (
                <div className="text-center py-16">
                    <div className="inline-flex items-center justify-center w-20 h-20 bg-blue-100 rounded-full mb-6">
                        <Wallet className="w-10 h-10 text-blue-600" />
                    </div>
                    <h2 className="text-2xl font-bold text-gray-900 mb-2">Henüz Portföyünüz Yok</h2>
                    <p className="text-gray-600 mb-6 max-w-md mx-auto">
                        İlk portföyünüzü oluşturarak yatırımlarınızı takip etmeye başlayın
                    </p>
                    <Button
                        onClick={() => navigate('/portfolios')}
                        size="lg"
                        className="bg-[#0066FF] hover:bg-[#0052CC]"
                    >
                        <Plus className="w-5 h-5 mr-2" />
                        İlk Portföyünüzü Oluşturun
                    </Button>
                </div>
            ) : (
                <>
                    {/* Main Stats */}
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                        <Card>
                            <CardHeader className="pb-2">
                                <CardTitle className="text-sm text-gray-600 flex items-center gap-2">
                                    <DollarSign className="w-4 h-4" />
                                    Toplam Net Değer
                                </CardTitle>
                            </CardHeader>
                            <CardContent>
                                <p className="text-3xl font-bold text-gray-900">
                                    ₺{stats.totalValue.toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                                </p>
                                <p className="text-xs text-gray-500 mt-1">
                                    {stats.portfolioCount} portföy
                                </p>
                            </CardContent>
                        </Card>

                        <Card>
                            <CardHeader className="pb-2">
                                <CardTitle className="text-sm text-gray-600 flex items-center gap-2">
                                    <Activity className="w-4 h-4" />
                                    Toplam Kar/Zarar
                                </CardTitle>
                            </CardHeader>
                            <CardContent>
                                <p className={`text-3xl font-bold flex items-center gap-2 ${
                                    stats.totalPnL >= 0 ? 'text-green-600' : 'text-red-600'
                                }`}>
                                    {stats.totalPnL >= 0 ? <TrendingUp className="w-6 h-6" /> : <TrendingDown className="w-6 h-6" />}
                                    {stats.totalPnL >= 0 ? '+' : ''}₺{Math.abs(stats.totalPnL).toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                                </p>
                                <p className={`text-xs mt-1 ${stats.pnlPercent >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                                    {stats.pnlPercent >= 0 ? '+' : ''}{stats.pnlPercent.toFixed(2)}%
                                </p>
                            </CardContent>
                        </Card>

                        <Card>
                            <CardHeader className="pb-2">
                                <CardTitle className="text-sm text-gray-600 flex items-center gap-2">
                                    <PieChartIcon className="w-4 h-4" />
                                    Toplam Yatırım
                                </CardTitle>
                            </CardHeader>
                            <CardContent>
                                <p className="text-3xl font-bold text-gray-900">
                                    ₺{stats.totalInvested.toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                                </p>
                                <p className="text-xs text-gray-500 mt-1">
                                    {stats.totalHoldings} varlık
                                </p>
                            </CardContent>
                        </Card>

                        <Card>
                            <CardHeader className="pb-2">
                                <CardTitle className="text-sm text-gray-600 flex items-center gap-2">
                                    <Wallet className="w-4 h-4" />
                                    Aktif Portföyler
                                </CardTitle>
                            </CardHeader>
                            <CardContent>
                                <p className="text-3xl font-bold text-gray-900">
                                    {stats.activePortfolios}
                                </p>
                                <p className="text-xs text-gray-500 mt-1">
                                    / {stats.portfolioCount} toplam
                                </p>
                            </CardContent>
                        </Card>
                    </div>

                    {/* Charts Row */}
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
                        {/* Portfolio Values Bar Chart */}
                        <Card>
                            <CardHeader>
                                <CardTitle>Portföy Değerleri</CardTitle>
                            </CardHeader>
                            <CardContent>
                                <div className="h-[300px]">
                                    <ResponsiveContainer width="100%" height="100%">
                                        <BarChart data={barChartData}>
                                            <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                                            <XAxis
                                                dataKey="name"
                                                stroke="#9ca3af"
                                                style={{ fontSize: '12px' }}
                                            />
                                            <YAxis
                                                stroke="#9ca3af"
                                                style={{ fontSize: '12px' }}
                                                tickFormatter={(value) => `₺${(value / 1000).toFixed(0)}k`}
                                            />
                                            <RechartsTooltip
                                                formatter={(value) => [`₺${value.toLocaleString('tr-TR', { minimumFractionDigits: 2 })}`, 'Değer']}
                                            />
                                            <Bar dataKey="value" fill="#0066FF" radius={[8, 8, 0, 0]} />
                                        </BarChart>
                                    </ResponsiveContainer>
                                </div>
                            </CardContent>
                        </Card>

                        {/* Asset Allocation Pie Chart */}
                        <Card>
                            <CardHeader>
                                <CardTitle>Portföy Tipi Dağılımı</CardTitle>
                            </CardHeader>
                            <CardContent>
                                <div className="h-[300px]">
                                    <ResponsiveContainer width="100%" height="100%">
                                        <PieChart>
                                            <Pie
                                                data={pieChartData}
                                                cx="50%"
                                                cy="50%"
                                                labelLine={false}
                                                label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                                                outerRadius={100}
                                                fill="#8884d8"
                                                dataKey="value"
                                            >
                                                {pieChartData.map((entry, index) => (
                                                    <Cell key={`cell-${index}`} fill={entry.color} />
                                                ))}
                                            </Pie>
                                            <RechartsTooltip
                                                formatter={(value) => `₺${value.toLocaleString('tr-TR', { minimumFractionDigits: 2 })}`}
                                            />
                                        </PieChart>
                                    </ResponsiveContainer>
                                </div>
                            </CardContent>
                        </Card>
                    </div>

                    {/* Performance & Transactions Row */}
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                        {/* Top/Bottom Performers */}
                        <Card>
                            <CardHeader>
                                <CardTitle>Performans</CardTitle>
                            </CardHeader>
                            <CardContent className="space-y-4">
                                {performers.topPerformer && (
                                    <div
                                        className="p-4 bg-green-50 border border-green-200 rounded-lg cursor-pointer hover:bg-green-100 transition-colors"
                                        onClick={() => navigate(`/portfolios/${performers.topPerformer.id}`)}
                                    >
                                        <div className="flex items-center justify-between mb-2">
                                            <p className="text-sm font-medium text-gray-600">🏆 En İyi Performans</p>
                                            <TrendingUp className="w-5 h-5 text-green-600" />
                                        </div>
                                        <p className="font-bold text-lg text-gray-900">{performers.topPerformer.name}</p>
                                        <div className="flex items-center justify-between mt-2">
                                            <p className="text-sm text-gray-600">
                                                ₺{performers.topPerformer.totalValue?.toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                                            </p>
                                            <p className="text-sm font-semibold text-green-600">
                                                +{performers.topPerformer.pnlPercent?.toFixed(2)}%
                                            </p>
                                        </div>
                                    </div>
                                )}

                                {performers.bottomPerformer && performers.topPerformer?.id !== performers.bottomPerformer?.id && (
                                    <div
                                        className="p-4 bg-red-50 border border-red-200 rounded-lg cursor-pointer hover:bg-red-100 transition-colors"
                                        onClick={() => navigate(`/portfolios/${performers.bottomPerformer.id}`)}
                                    >
                                        <div className="flex items-center justify-between mb-2">
                                            <p className="text-sm font-medium text-gray-600">📉 Dikkat Gereken</p>
                                            <TrendingDown className="w-5 h-5 text-red-600" />
                                        </div>
                                        <p className="font-bold text-lg text-gray-900">{performers.bottomPerformer.name}</p>
                                        <div className="flex items-center justify-between mt-2">
                                            <p className="text-sm text-gray-600">
                                                ₺{performers.bottomPerformer.totalValue?.toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                                            </p>
                                            <p className="text-sm font-semibold text-red-600">
                                                {performers.bottomPerformer.pnlPercent?.toFixed(2)}%
                                            </p>
                                        </div>
                                    </div>
                                )}

                                <Button
                                    variant="outline"
                                    className="w-full"
                                    onClick={() => navigate('/portfolios')}
                                >
                                    Tüm Portföyleri Görüntüle
                                    <ArrowRight className="w-4 h-4 ml-2" />
                                </Button>
                            </CardContent>
                        </Card>

                        {/* Recent Transactions */}
                        <Card>
                            <CardHeader>
                                <CardTitle>Son İşlemler</CardTitle>
                            </CardHeader>
                            <CardContent>
                                {recentTransactions.length === 0 ? (
                                    <div className="text-center py-8">
                                        <p className="text-gray-500">Henüz işlem yok</p>
                                    </div>
                                ) : (
                                    <div className="space-y-3">
                                        {recentTransactions.map((tx) => (
                                            <div key={tx.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors">
                                                <div className="flex items-center gap-3">
                                                    {tx.transactionType === 'BUY' ? (
                                                        <div className="p-2 bg-green-100 rounded-full">
                                                            <TrendingUp className="w-4 h-4 text-green-600" />
                                                        </div>
                                                    ) : (
                                                        <div className="p-2 bg-red-100 rounded-full">
                                                            <TrendingDown className="w-4 h-4 text-red-600" />
                                                        </div>
                                                    )}
                                                    <div>
                                                        <p className="font-semibold text-sm text-gray-900">
                                                            {tx.instrumentSymbol}
                                                        </p>
                                                        <p className="text-xs text-gray-500">
                                                            {formatDate(tx.transactionDate)}
                                                        </p>
                                                    </div>
                                                </div>
                                                <div className="text-right">
                                                    <p className={`font-semibold text-sm ${
                                                        tx.transactionType === 'BUY' ? 'text-green-600' : 'text-red-600'
                                                    }`}>
                                                        {tx.transactionType === 'BUY' ? '-' : '+'}₺{(tx.netAmount || tx.totalAmount || 0).toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                                                    </p>
                                                    <p className="text-xs text-gray-500">
                                                        {tx.quantity} adet
                                                    </p>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </CardContent>
                        </Card>
                    </div>
                </>
            )}
        </div>
    );
}