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

import { getPortfolioSummary, getAllPortfolios } from '../../API/portfolioApi';

export default function DashboardPage() {
    const navigate = useNavigate();

    const [summary, setSummary] = useState(null);
    const [portfolios, setPortfolios] = useState([]);
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

            // Tek bir API call - Backend summary endpoint
            const summaryData = await getPortfolioSummary();
            setSummary(summaryData);

            // Portfolios for bar chart (optional - eğer summary'de portfolios listesi yoksa)
            const portfoliosData = await getAllPortfolios();
            setPortfolios(portfoliosData || []);

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

    const typeColors = {
        'FOREX': '#3B82F6',
        'STOCK': '#8B5CF6',
        'FUND': '#10B981',
        'PRECIOUS': '#F59E0B',
        'CRYPTO': '#EC4899',
        'BOND': '#6366F1',
        'EUROBOND': '#14B8A6',
        'PERSONAL': '#3B82F6',
        'BUSINESS': '#8B5CF6',
        'RETIREMENT': '#10B981',
        'SAVINGS': '#F59E0B'
    };

    const typeLabels = {
        'PERSONAL': 'Bireysel',
        'BUSINESS': 'İş',
        'RETIREMENT': 'Emeklilik',
        'SAVINGS': 'Tasarruf',
        'FOREX': 'Döviz',
        'STOCK': 'Hisse',
        'FUND': 'Fon',
        'PRECIOUS': 'Kıymetli Maden',
        'CRYPTO': 'Kripto',
        'BOND': 'Tahvil',
        'EUROBOND': 'Eurobond'
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

    // Empty state
    if (!summary || summary.totalPortfolios === 0) {
        return (
            <div className="p-8">
                <div className="mb-8 flex items-center justify-between">
                    <div>
                        <h1 className="text-3xl font-bold mb-2">Dashboard</h1>
                        <p className="text-gray-600">Portföylerinizin genel görünümü</p>
                    </div>
                </div>

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
            </div>
        );
    }

    // Prepare chart data
    const pieChartData = (summary.assetAllocation || []).map(item => ({
        name: typeLabels[item.instrumentType] || item.instrumentType,
        value: item.totalValue || 0,
        color: typeColors[item.instrumentType] || '#6B7280'
    }));

    const barChartData = portfolios.map(p => ({
        name: p.name.substring(0, 15) + (p.name.length > 15 ? '...' : ''),
        value: p.totalValue || 0,
        pnl: p.unrealizedPnL || 0
    }));

    // Count active portfolios from all portfolios
    const activePortfolios = portfolios.filter(p => p.active).length;

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
                            ₺{(summary.totalValue || 0).toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                        </p>
                        <p className="text-xs text-gray-500 mt-1">
                            {summary.totalPortfolios || 0} portföy
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
                            (summary.totalUnrealizedPnL || 0) >= 0 ? 'text-green-600' : 'text-red-600'
                        }`}>
                            {(summary.totalUnrealizedPnL || 0) >= 0 ? <TrendingUp className="w-6 h-6" /> : <TrendingDown className="w-6 h-6" />}
                            {(summary.totalUnrealizedPnL || 0) >= 0 ? '+' : ''}₺{Math.abs(summary.totalUnrealizedPnL || 0).toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                        </p>
                        <p className={`text-xs mt-1 ${(summary.totalPnLPercent || 0) >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                            {(summary.totalPnLPercent || 0) >= 0 ? '+' : ''}{(summary.totalPnLPercent || 0).toFixed(2)}%
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
                            ₺{(summary.totalInvested || 0).toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                        </p>
                        <p className="text-xs text-gray-500 mt-1">
                            {summary.assetAllocation?.reduce((sum, item) => sum + (item.count || 0), 0) || 0} varlık
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
                            {activePortfolios}
                        </p>
                        <p className="text-xs text-gray-500 mt-1">
                            / {summary.totalPortfolios || 0} toplam
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
                        <CardTitle>Varlık Dağılımı</CardTitle>
                    </CardHeader>
                    <CardContent>
                        {pieChartData.length === 0 ? (
                            <div className="h-[300px] flex items-center justify-center text-gray-500">
                                Henüz varlık yok
                            </div>
                        ) : (
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
                        )}
                    </CardContent>
                </Card>
            </div>

            {/* Performance & Quick Links Row */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* ALL PORTFOLIOS */}
                <Card>
                    <CardHeader>
                        <CardTitle>Tüm Portföyler ({portfolios.length})</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-3 max-h-[400px] overflow-y-auto">
                        {portfolios.length === 0 ? (
                            <div className="text-center py-8">
                                <p className="text-gray-500">Henüz portföy yok</p>
                            </div>
                        ) : (
                            portfolios
                                .sort((a, b) => (b.pnlPercent || 0) - (a.pnlPercent || 0)) // En iyiden en kötüye sırala
                                .map((portfolio, index) => (
                                    <div
                                        key={portfolio.id}
                                        className={`p-4 border rounded-lg cursor-pointer transition-colors ${
                                            (portfolio.pnlPercent || 0) >= 0
                                                ? 'bg-green-50 border-green-200 hover:bg-green-100'
                                                : 'bg-red-50 border-red-200 hover:bg-red-100'
                                        }`}
                                        onClick={() => navigate(`/portfolios/${portfolio.id}`)}
                                    >
                                        <div className="flex items-center justify-between mb-2">
                                            <div className="flex items-center gap-2">
                                                {index === 0 && (portfolio.pnlPercent || 0) > 0 && (
                                                    <span className="text-lg">🏆</span>
                                                )}
                                                {index === portfolios.length - 1 && (portfolio.pnlPercent || 0) < 0 && (
                                                    <span className="text-lg">📉</span>
                                                )}
                                                <p className="text-sm font-medium text-gray-600">
                                                    {portfolio.name}
                                                </p>
                                            </div>
                                            {(portfolio.pnlPercent || 0) >= 0 ? (
                                                <TrendingUp className="w-5 h-5 text-green-600" />
                                            ) : (
                                                <TrendingDown className="w-5 h-5 text-red-600" />
                                            )}
                                        </div>
                                        <div className="flex items-center justify-between">
                                            <p className="text-sm text-gray-600">
                                                ₺{(portfolio.totalValue || 0).toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                                            </p>
                                            <p className={`text-sm font-semibold ${
                                                (portfolio.pnlPercent || 0) >= 0 ? 'text-green-600' : 'text-red-600'
                                            }`}>
                                                {(portfolio.pnlPercent || 0) >= 0 ? '+' : ''}{(portfolio.pnlPercent || 0).toFixed(2)}%
                                            </p>
                                        </div>
                                        <div className="mt-2 pt-2 border-t border-gray-200">
                                            <p className="text-xs text-gray-500">
                                                {portfolio.holdingCount || 0} varlık • Yatırım: ₺{(portfolio.totalInvested || 0).toLocaleString('tr-TR', { maximumFractionDigits: 0 })}
                                            </p>
                                        </div>
                                    </div>
                                ))
                        )}

                        <Button
                            variant="outline"
                            className="w-full mt-3"
                            onClick={() => navigate('/portfolios')}
                        >
                            Portföy Yönetimi
                            <ArrowRight className="w-4 h-4 ml-2" />
                        </Button>
                    </CardContent>
                </Card>

                {/* Quick Actions */}
                <Card>
                    <CardHeader>
                        <CardTitle>Hızlı Erişim</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-3">
                        <Button
                            variant="outline"
                            className="w-full justify-start"
                            onClick={() => navigate('/portfolios')}
                        >
                            <Wallet className="w-5 h-5 mr-3" />
                            Portföy Listesi
                        </Button>
                        <Button
                            variant="outline"
                            className="w-full justify-start"
                            onClick={() => navigate('/instruments')}
                        >
                            <Activity className="w-5 h-5 mr-3" />
                            Enstrümanlar
                        </Button>
                        <Button
                            variant="outline"
                            className="w-full justify-start"
                            onClick={() => navigate('/watchlist')}
                        >
                            <TrendingUp className="w-5 h-5 mr-3" />
                            Watchlist
                        </Button>

                        <div className="pt-4 border-t border-gray-200">
                            <p className="text-sm text-gray-600 mb-3">Önerilen İşlemler</p>
                            <div className="space-y-2">
                                <div className="p-3 bg-blue-50 rounded-lg">
                                    <p className="text-xs font-medium text-blue-900">💡 Portföyünüzü çeşitlendirin</p>
                                    <p className="text-xs text-blue-700 mt-1">Farklı varlık sınıflarına yatırım yaparak riski azaltın</p>
                                </div>
                                <div className="p-3 bg-green-50 rounded-lg">
                                    <p className="text-xs font-medium text-green-900">📈 Performansı takip edin</p>
                                    <p className="text-xs text-green-700 mt-1">Düzenli olarak portföy performansınızı gözden geçirin</p>
                                </div>
                            </div>
                        </div>
                    </CardContent>
                </Card>
            </div>
        </div>
    );
}