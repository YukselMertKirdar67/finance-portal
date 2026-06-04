import React, { useState, useEffect } from 'react';
import { Search, X, Plus, Loader2, Calendar } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../UI/Card';
import { Button } from '../UI/Button';
import { useTranslation } from 'react-i18next';
import { LineChart, Line, XAxis, YAxis, Tooltip, CartesianGrid, ResponsiveContainer, Legend } from 'recharts';
import { compareInstruments, searchInstruments } from '../../API/comparisonApi.js';

export default function ComparisonPage() {
    const { t, i18n } = useTranslation();

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

    const timeframes = [
        { value: '1H', label: t('comparison.tf1W') },
        { value: '1A', label: t('comparison.tf1M') },
        { value: '3A', label: t('comparison.tf3M') },
        { value: '6A', label: t('comparison.tf6M') },
        { value: '1Y', label: t('comparison.tf1Y') },
    ];

    useEffect(() => {
        if (searchTerm1.length >= 2) {
            const timer = setTimeout(async () => {
                try {
                    const data = await searchInstruments(searchTerm1);
                    setSearchResults1(data.content || []);
                } catch (e) { console.error('Search error:', e); }
            }, 300);
            return () => clearTimeout(timer);
        } else { setSearchResults1([]); }
    }, [searchTerm1]);

    useEffect(() => {
        if (searchTerm2.length >= 2) {
            const timer = setTimeout(async () => {
                try {
                    const data = await searchInstruments(searchTerm2);
                    setSearchResults2(data.content || []);
                } catch (e) { console.error('Search error:', e); }
            }, 300);
            return () => clearTimeout(timer);
        } else { setSearchResults2([]); }
    }, [searchTerm2]);

    useEffect(() => {
        if (instrument1 && instrument2) { handleCompare(); }
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

    const selectInstrument1 = (inst) => { setInstrument1(inst); setShowSelector1(false); setSearchTerm1(''); };
    const selectInstrument2 = (inst) => { setInstrument2(inst); setShowSelector2(false); setSearchTerm2(''); };

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

    const chartData = comparisonData?.historicalData?.map(point => ({
        date: new Date(point.date).toLocaleDateString(i18n.language === 'en' ? 'en-US' : 'tr-TR', { day: '2-digit', month: 'short' }),
        inst1: parseFloat(point.price1),
        inst2: parseFloat(point.price2),
    })) || [];

    const metrics1 = comparisonData?.metrics?.instrument1Metrics;
    const metrics2 = comparisonData?.metrics?.instrument2Metrics;

    const renderSelector = (num, instrument, showSelector, searchTerm, searchResults, setShow, setSearch, selectFn, clearFn) => (
        <Card className="border-0 shadow-sm">
            <CardHeader className="pb-3">
                <CardTitle className="text-lg font-semibold">
                    {t('comparison.instrument', { num })}
                </CardTitle>
            </CardHeader>
            <CardContent>
                {!instrument ? (
                    showSelector ? (
                        <div>
                            <div className="relative mb-3">
                                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
                                <input
                                    type="text"
                                    placeholder={t('markets.searchPlaceholder')}
                                    value={searchTerm}
                                    onChange={(e) => setSearch(e.target.value)}
                                    className="w-full pl-10 pr-3 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    autoFocus
                                />
                            </div>
                            <div className="max-h-64 overflow-y-auto space-y-1">
                                {searchResults.length > 0 ? (
                                    searchResults.map(inst => (
                                        <button
                                            key={inst.id}
                                            onClick={() => selectFn(inst)}
                                            className="w-full text-left px-3 py-2 hover:bg-gray-50 rounded-lg transition-colors"
                                        >
                                            <p className="font-medium text-gray-900">{inst.name}</p>
                                            <p className="text-xs text-gray-500">{inst.symbol} • {inst.type}</p>
                                        </button>
                                    ))
                                ) : (
                                    <p className="text-sm text-gray-400 text-center py-4">
                                        {searchTerm.length >= 2 ? t('comparison.noResults') : t('comparison.startSearch')}
                                    </p>
                                )}
                            </div>
                            <Button variant="outline" className="w-full mt-3"
                                    onClick={() => { setShow(false); setSearch(''); }}>
                                {t('common.cancel')}
                            </Button>
                        </div>
                    ) : (
                        <Button variant="outline" className="w-full h-24 hover:bg-blue-50 hover:border-blue-300 transition-all"
                                onClick={() => setShow(true)}>
                            <Plus className="w-5 h-5 mr-2" />
                            {t('comparison.selectInstrument')}
                        </Button>
                    )
                ) : (
                    <div>
                        <div className="flex items-center justify-between mb-2">
                            <div>
                                <p className="text-sm font-medium text-gray-900">{instrument.name}</p>
                                <p className="text-xs text-gray-500">{instrument.symbol} • {instrument.type}</p>
                            </div>
                            <Button variant="ghost" size="sm" onClick={clearFn}>
                                <X className="w-4 h-4" />
                            </Button>
                        </div>
                        <p className="text-3xl font-bold text-gray-900">
                            {formatPrice(num === 1 ? comparisonData?.instrument1?.currentPrice : comparisonData?.instrument2?.currentPrice)}
                        </p>
                    </div>
                )}
            </CardContent>
        </Card>
    );

    return (
        <div className="min-h-screen bg-gray-50 p-8">
            <div className="max-w-7xl mx-auto">

                <div className="mb-6">
                    <h1 className="text-3xl font-bold text-gray-900 mb-2">{t('comparison.title')}</h1>
                    <p className="text-gray-600">{t('comparison.subtitle')}</p>
                </div>

                {/* Instrument Selectors */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
                    {renderSelector(
                        1, instrument1, showSelector1, searchTerm1, searchResults1,
                        setShowSelector1, setSearchTerm1, selectInstrument1,
                        () => { setInstrument1(null); setComparisonData(null); }
                    )}
                    {renderSelector(
                        2, instrument2, showSelector2, searchTerm2, searchResults2,
                        setShowSelector2, setSearchTerm2, selectInstrument2,
                        () => { setInstrument2(null); setComparisonData(null); }
                    )}
                </div>

                {/* Timeframe Selector */}
                {instrument1 && instrument2 && (
                    <div className="mb-6 flex items-center gap-3">
                        <Calendar className="w-5 h-5 text-gray-500" />
                        <div className="flex gap-2">
                            {timeframes.map(tf => (
                                <button
                                    key={tf.value}
                                    onClick={() => setTimeframe(tf.value)}
                                    className={`px-4 py-2 text-sm font-semibold rounded-lg transition-all ${
                                        timeframe === tf.value
                                            ? 'bg-blue-600 text-white shadow-md'
                                            : 'bg-white text-gray-600 hover:bg-gray-50 border border-gray-200'
                                    }`}
                                >
                                    {tf.label}
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
                                <CardTitle>{t('comparison.priceComparison')}</CardTitle>
                            </CardHeader>
                            <CardContent>
                                {chartData.length > 0 ? (
                                    <div className="h-[400px]">
                                        <ResponsiveContainer width="100%" height="100%">
                                            <LineChart data={chartData}>
                                                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                                                <XAxis dataKey="date" stroke="#9ca3af" tick={{ fontSize: 11 }} />
                                                <YAxis yAxisId="left" stroke="#3B82F6" tick={{ fontSize: 11 }} />
                                                <YAxis yAxisId="right" orientation="right" stroke="#EC4899" tick={{ fontSize: 11 }} />
                                                <Tooltip
                                                    formatter={(value) => formatPrice(value)}
                                                    contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                                                />
                                                <Legend />
                                                <Line yAxisId="left" type="monotone" dataKey="inst1" stroke="#3B82F6" strokeWidth={2} name={instrument1.symbol} dot={false} />
                                                <Line yAxisId="right" type="monotone" dataKey="inst2" stroke="#EC4899" strokeWidth={2} name={instrument2.symbol} dot={false} />
                                            </LineChart>
                                        </ResponsiveContainer>
                                    </div>
                                ) : (
                                    <div className="h-[400px] flex items-center justify-center text-gray-400">
                                        <p className="text-sm">{t('comparison.noHistoricalData')}</p>
                                    </div>
                                )}
                            </CardContent>
                        </Card>

                        {/* Metrics Table */}
                        <Card className="border-0 shadow-sm">
                            <CardHeader>
                                <CardTitle>{t('comparison.performanceComparison')}</CardTitle>
                            </CardHeader>
                            <CardContent>
                                <div className="overflow-x-auto">
                                    <table className="w-full">
                                        <thead>
                                        <tr className="border-b border-gray-200">
                                            <th className="text-left py-3 px-4 text-sm font-semibold text-gray-600">{t('comparison.metric')}</th>
                                            <th className="text-right py-3 px-4 text-sm font-semibold text-gray-600">{instrument1.symbol}</th>
                                            <th className="text-right py-3 px-4 text-sm font-semibold text-gray-600">{instrument2.symbol}</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {[
                                            {
                                                label: t('instrumentDetail.currentPrice'),
                                                v1: formatPrice(comparisonData?.instrument1?.currentPrice),
                                                v2: formatPrice(comparisonData?.instrument2?.currentPrice),
                                            },
                                            {
                                                label: t('comparison.periodChange'),
                                                v1: formatPercent(metrics1?.periodChange),
                                                v2: formatPercent(metrics2?.periodChange),
                                                colored: true,
                                                val1: metrics1?.periodChange,
                                                val2: metrics2?.periodChange,
                                            },
                                            {
                                                label: t('comparison.volatility'),
                                                v1: formatPercent(metrics1?.volatility),
                                                v2: formatPercent(metrics2?.volatility),
                                            },
                                            {
                                                label: t('comparison.highest'),
                                                v1: formatPrice(metrics1?.highestPrice),
                                                v2: formatPrice(metrics2?.highestPrice),
                                                color: 'text-emerald-600',
                                            },
                                            {
                                                label: t('comparison.lowest'),
                                                v1: formatPrice(metrics1?.lowestPrice),
                                                v2: formatPrice(metrics2?.lowestPrice),
                                                color: 'text-red-500',
                                            },
                                            {
                                                label: t('comparison.priceRange'),
                                                v1: formatPrice(metrics1?.priceRange),
                                                v2: formatPrice(metrics2?.priceRange),
                                            },
                                        ].map((row, i, arr) => (
                                            <tr key={row.label} className={i !== arr.length - 1 ? 'border-b border-gray-100' : ''}>
                                                <td className="py-4 px-4 text-gray-900">{row.label}</td>
                                                <td className={`py-4 px-4 text-right font-medium ${
                                                    row.colored
                                                        ? (row.val1 || 0) >= 0 ? 'text-emerald-600 font-semibold' : 'text-red-500 font-semibold'
                                                        : row.color || ''
                                                }`}>
                                                    {row.v1}
                                                </td>
                                                <td className={`py-4 px-4 text-right font-medium ${
                                                    row.colored
                                                        ? (row.val2 || 0) >= 0 ? 'text-emerald-600 font-semibold' : 'text-red-500 font-semibold'
                                                        : row.color || ''
                                                }`}>
                                                    {row.v2}
                                                </td>
                                            </tr>
                                        ))}
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
                                <h3 className="text-xl font-semibold text-gray-900 mb-2">{t('comparison.emptyTitle')}</h3>
                                <p className="text-gray-600">{t('comparison.emptyDesc')}</p>
                            </div>
                        </CardContent>
                    </Card>
                )}
            </div>
        </div>
    );
}