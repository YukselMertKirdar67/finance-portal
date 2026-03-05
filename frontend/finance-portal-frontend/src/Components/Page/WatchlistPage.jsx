import React, { useState, useEffect } from 'react';
import { TrendingUp, TrendingDown, Star, Plus, X, Search, Trash2, RefreshCw } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../UI/Card';
import { Button } from '../UI/Button';
import { useNavigate } from 'react-router-dom';
import { getWatchlist, addToWatchlist, removeFromWatchlist, searchInstruments } from '../../API/watchlistApi';

export default function WatchlistPage() {
    const navigate = useNavigate();
    const [watchlistItems, setWatchlistItems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showAddModal, setShowAddModal] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');
    const [searchResults, setSearchResults] = useState([]);
    const [selectedInstrument, setSelectedInstrument] = useState(null);
    const [searching, setSearching] = useState(false);

    useEffect(() => {
        fetchWatchlist();
    }, []);

    useEffect(() => {
        const delaySearch = setTimeout(() => {
            if (searchTerm.length >= 2) {
                handleSearch();
            } else {
                setSearchResults([]);
            }
        }, 300);

        return () => clearTimeout(delaySearch);
    }, [searchTerm]);

    const fetchWatchlist = async () => {
        try {
            setLoading(true);
            const data = await getWatchlist(0, 100);
            setWatchlistItems(data.content || []);
        } catch (error) {
            console.error('Watchlist fetch error:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleSearch = async () => {
        try {
            setSearching(true);
            const data = await searchInstruments(searchTerm);
            setSearchResults(data.content || []);
        } catch (error) {
            console.error('Search error:', error);
        } finally {
            setSearching(false);
        }
    };

    const handleAddToWatchlist = async () => {
        if (!selectedInstrument) return;

        try {
            await addToWatchlist(selectedInstrument.id);
            setShowAddModal(false);
            setSelectedInstrument(null);
            setSearchTerm('');
            setSearchResults([]);
            fetchWatchlist(); // Listeyi yenile
        } catch (error) {
            console.error('Add error:', error);
            alert('Eklenirken hata oluştu');
        }
    };

    const handleRemoveFromWatchlist = async (item) => {
        if (!confirm(`${item.instrument.symbol} takip listesinden çıkarılsın mı?`)) return;

        try {
            await removeFromWatchlist(item.instrument.id);
            fetchWatchlist(); // Listeyi yenile
        } catch (error) {
            console.error('Remove error:', error);
            alert('Çıkarılırken hata oluştu');
        }
    };

    const getCategoryColor = (type) => {
        const colors = {
            'FOREX': '#3B82F6',
            'STOCK': '#8B5CF6',
            'CRYPTO': '#EC4899',
            'PRECIOUS': '#F59E0B',
            'FUND': '#10B981',
            'BOND': '#6366F1',
        };
        return colors[type] || '#6B7280';
    };

    const getCategoryName = (type) => {
        const names = {
            'FOREX': 'Döviz',
            'STOCK': 'Hisse',
            'CRYPTO': 'Kripto',
            'PRECIOUS': 'Altın/Gümüş',
            'FUND': 'Fon',
            'BOND': 'Tahvil',
        };
        return names[type] || type;
    };

    const formatPrice = (price) => {
        if (!price) return '0.00';
        const num = parseFloat(price);
        if (num > 1000) return num.toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        if (num > 1) return num.toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 4 });
        return num.toLocaleString('tr-TR', { minimumFractionDigits: 4, maximumFractionDigits: 6 });
    };

    if (loading) {
        return (
            <div className="min-h-screen bg-gray-50 flex items-center justify-center">
                <div className="text-center">
                    <div className="w-16 h-16 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
                    <p className="text-gray-600">Yükleniyor...</p>
                </div>
            </div>
        );
    }

    const risingCount = watchlistItems.filter(item =>
        item.instrument.currentPrice?.changePercent && parseFloat(item.instrument.currentPrice.changePercent) > 0
    ).length;

    const fallingCount = watchlistItems.filter(item =>
        item.instrument.currentPrice?.changePercent && parseFloat(item.instrument.currentPrice.changePercent) < 0
    ).length;

    const categoriesCount = new Set(watchlistItems.map(item => item.instrument.type)).size;

    return (
        <div className="min-h-screen bg-gray-50 p-8">
            <div className="max-w-7xl mx-auto">
                <div className="mb-6 flex items-center justify-between">
                    <div>
                        <h1 className="text-4xl font-bold text-gray-900 mb-2">Takip Listesi</h1>
                        <p className="text-gray-600">Favori enstrümanlarınızı takip edin</p>
                    </div>
                    <div className="flex gap-3">
                        <Button
                            variant="outline"
                            onClick={fetchWatchlist}
                            className="border-gray-300"
                        >
                            <RefreshCw className="w-5 h-5 mr-2" />
                            Yenile
                        </Button>
                        <Button
                            variant="default"
                            onClick={() => setShowAddModal(true)}
                            className="bg-blue-600 hover:bg-blue-700"
                        >
                            <Plus className="w-5 h-5 mr-2" />
                            Enstrüman Ekle
                        </Button>
                    </div>
                </div>

                {/* Summary Stats */}
                <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-6">
                    <Card className="border-0 shadow-sm">
                        <CardHeader className="pb-2">
                            <CardTitle className="text-sm text-gray-600">Toplam Enstrüman</CardTitle>
                        </CardHeader>
                        <CardContent>
                            <p className="text-3xl font-bold text-gray-900">{watchlistItems.length}</p>
                        </CardContent>
                    </Card>

                    <Card className="border-0 shadow-sm">
                        <CardHeader className="pb-2">
                            <CardTitle className="text-sm text-gray-600">Yükselenler</CardTitle>
                        </CardHeader>
                        <CardContent>
                            <p className="text-3xl font-bold text-emerald-600">{risingCount}</p>
                        </CardContent>
                    </Card>

                    <Card className="border-0 shadow-sm">
                        <CardHeader className="pb-2">
                            <CardTitle className="text-sm text-gray-600">Düşenler</CardTitle>
                        </CardHeader>
                        <CardContent>
                            <p className="text-3xl font-bold text-red-500">{fallingCount}</p>
                        </CardContent>
                    </Card>

                    <Card className="border-0 shadow-sm">
                        <CardHeader className="pb-2">
                            <CardTitle className="text-sm text-gray-600">Kategoriler</CardTitle>
                        </CardHeader>
                        <CardContent>
                            <p className="text-3xl font-bold text-gray-900">{categoriesCount}</p>
                        </CardContent>
                    </Card>
                </div>

                {/* Watchlist Table */}
                <Card className="border-0 shadow-sm">
                    <CardHeader>
                        <div className="flex items-center justify-between">
                            <CardTitle>Takip Edilen Enstrümanlar</CardTitle>
                            <div className="flex items-center gap-2 text-sm text-gray-600">
                                <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />
                                <span>{watchlistItems.length} enstrüman</span>
                            </div>
                        </div>
                    </CardHeader>
                    <CardContent>
                        {watchlistItems.length === 0 ? (
                            <div className="text-center py-12">
                                <Star className="w-16 h-16 text-gray-300 mx-auto mb-4" />
                                <p className="text-gray-500 mb-4">Takip listeniz boş</p>
                                <Button
                                    onClick={() => setShowAddModal(true)}
                                    className="bg-blue-600 hover:bg-blue-700"
                                >
                                    <Plus className="w-5 h-5 mr-2" />
                                    İlk Enstrümanı Ekle
                                </Button>
                            </div>
                        ) : (
                            <div className="overflow-x-auto">
                                <table className="w-full">
                                    <thead>
                                    <tr className="border-b border-gray-200">
                                        <th className="text-left py-4 px-4 text-sm font-semibold text-gray-700">Enstrüman</th>
                                        <th className="text-left py-4 px-4 text-sm font-semibold text-gray-700">Kategori</th>
                                        <th className="text-right py-4 px-4 text-sm font-semibold text-gray-700">Fiyat</th>
                                        <th className="text-right py-4 px-4 text-sm font-semibold text-gray-700">Değişim</th>
                                        <th className="text-right py-4 px-4 text-sm font-semibold text-gray-700">Günlük Yüksek</th>
                                        <th className="text-right py-4 px-4 text-sm font-semibold text-gray-700">Günlük Düşük</th>
                                        <th className="text-right py-4 px-4 text-sm font-semibold text-gray-700"></th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {watchlistItems.map((item) => {
                                        const inst = item.instrument;
                                        const price = inst.currentPrice;
                                        const isPositive = price?.changePercent && parseFloat(price.changePercent) >= 0;

                                        return (
                                            <tr
                                                key={item.id}
                                                className="border-b border-gray-100 hover:bg-gray-50 transition-colors cursor-pointer"
                                                onClick={() => navigate(`/instruments/detail/${inst.id}`)}
                                            >
                                                <td className="py-4 px-4">
                                                    <div className="flex items-center gap-3">
                                                        <Star className="w-4 h-4 fill-yellow-400 text-yellow-400 flex-shrink-0" />
                                                        <div>
                                                            <p className="font-semibold text-gray-900">{inst.symbol}</p>
                                                            <p className="text-sm text-gray-500">{inst.name}</p>
                                                        </div>
                                                    </div>
                                                </td>
                                                <td className="py-4 px-4">
                                                    <div className="flex items-center gap-2">
                                                        <div
                                                            className="w-2 h-2 rounded-full flex-shrink-0"
                                                            style={{ backgroundColor: getCategoryColor(inst.type) }}
                                                        />
                                                        <span className="text-sm text-gray-600">
                                                                {getCategoryName(inst.type)}
                                                            </span>
                                                    </div>
                                                </td>
                                                <td className="py-4 px-4 text-right">
                                                        <span className="font-semibold text-gray-900">
                                                            {formatPrice(price?.current)}
                                                        </span>
                                                </td>
                                                <td className="py-4 px-4 text-right">
                                                    <div className={`flex items-center justify-end gap-1 ${
                                                        isPositive ? 'text-emerald-600' : 'text-red-500'
                                                    }`}>
                                                        {isPositive ? (
                                                            <TrendingUp className="w-4 h-4" />
                                                        ) : (
                                                            <TrendingDown className="w-4 h-4" />
                                                        )}
                                                        <div className="text-right">
                                                            <p className="text-sm font-semibold">
                                                                {formatPrice(price?.changeAmount)}
                                                            </p>
                                                            <p className="text-xs">
                                                                {price?.changePercent ? `${parseFloat(price.changePercent).toFixed(2)}%` : '0.00%'}
                                                            </p>
                                                        </div>
                                                    </div>
                                                </td>
                                                <td className="py-4 px-4 text-right">
                                                        <span className="text-sm text-gray-600">
                                                            {formatPrice(price?.high)}
                                                        </span>
                                                </td>
                                                <td className="py-4 px-4 text-right">
                                                        <span className="text-sm text-gray-600">
                                                            {formatPrice(price?.low)}
                                                        </span>
                                                </td>
                                                <td className="py-4 px-4 text-right">
                                                    <Button
                                                        variant="ghost"
                                                        size="sm"
                                                        onClick={(e) => {
                                                            e.stopPropagation();
                                                            handleRemoveFromWatchlist(item);
                                                        }}
                                                        className="text-gray-400 hover:text-red-600"
                                                    >
                                                        <Trash2 className="w-4 h-4" />
                                                    </Button>
                                                </td>
                                            </tr>
                                        );
                                    })}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </CardContent>
                </Card>

                {/* Add to Watchlist Modal */}
                {showAddModal && (
                    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
                        <div className="bg-white rounded-lg shadow-xl w-full max-w-md">
                            <div className="p-6">
                                <div className="flex items-center justify-between mb-6">
                                    <h2 className="text-2xl font-bold text-gray-900">Takip Listesine Ekle</h2>
                                    <button
                                        className="text-gray-400 hover:text-gray-600 transition-colors"
                                        onClick={() => {
                                            setShowAddModal(false);
                                            setSelectedInstrument(null);
                                            setSearchTerm('');
                                            setSearchResults([]);
                                        }}
                                    >
                                        <X className="w-6 h-6" />
                                    </button>
                                </div>

                                <div className="space-y-4">
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-2">
                                            Enstrüman Ara
                                        </label>
                                        <div className="relative">
                                            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
                                            <input
                                                type="text"
                                                className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                                placeholder="Sembol, isim veya kategori..."
                                                value={searchTerm}
                                                onChange={(e) => setSearchTerm(e.target.value)}
                                                autoFocus
                                            />
                                        </div>

                                        {searchTerm.length >= 2 && (
                                            <div className="mt-2 max-h-64 overflow-y-auto border border-gray-200 rounded-lg">
                                                {searching ? (
                                                    <div className="px-4 py-3 text-sm text-gray-500 text-center">
                                                        Aranıyor...
                                                    </div>
                                                ) : searchResults.length > 0 ? (
                                                    searchResults.map((inst) => (
                                                        <button
                                                            key={inst.id}
                                                            className={`w-full flex items-center justify-between px-4 py-3 hover:bg-gray-50 transition-colors ${
                                                                selectedInstrument?.id === inst.id
                                                                    ? 'bg-blue-50 border-l-4 border-blue-600'
                                                                    : ''
                                                            }`}
                                                            onClick={() => {
                                                                setSelectedInstrument(inst);
                                                                setSearchTerm('');
                                                            }}
                                                        >
                                                            <div className="flex items-center gap-3">
                                                                <div
                                                                    className="w-3 h-3 rounded-full"
                                                                    style={{ backgroundColor: getCategoryColor(inst.type) }}
                                                                />
                                                                <div className="text-left">
                                                                    <p className="font-medium text-gray-900">{inst.name}</p>
                                                                    <p className="text-xs text-gray-500">{inst.symbol}</p>
                                                                </div>
                                                            </div>
                                                            <span className="text-sm text-gray-600">
                                                                {getCategoryName(inst.type)}
                                                            </span>
                                                        </button>
                                                    ))
                                                ) : (
                                                    <div className="px-4 py-3 text-sm text-gray-500 text-center">
                                                        Enstrüman bulunamadı
                                                    </div>
                                                )}
                                            </div>
                                        )}

                                        {selectedInstrument && !searchTerm && (
                                            <div className="mt-2 p-3 bg-blue-50 border border-blue-200 rounded-lg flex items-center justify-between">
                                                <div className="flex items-center gap-2">
                                                    <Star className="w-4 h-4 text-yellow-500" />
                                                    <span className="font-medium text-gray-900">
                                                        {selectedInstrument.name}
                                                    </span>
                                                </div>
                                                <button
                                                    onClick={() => setSelectedInstrument(null)}
                                                    className="text-gray-400 hover:text-gray-600"
                                                >
                                                    <X className="w-4 h-4" />
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                </div>

                                <div className="flex gap-3 mt-6">
                                    <Button
                                        variant="outline"
                                        className="flex-1"
                                        onClick={() => {
                                            setShowAddModal(false);
                                            setSelectedInstrument(null);
                                            setSearchTerm('');
                                            setSearchResults([]);
                                        }}
                                    >
                                        İptal
                                    </Button>
                                    <Button
                                        variant="default"
                                        className="flex-1 bg-blue-600 hover:bg-blue-700"
                                        onClick={handleAddToWatchlist}
                                        disabled={!selectedInstrument}
                                    >
                                        Ekle
                                    </Button>
                                </div>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}