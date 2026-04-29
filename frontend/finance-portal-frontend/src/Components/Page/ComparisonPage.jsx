import React, { useState, useEffect } from 'react';
import { Search, X, Plus, Loader2, Calendar } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../UI/Card';
import { Button } from '../UI/Button';
import { LineChart, Line, XAxis, YAxis, Tooltip, CartesianGrid, ResponsiveContainer, Legend } from 'recharts';
import { compareInstruments, searchInstruments } from '../../API/comparisonApi.js';

export default function ComparisonPage() {
    const [instrument1, setInstrument1] = useState(null);
    const [instrument2, setInstrument2] = useState(null);
    const [showSelector1, setShowSelector1] = useState(false);
    const [showSelector2, setShowSelector2] = useState(false);
    const [searchTerm1, setSearchTerm1] = useState('');
    const [searchTerm2, setSearchTerm2] = useState('');
    const [searchResults1, setSearchResults1] = useState([]);
    const [searchResults2, setSearchResults2] = useState([]);
    const [comparing, setComparing] = useState(false);
    const [comparisonData, setComparisonData] = useState(null);
    const [timeframe, setTimeframe] = useState('1A');

    // Enstrüman ara (debounced)
    useEffect(() => {
        if (searchTerm1.length >= 2) {
            const timer = setTimeout(async () => {
                try {
                    const data = await searchInstruments(searchTerm1);
                    setSearchResults1(data.content || []);
                } catch (e) {
                    console.error('Search error:', e);
                }
            }, 300);
            return () => clearTimeout(timer);
        } else {
            setSearchResults1([]);
        }
    }, [searchTerm1]);

    useEffect(() => {
        if (searchTerm2.length >= 2) {
            const timer = setTimeout(async () => {
                try {
                    const data = await searchInstruments(searchTerm2);
                    setSearchResults2(data.content || []);
                } catch (e) {
                    console.error('Search error:', e);
                }
            }, 300);
            return () => clearTimeout(timer);
        } else {
            setSearchResults2([]);
        }
    }, [searchTerm2]);

    // Karşılaştır - timeframe değişince otomatik çağır
    useEffect(() => {
        if (instrument1 && instrument2) {
            handleCompare();
        }
    }, [instrument1, instrument2, timeframe]);

    const handleCompare = async () => {
        if (!instrument1 || !instrument2) return;

        setComparing(true);
        try {
            const data = await compareInstruments(instrument1.id, instrument2.id, timeframe);
            setComparisonData(data);
        } catch (e) {
            console.error('Compare error:', e);
        } finally {
            setComparing(false);
        }
    };

    const selectInstrument1 = (inst) => {
        setInstrument1(inst);
        setShowSelector1(false);
        setSearchTerm1('');
    };

    const selectInstrument2 = (inst) => {
        setInstrument2(inst);
        setShowSelector2(false);
        setSearchTerm2('');
    };

    const formatPrice = (price) => {
        if (!price && price !== 0) return '-';
        if (price > 1000) return price.toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        if (price > 1) return price.toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 4 });
        return price.toLocaleString('tr-TR', { minimumFractionDigits: 4, maximumFractionDigits: 6 });
    };

    const formatPercent = (value) => {
        if (!value && value !== 0) return '-';
        return `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`;
    };

    // Chart data
    const chartData = comparisonData?.historicalData?.map(point => ({
        date: new Date(point.date).toLocaleDateString('tr-TR', { day: '2-digit', month: 'short' }),
        inst1: parseFloat(point.price1),
        inst2: parseFloat(point.price2),
    })) || [];

    const metrics1 = comparisonData?.metrics?.instrument1Metrics;
    const metrics2 = comparisonData?.metrics?.instrument2Metrics;

    return (
        <div className="min-h-screen bg-gray-50 p-8">
            <div className="max-w-7xl mx-auto">
                <div className="mb-6">
                    <h1 className="text-3xl font-bold text-gray-900 mb-2">Enstrüman Karşılaştırma</h1>
                    <p className="text-gray-600">İki farklı enstrümanı karşılaştırın ve performanslarını analiz edin</p>
                </div>

                {/* Instrument Selectors */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
                    {/* Selector 1 */}
                    <Card className="border-0 shadow-sm">
                        <CardHeader className="pb-3">
                            <CardTitle className="text-lg font-semibold">1. Enstrüman</CardTitle>
                        </CardHeader>
                        <CardContent>
                            {!instrument1 ? (
                                showSelector1 ? (
                                    <div>
                                        <div className="relative mb-3">
                                            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
                                            <input
                                                type="text"
                                                placeholder="Sembol veya isim ara..."
                                                value={searchTerm1}
                                                onChange={(e) => setSearchTerm1(e.target.value)}
                                                className="w-full pl-10 pr-3 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                                autoFocus
                                            />
                                        </div>
                                        <div className="max-h-64 overflow-y-auto space-y-1">
                                            {searchResults1.length > 0 ? (
                                                searchResults1.map(inst => (
                                                    <button
                                                        key={inst.id}
                                                        onClick={() => selectInstrument1(inst)}
                                                        className="w-full text-left px-3 py-2 hover:bg-gray-50 rounded-lg transition-colors"
                                                    >
                                                        <div className="flex items-center justify-between">
                                                            <div>
                                                                <p className="font-medium text-gray-900">{inst.name}</p>
                                                                <p className="text-xs text-gray-500">{inst.symbol} • {inst.type}</p>
                                                            </div>
                                                        </div>
                                                    </button>
                                                ))
                                            ) : (
                                                <p className="text-sm text-gray-400 text-center py-4">
                                                    {searchTerm1.length >= 2 ? 'Sonuç bulunamadı' : 'Aramaya başlayın...'}
                                                </p>
                                            )}
                                        </div>
                                        <Button
                                            variant="outline"
                                            className="w-full mt-3"
                                            onClick={() => {
                                                setShowSelector1(false);
                                                setSearchTerm1('');
                                            }}
                                        >
                                            İptal
                                        </Button>
                                    </div>
                                ) : (
                                    <Button
                                        variant="outline"
                                        className="w-full h-24 hover:bg-blue-50 hover:border-blue-300 transition-all"
                                        onClick={() => setShowSelector1(true)}
                                    >
                                        <Plus className="w-5 h-5 mr-2" />
                                        Enstrüman Seç
                                    </Button>
                                )
                            ) : (
                                <div className="relative">
                                    <div className="mb-3">
                                        <div className="flex items-center justify-between mb-2">
                                            <div>
                                                <p className="text-sm font-medium text-gray-900">{instrument1.name}</p>
                                                <p className="text-xs text-gray-500">{instrument1.symbol} • {instrument1.type}</p>
                                            </div>
                                            <Button
                                                variant="ghost"
                                                size="sm"
                                                onClick={() => {
                                                    setInstrument1(null);
                                                    setComparisonData(null);
                                                }}
                                            >
                                                <X className="w-4 h-4" />
                                            </Button>
                                        </div>
                                        <p className="text-3xl font-bold text-gray-900">
                                            {formatPrice(comparisonData?.instrument1?.currentPrice)}
                                        </p>
                                    </div>
                                </div>
                            )}
                        </CardContent>
                    </Card>

                    {/* Selector 2 */}
                    <Card className="border-0 shadow-sm">
                        <CardHeader className="pb-3">
                            <CardTitle className="text-lg font-semibold">2. Enstrüman</CardTitle>
                        </CardHeader>
                        <CardContent>
                            {!instrument2 ? (
                                showSelector2 ? (
                                    <div>
                                        <div className="relative mb-3">
                                            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
                                            <input
                                                type="text"
                                                placeholder="Sembol veya isim ara..."
                                                value={searchTerm2}
                                                onChange={(e) => setSearchTerm2(e.target.value)}
                                                className="w-full pl-10 pr-3 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                                autoFocus
                                            />
                                        </div>
                                        <div className="max-h-64 overflow-y-auto space-y-1">
                                            {searchResults2.length > 0 ? (
                                                searchResults2.map(inst => (
                                                    <button
                                                        key={inst.id}
                                                        onClick={() => selectInstrument2(inst)}
                                                        className="w-full text-left px-3 py-2 hover:bg-gray-50 rounded-lg transition-colors"
                                                    >
                                                        <div className="flex items-center justify-between">
                                                            <div>
                                                                <p className="font-medium text-gray-900">{inst.name}</p>
                                                                <p className="text-xs text-gray-500">{inst.symbol} • {inst.type}</p>
                                                            </div>
                                                        </div>
                                                    </button>
                                                ))
                                            ) : (
                                                <p className="text-sm text-gray-400 text-center py-4">
                                                    {searchTerm2.length >= 2 ? 'Sonuç bulunamadı' : 'Aramaya başlayın...'}
                                                </p>
                                            )}
                                        </div>
                                        <Button
                                            variant="outline"
                                            className="w-full mt-3"
                                            onClick={() => {
                                                setShowSelector2(false);
                                                setSearchTerm2('');
                                            }}
                                        >
                                            İptal
                                        </Button>
                                    </div>
                                ) : (
                                    <Button
                                        variant="outline"
                                        className="w-full h-24 hover:bg-blue-50 hover:border-blue-300 transition-all"
                                        onClick={() => setShowSelector2(true)}
                                    >
                                        <Plus className="w-5 h-5 mr-2" />
                                        Enstrüman Seç
                                    </Button>
                                )
                            ) : (
                                <div className="relative">
                                    <div className="mb-3">
                                        <div className="flex items-center justify-between mb-2">
                                            <div>
                                                <p className="text-sm font-medium text-gray-900">{instrument2.name}</p>
                                                <p className="text-xs text-gray-500">{instrument2.symbol} • {instrument2.type}</p>
                                            </div>
                                            <Button
                                                variant="ghost"
                                                size="sm"
                                                onClick={() => {
                                                    setInstrument2(null);
                                                    setComparisonData(null);
                                                }}
                                            >
                                                <X className="w-4 h-4" />
                                            </Button>
                                        </div>
                                        <p className="text-3xl font-bold text-gray-900">
                                            {formatPrice(comparisonData?.instrument2?.currentPrice)}
                                        </p>
                                    </div>
                                </div>
                            )}
                        </CardContent>
                    </Card>
                </div>

                {/* Timeframe Selector */}
                {instrument1 && instrument2 && (
                    <div className="mb-6 flex items-center gap-3">
                        <Calendar className="w-5 h-5 text-gray-500" />
                        <div className="flex gap-2">
                            {['1H', '1A', '3A', '6A', '1Y'].map(tf => (
                                <button
                                    key={tf}
                                    onClick={() => setTimeframe(tf)}
                                    className={`px-4 py-2 text-sm font-semibold rounded-lg transition-all ${
                                        timeframe === tf
                                            ? 'bg-blue-600 text-white shadow-md'
                                            : 'bg-white text-gray-600 hover:bg-gray-50 border border-gray-200'
                                    }`}
                                >
                                    {tf === '1H' ? '1 Hafta' :
                                        tf === '1A' ? '1 Ay' :
                                            tf === '3A' ? '3 Ay' :
                                                tf === '6A' ? '6 Ay' : '1 Yıl'}
                                </button>
                            ))}
                        </div>
                    </div>
                )}

                {/* Loading */}
                {comparing && (
                    <div className="flex items-center justify-center py-12">
                        <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
                    </div>
                )}

                {/* Comparison Results */}
                {!comparing && comparisonData && (
                    <>
                        {/* Price Chart */}
                        <Card className="border-0 shadow-sm mb-6">
                            <CardHeader>
                                <CardTitle>Fiyat Karşılaştırması</CardTitle>
                            </CardHeader>
                            <CardContent>
                                {chartData.length > 0 ? (
                                    <div className="h-[400px]">
                                        <ResponsiveContainer width="100%" height="100%">
                                            <LineChart data={chartData}>
                                                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                                                <XAxis dataKey="date" stroke="#9ca3af" tick={{ fontSize: 11 }} />
                                                <YAxis
                                                    yAxisId="left"
                                                    stroke="#3B82F6"
                                                    tick={{ fontSize: 11 }}
                                                />
                                                <YAxis
                                                    yAxisId="right"
                                                    orientation="right"
                                                    stroke="#EC4899"
                                                    tick={{ fontSize: 11 }}
                                                />
                                                <Tooltip
                                                    formatter={(value) => formatPrice(value)}
                                                    contentStyle={{
                                                        borderRadius: '8px',
                                                        border: 'none',
                                                        boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
                                                    }}
                                                />
                                                <Legend />
                                                <Line
                                                    yAxisId="left"
                                                    type="monotone"
                                                    dataKey="inst1"
                                                    stroke="#3B82F6"
                                                    strokeWidth={2}
                                                    name={instrument1.symbol}
                                                    dot={false}
                                                />
                                                <Line
                                                    yAxisId="right"
                                                    type="monotone"
                                                    dataKey="inst2"
                                                    stroke="#EC4899"
                                                    strokeWidth={2}
                                                    name={instrument2.symbol}
                                                    dot={false}
                                                />
                                            </LineChart>
                                        </ResponsiveContainer>
                                    </div>
                                ) : (
                                    <div className="h-[400px] flex items-center justify-center text-gray-400">
                                        <p className="text-sm">Yeterli tarihsel veri yok</p>
                                    </div>
                                )}
                            </CardContent>
                        </Card>

                        {/* Metrics Table */}
                        <Card className="border-0 shadow-sm">
                            <CardHeader>
                                <CardTitle>Performans Karşılaştırması</CardTitle>
                            </CardHeader>
                            <CardContent>
                                <div className="overflow-x-auto">
                                    <table className="w-full">
                                        <thead>
                                        <tr className="border-b border-gray-200">
                                            <th className="text-left py-3 px-4 text-sm font-semibold text-gray-600">Metrik</th>
                                            <th className="text-right py-3 px-4 text-sm font-semibold text-gray-600">{instrument1.symbol}</th>
                                            <th className="text-right py-3 px-4 text-sm font-semibold text-gray-600">{instrument2.symbol}</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        <tr className="border-b border-gray-100">
                                            <td className="py-4 px-4 text-gray-900">Güncel Fiyat</td>
                                            <td className="py-4 px-4 text-right font-medium">{formatPrice(comparisonData?.instrument1?.currentPrice)}</td>
                                            <td className="py-4 px-4 text-right font-medium">{formatPrice(comparisonData?.instrument2?.currentPrice)}</td>
                                        </tr>
                                        <tr className="border-b border-gray-100">
                                            <td className="py-4 px-4 text-gray-900">Dönem Değişimi</td>
                                            <td className={`py-4 px-4 text-right font-semibold ${
                                                (metrics1?.periodChange || 0) >= 0 ? 'text-emerald-600' : 'text-red-500'
                                            }`}>
                                                {formatPercent(metrics1?.periodChange)}
                                            </td>
                                            <td className={`py-4 px-4 text-right font-semibold ${
                                                (metrics2?.periodChange || 0) >= 0 ? 'text-emerald-600' : 'text-red-500'
                                            }`}>
                                                {formatPercent(metrics2?.periodChange)}
                                            </td>
                                        </tr>
                                        <tr className="border-b border-gray-100">
                                            <td className="py-4 px-4 text-gray-900">Volatilite</td>
                                            <td className="py-4 px-4 text-right">{formatPercent(metrics1?.volatility)}</td>
                                            <td className="py-4 px-4 text-right">{formatPercent(metrics2?.volatility)}</td>
                                        </tr>
                                        <tr className="border-b border-gray-100">
                                            <td className="py-4 px-4 text-gray-900">En Yüksek</td>
                                            <td className="py-4 px-4 text-right text-emerald-600 font-medium">{formatPrice(metrics1?.highestPrice)}</td>
                                            <td className="py-4 px-4 text-right text-emerald-600 font-medium">{formatPrice(metrics2?.highestPrice)}</td>
                                        </tr>
                                        <tr className="border-b border-gray-100">
                                            <td className="py-4 px-4 text-gray-900">En Düşük</td>
                                            <td className="py-4 px-4 text-right text-red-500 font-medium">{formatPrice(metrics1?.lowestPrice)}</td>
                                            <td className="py-4 px-4 text-right text-red-500 font-medium">{formatPrice(metrics2?.lowestPrice)}</td>
                                        </tr>
                                        <tr>
                                            <td className="py-4 px-4 text-gray-900">Fiyat Aralığı</td>
                                            <td className="py-4 px-4 text-right">{formatPrice(metrics1?.priceRange)}</td>
                                            <td className="py-4 px-4 text-right">{formatPrice(metrics2?.priceRange)}</td>
                                        </tr>
                                        </tbody>
                                    </table>
                                </div>
                            </CardContent>
                        </Card>
                    </>
                )}

                {/* Empty State */}
                {!instrument1 && !instrument2 && !comparing && (
                    <Card className="border-0 shadow-sm py-12">
                        <CardContent className="text-center">
                            <div className="max-w-md mx-auto">
                                <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
                                    <Search className="w-8 h-8 text-blue-600" />
                                </div>
                                <h3 className="text-xl font-semibold text-gray-900 mb-2">Karşılaştırma Başlatın</h3>
                                <p className="text-gray-600">
                                    Yukarıdaki kartlardan iki enstrüman seçerek karşılaştırma yapmaya başlayın
                                </p>
                            </div>
                        </CardContent>
                    </Card>
                )}
            </div>
        </div>
    );
}