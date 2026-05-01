import React, { useState, useEffect } from 'react';
import {
    TrendingUp, TrendingDown, Star, ArrowLeft, Loader2,
    RefreshCw, Building2, Globe, Coins, Activity, Bell, Trash2
} from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '../UI/Card';
import { Button } from '../UI/Button';
import {
    AreaChart, Area, XAxis, YAxis, Tooltip,
    CartesianGrid, ResponsiveContainer
} from 'recharts';
import { getInstrumentById, getHistoricalPrices } from '../../API/instrumentsApi';
import { addToWatchlist, removeFromWatchlist, isInWatchlist } from '../../API/watchlistApi';
import { createPriceAlert, getActiveUserAlerts, deletePriceAlert } from '../../API/priceAlertApi';

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
    const [watchlistLoading, setWatchlistLoading] = useState(false);
    const [timeframe, setTimeframe] = useState('1H');

    // Fiyat Alarmı State'leri
    const [showAlertModal, setShowAlertModal] = useState(false);
    const [alertTargetPrice, setAlertTargetPrice] = useState('');
    const [alertCondition, setAlertCondition] = useState('ABOVE');
    const [alertLoading, setAlertLoading] = useState(false);
    const [activeAlerts, setActiveAlerts] = useState([]);

    useEffect(() => {
        fetchInstrument();
    }, [id]);

    useEffect(() => {
        if (instrument) {
            checkWatchlistStatus();
            fetchHistory();
            fetchActiveAlerts();
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

    const checkWatchlistStatus = async () => {
        try {
            const status = await isInWatchlist(id);
            setInWatchlist(status);
        } catch (error) {
            console.error('Watchlist check error:', error);
        }
    };

    const fetchActiveAlerts = async () => {
        try {
            const data = await getActiveUserAlerts();
            setActiveAlerts(data.filter(a => a.instrumentSymbol === instrument?.symbol));
        } catch (e) {
            console.error('Alert fetch error:', e);
        }
    };

    const handleToggleWatchlist = async () => {
        setWatchlistLoading(true);
        try {
            if (inWatchlist) {
                await removeFromWatchlist(id);
                setInWatchlist(false);
            } else {
                await addToWatchlist(id);
                setInWatchlist(true);
            }
        } catch (error) {
            console.error('Watchlist toggle error:', error);
            alert('İşlem başarısız');
        } finally {
            setWatchlistLoading(false);
        }
    };

    const fetchHistory = async () => {
        if (!instrument) return;
        try {
            const end = new Date();
            const start = new Date();

            switch (timeframe) {
                case '1H': start.setDate(end.getDate() - 7); break;
                case '1A': start.setMonth(end.getMonth() - 1); break;
                case '3A': start.setMonth(end.getMonth() - 3); break;
                case '6A': start.setMonth(end.getMonth() - 6); break;
                case '1Y': start.setFullYear(end.getFullYear() - 1); break;
                default: start.setMonth(end.getMonth() - 1);
            }

            const startDate = start.toISOString().split('T')[0];
            const endDate = end.toISOString().split('T')[0];

            const data = await getHistoricalPrices(id, startDate, endDate);
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

    const handleCreateAlert = async () => {
        if (!alertTargetPrice) return;
        setAlertLoading(true);
        try {
            await createPriceAlert(parseInt(id), parseFloat(alertTargetPrice), alertCondition);
            setShowAlertModal(false);
            setAlertTargetPrice('');
            await fetchActiveAlerts();
        } catch (e) {
            console.error('Alert create error:', e);
            alert('Alarm oluşturulamadı');
        } finally {
            setAlertLoading(false);
        }
    };

    const handleDeleteAlert = async (alertId) => {
        try {
            await deletePriceAlert(alertId);
            await fetchActiveAlerts();
        } catch (e) {
            console.error('Alert delete error:', e);
        }
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

    const chartData = history.length > 0
        ? history.map(h => ({ time: h.date, value: h.close }))
        : [];

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
            <div className="h-1 w-full" style={{ backgroundColor: accentColor }} />

            <div className="bg-white border-b border-gray-200 px-8 py-6">
                <div className="max-w-7xl mx-auto">
                    <div className="flex items-center justify-between mb-6">
                        <Button variant="ghost" onClick={() => navigate(-1)} className="-ml-2 text-gray-500">
                            <ArrowLeft className="w-4 h-4 mr-2" />
                            Geri
                        </Button>
                        <div className="flex gap-2">
                            <Button variant="outline" size="sm" onClick={handleRefresh} disabled={refreshing}>
                                <RefreshCw className={`w-4 h-4 mr-2 ${refreshing ? 'animate-spin' : ''}`} />
                                Yenile
                            </Button>
                            {/* ✅ Fiyat Alarmı Butonu */}
                            <Button variant="outline" size="sm" onClick={() => setShowAlertModal(true)}>
                                <Bell className="w-4 h-4 mr-2" />
                                Fiyat Alarmı
                            </Button>
                            <Button
                                variant={inWatchlist ? 'default' : 'outline'}
                                size="sm"
                                onClick={handleToggleWatchlist}
                                disabled={watchlistLoading}
                                className={inWatchlist ? 'bg-yellow-500 hover:bg-yellow-600 text-white' : ''}
                            >
                                <Star className={`w-4 h-4 mr-2 ${inWatchlist ? 'fill-white' : ''}`} />
                                {watchlistLoading ? 'İşleniyor...' : inWatchlist ? 'Takipte' : 'Takip Et'}
                            </Button>
                        </div>
                    </div>

                    <div className="flex items-start justify-between">
                        <div>
                            <div className="flex items-center gap-3 mb-2">
                                <span className="text-xs font-bold px-3 py-1 rounded-full text-white" style={{ backgroundColor: accentColor }}>
                                    {TYPE_LABELS[instrument.type] || instrument.type}
                                </span>
                                <span className="text-xs text-gray-400 font-mono bg-gray-100 px-2 py-1 rounded">
                                    {instrument.exchange}
                                </span>
                                <span className={`text-xs font-semibold px-2 py-1 rounded-full ${instrument.active ? 'bg-emerald-50 text-emerald-600' : 'bg-red-50 text-red-500'}`}>
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
                                <div className={`flex items-center justify-end gap-2 text-lg font-semibold ${isPositive ? 'text-emerald-600' : 'text-red-500'}`}>
                                    {isPositive ? <TrendingUp className="w-5 h-5" /> : <TrendingDown className="w-5 h-5" />}
                                    <span>
                                        {isPositive ? '+' : ''}{formatPrice(price.changeAmount)} ({Math.abs(price.changePercent).toFixed(2)}%)
                                    </span>
                                </div>
                                <p className="text-xs text-gray-400 mt-2">{formatDate(price.timestamp)}</p>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            <div className="max-w-7xl mx-auto p-8">
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
                    <div className="lg:col-span-2">
                        <Card className="border-0 shadow-sm">
                            <CardHeader className="pb-4">
                                <div className="flex items-center justify-between">
                                    <CardTitle className="text-lg font-bold text-gray-900">Fiyat Grafiği</CardTitle>
                                    <div className="flex gap-1">
                                        {['1H', '1A', '3A', '6A', '1Y'].map(tf => (
                                            <button
                                                key={tf}
                                                onClick={() => setTimeframe(tf)}
                                                className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition-all ${
                                                    timeframe === tf ? 'text-white' : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
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
                                                    contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
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

                    <div className="flex flex-col gap-4">
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

                        {/* Aktif Alarmlar Kartı */}
                        <Card className="border-0 shadow-sm">
                            <CardHeader className="pb-3">
                                <CardTitle className="text-sm font-semibold text-gray-500 uppercase tracking-wide flex items-center gap-2">
                                    <Bell className="w-4 h-4" />
                                    Fiyat Alarmları
                                </CardTitle>
                            </CardHeader>
                            <CardContent className="pt-0">
                                {activeAlerts.length === 0 ? (
                                    <p className="text-sm text-gray-400 mb-3">Aktif alarm yok</p>
                                ) : (
                                    <div className="space-y-2 mb-3">
                                        {activeAlerts.map(alert => (
                                            <div key={alert.id} className="flex items-center justify-between py-2 border-b border-gray-50 last:border-0">
                                                <div>
                                                    <p className="text-sm font-medium text-gray-900">
                                                        {alert.condition === 'ABOVE' ? '↑' : '↓'} {formatPrice(alert.targetPrice)} {instrument.currency}
                                                    </p>
                                                    <p className="text-xs text-gray-400">
                                                        {alert.condition === 'ABOVE' ? 'Üzerine çıkınca' : 'Altına düşünce'}
                                                    </p>
                                                </div>
                                                <button
                                                    onClick={() => handleDeleteAlert(alert.id)}
                                                    className="text-red-400 hover:text-red-600"
                                                >
                                                    <Trash2 className="w-4 h-4" />
                                                </button>
                                            </div>
                                        ))}
                                    </div>
                                )}
                                <Button variant="outline" size="sm" className="w-full" onClick={() => setShowAlertModal(true)}>
                                    <Bell className="w-4 h-4 mr-2" />
                                    Alarm Ekle
                                </Button>
                            </CardContent>
                        </Card>

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

                        {instrument.description && (
                            <Card className="border-0 shadow-sm">
                                <CardHeader className="pb-3">
                                    <CardTitle className="text-sm font-semibold text-gray-500 uppercase tracking-wide">Açıklama</CardTitle>
                                </CardHeader>
                                <CardContent className="pt-0">
                                    <p className="text-sm text-gray-600 leading-relaxed">{instrument.description}</p>
                                </CardContent>
                            </Card>
                        )}
                    </div>
                </div>
            </div>

            {/* Fiyat Alarmı Modal */}
            {showAlertModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-xl shadow-xl max-w-md w-full p-6">
                        <h3 className="text-xl font-bold text-gray-900 mb-4">Fiyat Alarmı Oluştur</h3>

                        <div className="mb-4">
                            <p className="text-sm text-gray-500 mb-4">
                                Güncel Fiyat: <span className="font-semibold text-gray-900">
                                    {formatPrice(price?.current)} {instrument.currency}
                                </span>
                            </p>

                            <label className="block text-sm font-medium text-gray-700 mb-2">Alarm Koşulu</label>
                            <div className="flex gap-2 mb-4">
                                <button
                                    onClick={() => setAlertCondition('ABOVE')}
                                    className={`flex-1 py-2 rounded-lg text-sm font-medium border transition-colors ${
                                        alertCondition === 'ABOVE'
                                            ? 'bg-green-50 border-green-500 text-green-700'
                                            : 'border-gray-200 text-gray-600'
                                    }`}
                                >
                                    ↑ Üzerine Çıkınca
                                </button>
                                <button
                                    onClick={() => setAlertCondition('BELOW')}
                                    className={`flex-1 py-2 rounded-lg text-sm font-medium border transition-colors ${
                                        alertCondition === 'BELOW'
                                            ? 'bg-red-50 border-red-500 text-red-700'
                                            : 'border-gray-200 text-gray-600'
                                    }`}
                                >
                                    ↓ Altına Düşünce
                                </button>
                            </div>

                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                Hedef Fiyat ({instrument.currency})
                            </label>
                            <input
                                type="number"
                                value={alertTargetPrice}
                                onChange={(e) => setAlertTargetPrice(e.target.value)}
                                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                placeholder="0.00"
                                step="0.01"
                            />
                        </div>

                        <div className="flex gap-3">
                            <Button
                                variant="outline"
                                className="flex-1"
                                onClick={() => { setShowAlertModal(false); setAlertTargetPrice(''); }}
                                disabled={alertLoading}
                            >
                                İptal
                            </Button>
                            <Button
                                className="flex-1"
                                onClick={handleCreateAlert}
                                disabled={!alertTargetPrice || alertLoading}
                            >
                                {alertLoading ? (
                                    <><Loader2 className="w-4 h-4 mr-2 animate-spin" />Oluşturuluyor...</>
                                ) : (
                                    <><Bell className="w-4 h-4 mr-2" />Alarm Oluştur</>
                                )}
                            </Button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}