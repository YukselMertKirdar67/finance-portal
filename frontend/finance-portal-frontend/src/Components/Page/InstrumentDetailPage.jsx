import React, { useState, useEffect, useRef } from 'react';
import {
    TrendingUp, TrendingDown, Star, ArrowLeft, Loader2,
    RefreshCw, Building2, Globe, Activity, Bell, Trash2
} from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Card, CardContent, CardHeader, CardTitle } from '../UI/Card';
import { Button } from '../UI/Button';
import { createChart, CandlestickSeries, LineSeries } from 'lightweight-charts';
import {
    AreaChart, Area, XAxis, YAxis, Tooltip,
    CartesianGrid, ResponsiveContainer, Line
} from 'recharts';
import { getInstrumentById, getHistoricalPrices } from '../../API/instrumentsApi';
import { addToWatchlist, removeFromWatchlist, isInWatchlist } from '../../API/watchlistApi';
import { createPriceAlert, getActiveUserAlerts, deletePriceAlert } from '../../API/priceAlertApi';
import { useInstrumentWebSocket } from '../../Hooks/useWebSocket';

const TYPE_COLORS = {
    FOREX: '#3B82F6', STOCK: '#8B5CF6', BOND: '#F59E0B',
    FUND: '#F97316', PRECIOUS: '#EAB308', CRYPTO: '#EC4899', VIOP: '#EF4444',
};

function CandlestickChart({ data }) {
    const chartContainerRef = useRef(null);
    const chartRef = useRef(null);

    useEffect(() => {
        if (!chartContainerRef.current || !data || data.length === 0) return;
        if (chartRef.current) { chartRef.current.remove(); chartRef.current = null; }

        const chart = createChart(chartContainerRef.current, {
            width: chartContainerRef.current.clientWidth, height: 450,
            layout: { background: { color: 'transparent' }, textColor: '#9ca3af' },
            grid: { vertLines: { color: '#f0f0f0' }, horzLines: { color: '#f0f0f0' } },
            crosshair: { mode: 1 },
            rightPriceScale: { borderColor: '#e5e7eb' },
            timeScale: { borderColor: '#e5e7eb', timeVisible: true },
        });
        chartRef.current = chart;

        const candleSeries = chart.addSeries(CandlestickSeries, {
            upColor: '#10b981', downColor: '#ef4444',
            borderUpColor: '#10b981', borderDownColor: '#ef4444',
            wickUpColor: '#10b981', wickDownColor: '#ef4444',
        });

        const chartData = data
            .filter(h => h.open && h.high && h.low && h.close)
            .map(h => ({
                time: h.date, open: parseFloat(h.open), high: parseFloat(h.high),
                low: parseFloat(h.low), close: parseFloat(h.close),
            }))
            .sort((a, b) => a.time.localeCompare(b.time));

        candleSeries.setData(chartData);
        const period = 20;
        const maData = chartData
            .map((item, index) => {
                if (index < period - 1) return null;
                const slice = chartData.slice(index - period + 1, index + 1);
                const avg = slice.reduce((sum, d) => sum + d.close, 0) / period;
                return { time: item.time, value: parseFloat(avg.toFixed(4)) };
            }).filter(Boolean);

        const maSeries = chart.addSeries(LineSeries, { color: '#f97316', lineWidth: 2, title: 'MA20' });
        maSeries.setData(maData);
        chart.timeScale().fitContent();

        const handleResize = () => {
            if (chartContainerRef.current && chartRef.current) {
                chartRef.current.applyOptions({ width: chartContainerRef.current.clientWidth });
            }
        };
        window.addEventListener('resize', handleResize);
        return () => {
            window.removeEventListener('resize', handleResize);
            if (chartRef.current) { chartRef.current.remove(); chartRef.current = null; }
        };
    }, [data]);

    return <div ref={chartContainerRef} className="w-full" style={{ height: '450px' }} />;
}

