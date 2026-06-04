import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
    RefreshCw, DollarSign, TrendingUp, Gem, FileText,
    BarChart3, CheckCircle, XCircle, Clock, AlertCircle,
    Loader2, Info, Bitcoin, History
} from 'lucide-react';
import {
    getUpdateStatus, updateAllInstruments, updateTcmb,
    updateUsStocks, updateBist, updateCrypto, updatePrecious,
    updateBonds, fetchAllHistoricalData, fetchForexHistoricalData,
    updateEtfs, updateTrBonds, fetchTrBondsHistorical,
    updateViop, fetchViopHistorical, updateInstrumentDetails
} from '../../API/adminInstrumentApi';
import { useAuth } from '../../context/AuthContext';

const AdminInstrumentUpdatePage = () => {
    const navigate = useNavigate();
    const { isAdmin } = useAuth();
    const { t } = useTranslation();

    const [status, setStatus] = useState(null);
    const [loading, setLoading] = useState(true);
    const [updating, setUpdating] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    useEffect(() => {
        if (!isAdmin) { navigate('/dashboard'); return; }
        fetchData();
        const interval = setInterval(() => fetchUpdateStatus(), 5000);
        return () => clearInterval(interval);
    }, [isAdmin, navigate]);

    const fetchData = async () => {
        setLoading(true);
        try {
            const statusData = await getUpdateStatus();
            setStatus(statusData);
        } catch {
            setError(t('admin.instruments.loadError'));
        } finally {
            setLoading(false);
        }
    };

    const fetchUpdateStatus = async () => {
        try {
            const data = await getUpdateStatus();
            setStatus(data);
        } catch (err) {
            console.error('Error fetching status:', err);
        }
    };

    const handleUpdateAll = async () => {
        setUpdating(true);
        setError(''); setSuccess('');
        try {
            const result = await updateAllInstruments();
            if (result.success) {
                setSuccess(t('admin.instruments.updateAllSuccess', { count: result.totalUpdated }));
                fetchUpdateStatus();
            } else {
                setError(t('admin.instruments.updateError'));
            }
        } catch {
            setError(t('admin.instruments.updateError'));
        } finally {
            setUpdating(false);
        }
    };

    const handleFetchHistorical = async () => {
        setUpdating(true);
        setError(''); setSuccess('');
        try {
            const result = await fetchAllHistoricalData();
            if (result.success) {
                setSuccess(t('admin.instruments.historicalSuccess'));
            } else {
                setError(t('admin.instruments.historicalError'));
            }
        } catch {
            setError(t('admin.instruments.historicalError'));
        } finally {
            setUpdating(false);
        }
    };

    const handleSingleUpdate = async (updateFn, name) => {
        setUpdating(true);
        setError(''); setSuccess('');
        try {
            const result = await updateFn();
            if (result.success) {
                setSuccess(t('admin.instruments.singleSuccess', { name, count: result.updatedCount }));
                fetchUpdateStatus();
            } else {
                setError(t('admin.instruments.singleError', { name }));
            }
        } catch {
            setError(t('admin.instruments.singleError', { name }));
        } finally {
            setUpdating(false);
        }
    };

    const formatLastUpdate = (timestamp) => {
        if (!timestamp) return t('admin.instruments.neverUpdated');
        const diffMs = new Date() - new Date(timestamp);
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMs / 3600000);
        const diffDays = Math.floor(diffMs / 86400000);
        if (diffMins < 1) return t('common.justNow');
        if (diffMins < 60) return `${diffMins} ${t('admin.instruments.minsAgo')}`;
        if (diffHours < 24) return `${diffHours} ${t('admin.instruments.hoursAgo')}`;
        return `${diffDays} ${t('admin.instruments.daysAgo')}`;
    };

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="text-center">
                    <Loader2 className="w-16 h-16 text-blue-600 animate-spin mx-auto mb-4" />
                    <p className="text-gray-600">{t('common.loading')}</p>
                </div>
            </div>
        );
    }

    const isDisabled = updating || status?.updating;

    return (
        <div className="p-6 bg-gray-50 min-h-screen">
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-gray-800 mb-2">{t('admin.instruments.title')}</h1>
                <p className="text-gray-600">{t('admin.instruments.subtitle')}</p>
            </div>

            {success && (
                <div className="mb-6 p-4 bg-green-50 border border-green-200 rounded-lg flex items-start gap-3">
                    <CheckCircle className="w-5 h-5 text-green-600 mt-0.5 flex-shrink-0" />
                    <p className="text-green-800">{success}</p>
                </div>
            )}
            {error && (
                <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
                    <XCircle className="w-5 h-5 text-red-600 mt-0.5 flex-shrink-0" />
                    <p className="text-red-800">{error}</p>
                </div>
            )}

            {/* Status Card */}
            <div className="bg-white rounded-xl shadow-md p-6 border border-gray-100 mb-6">
                <div className="flex items-center justify-between mb-6">
                    <h2 className="text-xl font-bold text-gray-800">{t('admin.instruments.updateStatus')}</h2>
                    {status?.updating ? (
                        <div className="flex items-center gap-2 text-blue-600">
                            <Loader2 className="w-5 h-5 animate-spin" />
                            <span className="font-semibold">{t('admin.instruments.updating')}</span>
                        </div>
                    ) : (
                        <div className="flex items-center gap-2 text-green-600">
                            <CheckCircle className="w-5 h-5" />
                            <span className="font-semibold">{t('admin.instruments.ready')}</span>
                        </div>
                    )}
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
                    <div className="flex items-start gap-3">
                        <Clock className="w-5 h-5 text-gray-400 mt-1" />
                        <div>
                            <p className="text-sm text-gray-500">{t('admin.news.lastUpdate')}</p>
                            <p className="font-semibold text-gray-900">{formatLastUpdate(status?.lastUpdateTime)}</p>
                        </div>
                    </div>
                    <div className="flex items-start gap-3">
                        <BarChart3 className="w-5 h-5 text-gray-400 mt-1" />
                        <div>
                            <p className="text-sm text-gray-500">{t('admin.instruments.totalUpdated')}</p>
                            <p className="font-semibold text-gray-900">{status?.totalUpdated || 0} {t('admin.instruments.instruments')}</p>
                        </div>
                    </div>
                    <div className="flex items-start gap-3">
                        <Info className="w-5 h-5 text-gray-400 mt-1" />
                        <div>
                            <p className="text-sm text-gray-500">{t('admin.users.colStatus')}</p>
                            <p className="font-semibold text-gray-900">{status?.message || t('admin.instruments.noInfo')}</p>
                        </div>
                    </div>
                </div>

                {status?.totalUpdated > 0 && (
                    <div className="border-t pt-6">
                        <h3 className="text-sm font-semibold text-gray-700 mb-4">{t('admin.instruments.detailedStats')}</h3>
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                            <StatBox label="TCMB Döviz" value={status?.tcmbUpdated || 0} color="text-blue-600" />
                            <StatBox label="Yahoo Finance" value={status?.yahooUpdated || 0} color="text-purple-600" />
                            <StatBox label={t('markets.bonds')} value={status?.bondsUpdated || 0} color="text-indigo-600" />
                            <StatBox label={t('admin.instruments.total')} value={status?.totalUpdated || 0} color="text-green-600" />
                        </div>
                    </div>
                )}

                {/* Main Buttons */}
                <div className="mt-6 pt-6 border-t grid grid-cols-1 md:grid-cols-2 gap-4">
                    <HistoricalButton
                        label={t('admin.instruments.updateAll')}
                        loadingLabel={t('admin.instruments.updating')}
                        icon={<RefreshCw className="w-5 h-5" />}
                        gradient="from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700"
                        onClick={handleUpdateAll}
                        disabled={isDisabled}
                    />
                    <HistoricalButton
                        label={t('admin.instruments.fetchHistorical')}
                        loadingLabel={t('admin.instruments.fetching')}
                        icon={<History className="w-5 h-5" />}
                        gradient="from-green-600 to-teal-600 hover:from-green-700 hover:to-teal-700"
                        onClick={handleFetchHistorical}
                        disabled={isDisabled}
                    />
                    <HistoricalButton
                        label={t('admin.instruments.tcmbForexHistorical')}
                        loadingLabel={t('admin.instruments.fetching')}
                        icon={<History className="w-5 h-5" />}
                        gradient="from-blue-500 to-cyan-500 hover:from-blue-600 hover:to-cyan-600"
                        onClick={async () => {
                            setUpdating(true); setError(''); setSuccess('');
                            try {
                                const result = await fetchForexHistoricalData(365);
                                result.success
                                    ? setSuccess(t('admin.instruments.tcmbForexSuccess'))
                                    : setError(t('admin.instruments.tcmbForexError'));
                            } catch { setError(t('admin.instruments.tcmbForexError')); }
                            finally { setUpdating(false); }
                        }}
                        disabled={isDisabled}
                    />
                    <HistoricalButton
                        label={t('admin.instruments.trBondsHistorical')}
                        loadingLabel={t('admin.instruments.fetching')}
                        icon={<History className="w-5 h-5" />}
                        gradient="from-red-500 to-rose-500 hover:from-red-600 hover:to-rose-600"
                        onClick={async () => {
                            setUpdating(true); setError(''); setSuccess('');
                            try {
                                const result = await fetchTrBondsHistorical(365);
                                result.success
                                    ? setSuccess(t('admin.instruments.trBondsSuccess'))
                                    : setError(t('admin.instruments.trBondsError'));
                            } catch { setError(t('admin.instruments.trBondsError')); }
                            finally { setUpdating(false); }
                        }}
                        disabled={isDisabled}
                    />
                    <HistoricalButton
                        label={t('admin.instruments.viopHistorical')}
                        loadingLabel={t('admin.instruments.fetching')}
                        icon={<History className="w-5 h-5" />}
                        gradient="from-orange-500 to-red-500 hover:from-orange-600 hover:to-red-600"
                        onClick={async () => {
                            setUpdating(true); setError(''); setSuccess('');
                            try {
                                const result = await fetchViopHistorical();
                                result.success
                                    ? setSuccess(t('admin.instruments.viopSuccess'))
                                    : setError(t('admin.instruments.viopError'));
                            } catch { setError(t('admin.instruments.viopError')); }
                            finally { setUpdating(false); }
                        }}
                        disabled={isDisabled}
                    />
                </div>
                <p className="text-sm text-gray-500 text-center mt-3">
                    ⚠️ {t('admin.instruments.historicalWarning')}
                </p>
            </div>

            {/* Individual Update Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-6">
                {[
                    { title: t('admin.instruments.tcmb'), desc: t('admin.instruments.tcmbDesc'), icon: <DollarSign className="w-6 h-6" />, bg: 'bg-blue-100', color: 'text-blue-600', fn: () => handleSingleUpdate(updateTcmb, 'TCMB') },
                    { title: t('admin.instruments.usStocks'), desc: 'AAPL, MSFT, GOOGL, TSLA...', icon: <TrendingUp className="w-6 h-6" />, bg: 'bg-purple-100', color: 'text-purple-600', fn: () => handleSingleUpdate(updateUsStocks, 'US Stocks') },
                    { title: t('admin.instruments.bist'), desc: t('admin.instruments.bistDesc'), icon: <BarChart3 className="w-6 h-6" />, bg: 'bg-green-100', color: 'text-green-600', fn: () => handleSingleUpdate(updateBist, 'BIST') },
                    { title: t('admin.instruments.crypto'), desc: 'BTC, ETH, BNB...', icon: <Bitcoin className="w-6 h-6" />, bg: 'bg-pink-100', color: 'text-pink-600', fn: () => handleSingleUpdate(updateCrypto, 'Crypto') },
                    { title: t('admin.instruments.precious'), desc: t('admin.instruments.preciousDesc'), icon: <Gem className="w-6 h-6" />, bg: 'bg-yellow-100', color: 'text-yellow-600', fn: () => handleSingleUpdate(updatePrecious, 'Precious') },
                    { title: t('admin.instruments.bonds'), desc: t('admin.instruments.bondsDesc'), icon: <FileText className="w-6 h-6" />, bg: 'bg-indigo-100', color: 'text-indigo-600', fn: () => handleSingleUpdate(updateBonds, 'Bonds') },
                    { title: t('admin.instruments.etfs'), desc: 'SPY, QQQ, GLD...', icon: <BarChart3 className="w-6 h-6" />, bg: 'bg-orange-100', color: 'text-orange-600', fn: () => handleSingleUpdate(updateEtfs, 'ETFs') },
                    { title: t('admin.instruments.trBonds'), desc: t('admin.instruments.trBondsDesc'), icon: <FileText className="w-6 h-6" />, bg: 'bg-red-100', color: 'text-red-600', fn: () => handleSingleUpdate(updateTrBonds, 'TR Bonds') },
                    { title: t('admin.instruments.viop'), desc: t('admin.instruments.viopDesc'), icon: <TrendingUp className="w-6 h-6" />, bg: 'bg-red-100', color: 'text-red-600', fn: () => handleSingleUpdate(updateViop, 'VIOP') },
                    { title: t('admin.instruments.details'), desc: t('admin.instruments.detailsDesc'), icon: <Info className="w-6 h-6" />, bg: 'bg-gray-100', color: 'text-gray-600', fn: () => handleSingleUpdate(updateInstrumentDetails, 'Details'), limit: t('admin.instruments.oneTime') },
                ].map((card) => (
                    <UpdateCard
                        key={card.title}
                        title={card.title}
                        description={card.desc}
                        icon={card.icon}
                        iconBg={card.bg}
                        iconColor={card.color}
                        limit={card.limit || t('admin.instruments.unlimited')}
                        onUpdate={card.fn}
                        disabled={isDisabled}
                        t={t}
                    />
                ))}
            </div>

            {/* Info Box */}
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
                <div className="flex items-start gap-3">
                    <Info className="w-6 h-6 text-blue-600 flex-shrink-0 mt-1" />
                    <div>
                        <h3 className="font-semibold text-blue-900 mb-2">{t('admin.instruments.infoTitle')}</h3>
                        <ul className="space-y-2 text-sm text-blue-800">
                            {[
                                { key: 'infoUpdateAll', desc: 'infoUpdateAllDesc' },
                                { key: 'infoHistorical', desc: 'infoHistoricalDesc' },
                                { key: 'infoTcmbForex', desc: 'infoTcmbForexDesc' },
                                { key: 'infoYahoo', desc: 'infoYahooDesc' },
                                { key: 'infoIsYatirim', desc: 'infoIsYatirimDesc' },
                                { key: 'infoTip', desc: 'infoTipDesc' },
                            ].map(item => (
                                <li key={item.key} className="flex items-start gap-2">
                                    <span className="text-blue-600 mt-1">•</span>
                                    <span><strong>{t(`admin.instruments.${item.key}`)}:</strong> {t(`admin.instruments.${item.desc}`)}</span>
                                </li>
                            ))}
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    );
};

