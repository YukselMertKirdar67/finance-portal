import React, { useState, useEffect } from 'react';
import { TrendingUp, TrendingDown, Plus, X, Search, RefreshCw } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../UI/Card';
import { Button } from '../UI/Button';
import { useParams } from 'react-router-dom';
import {
    PieChart,
    Pie,
    Cell,
    ResponsiveContainer,
    Tooltip as RechartsTooltip,
    LineChart,
    Line,
    XAxis,
    YAxis,
    CartesianGrid,
    ReferenceLine
} from 'recharts';

import {
    getPortfolioDetail,
    getAssetAllocation,
    createTransaction,
    getPortfolioPerformance,
    updatePortfolio,
    hardDeletePortfolio
} from '../../API/portfolioApi';

import {
    getAllInstruments,
    getInstrumentPrice
} from '../../API/instrumentsApi';

export default function PortfolioPage() {
    const { id } = useParams();
    const PORTFOLIO_ID = parseInt(id);

    const [portfolio, setPortfolio] = useState(null);
    const [holdings, setHoldings] = useState([]);
    const [assetAllocation, setAssetAllocation] = useState([]);
    const [availableInstruments, setAvailableInstruments] = useState([]);
    const [performanceData, setPerformanceData] = useState([]);

    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState(null);
    const [showEditModal, setShowEditModal] = useState(false);
    const [editFormData, setEditFormData] = useState({
        name: '',
        description: '',
        active: true
    });

    const [activeFilter, setActiveFilter] = useState('Tümü');
    const [showAddModal, setShowAddModal] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');
    const [selectedInstrument, setSelectedInstrument] = useState(null);
    const [transactionType, setTransactionType] = useState('BUY');
    const [quantity, setQuantity] = useState('');
    const [price, setPrice] = useState('');
    const [commission, setCommission] = useState('');
    const [tax, setTax] = useState('');
    const [notes, setNotes] = useState('');
    const [submitting, setSubmitting] = useState(false);

    useEffect(() => {
        loadAllData();
    }, []);

    const loadAllData = async () => {
        setLoading(true);
        await Promise.all([
            loadPortfolioData(),
            loadInstruments(),
            loadPerformanceData()
        ]);
        setLoading(false);
    };

    const loadPortfolioData = async () => {
        try {
            setError(null);

            const portfolioData = await getPortfolioDetail(PORTFOLIO_ID);
            setPortfolio(portfolioData);
            setHoldings(portfolioData.holdings || []);

            const allocationData = await getAssetAllocation(PORTFOLIO_ID);
            setAssetAllocation(allocationData || []);

        } catch (err) {
            console.error('Error loading portfolio:', err);
            setError(err.response?.data?.message || 'Portföy yüklenirken hata oluştu');
        }
    };

    const loadInstruments = async () => {
        try {
            const data = await getAllInstruments(0, 100);
            setAvailableInstruments(data.content || []);
        } catch (err) {
            console.error('Error loading instruments:', err);
        }
    };

    const loadPerformanceData = async () => {
            try {
                const data = await getPortfolioPerformance(PORTFOLIO_ID, 30);
                setPerformanceData(data);
                console.log('✅ Performance data loaded:', data);
            } catch (err) {
                console.error('❌ Error loading performance data:', err);
                // Hata olsa da sayfa çalışmaya devam etsin
                setPerformanceData([]);
            }
    };

    const handleRefresh = async () => {
        setRefreshing(true);
        await loadPortfolioData();
        await loadPerformanceData();
        setRefreshing(false);
    };

    const handleUpdatePortfolio = async () => {
        if (!editFormData.name.trim()) {
            alert('Portföy adı boş olamaz');
            return;
        }

        try {
            setSubmitting(true);

            await updatePortfolio(PORTFOLIO_ID, {
                name: editFormData.name,
                description: editFormData.description || undefined,
                active: editFormData.active
            });

            setShowEditModal(false);
            await loadPortfolioData();

            alert('Portföy başarıyla güncellendi!');
        } catch (err) {
            console.error('Error updating portfolio:', err);
            alert(err.response?.data?.message || 'Portföy güncellenirken hata oluştu');
        } finally {
            setSubmitting(false);
        }
    };

    const handleDeletePortfolio = async () => {
        const confirmDelete = window.confirm(
            `"${portfolio.name}" portföyünü KALICI olarak silmek istediğinizden emin misiniz?\n\n` +
            `Bu işlem GERİ ALINAMAZ! Tüm varlıklar ve işlemler silinecektir.`
        );

        if (!confirmDelete) return;

        try {
            setSubmitting(true);

            await hardDeletePortfolio(PORTFOLIO_ID);  // ⭐ hardDeletePortfolio kullan

            alert('Portföy kalıcı olarak silindi!');

            // Redirect to portfolio list
            window.location.href = '/portfolios';

        } catch (err) {
            console.error('Error deleting portfolio:', err);
            alert(err.response?.data?.message || 'Portföy silinirken hata oluştu');
        } finally {
            setSubmitting(false);
        }
    };

    const categories = ['Tümü', ...Array.from(new Set(holdings.map(h => h.instrumentType)))];

    const filteredHoldings = activeFilter === 'Tümü'
        ? holdings
        : holdings.filter(h => h.instrumentType === activeFilter);

    const typeColors = {
        'FOREX': '#3B82F6',
        'STOCK': '#8B5CF6',
        'FUND': '#10B981',
        'PRECIOUS': '#F59E0B',
        'CRYPTO': '#EC4899',
        'BOND': '#6366F1',
        'EUROBOND': '#14B8A6',
    };

    const pieChartData = assetAllocation.map(item => ({
        name: item.instrumentType,
        value: item.totalValue,
        color: typeColors[item.instrumentType] || '#6B7280',
        percentage: item.percentage
    }));

    const filteredInstruments = (() => {
        let instruments = availableInstruments;

        // SELL ise sadece portföydeki enstrümanları göster
        if (transactionType === 'SELL') {
            const holdingInstrumentIds = holdings.map(h => h.instrumentId);
            instruments = instruments.filter(inst => holdingInstrumentIds.includes(inst.id));
        }

        // Search filtresi uygula
        if (searchTerm) {
            instruments = instruments.filter(inst =>
                inst.symbol?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                inst.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                inst.type?.toLowerCase().includes(searchTerm.toLowerCase())
            );
        }

        return instruments;
    })();

    const handleAddTransaction = async () => {
        if (!selectedInstrument || !quantity || !price) {
            alert('Lütfen tüm zorunlu alanları doldurun');
            return;
        }

        // Satış kontrolü - elinde olmayan miktarı satamaz
        if (transactionType === 'SELL') {
            const holding = holdings.find(h => h.instrumentId === selectedInstrument.id);
            if (!holding || parseFloat(quantity) > holding.quantity) {
                alert(`Yetersiz miktar! Mevcut: ${holding ? holding.quantity : 0}`);
                return;
            }
        }

        try {
            setSubmitting(true);

            const transactionData = {
                instrumentId: selectedInstrument.id,
                transactionType: transactionType,
                quantity: parseFloat(quantity),
                price: parseFloat(price),
                commission: commission ? parseFloat(commission) : undefined,
                tax: tax ? parseFloat(tax) : undefined,
                notes: notes || undefined,
            };

            await createTransaction(PORTFOLIO_ID, transactionData);

            resetForm();
            await loadPortfolioData();
            await loadPerformanceData();

            alert(`${transactionType === 'BUY' ? 'Alış' : 'Satış'} işlemi başarıyla eklendi!`);
        } catch (err) {
            console.error('Error creating transaction:', err);
            const errorMsg = err.response?.data?.message || 'İşlem eklenirken hata oluştu';
            alert(errorMsg);
        } finally {
            setSubmitting(false);
        }
    };

    const resetForm = () => {
        setShowAddModal(false);
        setSelectedInstrument(null);
        setTransactionType('BUY');
        setQuantity('');
        setPrice('');
        setCommission('');
        setTax('');
        setNotes('');
        setSearchTerm('');
    };

    const calculateTotal = () => {
        if (!quantity || !price) return 0;
        const baseAmount = parseFloat(quantity) * parseFloat(price);
        const fees = (parseFloat(commission) || 0) + (parseFloat(tax) || 0);
        return baseAmount + fees;
    };

    const getCurrentPrice = async (instrumentId) => {
        try {
            const priceData = await getInstrumentPrice(instrumentId);
            return priceData.current || priceData.price || 0;
        } catch (err) {
            console.error('Error fetching price:', err);
            return 0;
        }
    };

    // Loading state
    if (loading) {
        return (
            <div className="p-8 flex items-center justify-center h-screen">
                <div className="text-center">
                    <div className="animate-spin rounded-full h-16 w-16 border-b-4 border-blue-600 mx-auto mb-4"></div>
                    <p className="text-lg text-gray-700 font-medium">Portföy yükleniyor...</p>
                    <p className="text-sm text-gray-500 mt-2">Lütfen bekleyin</p>
                </div>
            </div>
        );
    }

    // Error state
    if (error) {
        return (
            <div className="p-8">
                <div className="bg-red-50 border-2 border-red-200 rounded-lg p-8 text-center max-w-md mx-auto">
                    <div className="text-red-600 text-5xl mb-4">⚠️</div>
                    <p className="text-red-800 font-semibold text-xl mb-2">Hata Oluştu</p>
                    <p className="text-red-600 mb-6">{error}</p>
                    <Button
                        className="bg-red-600 hover:bg-red-700"
                        onClick={() => {
                            setError(null);
                            loadAllData();
                        }}
                    >
                        <RefreshCw className="w-4 h-4 mr-2" />
                        Tekrar Dene
                    </Button>
                </div>
            </div>
        );
    }

    // No portfolio state
    if (!portfolio) {
        return (
            <div className="p-8">
                <div className="bg-yellow-50 border-2 border-yellow-200 rounded-lg p-8 text-center max-w-md mx-auto">
                    <div className="text-yellow-600 text-5xl mb-4">📂</div>
                    <p className="text-yellow-800 font-semibold text-xl mb-2">Portföy Bulunamadı</p>
                    <p className="text-yellow-600">ID: {PORTFOLIO_ID} numaralı portföy bulunamadı</p>
                </div>
            </div>
        );
    }

    return (
        <div className="p-8">
            {/* Header */}
            <div className="mb-6 flex items-center justify-between">
                <div className="flex-1">
                    <div className="flex items-center gap-3">
                        <h1 className="text-3xl font-bold">{portfolio.name}</h1>
                        {/* ⭐ Active/Passive Badge */}
                        {portfolio.active ? (
                            <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-green-100 text-green-800">
                    Aktif
                </span>
                        ) : (
                            <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-gray-100 text-gray-800">
                    Pasif
                </span>
                        )}
                    </div>
                    <p className="text-gray-600 mt-2">{portfolio.description || 'Yatırımlarınızın genel görünümü'}</p>
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

                    {/* ⭐ Düzenle Butonu */}
                    <Button
                        variant="outline"
                        onClick={() => {
                            setEditFormData({
                                name: portfolio.name,
                                description: portfolio.description || '',
                                active: portfolio.active
                            });
                            setShowEditModal(true);
                        }}
                    >
                        Düzenle
                    </Button>

                    {/* ⭐ Sil Butonu */}
                    <Button
                        variant="outline"
                        className="text-red-600 hover:text-red-700 hover:bg-red-50"
                        onClick={handleDeletePortfolio}
                    >
                        Sil
                    </Button>

                    <Button
                        variant="default"
                        onClick={() => setShowAddModal(true)}
                        className="bg-[#0066FF] hover:bg-[#0052CC]"
                    >
                        <Plus className="w-5 h-5 mr-2" />
                        İşlem Ekle
                    </Button>
                </div>
            </div>

            {/* Summary Cards */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-6">
                <Card>
                    <CardHeader className="pb-2">
                        <CardTitle className="text-sm text-gray-600">Toplam Değer</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <p className="text-3xl font-semibold text-gray-900">
                            {portfolio.currency === 'TRY' ? '₺' : '$'}
                            {portfolio.currentValue?.toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                        </p>
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader className="pb-2">
                        <CardTitle className="text-sm text-gray-600">Yatırılan</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <p className="text-3xl font-semibold text-gray-900">
                            {portfolio.currency === 'TRY' ? '₺' : '$'}
                            {portfolio.totalInvested?.toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                        </p>
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader className="pb-2">
                        <CardTitle className="text-sm text-gray-600">Kar/Zarar</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <p className={`text-3xl font-semibold flex items-center gap-2 ${
                            portfolio.unrealizedPnL >= 0 ? 'text-green-600' : 'text-red-600'
                        }`}>
                            {portfolio.unrealizedPnL >= 0 ? <TrendingUp className="w-6 h-6" /> : <TrendingDown className="w-6 h-6" />}
                            {portfolio.unrealizedPnL >= 0 ? '+' : ''}
                            {portfolio.currency === 'TRY' ? '₺' : '$'}
                            {portfolio.unrealizedPnL?.toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                        </p>
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader className="pb-2">
                        <CardTitle className="text-sm text-gray-600">Getiri Oranı</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <p className={`text-3xl font-semibold flex items-center gap-2 ${
                            portfolio.pnlPercent >= 0 ? 'text-green-600' : 'text-red-600'
                        }`}>
                            {portfolio.pnlPercent >= 0 ? <TrendingUp className="w-6 h-6" /> : <TrendingDown className="w-6 h-6" />}
                            {portfolio.pnlPercent >= 0 ? '+' : ''}{portfolio.pnlPercent?.toFixed(2)}%
                        </p>
                    </CardContent>
                </Card>
            </div>

            {/* Performance Chart */}
            {performanceData.length > 0 && (
                <Card className="mb-6">
                    <CardHeader>
                        <CardTitle>Portföy Performansı (Son 30 Gün)</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="h-[400px]">
                            <ResponsiveContainer width="100%" height="100%">
                                <LineChart data={performanceData}>
                                    <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                                    <XAxis
                                        dataKey="date"
                                        stroke="#9ca3af"
                                        style={{ fontSize: '12px' }}
                                    />
                                    <YAxis
                                        stroke="#9ca3af"
                                        style={{ fontSize: '12px' }}
                                        tickFormatter={(value) => `₺${value.toLocaleString('tr-TR')}`}
                                    />
                                    <RechartsTooltip
                                        formatter={(value) => [`₺${value.toLocaleString('tr-TR', { minimumFractionDigits: 2 })}`, 'Değer']}
                                        labelFormatter={(label) => `Tarih: ${label}`}
                                    />
                                    <Line
                                        type="monotone"
                                        dataKey="value"
                                        stroke={portfolio.unrealizedPnL >= 0 ? '#10B981' : '#EF4444'}
                                        strokeWidth={3}
                                        dot={{ fill: portfolio.unrealizedPnL >= 0 ? '#10B981' : '#EF4444', r: 4 }}
                                        activeDot={{ r: 6 }}
                                    />
                                    <ReferenceLine
                                        y={portfolio.initialBalance}
                                        stroke="#6B7280"
                                        strokeDasharray="3 3"
                                        label={{ value: 'Başlangıç', position: 'right', fill: '#6B7280' }}
                                    />
                                </LineChart>
                            </ResponsiveContainer>
                        </div>
                    </CardContent>
                </Card>
            )}

            {/* Pie Chart */}
            {pieChartData.length > 0 && (
                <Card className="mb-6">
                    <CardHeader>
                        <CardTitle>Portföy Dağılımı</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-center">
                            <div className="h-[400px]">
                                <ResponsiveContainer width="100%" height="100%">
                                    <PieChart>
                                        <Pie
                                            data={pieChartData}
                                            cx="50%"
                                            cy="50%"
                                            labelLine={false}
                                            label={({ name, percentage }) => `${name} ${percentage?.toFixed(1)}%`}
                                            outerRadius={140}
                                            fill="#8884d8"
                                            dataKey="value"
                                        >
                                            {pieChartData.map((entry, index) => (
                                                <Cell key={`cell-${index}`} fill={entry.color} />
                                            ))}
                                        </Pie>
                                        <RechartsTooltip
                                            formatter={(value) => `₺${value?.toLocaleString('tr-TR', { minimumFractionDigits: 2 })}`}
                                        />
                                    </PieChart>
                                </ResponsiveContainer>
                            </div>
                            <div className="grid grid-cols-1 gap-4">
                                {assetAllocation.map((item) => (
                                    <div key={item.instrumentType} className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                                        <div className="flex items-center gap-3">
                                            <div
                                                className="w-4 h-4 rounded-full"
                                                style={{ backgroundColor: typeColors[item.instrumentType] || '#6B7280' }}
                                            />
                                            <span className="font-medium text-gray-900">{item.instrumentType}</span>
                                            <span className="text-sm text-gray-500">({item.count} adet)</span>
                                        </div>
                                        <div className="text-right">
                      <span className="text-lg font-semibold text-gray-900">
                        ₺{item.totalValue?.toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                      </span>
                                            <p className="text-sm text-gray-600">{item.percentage?.toFixed(1)}%</p>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </CardContent>
                </Card>
            )}

            {/* Holdings Table */}
            <Card>
                <CardHeader>
                    <div className="flex items-center justify-between flex-wrap gap-4">
                        <CardTitle>Varlıklarım ({filteredHoldings.length})</CardTitle>
                        <div className="flex flex-wrap gap-2">
                            {categories.map((category) => (
                                <Button
                                    key={category}
                                    variant={activeFilter === category ? 'default' : 'outline'}
                                    size="sm"
                                    onClick={() => setActiveFilter(category)}
                                    className={activeFilter === category ? 'bg-[#0066FF] hover:bg-[#0052CC]' : ''}
                                >
                                    {category}
                                </Button>
                            ))}
                        </div>
                    </div>
                </CardHeader>
                <CardContent>
                    <div className="overflow-x-auto">
                        <table className="w-full">
                            <thead>
                            <tr className="border-b border-gray-200">
                                <th className="text-left py-3 px-4 text-sm font-semibold text-gray-600">Enstrüman</th>
                                <th className="text-left py-3 px-4 text-sm font-semibold text-gray-600">Tür</th>
                                <th className="text-right py-3 px-4 text-sm font-semibold text-gray-600">Miktar</th>
                                <th className="text-right py-3 px-4 text-sm font-semibold text-gray-600">Ort. Fiyat</th>
                                <th className="text-right py-3 px-4 text-sm font-semibold text-gray-600">Güncel Fiyat</th>
                                <th className="text-right py-3 px-4 text-sm font-semibold text-gray-600">Toplam Değer</th>
                                <th className="text-right py-3 px-4 text-sm font-semibold text-gray-600">Kar/Zarar</th>
                            </tr>
                            </thead>
                            <tbody>
                            {filteredHoldings.length === 0 ? (
                                <tr>
                                    <td colSpan={7} className="text-center py-12">
                                        <div className="text-gray-400 text-6xl mb-4">📊</div>
                                        <p className="text-gray-500 text-lg font-medium">Henüz varlık bulunmuyor</p>
                                        <p className="text-gray-400 text-sm mt-2">İlk işleminizi ekleyerek başlayın</p>
                                    </td>
                                </tr>
                            ) : (
                                filteredHoldings.map((holding) => (
                                    <tr key={holding.holdingId} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                                        <td className="py-4 px-4">
                                            <p className="font-semibold text-gray-900">{holding.instrumentSymbol}</p>
                                            <p className="text-xs text-gray-500 mt-0.5">{holding.instrumentName}</p>
                                        </td>
                                        <td className="py-4 px-4">
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                          {holding.instrumentType}
                        </span>
                                        </td>
                                        <td className="py-4 px-4 text-right">
                        <span className="text-sm font-medium text-gray-900">
                          {holding.quantity?.toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 8 })}
                        </span>
                                        </td>
                                        <td className="py-4 px-4 text-right">
                        <span className="text-sm text-gray-700">
                          ₺{holding.averageBuyPrice?.toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 6 })}
                        </span>
                                        </td>
                                        <td className="py-4 px-4 text-right">
                        <span className="text-sm font-medium text-gray-900">
                          ₺{holding.currentPrice?.toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 6 })}
                        </span>
                                        </td>
                                        <td className="py-4 px-4 text-right">
                        <span className="font-semibold text-gray-900">
                          ₺{holding.currentValue?.toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                        </span>
                                        </td>
                                        <td className="py-4 px-4 text-right">
                                            <div className={`flex items-center justify-end gap-1 ${
                                                holding.unrealizedPnL >= 0 ? 'text-green-600' : 'text-red-600'
                                            }`}>
                                                {holding.unrealizedPnL >= 0 ? <TrendingUp className="w-4 h-4" /> : <TrendingDown className="w-4 h-4" />}
                                                <div className="text-right">
                                                    <p className="text-sm font-semibold">
                                                        {holding.unrealizedPnL >= 0 ? '+' : ''}
                                                        ₺{Math.abs(holding.unrealizedPnL)?.toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                                                    </p>
                                                    <p className="text-xs font-medium">
                                                        {holding.pnlPercent >= 0 ? '+' : ''}{holding.pnlPercent?.toFixed(2)}%
                                                    </p>
                                                </div>
                                            </div>
                                        </td>
                                    </tr>
                                ))
                            )}
                            </tbody>
                        </table>
                    </div>
                </CardContent>
            </Card>
            {/* Edit Portfolio Modal */}
            {showEditModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-lg shadow-2xl w-full max-w-md">
                        <div className="p-6 border-b border-gray-200">
                            <h2 className="text-2xl font-bold text-gray-900">Portföyü Düzenle</h2>
                        </div>

                        <div className="p-6 space-y-5">
                            {/* Portfolio Name */}
                            <div>
                                <label className="block text-sm font-semibold text-gray-700 mb-2">
                                    Portföy Adı *
                                </label>
                                <input
                                    type="text"
                                    className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#0066FF] focus:border-transparent"
                                    placeholder="Portföy adı"
                                    value={editFormData.name}
                                    onChange={(e) => setEditFormData({ ...editFormData, name: e.target.value })}
                                    disabled={submitting}
                                />
                            </div>

                            {/* Description */}
                            <div>
                                <label className="block text-sm font-semibold text-gray-700 mb-2">
                                    Açıklama
                                </label>
                                <textarea
                                    className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#0066FF] focus:border-transparent resize-none"
                                    placeholder="Portföy açıklaması (opsiyonel)"
                                    rows={3}
                                    value={editFormData.description}
                                    onChange={(e) => setEditFormData({ ...editFormData, description: e.target.value })}
                                    disabled={submitting}
                                />
                            </div>

                            {/* Active Status */}
                            <div>
                                <label className="flex items-center gap-3">
                                    <input
                                        type="checkbox"
                                        className="w-5 h-5 text-[#0066FF] border-gray-300 rounded focus:ring-[#0066FF]"
                                        checked={editFormData.active}
                                        onChange={(e) => setEditFormData({ ...editFormData, active: e.target.checked })}
                                        disabled={submitting}
                                    />
                                    <span className="text-sm font-semibold text-gray-700">
                            Portföy Aktif
                        </span>
                                </label>
                                <p className="text-xs text-gray-500 mt-1 ml-8">
                                    Pasif portföyler liste sayfasında gizlenir
                                </p>
                            </div>
                        </div>

                        {/* Action Buttons */}
                        <div className="p-6 border-t border-gray-200 flex gap-3">
                            <Button
                                variant="outline"
                                className="flex-1 h-12 font-semibold border-2"
                                onClick={() => setShowEditModal(false)}
                                disabled={submitting}
                            >
                                İptal
                            </Button>
                            <Button
                                className="flex-1 h-12 font-semibold bg-[#0066FF] hover:bg-[#0052CC]"
                                onClick={handleUpdatePortfolio}
                                disabled={!editFormData.name.trim() || submitting}
                            >
                                {submitting ? (
                                    <>
                                        <RefreshCw className="w-5 h-5 mr-2 animate-spin" />
                                        Güncelleniyor...
                                    </>
                                ) : (
                                    'Güncelle'
                                )}
                            </Button>
                        </div>
                    </div>
                </div>
            )}

            {/* Add Transaction Modal */}
            {showAddModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-lg shadow-2xl w-full max-w-md max-h-[90vh] overflow-y-auto">
                        <div className="sticky top-0 bg-white border-b border-gray-200 p-6 flex items-center justify-between">
                            <h2 className="text-2xl font-bold text-gray-900">İşlem Ekle</h2>
                            <button
                                className="text-gray-400 hover:text-gray-600 transition-colors p-1 rounded-full hover:bg-gray-100"
                                onClick={resetForm}
                                disabled={submitting}
                            >
                                <X className="w-6 h-6" />
                            </button>
                        </div>

                        <div className="p-6 space-y-5">
                            {/* Transaction Type */}
                            <div>
                                <label className="block text-sm font-semibold text-gray-700 mb-2">İşlem Tipi *</label>
                                <div className="grid grid-cols-2 gap-3">
                                    <Button
                                        type="button"
                                        variant={transactionType === 'BUY' ? 'default' : 'outline'}
                                        onClick={() => setTransactionType('BUY')}
                                        className={`h-12 font-semibold ${transactionType === 'BUY' ? 'bg-green-600 hover:bg-green-700' : 'border-2'}`}
                                        disabled={submitting}
                                    >
                                        ALIŞ
                                    </Button>
                                    <Button
                                        type="button"
                                        variant={transactionType === 'SELL' ? 'default' : 'outline'}
                                        onClick={() => setTransactionType('SELL')}
                                        className={`h-12 font-semibold ${transactionType === 'SELL' ? 'bg-red-600 hover:bg-red-700' : 'border-2'}`}
                                        disabled={submitting}
                                    >
                                        SATIŞ
                                    </Button>
                                </div>
                            </div>

                            {/* Instrument Search */}
                            <div>
                                <label className="block text-sm font-semibold text-gray-700 mb-2">
                                    Enstrüman Seç *
                                    {transactionType === 'SELL' && (
                                        <span className="text-xs text-gray-500 ml-2">(Sadece portföyünüzdeki varlıklar)</span>
                                    )}
                                </label>
                                <div className="relative">
                                    <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
                                    <input
                                        type="text"
                                        className="w-full pl-10 pr-4 py-3 border-2 border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#0066FF] focus:border-transparent"
                                        placeholder="Enstrüman ara..."
                                        value={searchTerm}
                                        onChange={(e) => setSearchTerm(e.target.value)}
                                        disabled={submitting}
                                    />
                                </div>
                                {searchTerm && (
                                    <div className="mt-2 max-h-60 overflow-y-auto border-2 border-gray-200 rounded-lg bg-white shadow-lg">
                                        {filteredInstruments.length > 0 ? (
                                            filteredInstruments.map((inst) => (
                                                <button
                                                    key={inst.id}
                                                    type="button"
                                                    className={`w-full flex items-center justify-between px-4 py-3 hover:bg-blue-50 transition-colors border-b border-gray-100 last:border-b-0 ${
                                                        selectedInstrument?.id === inst.id ? 'bg-blue-50 border-l-4 border-[#0066FF]' : ''
                                                    }`}
                                                    onClick={async () => {
                                                        setSelectedInstrument(inst);
                                                        const currentPrice = await getCurrentPrice(inst.id);
                                                        setPrice(currentPrice.toString());
                                                        setSearchTerm('');
                                                    }}
                                                    disabled={submitting}
                                                >
                                                    <div className="text-left">
                                                        <p className="font-semibold text-gray-900">{inst.symbol}</p>
                                                        <p className="text-xs text-gray-500 mt-0.5">{inst.name}</p>
                                                    </div>
                                                    <span className="text-xs text-gray-500">{inst.type}</span>
                                                </button>
                                            ))
                                        ) : (
                                            <div className="px-4 py-6 text-center">
                                                <p className="text-gray-500 font-medium">
                                                    {transactionType === 'SELL'
                                                        ? 'Portföyünüzde bu kriterde varlık yok'
                                                        : 'Enstrüman bulunamadı'
                                                    }
                                                </p>
                                            </div>
                                        )}
                                    </div>
                                )}
                                {selectedInstrument && !searchTerm && (
                                    <div className="mt-2 p-4 bg-blue-50 border-2 border-blue-200 rounded-lg">
                                        <div className="flex items-center justify-between">
                                            <div className="flex items-center gap-2">
                                                <div
                                                    className="w-3 h-3 rounded-full"
                                                    style={{ backgroundColor: typeColors[selectedInstrument.type] || '#6B7280' }}
                                                />
                                                <div>
                                                    <span className="font-semibold text-gray-900 block">{selectedInstrument.symbol}</span>
                                                    <span className="text-xs text-gray-600">{selectedInstrument.name}</span>
                                                    {transactionType === 'SELL' && (() => {
                                                        const holding = holdings.find(h => h.instrumentId === selectedInstrument.id);
                                                        return holding ? (
                                                            <span className="text-xs text-green-600 block mt-1">
                                Mevcut: {holding.quantity.toFixed(2)}
                              </span>
                                                        ) : null;
                                                    })()}
                                                </div>
                                            </div>
                                            <button
                                                type="button"
                                                onClick={() => setSelectedInstrument(null)}
                                                className="text-gray-400 hover:text-gray-600 p-1 rounded-full hover:bg-blue-100"
                                                disabled={submitting}
                                            >
                                                <X className="w-4 h-4" />
                                            </button>
                                        </div>
                                    </div>
                                )}
                            </div>

                            {/* Quantity */}
                            <div>
                                <label className="block text-sm font-semibold text-gray-700 mb-2">
                                    Miktar *
                                    {transactionType === 'SELL' && selectedInstrument && (
                                        <span className="text-xs text-gray-500 ml-2">
                      (Mevcut: {(() => {
                                            const holding = holdings.find(h => h.instrumentId === selectedInstrument.id);
                                            return holding ? holding.quantity.toFixed(2) : '0';
                                        })()})
                    </span>
                                    )}
                                </label>
                                <input
                                    type="number"
                                    step="any"
                                    className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#0066FF] focus:border-transparent"
                                    placeholder="Örn: 100"
                                    value={quantity}
                                    onChange={(e) => setQuantity(e.target.value)}
                                    disabled={submitting}
                                />
                            </div>

                            {/* Price */}
                            <div>
                                <label className="block text-sm font-semibold text-gray-700 mb-2">Fiyat (₺) *</label>
                                <input
                                    type="number"
                                    step="any"
                                    className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#0066FF] focus:border-transparent"
                                    placeholder="Örn: 34.50"
                                    value={price}
                                    onChange={(e) => setPrice(e.target.value)}
                                    disabled={submitting}
                                />
                            </div>

                            {/* Commission */}
                            <div>
                                <label className="block text-sm font-semibold text-gray-700 mb-2">Komisyon (₺)</label>
                                <input
                                    type="number"
                                    step="any"
                                    className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#0066FF] focus:border-transparent"
                                    placeholder="Opsiyonel"
                                    value={commission}
                                    onChange={(e) => setCommission(e.target.value)}
                                    disabled={submitting}
                                />
                            </div>

                            {/* Tax */}
                            <div>
                                <label className="block text-sm font-semibold text-gray-700 mb-2">Vergi (₺)</label>
                                <input
                                    type="number"
                                    step="any"
                                    className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#0066FF] focus:border-transparent"
                                    placeholder="Opsiyonel"
                                    value={tax}
                                    onChange={(e) => setTax(e.target.value)}
                                    disabled={submitting}
                                />
                            </div>

                            {/* Notes */}
                            <div>
                                <label className="block text-sm font-semibold text-gray-700 mb-2">Notlar</label>
                                <textarea
                                    className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#0066FF] focus:border-transparent resize-none"
                                    placeholder="İşlem notları (opsiyonel)"
                                    rows={3}
                                    value={notes}
                                    onChange={(e) => setNotes(e.target.value)}
                                    disabled={submitting}
                                />
                            </div>

                            {/* Summary */}
                            {quantity && price && (
                                <div className="p-4 bg-gradient-to-r from-blue-50 to-indigo-50 border-2 border-blue-200 rounded-lg">
                                    <p className="text-sm font-semibold text-gray-700 mb-1">Toplam Tutar</p>
                                    <p className="text-3xl font-bold text-gray-900">
                                        ₺{calculateTotal().toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                                    </p>
                                    {(commission || tax) && (
                                        <p className="text-xs text-gray-600 mt-2">
                                            İşlem Tutarı: ₺{(parseFloat(quantity) * parseFloat(price)).toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                                            <br />
                                            Ek Masraflar: ₺{((parseFloat(commission) || 0) + (parseFloat(tax) || 0)).toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                                        </p>
                                    )}
                                </div>
                            )}
                        </div>

                        {/* Action Buttons */}
                        <div className="sticky bottom-0 bg-gray-50 border-t border-gray-200 p-6 flex gap-3">
                            <Button
                                variant="outline"
                                className="flex-1 h-12 font-semibold border-2"
                                onClick={resetForm}
                                disabled={submitting}
                            >
                                İptal
                            </Button>
                            <Button
                                variant="default"
                                className={`flex-1 h-12 font-semibold ${
                                    transactionType === 'BUY'
                                        ? 'bg-green-600 hover:bg-green-700'
                                        : 'bg-red-600 hover:bg-red-700'
                                }`}
                                onClick={handleAddTransaction}
                                disabled={!selectedInstrument || !quantity || !price || submitting}
                            >
                                {submitting ? (
                                    <>
                                        <RefreshCw className="w-5 h-5 mr-2 animate-spin" />
                                        İşleniyor...
                                    </>
                                ) : (
                                    <>
                                        {transactionType === 'BUY' ? 'AL' : 'SAT'}
                                    </>
                                )}
                            </Button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}