export default function InstrumentDetailPage() {
    const navigate = useNavigate();
    const { id } = useParams();
    const { t, i18n } = useTranslation();

    const [instrument, setInstrument] = useState(null);
    const [history, setHistory] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [inWatchlist, setInWatchlist] = useState(false);
    const [watchlistLoading, setWatchlistLoading] = useState(false);
    const [timeframe, setTimeframe] = useState('1H');
    const [chartType, setChartType] = useState('candlestick');
    const [showAlertModal, setShowAlertModal] = useState(false);
    const [alertTargetPrice, setAlertTargetPrice] = useState('');
    const [alertCondition, setAlertCondition] = useState('ABOVE');
    const [alertLoading, setAlertLoading] = useState(false);
    const [activeAlerts, setActiveAlerts] = useState([]);
    const [livePrice, setLivePrice] = useState(null);

    const TYPE_LABELS = {
        FOREX: t('markets.forex'),
        STOCK: t('markets.stocks'),
        BOND: t('markets.bonds'),
        FUND: t('instruments.fund.title'),
        PRECIOUS: t('markets.precious'),
        CRYPTO: t('markets.crypto'),
        VIOP: t('markets.viop'),
    };

    useEffect(() => { fetchInstrument(); }, [id]);
    useEffect(() => {
        if (instrument) { checkWatchlistStatus(); fetchHistory(); fetchActiveAlerts(); }
    }, [instrument, timeframe]);

    useInstrumentWebSocket(instrument?.id, (priceUpdate) => {
        setLivePrice(priceUpdate);
    });

    const fetchInstrument = async () => {
        try {
            setLoading(true);
            setError(null);
            const data = await getInstrumentById(id);
            setInstrument(data);
        } catch (e) {
            setError(t('instrumentDetail.loadError'));
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
            alert(t('instrumentDetail.watchlistError'));
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
            const data = await getHistoricalPrices(id, start.toISOString().split('T')[0], end.toISOString().split('T')[0]);
            setHistory(data || []);
        } catch (e) {
            console.error('History fetch error:', e);
        }
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
            alert(t('instrumentDetail.alertCreateError'));
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
        const locale = i18n.language === 'en' ? 'en-US' : 'tr-TR';
        return new Date(dateStr).toLocaleString(locale, {
            day: '2-digit', month: 'long', year: 'numeric',
            hour: '2-digit', minute: '2-digit',
        });
    };

    const getTypeSpecificFields = () => {
        const fields = [];
        if (instrument.sector) fields.push({ label: t('instrumentDetail.fields.sector'), value: instrument.sector });
        if (instrument.marketCap) fields.push({ label: t('instrumentDetail.fields.marketCap'), value: instrument.marketCap.toLocaleString('tr-TR') });
        if (instrument.baseCurrency) fields.push({ label: t('instrumentDetail.fields.baseCurrency'), value: instrument.baseCurrency });
        if (instrument.quoteCurrency) fields.push({ label: t('instrumentDetail.fields.quoteCurrency'), value: instrument.quoteCurrency });
        if (instrument.blockchain) fields.push({ label: t('instrumentDetail.fields.blockchain'), value: instrument.blockchain });
        if (instrument.totalSupply) fields.push({ label: t('instrumentDetail.fields.totalSupply'), value: instrument.totalSupply.toLocaleString('tr-TR') });
        if (instrument.circulatingSupply) fields.push({ label: t('instrumentDetail.fields.circulatingSupply'), value: instrument.circulatingSupply.toLocaleString('tr-TR') });
        if (instrument.maturityDate) fields.push({ label: t('instrumentDetail.fields.maturityDate'), value: new Date(instrument.maturityDate).toLocaleDateString(i18n.language === 'en' ? 'en-US' : 'tr-TR') });
        if (instrument.couponRate) fields.push({ label: t('instrumentDetail.fields.couponRate'), value: `%${instrument.couponRate}` });
        if (instrument.faceValue) fields.push({ label: t('instrumentDetail.fields.faceValue'), value: instrument.faceValue.toLocaleString('tr-TR') });
        if (instrument.issuer) fields.push({ label: t('instrumentDetail.fields.issuer'), value: instrument.issuer });
        if (instrument.metalType) fields.push({ label: t('instrumentDetail.fields.metalType'), value: instrument.metalType });
        if (instrument.unit) fields.push({ label: t('instrumentDetail.fields.unit'), value: instrument.unit });
        if (instrument.fundCode) fields.push({ label: t('instrumentDetail.fields.fundCode'), value: instrument.fundCode });
        if (instrument.fundType) fields.push({ label: t('instrumentDetail.fields.fundType'), value: instrument.fundType });
        if (instrument.totalValue) fields.push({ label: t('instrumentDetail.fields.portfolioSize'), value: instrument.totalValue.toLocaleString('tr-TR') + ' USD' });
        if (instrument.investorCount) fields.push({ label: t('instrumentDetail.fields.investorCount'), value: instrument.investorCount.toLocaleString('tr-TR') });
        if (instrument.underlyingAsset) fields.push({ label: t('instrumentDetail.fields.underlyingAsset'), value: instrument.underlyingAsset });
        if (instrument.contractType) fields.push({ label: t('instrumentDetail.fields.contractType'), value: instrument.contractType });
        if (instrument.expiryDate) fields.push({ label: t('instrumentDetail.fields.expiryDate'), value: new Date(instrument.expiryDate).toLocaleDateString(i18n.language === 'en' ? 'en-US' : 'tr-TR') });
        if (instrument.initialMargin) fields.push({ label: t('instrumentDetail.fields.initialMargin'), value: '₺' + instrument.initialMargin.toLocaleString('tr-TR') });
        return fields;
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
                <p className="text-red-500 mb-4">{error || t('instrumentDetail.notFound')}</p>
                <Button onClick={() => navigate(-1)}>{t('common.back')}</Button>
            </div>
        );
    }

    const price = livePrice ? {
        current: livePrice.currentPrice, changeAmount: livePrice.changeAmount,
        changePercent: livePrice.changePercent, previousClose: livePrice.previousClose,
        open: instrument.currentPrice?.open, high: instrument.currentPrice?.high,
        low: instrument.currentPrice?.low, timestamp: livePrice.timestamp,
        yieldRate: instrument.currentPrice?.yieldRate,
    } : instrument.currentPrice;

    const isPositive = (price?.changePercent || 0) > 0;
    const isNeutral = (price?.changePercent || 0) === 0;
    const accentColor = TYPE_COLORS[instrument.type] || '#3B82F6';
    const typeFields = getTypeSpecificFields();

    const areaChartData = history.map(h => ({ time: h.date, value: h.close }));
    const period = 20;
    const areaChartDataWithMA = areaChartData.map((item, index) => {
        if (index < period - 1) return { ...item, ma: null };
        const slice = areaChartData.slice(index - period + 1, index + 1);
        const avg = slice.reduce((sum, d) => sum + d.value, 0) / period;
        return { ...item, ma: parseFloat(avg.toFixed(4)) };
    });

    return (
        <div className="min-h-screen bg-gray-50">
            <div className="h-1 w-full" style={{ backgroundColor: accentColor }} />

            <div className="bg-white border-b border-gray-200 px-8 py-6">
                <div className="max-w-7xl mx-auto">
                    <div className="flex items-center justify-between mb-6">
                        <Button variant="ghost" onClick={() => navigate(-1)} className="-ml-2 text-gray-500">
                            <ArrowLeft className="w-4 h-4 mr-2" />
                            {t('common.back')}
                        </Button>
                        <div className="flex gap-2">
                            <Button variant="outline" size="sm" onClick={() => setShowAlertModal(true)}>
                                <Bell className="w-4 h-4 mr-2" />
                                {t('instrumentDetail.priceAlert')}
                            </Button>
                            <Button
                                variant={inWatchlist ? 'default' : 'outline'}
                                size="sm"
                                onClick={handleToggleWatchlist}
                                disabled={watchlistLoading}
                                className={inWatchlist ? 'bg-yellow-500 hover:bg-yellow-600 text-white' : ''}
                            >
                                <Star className={`w-4 h-4 mr-2 ${inWatchlist ? 'fill-white' : ''}`} />
                                {watchlistLoading
                                    ? t('instrumentDetail.processing')
                                    : inWatchlist
                                        ? t('instrumentDetail.following')
                                        : t('instrumentDetail.follow')}
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
                                    {instrument.active ? t('profile.active') : t('portfolio.passive')}
                                </span>
                            </div>
                            <h1 className="text-4xl font-bold text-gray-900 mb-1">{instrument.name}</h1>
                            <p className="text-gray-400 font-mono">{instrument.symbol}</p>
                        </div>

                        {price && (
                            <div className="text-right">
                                {instrument.type === 'BOND' ? (
                                    <p className="text-5xl font-bold text-gray-900 mb-2">
                                        %{formatPrice(price.current)}
                                        <span className="text-lg text-gray-400 ml-2">{t('instrumentDetail.yield')}</span>
                                    </p>
                                ) : (
                                    <p className="text-5xl font-bold text-gray-900 mb-2">
                                        {formatPrice(price.current)}
                                        <span className="text-lg text-gray-400 ml-2">{instrument.currency}</span>
                                    </p>
                                )}
                                <div className={`flex items-center justify-end gap-2 text-lg font-semibold ${
                                    isNeutral ? 'text-gray-500' : isPositive ? 'text-emerald-600' : 'text-red-500'
                                }`}>
                                    {isNeutral ? <span>—</span> : isPositive ? <TrendingUp className="w-5 h-5" /> : <TrendingDown className="w-5 h-5" />}
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
                        {instrument.type === 'BOND' ? (
                            <>
                                {[
                                    { label: t('instrumentDetail.currentYield'), value: `%${formatPrice(price.current)}`, color: 'text-amber-600' },
                                    { label: t('instrumentDetail.prevClose'), value: `%${formatPrice(price.previousClose)}`, color: 'text-gray-900' },
                                    { label: t('instrumentDetail.change'), value: `${price.changeAmount >= 0 ? '+' : ''}${formatPrice(price.changeAmount)}`, color: price.changeAmount >= 0 ? 'text-emerald-600' : 'text-red-500' },
                                    { label: t('instrumentDetail.changePercent'), value: `${price.changePercent >= 0 ? '+' : ''}${Math.abs(price.changePercent).toFixed(2)}%`, color: price.changePercent >= 0 ? 'text-emerald-600' : 'text-red-500' },
                                ].map(stat => (
                                    <Card key={stat.label} className="border-0 shadow-sm">
                                        <CardContent className="pt-5 pb-5">
                                            <p className="text-xs text-gray-500 uppercase tracking-wide mb-2">{stat.label}</p>
                                            <p className={`text-xl font-bold ${stat.color}`}>{stat.value}</p>
                                        </CardContent>
                                    </Card>
                                ))}
                            </>
                        ) : (
                            <>
                                {[
                                    { label: t('instrumentDetail.open'), value: formatPrice(price.open), color: 'text-gray-900' },
                                    { label: t('instrumentDetail.dayHigh'), value: formatPrice(price.high), color: 'text-emerald-600' },
                                    { label: t('instrumentDetail.dayLow'), value: formatPrice(price.low), color: 'text-red-500' },
                                    { label: t('instrumentDetail.prevClose'), value: formatPrice(price.previousClose), color: 'text-gray-900' },
                                ].map(stat => (
                                    <Card key={stat.label} className="border-0 shadow-sm">
                                        <CardContent className="pt-5 pb-5">
                                            <p className="text-xs text-gray-500 uppercase tracking-wide mb-2">{stat.label}</p>
                                            <p className={`text-xl font-bold ${stat.color}`}>{stat.value}</p>
                                        </CardContent>
                                    </Card>
                                ))}
                            </>
                        )}
                    </div>
                )}

                {price?.yieldRate && (
                    <Card className="border-0 shadow-sm mb-6 border-l-4" style={{ borderLeftColor: accentColor }}>
                        <CardContent className="pt-5 pb-5">
                            <div className="flex items-center gap-3">
                                <Activity className="w-5 h-5" style={{ color: accentColor }} />
                                <div>
                                    <p className="text-xs text-gray-500 uppercase tracking-wide">{t('instrumentDetail.yieldRate')}</p>
                                    <p className="text-2xl font-bold text-gray-900">%{price.yieldRate.toFixed(2)}</p>
                                </div>
                            </div>
                        </CardContent>
                    </Card>
                )}

                {/* Chart */}
                <Card className="border-0 shadow-sm mb-6">
                    <CardHeader className="pb-4">
                        <div className="flex items-center justify-between">
                            <div className="flex items-center gap-3">
                                <CardTitle className="text-lg font-bold text-gray-900">{t('instrumentDetail.priceChart')}</CardTitle>
                                <div className="flex items-center bg-gray-100 rounded-lg p-1">
                                    <button
                                        onClick={() => setChartType('candlestick')}
                                        className={`px-3 py-1 text-xs font-semibold rounded-md transition-all ${
                                            chartType === 'candlestick' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'
                                        }`}
                                    >
                                        {t('instrumentDetail.candlestick')}
                                    </button>
                                    <button
                                        onClick={() => setChartType('area')}
                                        className={`px-3 py-1 text-xs font-semibold rounded-md transition-all ${
                                            chartType === 'area' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'
                                        }`}
                                    >
                                        {t('instrumentDetail.area')}
                                    </button>
                                </div>
                            </div>
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
                        {history.length > 0 ? (
                            chartType === 'candlestick' ? (
                                <CandlestickChart data={history} />
                            ) : (
                                <div className="h-[450px]">
                                    <ResponsiveContainer width="100%" height="100%">
                                        <AreaChart data={areaChartDataWithMA}>
                                            <defs>
                                                <linearGradient id="colorValue" x1="0" y1="0" x2="0" y2="1">
                                                    <stop offset="5%" stopColor={accentColor} stopOpacity={0.2} />
                                                    <stop offset="95%" stopColor={accentColor} stopOpacity={0} />
                                                </linearGradient>
                                            </defs>
                                            <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                                            <XAxis dataKey="time" stroke="#9ca3af" tick={{ fontSize: 11 }} />
                                            <YAxis stroke="#9ca3af" tick={{ fontSize: 11 }} domain={['dataMin - 0.1', 'dataMax + 0.1']} tickFormatter={(v) => v.toFixed(2)} />
                                            <Tooltip
                                                formatter={(value) => [formatPrice(value), t('instrumentDetail.price')]}
                                                contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                                            />
                                            <Area type="monotone" dataKey="value" stroke={accentColor} strokeWidth={2} fill="url(#colorValue)" />
                                            <Line type="monotone" dataKey="ma" stroke="#f97316" strokeWidth={2} dot={false} name="MA20" />
                                        </AreaChart>
                                    </ResponsiveContainer>
                                </div>
                            )
                        ) : (
                            <div className="h-[450px] flex items-center justify-center text-gray-400">
                                <div className="text-center">
                                    <Activity className="w-8 h-8 mx-auto mb-2 opacity-50" />
                                    <p className="text-sm">{t('instrumentDetail.noHistory')}</p>
                                </div>
                            </div>
                        )}
                    </CardContent>
                </Card>

                {/* Info Cards */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <Card className="border-0 shadow-sm">
                        <CardHeader className="pb-3">
                            <CardTitle className="text-sm font-semibold text-gray-500 uppercase tracking-wide flex items-center gap-2">
                                <Globe className="w-4 h-4" />
                                {t('instrumentDetail.generalInfo')}
                            </CardTitle>
                        </CardHeader>
                        <CardContent className="pt-0">
                            <div className="space-y-3">
                                {[
                                    { label: t('holding.type'), value: TYPE_LABELS[instrument.type] || instrument.type },
                                    { label: t('instrumentDetail.exchange'), value: instrument.exchange },
                                    { label: t('portfolio.currency'), value: instrument.currency },
                                ].map(field => (
                                    <div key={field.label} className="flex justify-between items-center py-2 border-b border-gray-50 last:border-0">
                                        <span className="text-sm text-gray-500">{field.label}</span>
                                        <span className="text-sm font-semibold text-gray-900">{field.value}</span>
                                    </div>
                                ))}
                            </div>
                        </CardContent>
                    </Card>

                    <Card className="border-0 shadow-sm">
                        <CardHeader className="pb-3">
                            <CardTitle className="text-sm font-semibold text-gray-500 uppercase tracking-wide flex items-center gap-2">
                                <Bell className="w-4 h-4" />
                                {t('alert.title')}
                            </CardTitle>
                        </CardHeader>
                        <CardContent className="pt-0">
                            {activeAlerts.length === 0 ? (
                                <p className="text-sm text-gray-400 mb-3">{t('alert.empty')}</p>
                            ) : (
                                <div className="space-y-2 mb-3">
                                    {activeAlerts.map(alert => (
                                        <div key={alert.id} className="flex items-center justify-between py-2 border-b border-gray-50 last:border-0">
                                            <div>
                                                <p className="text-sm font-medium text-gray-900">
                                                    {alert.condition === 'ABOVE' ? '↑' : '↓'} {formatPrice(alert.targetPrice)} {instrument.currency}
                                                </p>
                                                <p className="text-xs text-gray-400">
                                                    {alert.condition === 'ABOVE' ? t('alert.aboveCondition') : t('alert.belowCondition')}
                                                </p>
                                            </div>
                                            <button onClick={() => handleDeleteAlert(alert.id)} className="text-red-400 hover:text-red-600">
                                                <Trash2 className="w-4 h-4" />
                                            </button>
                                        </div>
                                    ))}
                                </div>
                            )}
                            <Button variant="outline" size="sm" className="w-full" onClick={() => setShowAlertModal(true)}>
                                <Bell className="w-4 h-4 mr-2" />
                                {t('instrumentDetail.addAlert')}
                            </Button>
                        </CardContent>
                    </Card>

                    {typeFields.length > 0 && (
                        <Card className="border-0 shadow-sm">
                            <CardHeader className="pb-3">
                                <CardTitle className="text-sm font-semibold text-gray-500 uppercase tracking-wide flex items-center gap-2">
                                    <Building2 className="w-4 h-4" />
                                    {TYPE_LABELS[instrument.type]} {t('instrumentDetail.info')}
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
                </div>

                {instrument.description && (
                    <Card className="border-0 shadow-sm mt-6">
                        <CardHeader className="pb-3">
                            <CardTitle className="text-sm font-semibold text-gray-500 uppercase tracking-wide">
                                {t('instrumentDetail.description')}
                            </CardTitle>
                        </CardHeader>
                        <CardContent className="pt-0">
                            <p className="text-sm text-gray-600 leading-relaxed">{instrument.description}</p>
                        </CardContent>
                    </Card>
                )}
            </div>

            {/* Alert Modal */}
            {showAlertModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-xl shadow-xl max-w-md w-full p-6">
                        <h3 className="text-xl font-bold text-gray-900 mb-4">{t('instrumentDetail.createAlert')}</h3>
                        <div className="mb-4">
                            <p className="text-sm text-gray-500 mb-4">
                                {t('instrumentDetail.currentPrice')}: <span className="font-semibold text-gray-900">
                                    {formatPrice(price?.current)} {instrument.currency}
                                </span>
                            </p>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                {t('alert.condition')}
                            </label>
                            <div className="flex gap-2 mb-4">
                                <button
                                    onClick={() => setAlertCondition('ABOVE')}
                                    className={`flex-1 py-2 rounded-lg text-sm font-medium border transition-colors ${
                                        alertCondition === 'ABOVE' ? 'bg-green-50 border-green-500 text-green-700' : 'border-gray-200 text-gray-600'
                                    }`}
                                >
                                    {t('alert.above')}
                                </button>
                                <button
                                    onClick={() => setAlertCondition('BELOW')}
                                    className={`flex-1 py-2 rounded-lg text-sm font-medium border transition-colors ${
                                        alertCondition === 'BELOW' ? 'bg-red-50 border-red-500 text-red-700' : 'border-gray-200 text-gray-600'
                                    }`}
                                >
                                    {t('alert.below')}
                                </button>
                            </div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                {t('alert.targetPrice')} ({instrument.currency})
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
                                {t('common.cancel')}
                            </Button>
                            <Button
                                className="flex-1"
                                onClick={handleCreateAlert}
                                disabled={!alertTargetPrice || alertLoading}
                            >
                                {alertLoading ? (
                                    <><Loader2 className="w-4 h-4 mr-2 animate-spin" />{t('instrumentDetail.creating')}</>
                                ) : (
                                    <><Bell className="w-4 h-4 mr-2" />{t('alert.create')}</>
                                )}
                            </Button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}