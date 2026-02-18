import React, { useState, useEffect } from 'react';
import {
    TrendingUp, TrendingDown, Star, ArrowLeft, Loader2,
    RefreshCw, Building2, Globe, Coins, Activity
} from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '../UI/Card';
import { Button } from '../UI/Button';
import {
    AreaChart, Area, XAxis, YAxis, Tooltip,
    CartesianGrid, ResponsiveContainer
} from 'recharts';
import { getInstrumentById, getHistoricalPrices } from '../../API/instrumentsApi';

const TYPE_COLORS = {
    FOREX: '#3B82F6',
    STOCK: '#8B5CF6',
    BOND: '#F59E0B',
    EUROBOND: '#6366F1',
    PRECIOUS: '#EAB308',
    CRYPTO: '#EC4899',
};

const TYPE_LABELS = {
    FOREX: 'Döviz',
    STOCK: 'Hisse Senedi',
    BOND: 'Tahvil/Bono',
    EUROBOND: 'EuroBond',
    PRECIOUS: 'Kıymetli Metal',
    CRYPTO: 'Kripto Para',
};

export default function InstrumentDetailPage() {
    const navigate = useNavigate();
    const { id } = useParams();

    const [instrument, setInstrument] = useState(null);
    const [history, setHistory] = useState([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState(null);
    const [inWatchlist, setInWatchlist] = useState(false);
    const [timeframe, setTimeframe] = useState('Bugün');

    useEffect(() => {
        fetchInstrument();
    }, [id]);

    useEffect(() => {
        if (instrument) {
            if (timeframe === 'Bugün') {
                // Bugün için tarihsel veri çekme, anlık fiyattan grafik oluştur
                setHistory([]);
            } else {
                fetchHistory();
            }
        }
    }, [instrument, timeframe]);

    const fetchInstrument = async () => {
        try {
            setLoading(true);
            setError(null);
            const data = await getInstrumentById(id);
            setInstrument(data);
        } catch (e) {
            console.error('Load instrument error:', e);
            setError('Enstrüman yüklenirken hata oluştu');
        } finally {
            setLoading(false);
        }
    };

    const fetchHistory = async () => {
        if (!instrument) return;
        try {
            const endDate = new Date().toISOString().split('T')[0];
            const startDate = new Date();
            switch (timeframe) {
                case '1H': startDate.setMonth(startDate.getMonth() - 1); break;
                case '3A': startDate.setMonth(startDate.getMonth() - 3); break;
                case '1Y': startDate.setFullYear(startDate.getFullYear() - 1); break;
                default: startDate.setMonth(startDate.getMonth() - 1);
            }
            const start = startDate.toISOString().split('T')[0];
            const data = await getHistoricalPrices(id, start, endDate);
            setHistory(data || []);
        } catch (e) {
            console.error('History fetch error:', e);
        }
    };

    const handleRefresh = async () => {
        setRefreshing(true);
        await fetchInstrument();
        setRefreshing(false);
    };

    const formatPrice = (price) => {
        if (!price && price !== 0) return '-';
        if (price > 1000) return price.toLocaleString('tr-TR', { minimumFractionDigits: 2 });
        if (price > 1) return price.toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 4 });
        return price.toLocaleString('tr-TR', { minimumFractionDigits: 4, maximumFractionDigits: 6 });
    };

    const formatDate = (dateStr) => {
        return new Date(dateStr).toLocaleString('tr-TR', {
            day: '2-digit', month: 'long', year: 'numeric',
            hour: '2-digit', minute: '2-digit',
        });
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center h-96">
                <Loader2 className="w-8 h-8 animate-spin text-gray-400" />
            </div>
        );
    }

    if (error || !instrument) {
        return (
            <div className="p-8 text-center">
                <p className="text-red-500 mb-4">{error || 'Enstrüman bulunamadı'}</p>
                <Button onClick={() => navigate(-1)}>Geri Dön</Button>
            </div>
        );
    }

    const price = instrument.currentPrice;
    const isPositive = (price?.changePercent || 0) >= 0;
    const accentColor = TYPE_COLORS[instrument.type] || '#3B82F6';

    // Chart data
    const chartData = timeframe === 'Bugün' && price
        ? [
            { time: '09:00', value: price.open },
            { time: '10:00', value: price.open + (price.current - price.open) * 0.2 },
            { time: '11:00', value: price.open + (price.current - price.open) * 0.4 },
            { time: '12:00', value: price.high },
            { time: '13:00', value: price.open + (price.current - price.open) * 0.6 },
            { time: '14:00', value: price.low },
            { time: '15:00', value: price.open + (price.current - price.open) * 0.8 },
            { time: '16:00', value: price.current },
        ]
        : history.length > 0
            ? history.map(h => ({ time: h.date, value: h.close }))
            : [];

    // Type-specific fields
    const getTypeSpecificFields = () => {
        const fields = [];
        if (instrument.sector) fields.push({ label: 'Sektör', value: instrument.sector });
        if (instrument.marketCap) fields.push({ label: 'Piyasa Değeri', value: instrument.marketCap.toLocaleString('tr-TR') });
        if (instrument.baseCurrency) fields.push({ label: 'Baz Para', value: instrument.baseCurrency });
        if (instrument.quoteCurrency) fields.push({ label: 'Karşı Para', value: instrument.quoteCurrency });
        if (instrument.blockchain) fields.push({ label: 'Blockchain', value: instrument.blockchain });
        if (instrument.totalSupply) fields.push({ label: 'Toplam Arz', value: instrument.totalSupply.toLocaleString('tr-TR') });
        if (instrument.circulatingSupply) fields.push({ label: 'Dolaşım Arzı', value: instrument.circulatingSupply.toLocaleString('tr-TR') });
        if (instrument.maturityDate) fields.push({ label: 'Vade Tarihi', value: new Date(instrument.maturityDate).toLocaleDateString('tr-TR') });
        if (instrument.couponRate) fields.push({ label: 'Kupon Oranı', value: `%${instrument.couponRate}` });
        if (instrument.faceValue) fields.push({ label: 'Nominal Değer', value: instrument.faceValue.toLocaleString('tr-TR') });
        if (instrument.issuer) fields.push({ label: 'İhraçcı', value: instrument.issuer });
        if (instrument.issueCurrency) fields.push({ label: 'İhraç Para Birimi', value: instrument.issueCurrency });
        if (instrument.metalType) fields.push({ label: 'Metal Türü', value: instrument.metalType });
        if (instrument.unit) fields.push({ label: 'Birim', value: instrument.unit });
        return fields;
    };

    const typeFields = getTypeSpecificFields();

    return (
        <div className="min-h-screen bg-gray-50">
            {/* Top accent bar */}
            <div className="h-1 w-full" style={{ backgroundColor: accentColor }} />

            {/* Header */}
            <div className="bg-white border-b border-gray-200 px-8 py-6">
                <div className="max-w-7xl mx-auto">
                    <div className="flex items-center justify-between mb-6">
                        <Button variant="ghost" onClick={() => navigate(-1)} className="-ml-2 text-gray-500">
                            <ArrowLeft className="w-4 h-4 mr-2" />
                            Geri
                        </Button>
                        <div className="flex gap-2">
                            <Button
                                variant="outline"
                                size="sm"
                                onClick={handleRefresh}
                                disabled={refreshing}
                            >
                                <RefreshCw className={`w-4 h-4 mr-2 ${refreshing ? 'animate-spin' : ''}`} />
                                Yenile
                            </Button>
                            <Button
                                variant={inWatchlist ? 'default' : 'outline'}
                                size="sm"
                                onClick={() => setInWatchlist(!inWatchlist)}
                            >
                                <Star className={`w-4 h-4 mr-2 ${inWatchlist ? 'fill-current' : ''}`} />
                                {inWatchlist ? 'Takipte' : 'Takip Et'}
                            </Button>
                        </div>
                    </div>

                    <div className="flex items-start justify-between">
                        <div>
                            <div className="flex items-center gap-3 mb-2">
                                <span
                                    className="text-xs font-bold px-3 py-1 rounded-full text-white"
                                    style={{ backgroundColor: accentColor }}
                                >
                                    {TYPE_LABELS[instrument.type] || instrument.type}
                                </span>
                                <span className="text-xs text-gray-400 font-mono bg-gray-100 px-2 py-1 rounded">
                                    {instrument.exchange}
                                </span>
                                <span className={`text-xs font-semibold px-2 py-1 rounded-full ${
                                    instrument.active ? 'bg-emerald-50 text-emerald-600' : 'bg-red-50 text-red-500'
                                }`}>
                                    {instrument.active ? 'Aktif' : 'Pasif'}
                                </span>
                            </div>
                            <h1 className="text-4xl font-bold text-gray-900 mb-1">{instrument.name}</h1>
                            <p className="text-gray-400 font-mono">{instrument.symbol}</p>
                        </div>

                        {price && (
                            <div className="text-right">
                                <p className="text-5xl font-bold text-gray-900 mb-2">
                                    {formatPrice(price.current)}
                                    <span className="text-lg text-gray-400 ml-2">{instrument.currency}</span>
                                </p>
                                <div className={`flex items-center justify-end gap-2 text-lg font-semibold ${
                                    isPositive ? 'text-emerald-600' : 'text-red-500'
                                }`}>
                                    {isPositive ? <TrendingUp className="w-5 h-5" /> : <TrendingDown className="w-5 h-5" />}
                                    <span>
                                        {isPositive ? '+' : ''}{formatPrice(price.changeAmount)} ({Math.abs(price.changePercent).toFixed(2)}%)
                                    </span>
                                </div>
                                <p className="text-xs text-gray-400 mt-2">
                                    {formatDate(price.timestamp)}
                                </p>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            <div className="max-w-7xl mx-auto p-8">
                {/* Stats Row */}
                {price && (
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
                        {[
                            { label: 'Açılış', value: formatPrice(price.open), color: 'text-gray-900' },
                            { label: 'Gün Yüksek', value: formatPrice(price.high), color: 'text-emerald-600' },
                            { label: 'Gün Düşük', value: formatPrice(price.low), color: 'text-red-500' },
                            { label: 'Önceki Kapanış', value: formatPrice(price.previousClose), color: 'text-gray-900' },
                        ].map(stat => (
                            <Card key={stat.label} className="border-0 shadow-sm">
                                <CardContent className="pt-5 pb-5">
                                    <p className="text-xs text-gray-500 uppercase tracking-wide mb-2">{stat.label}</p>
                                    <p className={`text-xl font-bold ${stat.color}`}>{stat.value}</p>
                                </CardContent>
                            </Card>
                        ))}
                    </div>
                )}

                {/* Yield Rate (for bonds) */}
                {price?.yieldRate && (
                    <Card className="border-0 shadow-sm mb-6 border-l-4" style={{ borderLeftColor: accentColor }}>
                        <CardContent className="pt-5 pb-5">
                            <div className="flex items-center gap-3">
                                <Activity className="w-5 h-5" style={{ color: accentColor }} />
                                <div>
                                    <p className="text-xs text-gray-500 uppercase tracking-wide">Getiri Oranı</p>
                                    <p className="text-2xl font-bold text-gray-900">%{price.yieldRate.toFixed(2)}</p>
                                </div>
                            </div>
                        </CardContent>
                    </Card>
                )}

                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    {/* Chart */}
                    <div className="lg:col-span-2">
                        <Card className="border-0 shadow-sm">
                            <CardHeader className="pb-4">
                                <div className="flex items-center justify-between">
                                    <CardTitle className="text-lg font-bold text-gray-900">Fiyat Grafiği</CardTitle>
                                    <div className="flex gap-1">
                                        {['Bugün', '1H', '3A', '1Y'].map(tf => (
                                            <button
                                                key={tf}
                                                onClick={() => setTimeframe(tf)}
                                                className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition-all ${
                                                    timeframe === tf
                                                        ? 'text-white'
                                                        : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
                                                }`}
                                                style={timeframe === tf ? { backgroundColor: accentColor } : {}}
                                            >
                                                {tf}
                                            </button>
                                        ))}
                                    </div>
                                </div>
                            </CardHeader>
                            <CardContent>
                                {chartData.length > 0 ? (
                                    <div className="h-[300px]">
                                        <ResponsiveContainer width="100%" height="100%">
                                            <AreaChart data={chartData}>
                                                <defs>
                                                    <linearGradient id="colorValue" x1="0" y1="0" x2="0" y2="1">
                                                        <stop offset="5%" stopColor={accentColor} stopOpacity={0.2} />
                                                        <stop offset="95%" stopColor={accentColor} stopOpacity={0} />
                                                    </linearGradient>
                                                </defs>
                                                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                                                <XAxis dataKey="time" stroke="#9ca3af" tick={{ fontSize: 11 }} />
                                                <YAxis
                                                    stroke="#9ca3af"
                                                    tick={{ fontSize: 11 }}
                                                    domain={['dataMin - 0.1', 'dataMax + 0.1']}
                                                    tickFormatter={(v) => v.toFixed(2)}
                                                />
                                                <Tooltip
                                                    formatter={(value) => [formatPrice(value), 'Fiyat']}
                                                    contentStyle={{
                                                        borderRadius: '8px',
                                                        border: 'none',
                                                        boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
                                                    }}
                                                />
                                                <Area
                                                    type="monotone"
                                                    dataKey="value"
                                                    stroke={accentColor}
                                                    strokeWidth={2}
                                                    fill="url(#colorValue)"
                                                />
                                            </AreaChart>
                                        </ResponsiveContainer>
                                    </div>
                                ) : (
                                    <div className="h-[300px] flex items-center justify-center text-gray-400">
                                        <div className="text-center">
                                            <Activity className="w-8 h-8 mx-auto mb-2 opacity-50" />
                                            <p className="text-sm">Tarihsel veri bulunamadı</p>
                                        </div>
                                    </div>
                                )}
                            </CardContent>
                        </Card>
                    </div>

                    {/* Info Panel */}
                    <div className="flex flex-col gap-4">
                        {/* General Info */}
                        <Card className="border-0 shadow-sm">
                            <CardHeader className="pb-3">
                                <CardTitle className="text-sm font-semibold text-gray-500 uppercase tracking-wide flex items-center gap-2">
                                    <Globe className="w-4 h-4" />
                                    Genel Bilgiler
                                </CardTitle>
                            </CardHeader>
                            <CardContent className="pt-0">
                                <div className="space-y-3">
                                    {[
                                        { label: 'Tür', value: TYPE_LABELS[instrument.type] || instrument.type },
                                        { label: 'Borsa', value: instrument.exchange },
                                        { label: 'Para Birimi', value: instrument.currency },
                                    ].map(field => (
                                        <div key={field.label} className="flex justify-between items-center py-2 border-b border-gray-50 last:border-0">
                                            <span className="text-sm text-gray-500">{field.label}</span>
                                            <span className="text-sm font-semibold text-gray-900">{field.value}</span>
                                        </div>
                                    ))}
                                </div>
                            </CardContent>
                        </Card>

                        {/* Type-specific Info */}
                        {typeFields.length > 0 && (
                            <Card className="border-0 shadow-sm">
                                <CardHeader className="pb-3">
                                    <CardTitle className="text-sm font-semibold text-gray-500 uppercase tracking-wide flex items-center gap-2">
                                        <Building2 className="w-4 h-4" />
                                        {TYPE_LABELS[instrument.type]} Bilgileri
                                    </CardTitle>
                                </CardHeader>
                                <CardContent className="pt-0">
                                    <div className="space-y-3">
                                        {typeFields.map(field => (
                                            <div key={field.label} className="flex justify-between items-center py-2 border-b border-gray-50 last:border-0">
                                                <span className="text-sm text-gray-500">{field.label}</span>
                                                <span className="text-sm font-semibold text-gray-900">{field.value}</span>
                                            </div>
                                        ))}
                                    </div>
                                </CardContent>
                            </Card>
                        )}

                        {/* Description */}
                        {instrument.description && (
                            <Card className="border-0 shadow-sm">
                                <CardHeader className="pb-3">
                                    <CardTitle className="text-sm font-semibold text-gray-500 uppercase tracking-wide">
                                        Açıklama
                                    </CardTitle>
                                </CardHeader>
                                <CardContent className="pt-0">
                                    <p className="text-sm text-gray-600 leading-relaxed">{instrument.description}</p>
                                </CardContent>
                            </Card>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}