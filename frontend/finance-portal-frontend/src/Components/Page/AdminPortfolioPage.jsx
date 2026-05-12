import React, { useState, useEffect } from 'react';
import { Briefcase, TrendingUp, TrendingDown, Trash2, RefreshCw, Search, CheckCircle, XCircle } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../UI/Card';
import { Button } from '../UI/Button';
import { getAllPortfoliosAdmin, getSystemStatistics, forceDeletePortfolio } from '../../API/adminPortfolioApi';

export default function AdminPortfolioPage() {
    const [portfolios, setPortfolios] = useState([]);
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [userIdFilter, setUserIdFilter] = useState('');
    const [error, setError] = useState(null);

    useEffect(() => {
        loadData();
    }, []);

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
            setError('Veriler yüklenirken hata oluştu');
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
            setError('Arama sırasında hata oluştu');
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (id, name) => {
        if (!window.confirm(`"${name}" portföyünü kalıcı olarak silmek istediğinizden emin misiniz?`)) return;
        try {
            await forceDeletePortfolio(id);
            setPortfolios(prev => prev.filter(p => p.id !== id));
        } catch {
            alert('Portföy silinemedi');
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
                <p className="text-gray-600">Yükleniyor...</p>
            </div>
        </div>
    );

    return (
        <div className="p-8">
            {/* Header */}
            <div className="mb-8 flex items-center justify-between">
                <div>
                    <h1 className="text-3xl font-bold mb-2">Portföy Yönetimi</h1>
                    <p className="text-gray-600">Tüm kullanıcıların portföylerini yönetin</p>
                </div>
                <Button variant="outline" onClick={handleRefresh} disabled={refreshing}>
                    <RefreshCw className={`w-5 h-5 mr-2 ${refreshing ? 'animate-spin' : ''}`} />
                    Yenile
                </Button>
            </div>

            {/* Error */}
            {error && (
                <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700">
                    {error}
                </div>
            )}

            {/* İstatistik Kartları */}
            {stats && (
                <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
                    <Card>
                        <CardHeader className="pb-2">
                            <CardTitle className="text-sm text-gray-600 flex items-center gap-2">
                                <Briefcase className="w-4 h-4" />
                                Toplam Portföy
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
                                Aktif Portföy
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
                                Pasif Portföy
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
                                Toplam Değer (TRY)
                            </CardTitle>
                        </CardHeader>
                        <CardContent>
                            <p className="text-2xl font-bold text-gray-900">
                                {formatCurrency(stats.totalValueTRY)}
                            </p>
                        </CardContent>
                    </Card>
                </div>
            )}

            {/* Filtre */}
            <div className="mb-6 flex gap-3 max-w-lg">
                <div className="relative flex-1">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                    <input
                        type="text"
                        placeholder="Kullanıcı ID ile filtrele..."
                        value={userIdFilter}
                        onChange={(e) => setUserIdFilter(e.target.value)}
                        className="w-full pl-10 pr-4 py-2.5 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                        onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
                    />
                </div>
                <Button onClick={handleSearch}>Ara</Button>
                {userIdFilter && (
                    <Button variant="outline" onClick={() => { setUserIdFilter(''); loadData(); }}>
                        Temizle
                    </Button>
                )}
            </div>

            {/* Tablo */}
            <Card>
                <CardHeader>
                    <CardTitle>Portföyler ({portfolios.length})</CardTitle>
                </CardHeader>
                <CardContent className="p-0">
                    <div className="overflow-x-auto">
                        <table className="w-full">
                            <thead className="bg-gray-50 border-b border-gray-200">
                            <tr>
                                <th className="text-left py-4 px-6 text-sm font-medium text-gray-600">Portföy</th>
                                <th className="text-left py-4 px-6 text-sm font-medium text-gray-600">Kullanıcı ID</th>
                                <th className="text-left py-4 px-6 text-sm font-medium text-gray-600">Tür</th>
                                <th className="text-right py-4 px-6 text-sm font-medium text-gray-600">Toplam Değer</th>
                                <th className="text-right py-4 px-6 text-sm font-medium text-gray-600">Kar/Zarar</th>
                                <th className="text-center py-4 px-6 text-sm font-medium text-gray-600">Durum</th>
                                <th className="text-center py-4 px-6 text-sm font-medium text-gray-600">İşlem</th>
                            </tr>
                            </thead>
                            <tbody>
                            {portfolios.length === 0 ? (
                                <tr>
                                    <td colSpan={7} className="text-center py-12 text-gray-500">
                                        Portföy bulunamadı
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
                                                <p className="text-xs text-gray-400">{portfolio.holdingCount || 0} varlık</p>
                                            </td>
                                            <td className="py-4 px-6">
                                                <span className="text-xs font-mono bg-gray-100 text-gray-600 px-2 py-1 rounded cursor-pointer"
                                                      onClick={() => setUserIdFilter(portfolio.userId)}
                                                      title={portfolio.userId}>
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
                                                    isNeutral ? 'text-gray-500' :
                                                        isPositive ? 'text-green-600' : 'text-red-600'
                                                }`}>
                                                    {isNeutral ? <span>—</span> :
                                                        isPositive ? <TrendingUp className="w-4 h-4" /> :
                                                            <TrendingDown className="w-4 h-4" />}
                                                    <span className="font-medium">
                                                            {isPositive ? '+' : ''}{formatCurrency(portfolio.unrealizedPnL, portfolio.currency)}
                                                        </span>
                                                </div>
                                            </td>
                                            <td className="py-4 px-6 text-center">
                                                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                                                        portfolio.active
                                                            ? 'bg-green-100 text-green-700'
                                                            : 'bg-gray-100 text-gray-600'
                                                    }`}>
                                                        {portfolio.active ? 'Aktif' : 'Pasif'}
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
        </div>
    );
}