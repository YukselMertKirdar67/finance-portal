import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    RefreshCw,
    DollarSign,
    TrendingUp,
    Gem,
    FileText,
    BarChart3,
    CheckCircle,
    XCircle,
    Clock,
    AlertCircle,
    Loader2,
    Info
} from 'lucide-react';
import {
    getUpdateStatus,
    updateAllInstruments,
    updateTcmb,
    updateFinnhub,
    updateBist,
    updatePrecious,
    updateBonds,
    getApiStats
} from '../../API/adminInstrumentApi';
import { useAuth } from '../../context/AuthContext';

const AdminInstrumentUpdatePage = () => {
    const navigate = useNavigate();
    const { isAdmin } = useAuth();

    const [status, setStatus] = useState(null);
    const [apiStats, setApiStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [updating, setUpdating] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    useEffect(() => {
        if (!isAdmin) {
            navigate('/dashboard');
            return;
        }

        fetchData();

        // Auto-refresh status every 5 seconds
        const interval = setInterval(() => {
            fetchUpdateStatus();
        }, 5000);

        return () => clearInterval(interval);
    }, [isAdmin, navigate]);

    const fetchData = async () => {
        setLoading(true);
        try {
            const [statusData, statsData] = await Promise.all([
                getUpdateStatus(),
                getApiStats()
            ]);
            setStatus(statusData);
            setApiStats(statsData.apis);
        } catch (err) {
            setError('Veriler yüklenirken hata oluştu');
            console.error('Error:', err);
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
        setError('');
        setSuccess('');

        try {
            const result = await updateAllInstruments();

            if (result.success) {
                setSuccess(`✅ ${result.totalUpdated} enstrüman güncellendi!`);
                fetchUpdateStatus();
            } else {
                setError('Güncelleme başarısız');
            }
        } catch (err) {
            setError('Güncelleme sırasında hata oluştu');
            console.error('Update error:', err);
        } finally {
            setUpdating(false);
        }
    };

    const handleSingleUpdate = async (updateFn, name) => {
        setUpdating(true);
        setError('');
        setSuccess('');

        try {
            const result = await updateFn();

            if (result.success) {
                setSuccess(`✅ ${name} güncellendi! (${result.updatedCount} enstrüman)`);
                fetchUpdateStatus();
            } else {
                setError(`${name} güncellenemedi`);
            }
        } catch (err) {
            setError(`${name} güncellenirken hata oluştu`);
            console.error('Update error:', err);
        } finally {
            setUpdating(false);
        }
    };

    const formatLastUpdate = (timestamp) => {
        if (!timestamp) return 'Hiç güncellenmedi';

        const date = new Date(timestamp);
        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMs / 3600000);
        const diffDays = Math.floor(diffMs / 86400000);

        if (diffMins < 1) return 'Az önce';
        if (diffMins < 60) return `${diffMins} dakika önce`;
        if (diffHours < 24) return `${diffHours} saat önce`;
        return `${diffDays} gün önce`;
    };

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="text-center">
                    <Loader2 className="w-16 h-16 text-blue-600 animate-spin mx-auto mb-4" />
                    <p className="text-gray-600">Yükleniyor...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="p-6 bg-gray-50 min-h-screen">

            {/* Header */}
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-gray-800 mb-2">
                    Enstrüman Fiyat Güncellemesi
                </h1>
                <p className="text-gray-600">
                    Finansal enstrümanların fiyatlarını güncelleyin
                </p>
            </div>

            {/* Success Message */}
            {success && (
                <div className="mb-6 p-4 bg-green-50 border border-green-200 rounded-lg flex items-start gap-3">
                    <CheckCircle className="w-5 h-5 text-green-600 mt-0.5 flex-shrink-0" />
                    <p className="text-green-800">{success}</p>
                </div>
            )}

            {/* Error Message */}
            {error && (
                <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
                    <XCircle className="w-5 h-5 text-red-600 mt-0.5 flex-shrink-0" />
                    <p className="text-red-800">{error}</p>
                </div>
            )}

            {/* Update Status Card */}
            <div className="bg-white rounded-xl shadow-md p-6 border border-gray-100 mb-6">
                <div className="flex items-center justify-between mb-6">
                    <h2 className="text-xl font-bold text-gray-800">Güncelleme Durumu</h2>

                    {status?.updating ? (
                        <div className="flex items-center gap-2 text-blue-600">
                            <Loader2 className="w-5 h-5 animate-spin" />
                            <span className="font-semibold">Güncelleniyor...</span>
                        </div>
                    ) : (
                        <div className="flex items-center gap-2 text-green-600">
                            <CheckCircle className="w-5 h-5" />
                            <span className="font-semibold">Hazır</span>
                        </div>
                    )}
                </div>

                {/* Last Update Info */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
                    <div className="flex items-start gap-3">
                        <Clock className="w-5 h-5 text-gray-400 mt-1" />
                        <div>
                            <p className="text-sm text-gray-500">Son Güncelleme</p>
                            <p className="font-semibold text-gray-900">
                                {formatLastUpdate(status?.lastUpdateTime)}
                            </p>
                        </div>
                    </div>

                    <div className="flex items-start gap-3">
                        <BarChart3 className="w-5 h-5 text-gray-400 mt-1" />
                        <div>
                            <p className="text-sm text-gray-500">Toplam Güncellenen</p>
                            <p className="font-semibold text-gray-900">
                                {status?.totalUpdated || 0} enstrüman
                            </p>
                        </div>
                    </div>

                    <div className="flex items-start gap-3">
                        <Info className="w-5 h-5 text-gray-400 mt-1" />
                        <div>
                            <p className="text-sm text-gray-500">Durum</p>
                            <p className="font-semibold text-gray-900">
                                {status?.message || 'Bilgi yok'}
                            </p>
                        </div>
                    </div>
                </div>

                {/* Update Stats */}
                {status?.totalUpdated > 0 && (
                    <div className="border-t pt-6">
                        <h3 className="text-sm font-semibold text-gray-700 mb-4">
                            Detaylı İstatistikler
                        </h3>
                        <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
                            <StatBox
                                label="TCMB Döviz"
                                value={status?.tcmbUpdated || 0}
                                color="text-blue-600"
                            />
                            <StatBox
                                label="Finnhub"
                                value={status?.finnhubUpdated || 0}
                                color="text-purple-600"
                            />
                            <StatBox
                                label="BIST"
                                value={status?.bistUpdated || 0}
                                color="text-green-600"
                            />
                            <StatBox
                                label="Kıymetli Metal"
                                value={status?.preciousUpdated || 0}
                                color="text-yellow-600"
                            />
                            <StatBox
                                label="Tahvil/Bono"
                                value={status?.bondsUpdated || 0}
                                color="text-indigo-600"
                            />
                        </div>
                    </div>
                )}

                {/* Main Update Button */}
                <div className="mt-6 pt-6 border-t">
                    <button
                        onClick={handleUpdateAll}
                        disabled={updating || status?.updating}
                        className="w-full flex items-center justify-center gap-3 px-6 py-4 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 disabled:from-gray-400 disabled:to-gray-500 text-white font-semibold rounded-lg transition shadow-lg hover:shadow-xl"
                    >
                        {(updating || status?.updating) ? (
                            <>
                                <Loader2 className="w-5 h-5 animate-spin" />
                                <span>Güncelleniyor...</span>
                            </>
                        ) : (
                            <>
                                <RefreshCw className="w-5 h-5" />
                                <span>Tüm Fiyatları Güncelle</span>
                            </>
                        )}
                    </button>
                    <p className="text-sm text-gray-500 text-center mt-3">
                        ⚠️ Tüm API'lerden güncel fiyatları çeker (2-3 dakika sürebilir)
                    </p>
                </div>
            </div>

            {/* Individual Update Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-6">

                {/* TCMB */}
                <UpdateCard
                    title="TCMB Döviz Kurları"
                    description="Merkez Bankası resmi kurları"
                    icon={<DollarSign className="w-6 h-6" />}
                    iconBg="bg-blue-100"
                    iconColor="text-blue-600"
                    limit={apiStats?.tcmb?.limit || 'Sınırsız'}
                    onUpdate={() => handleSingleUpdate(updateTcmb, 'TCMB Kurları')}
                    disabled={updating || status?.updating}
                />

                {/* Finnhub */}
                <UpdateCard
                    title="Finnhub - US Stocks"
                    description="ABD hisse senetleri ve kripto"
                    icon={<TrendingUp className="w-6 h-6" />}
                    iconBg="bg-purple-100"
                    iconColor="text-purple-600"
                    limit={apiStats?.finnhub?.limit || '60/dakika'}
                    onUpdate={() => handleSingleUpdate(updateFinnhub, 'Finnhub')}
                    disabled={updating || status?.updating}
                />

                {/* BIST */}
                <UpdateCard
                    title="TwelveData - BIST"
                    description="Borsa İstanbul hisseleri"
                    icon={<BarChart3 className="w-6 h-6" />}
                    iconBg="bg-green-100"
                    iconColor="text-green-600"
                    limit={apiStats?.twelveData?.limit || '8/dakika'}
                    onUpdate={() => handleSingleUpdate(updateBist, 'BIST Hisseleri')}
                    disabled={updating || status?.updating}
                />

                {/* Precious Metals */}
                <UpdateCard
                    title="AlphaVantage - Metaller"
                    description="Altın, gümüş, platin, paladyum"
                    icon={<Gem className="w-6 h-6" />}
                    iconBg="bg-yellow-100"
                    iconColor="text-yellow-600"
                    limit={apiStats?.alphaVantage?.limit || '25/gün'}
                    onUpdate={() => handleSingleUpdate(updatePrecious, 'Kıymetli Metaller')}
                    disabled={updating || status?.updating}
                />

                {/* Bonds */}
                <UpdateCard
                    title="TCMB EVDS - Tahvil"
                    description="Devlet tahvili getiri oranları"
                    icon={<FileText className="w-6 h-6" />}
                    iconBg="bg-indigo-100"
                    iconColor="text-indigo-600"
                    limit={apiStats?.tcmbEvds?.limit || 'Sınırsız'}
                    onUpdate={() => handleSingleUpdate(updateBonds, 'Tahvil/Bono')}
                    disabled={updating || status?.updating}
                />
            </div>

            {/* Info Box */}
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
                <div className="flex items-start gap-3">
                    <Info className="w-6 h-6 text-blue-600 flex-shrink-0 mt-1" />
                    <div>
                        <h3 className="font-semibold text-blue-900 mb-2">
                            Güncelleme Hakkında
                        </h3>
                        <ul className="space-y-2 text-sm text-blue-800">
                            <li className="flex items-start gap-2">
                                <span className="text-blue-600 mt-1">•</span>
                                <span>
                                    <strong>Tümünü Güncelle:</strong> Tüm API'lerden sırayla veri çeker.
                                    Rate limit koruması nedeniyle 2-3 dakika sürer.
                                </span>
                            </li>
                            <li className="flex items-start gap-2">
                                <span className="text-blue-600 mt-1">•</span>
                                <span>
                                    <strong>Tekil Güncelleme:</strong> Sadece seçtiğiniz API'den veri çeker.
                                    Daha hızlı ama sınırlı kapsam.
                                </span>
                            </li>
                            <li className="flex items-start gap-2">
                                <span className="text-blue-600 mt-1">•</span>
                                <span>
                                    <strong>Rate Limit:</strong> Her API'nin istek limiti var.
                                    Aşırı sık güncelleme yapmayın.
                                </span>
                            </li>
                            <li className="flex items-start gap-2">
                                <span className="text-blue-600 mt-1">•</span>
                                <span>
                                    <strong>Öneri:</strong> Günde 1-2 kez "Tümünü Güncelle" butonu yeterli.
                                    Acil durumlarda tekil güncellemeler kullanın.
                                </span>
                            </li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    );
};

// Stat Box Component
const StatBox = ({ label, value, color }) => {
    return (
        <div className="bg-gray-50 rounded-lg p-3 text-center">
            <p className="text-xs text-gray-600 mb-1">{label}</p>
            <p className={`text-2xl font-bold ${color}`}>{value}</p>
        </div>
    );
};

// Update Card Component
const UpdateCard = ({
                        title,
                        description,
                        icon,
                        iconBg,
                        iconColor,
                        limit,
                        onUpdate,
                        disabled
                    }) => {
    return (
        <div className="bg-white rounded-xl shadow-md p-6 border border-gray-100">
            <div className="flex items-start justify-between mb-4">
                <div className={`${iconBg} ${iconColor} p-3 rounded-lg`}>
                    {icon}
                </div>
                <span className="text-xs bg-gray-100 text-gray-600 px-2 py-1 rounded-full">
                    {limit}
                </span>
            </div>

            <h3 className="text-lg font-bold text-gray-800 mb-2">{title}</h3>
            <p className="text-sm text-gray-600 mb-4">{description}</p>

            <button
                onClick={onUpdate}
                disabled={disabled}
                className="w-full flex items-center justify-center gap-2 px-4 py-2 bg-gray-800 hover:bg-gray-900 disabled:bg-gray-400 text-white font-semibold rounded-lg transition"
            >
                {disabled ? (
                    <>
                        <Loader2 className="w-4 h-4 animate-spin" />
                        <span>Güncelleniyor...</span>
                    </>
                ) : (
                    <>
                        <RefreshCw className="w-4 h-4" />
                        <span>Güncelle</span>
                    </>
                )}
            </button>
        </div>
    );
};

export default AdminInstrumentUpdatePage;