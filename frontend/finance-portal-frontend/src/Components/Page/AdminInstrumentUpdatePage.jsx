import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    RefreshCw, DollarSign, TrendingUp, Gem, FileText,
    BarChart3, CheckCircle, XCircle, Clock, AlertCircle,
    Loader2, Info, Bitcoin, History
} from 'lucide-react';
import {
    getUpdateStatus, updateAllInstruments, updateTcmb,
    updateUsStocks, updateBist, updateCrypto, updatePrecious,
    updateBonds, fetchAllHistoricalData
} from '../../API/adminInstrumentApi';
import { useAuth } from '../../context/AuthContext';

const AdminInstrumentUpdatePage = () => {
    const navigate = useNavigate();
    const { isAdmin } = useAuth();

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
        } catch{
            setError('Veriler yüklenirken hata oluştu');
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
                setSuccess(`✅ ${result.totalUpdated} enstrüman güncellendi!`);
                fetchUpdateStatus();
            } else {
                setError('Güncelleme başarısız');
            }
        } catch{
            setError('Güncelleme sırasında hata oluştu');
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
                setSuccess('✅ Geçmiş veriler başarıyla çekildi!');
            } else {
                setError('Geçmiş veri çekilemedi');
            }
        } catch{
            setError('Geçmiş veri çekilirken hata oluştu');
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
                setSuccess(`✅ ${name} güncellendi! (${result.updatedCount} enstrüman)`);
                fetchUpdateStatus();
            } else {
                setError(`${name} güncellenemedi`);
            }
        } catch{
            setError(`${name} güncellenirken hata oluştu`);
        } finally {
            setUpdating(false);
        }
    };

    const formatLastUpdate = (timestamp) => {
        if (!timestamp) return 'Hiç güncellenmedi';
        const diffMs = new Date() - new Date(timestamp);
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
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-gray-800 mb-2">Enstrüman Fiyat Güncellemesi</h1>
                <p className="text-gray-600">Finansal enstrümanların fiyatlarını güncelleyin</p>
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

                <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
                    <div className="flex items-start gap-3">
                        <Clock className="w-5 h-5 text-gray-400 mt-1" />
                        <div>
                            <p className="text-sm text-gray-500">Son Güncelleme</p>
                            <p className="font-semibold text-gray-900">{formatLastUpdate(status?.lastUpdateTime)}</p>
                        </div>
                    </div>
                    <div className="flex items-start gap-3">
                        <BarChart3 className="w-5 h-5 text-gray-400 mt-1" />
                        <div>
                            <p className="text-sm text-gray-500">Toplam Güncellenen</p>
                            <p className="font-semibold text-gray-900">{status?.totalUpdated || 0} enstrüman</p>
                        </div>
                    </div>
                    <div className="flex items-start gap-3">
                        <Info className="w-5 h-5 text-gray-400 mt-1" />
                        <div>
                            <p className="text-sm text-gray-500">Durum</p>
                            <p className="font-semibold text-gray-900">{status?.message || 'Bilgi yok'}</p>
                        </div>
                    </div>
                </div>

                {/* Stats — Yahoo olarak güncellendi */}
                {status?.totalUpdated > 0 && (
                    <div className="border-t pt-6">
                        <h3 className="text-sm font-semibold text-gray-700 mb-4">Detaylı İstatistikler</h3>
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                            <StatBox label="TCMB Döviz" value={status?.tcmbUpdated || 0} color="text-blue-600" />
                            <StatBox label="Yahoo Finance" value={status?.yahooUpdated || 0} color="text-purple-600" />
                            <StatBox label="Tahvil/Bono" value={status?.bondsUpdated || 0} color="text-indigo-600" />
                            <StatBox label="Toplam" value={status?.totalUpdated || 0} color="text-green-600" />
                        </div>
                    </div>
                )}

                {/* Ana butonlar */}
                <div className="mt-6 pt-6 border-t grid grid-cols-1 md:grid-cols-2 gap-4">
                    <button
                        onClick={handleUpdateAll}
                        disabled={updating || status?.updating}
                        className="w-full flex items-center justify-center gap-3 px-6 py-4 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 disabled:from-gray-400 disabled:to-gray-500 text-white font-semibold rounded-lg transition shadow-lg"
                    >
                        {(updating || status?.updating) ? (
                            <><Loader2 className="w-5 h-5 animate-spin" /><span>Güncelleniyor...</span></>
                        ) : (
                            <><RefreshCw className="w-5 h-5" /><span>Tüm Fiyatları Güncelle</span></>
                        )}
                    </button>

                    {/* Geçmiş veri butonu */}
                    <button
                        onClick={handleFetchHistorical}
                        disabled={updating || status?.updating}
                        className="w-full flex items-center justify-center gap-3 px-6 py-4 bg-gradient-to-r from-green-600 to-teal-600 hover:from-green-700 hover:to-teal-700 disabled:from-gray-400 disabled:to-gray-500 text-white font-semibold rounded-lg transition shadow-lg"
                    >
                        {(updating || status?.updating) ? (
                            <><Loader2 className="w-5 h-5 animate-spin" /><span>Çekiliyor...</span></>
                        ) : (
                            <><History className="w-5 h-5" /><span>Geçmiş Verileri Çek (1 Yıl)</span></>
                        )}
                    </button>
                </div>
                <p className="text-sm text-gray-500 text-center mt-3">
                    ⚠️ Geçmiş veri çekme işlemi uzun sürebilir (5-10 dakika)
                </p>
            </div>

            {/* Tekil güncelleme kartları */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-6">
                <UpdateCard
                    title="TCMB Döviz Kurları"
                    description="Merkez Bankası resmi kurları"
                    icon={<DollarSign className="w-6 h-6" />}
                    iconBg="bg-blue-100" iconColor="text-blue-600"
                    limit="Sınırsız"
                    onUpdate={() => handleSingleUpdate(updateTcmb, 'TCMB Kurları')}
                    disabled={updating || status?.updating}
                />
                <UpdateCard
                    title="Yahoo - ABD Hisseleri"
                    description="AAPL, MSFT, GOOGL, TSLA ve daha fazlası"
                    icon={<TrendingUp className="w-6 h-6" />}
                    iconBg="bg-purple-100" iconColor="text-purple-600"
                    limit="Sınırsız"
                    onUpdate={() => handleSingleUpdate(updateUsStocks, 'ABD Hisseleri')}
                    disabled={updating || status?.updating}
                />
                <UpdateCard
                    title="Yahoo - BIST Hisseleri"
                    description="Türkiye borsası hisse senetleri"
                    icon={<BarChart3 className="w-6 h-6" />}
                    iconBg="bg-green-100" iconColor="text-green-600"
                    limit="Sınırsız"
                    onUpdate={() => handleSingleUpdate(updateBist, 'BIST Hisseleri')}
                    disabled={updating || status?.updating}
                />
                <UpdateCard
                    title="Yahoo - Kripto Paralar"
                    description="BTC, ETH, BNB ve diğer kriptolar"
                    icon={<Bitcoin className="w-6 h-6" />}
                    iconBg="bg-pink-100" iconColor="text-pink-600"
                    limit="Sınırsız"
                    onUpdate={() => handleSingleUpdate(updateCrypto, 'Kripto Paralar')}
                    disabled={updating || status?.updating}
                />
                <UpdateCard
                    title="Yahoo - Kıymetli Metaller"
                    description="Altın, gümüş, platin, paladyum"
                    icon={<Gem className="w-6 h-6" />}
                    iconBg="bg-yellow-100" iconColor="text-yellow-600"
                    limit="Sınırsız"
                    onUpdate={() => handleSingleUpdate(updatePrecious, 'Kıymetli Metaller')}
                    disabled={updating || status?.updating}
                />
                <UpdateCard
                    title="Yahoo Finance - Tahvil"
                    description="ABD 10Y, 30Y, 5Y tahvil ve 3 aylık hazine bonosu"
                    icon={<FileText className="w-6 h-6" />}
                    iconBg="bg-indigo-100" iconColor="text-indigo-600"
                    limit="Sınırsız"
                    onUpdate={() => handleSingleUpdate(updateBonds, 'Tahvil/Bono')}
                    disabled={updating || status?.updating}
                />
            </div>

            {/* Info Box */}
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
                <div className="flex items-start gap-3">
                    <Info className="w-6 h-6 text-blue-600 flex-shrink-0 mt-1" />
                    <div>
                        <h3 className="font-semibold text-blue-900 mb-2">Güncelleme Hakkında</h3>
                        <ul className="space-y-2 text-sm text-blue-800">
                            <li className="flex items-start gap-2">
                                <span className="text-blue-600 mt-1">•</span>
                                <span><strong>Tümünü Güncelle:</strong> TCMB + Yahoo Finance + Tahvil/Bono verilerini çeker.</span>
                            </li>
                            <li className="flex items-start gap-2">
                                <span className="text-blue-600 mt-1">•</span>
                                <span><strong>Geçmiş Verileri Çek:</strong> Tüm enstrümanlar için 1 yıllık geçmiş fiyat verisi çeker. Grafiklerde gösterilir.</span>
                            </li>
                            <li className="flex items-start gap-2">
                                <span className="text-blue-600 mt-1">•</span>
                                <span><strong>Yahoo Finance:</strong> ABD hisseleri, BIST, kripto ve kıymetli metaller için sınırsız ücretsiz veri.</span>
                            </li>
                            <li className="flex items-start gap-2">
                                <span className="text-blue-600 mt-1">•</span>
                                <span><strong>Öneri:</strong> Günde 1-2 kez "Tümünü Güncelle" yeterli. Geçmiş veriyi sadece bir kez çekmeniz yeterli.</span>
                            </li>
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

const UpdateCard = ({ title, description, icon, iconBg, iconColor, limit, onUpdate, disabled }) => (
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
            {disabled ? (
                <><Loader2 className="w-4 h-4 animate-spin" /><span>Güncelleniyor...</span></>
            ) : (
                <><RefreshCw className="w-4 h-4" /><span>Güncelle</span></>
            )}
        </button>
    </div>
);

export default AdminInstrumentUpdatePage;