const StatBox = ({ label, value, color }) => (
    <div className="bg-gray-50 rounded-lg p-3 text-center">
        <p className="text-xs text-gray-600 mb-1">{label}</p>
        <p className={`text-2xl font-bold ${color}`}>{value}</p>
    </div>
);

const HistoricalButton = ({ label, loadingLabel, icon, gradient, onClick, disabled }) => (
    <button
        onClick={onClick}
        disabled={disabled}
        className={`w-full flex items-center justify-center gap-3 px-6 py-4 bg-gradient-to-r ${gradient} disabled:from-gray-400 disabled:to-gray-500 text-white font-semibold rounded-lg transition shadow-lg`}
    >
        {disabled
            ? <><Loader2 className="w-5 h-5 animate-spin" /><span>{loadingLabel}</span></>
            : <>{icon}<span>{label}</span></>}
    </button>
);

const UpdateCard = ({ title, description, icon, iconBg, iconColor, limit, onUpdate, disabled, t }) => (
    <div className="bg-white rounded-xl shadow-md p-6 border border-gray-100">
        <div className="flex items-start justify-between mb-4">
            <div className={`${iconBg} ${iconColor} p-3 rounded-lg`}>{icon}</div>
            <span className="text-xs bg-gray-100 text-gray-600 px-2 py-1 rounded-full">{limit}</span>
        </div>
        <h3 className="text-lg font-bold text-gray-800 mb-2">{title}</h3>
        <p className="text-sm text-gray-600 mb-4">{description}</p>
        <button
            onClick={onUpdate}
            disabled={disabled}
            className="w-full flex items-center justify-center gap-2 px-4 py-2 bg-gray-800 hover:bg-gray-900 disabled:bg-gray-400 text-white font-semibold rounded-lg transition"
        >
            {disabled
                ? <><Loader2 className="w-4 h-4 animate-spin" /><span>{t('admin.instruments.updating')}</span></>
                : <><RefreshCw className="w-4 h-4" /><span>{t('common.refresh')}</span></>}
        </button>
    </div>
);

export default AdminInstrumentUpdatePage;