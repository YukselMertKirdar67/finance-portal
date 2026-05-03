import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    RefreshCw, Trash2, Download, BarChart3,
    CheckCircle, XCircle, Loader2, Info,
    Newspaper, Tag, Clock
} from 'lucide-react';
import {
    getNewsStats, fetchNewsFromApi, refreshAllNews,
    deleteAllNews, deleteNewsByCategory, getNewsCategories
} from '../../API/adminNewsApi';
import { useAuth } from '../../context/AuthContext';

const AdminNewsDashboard = () => {
    const navigate = useNavigate();
    const { isAdmin } = useAuth();

    const [stats, setStats] = useState(null);
    const [categories, setCategories] = useState([]);
    const [loading, setLoading] = useState(true);
    const [updating, setUpdating] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    useEffect(() => {
        if (!isAdmin) { navigate('/dashboard'); return; }
        fetchData();
    }, [isAdmin, navigate]);

    const fetchData = async () => {
        setLoading(true);
        try {
            const [statsData, categoriesData] = await Promise.all([
                getNewsStats(),
                getNewsCategories()
            ]);
            setStats(statsData.stats);
            setCategories(categoriesData.categories || []);
        } catch {
            setError('Veriler yüklenirken hata oluştu');
        } finally {
            setLoading(false);
        }
    };

    const showSuccess = (msg) => {
        setSuccess(msg);
        setError('');
        setTimeout(() => setSuccess(''), 5000);
    };

    const showError = (msg) => {
        setError(msg);
        setSuccess('');
    };

    const handleFetch = async () => {
        setUpdating(true);
        setError(''); setSuccess('');
        try {
            const result = await fetchNewsFromApi();
            if (result.success) {
                showSuccess(`✅ ${result.stats.saved} haber kaydedildi!`);
                fetchData();
            } else {
                showError('Haber çekme başarısız');
            }
        } catch {
            showError('Haber çekilirken hata oluştu');
        } finally {
            setUpdating(false);
        }
    };

    const handleRefresh = async () => {
        if (!window.confirm('Tüm haberler silinip yeniden çekilecek. Emin misiniz?')) return;
        setUpdating(true);
        setError(''); setSuccess('');
        try {
            const result = await refreshAllNews();
            if (result.success) {
                showSuccess(`✅ ${result.deletedCount} haber silindi, ${result.stats?.saved || 0} yeni haber eklendi!`);
                fetchData();
            } else {
                showError('Yenileme başarısız');
            }
        } catch {
            showError('Yenileme sırasında hata oluştu');
        } finally {
            setUpdating(false);
        }
    };

    const handleDeleteAll = async () => {
        if (!window.confirm('Tüm haberler kalıcı olarak silinecek. Emin misiniz?')) return;
        setUpdating(true);
        setError(''); setSuccess('');
        try {
            const result = await deleteAllNews();
            if (result.success) {
                showSuccess(`✅ ${result.deletedCount} haber silindi`);
                fetchData();
            } else {
                showError('Silme işlemi başarısız');
            }
        } catch {
            showError('Silme sırasında hata oluştu');
        } finally {
            setUpdating(false);
        }
    };

    const handleDeleteByCategory = async (category) => {
        if (!window.confirm(`"${category}" kategorisindeki tüm haberler silinecek. Emin misiniz?`)) return;
        setUpdating(true);
        setError(''); setSuccess('');
        try {
            const result = await deleteNewsByCategory(category);
            if (result.success) {
                showSuccess(`✅ ${result.deletedCount} haber silindi`);
                fetchData();
            } else {
                showError('Silme işlemi başarısız');
            }
        } catch {
            showError('Silme sırasında hata oluştu');
        } finally {
            setUpdating(false);
        }
    };

    const formatDate = (dateStr) => {
        if (!dateStr) return 'Bilgi yok';
        return new Date(dateStr).toLocaleString('tr-TR', {
            day: '2-digit', month: 'long', year: 'numeric',
            hour: '2-digit', minute: '2-digit'
        });
    };

    const getCategoryColor = (category) => {
        switch (category) {
            case 'FINANS': return { bg: 'bg-blue-100', text: 'text-blue-700', border: 'border-blue-200' };
            case 'DOVIZ': return { bg: 'bg-green-100', text: 'text-green-700', border: 'border-green-200' };
            case 'KRIPTO': return { bg: 'bg-purple-100', text: 'text-purple-700', border: 'border-purple-200' };
            default: return { bg: 'bg-gray-100', text: 'text-gray-700', border: 'border-gray-200' };
        }
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
                <h1 className="text-3xl font-bold text-gray-800 mb-2">Haber Yönetimi</h1>
                <p className="text-gray-600">Finansal haberleri yönetin ve güncelleyin</p>
            </div>

            {/* Bildirimler */}
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

            {/* İstatistik Kartları */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
                <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
                    <div className="flex items-center gap-3 mb-2">
                        <div className="bg-blue-100 p-2 rounded-lg">
                            <Newspaper className="w-5 h-5 text-blue-600" />
                        </div>
                        <p className="text-sm text-gray-500">Toplam Haber</p>
                    </div>
                    <p className="text-3xl font-bold text-gray-900">{stats?.totalNews || 0}</p>
                </div>

                <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
                    <div className="flex items-center gap-3 mb-2">
                        <div className="bg-purple-100 p-2 rounded-lg">
                            <Tag className="w-5 h-5 text-purple-600" />
                        </div>
                        <p className="text-sm text-gray-500">Kategori Sayısı</p>
                    </div>
                    <p className="text-3xl font-bold text-gray-900">{stats?.totalCategories || 0}</p>
                </div>

                <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
                    <div className="flex items-center gap-3 mb-2">
                        <div className="bg-green-100 p-2 rounded-lg">
                            <Clock className="w-5 h-5 text-green-600" />
                        </div>
                        <p className="text-sm text-gray-500">Son Güncelleme</p>
                    </div>
                    <p className="text-sm font-semibold text-gray-900">{formatDate(stats?.lastUpdate)}</p>
                </div>
            </div>

            {/* Aksiyon Butonları */}
            <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100 mb-6">
                <h2 className="text-xl font-bold text-gray-800 mb-4">İşlemler</h2>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <button
                        onClick={handleFetch}
                        disabled={updating}
                        className="flex items-center justify-center gap-2 px-4 py-3 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 disabled:from-gray-400 disabled:to-gray-500 text-white font-semibold rounded-lg transition shadow-md"
                    >
                        {updating ? (
                            <><Loader2 className="w-5 h-5 animate-spin" /><span>İşleniyor...</span></>
                        ) : (
                            <><Download className="w-5 h-5" /><span>Haber Çek</span></>
                        )}
                    </button>

                    <button
                        onClick={handleRefresh}
                        disabled={updating}
                        className="flex items-center justify-center gap-2 px-4 py-3 bg-gradient-to-r from-green-600 to-teal-600 hover:from-green-700 hover:to-teal-700 disabled:from-gray-400 disabled:to-gray-500 text-white font-semibold rounded-lg transition shadow-md"
                    >
                        {updating ? (
                            <><Loader2 className="w-5 h-5 animate-spin" /><span>İşleniyor...</span></>
                        ) : (
                            <><RefreshCw className="w-5 h-5" /><span>Tümünü Yenile</span></>
                        )}
                    </button>

                    <button
                        onClick={handleDeleteAll}
                        disabled={updating}
                        className="flex items-center justify-center gap-2 px-4 py-3 bg-gradient-to-r from-red-600 to-rose-600 hover:from-red-700 hover:to-rose-700 disabled:from-gray-400 disabled:to-gray-500 text-white font-semibold rounded-lg transition shadow-md"
                    >
                        {updating ? (
                            <><Loader2 className="w-5 h-5 animate-spin" /><span>İşleniyor...</span></>
                        ) : (
                            <><Trash2 className="w-5 h-5" /><span>Tümünü Sil</span></>
                        )}
                    </button>
                </div>
            </div>

            {/* Kategori Kartları */}
            <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100 mb-6">
                <h2 className="text-xl font-bold text-gray-800 mb-4">Kategori Yönetimi</h2>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    {categories.map(category => {
                        const colors = getCategoryColor(category);
                        const count = stats?.categoryCounts?.[category] || 0;
                        return (
                            <div
                                key={category}
                                className={`${colors.bg} ${colors.border} border rounded-xl p-5`}
                            >
                                <div className="flex items-center justify-between mb-3">
                                    <span className={`font-bold text-lg ${colors.text}`}>{category}</span>
                                    <span className={`text-2xl font-bold ${colors.text}`}>{count}</span>
                                </div>
                                <p className={`text-sm ${colors.text} opacity-70 mb-4`}>
                                    {count} haber mevcut
                                </p>
                                <button
                                    onClick={() => handleDeleteByCategory(category)}
                                    disabled={updating || count === 0}
                                    className="w-full flex items-center justify-center gap-2 px-3 py-2 bg-white hover:bg-red-50 disabled:opacity-50 text-red-600 font-semibold rounded-lg border border-red-200 transition text-sm"
                                >
                                    <Trash2 className="w-4 h-4" />
                                    Kategoriyi Sil
                                </button>
                            </div>
                        );
                    })}
                </div>
            </div>

            {/* Bilgi Kutusu */}
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
                <div className="flex items-start gap-3">
                    <Info className="w-6 h-6 text-blue-600 flex-shrink-0 mt-1" />
                    <div>
                        <h3 className="font-semibold text-blue-900 mb-2">Haber Yönetimi Hakkında</h3>
                        <ul className="space-y-2 text-sm text-blue-800">
                            <li className="flex items-start gap-2">
                                <span className="text-blue-600 mt-1">•</span>
                                <span><strong>Haber Çek:</strong> NewsAPI'dan yeni haberleri çeker, mevcut haberler korunur.</span>
                            </li>
                            <li className="flex items-start gap-2">
                                <span className="text-blue-600 mt-1">•</span>
                                <span><strong>Tümünü Yenile:</strong> Tüm haberler silinir ve API'dan yeniden çekilir.</span>
                            </li>
                            <li className="flex items-start gap-2">
                                <span className="text-blue-600 mt-1">•</span>
                                <span><strong>Kategori Sil:</strong> Sadece o kategorideki haberler silinir.</span>
                            </li>
                            <li className="flex items-start gap-2">
                                <span className="text-blue-600 mt-1">•</span>
                                <span><strong>Öneri:</strong> Günde 1-2 kez "Haber Çek" yeterlidir.</span>
                            </li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default AdminNewsDashboard;