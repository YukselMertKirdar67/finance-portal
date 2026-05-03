import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Users,
    Briefcase,
    TrendingUp,
    Activity,
    UserCheck,
    UserX,
    ArrowUpRight,
    ArrowDownRight,
    Eye,
    RefreshCw,
    Newspaper
} from 'lucide-react';
import { getAdminStats } from '../../API/adminApi';
import { useAuth } from '../../context/AuthContext';

const AdminDashboard = () => {
    const navigate = useNavigate();
    const { isAdmin } = useAuth();

    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        // Admin değilse dashboard'a yönlendir
        if (!isAdmin) {
            navigate('/dashboard');
            return;
        }

        fetchStats();
    }, [isAdmin, navigate]);

    const fetchStats = async () => {
        setLoading(true);
        setError('');

        try {
            const data = await getAdminStats();
            setStats(data);
        } catch (err) {
            setError('İstatistikler yüklenirken bir hata oluştu');
            console.error('Error fetching stats:', err);
        } finally {
            setLoading(false);
        }
    };

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="text-center">
                    <div className="w-16 h-16 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
                    <p className="text-gray-600">İstatistikler yükleniyor...</p>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="bg-red-50 border border-red-200 rounded-lg p-6 max-w-md">
                    <p className="text-red-800">{error}</p>
                    <button
                        onClick={fetchStats}
                        className="mt-4 px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg transition"
                    >
                        Tekrar Dene
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="p-6 bg-gray-50 min-h-screen">

            {/* Header */}
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-gray-800 mb-2">Admin Dashboard</h1>
                <p className="text-gray-600">Sistem geneli istatistikler ve yönetim</p>
            </div>

            {/* Stats Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">

                {/* Total Users */}
                <StatCard
                    title="Toplam Kullanıcı"
                    value={stats?.totalUsers || 0}
                    icon={<Users className="w-8 h-8" />}
                    iconBg="bg-blue-100"
                    iconColor="text-blue-600"
                    trend={null}
                    onClick={() => navigate('/admin/users')}
                />

                {/* Active Users */}
                <StatCard
                    title="Aktif Kullanıcı"
                    value={stats?.activeUsers || 0}
                    icon={<UserCheck className="w-8 h-8" />}
                    iconBg="bg-green-100"
                    iconColor="text-green-600"
                    subtitle={`${stats?.disabledUsers || 0} devre dışı`}
                />

                {/* Total Portfolios */}
                <StatCard
                    title="Toplam Portföy"
                    value={stats?.totalPortfolios || 0}
                    icon={<Briefcase className="w-8 h-8" />}
                    iconBg="bg-purple-100"
                    iconColor="text-purple-600"
                    subtitle={`${stats?.activePortfolios || 0} aktif`}
                />

                {/* Total Transactions */}
                <StatCard
                    title="Toplam İşlem"
                    value={stats?.totalTransactions || 0}
                    icon={<Activity className="w-8 h-8" />}
                    iconBg="bg-orange-100"
                    iconColor="text-orange-600"
                    subtitle={`${stats?.buyTransactions || 0} alış / ${stats?.sellTransactions || 0} satış`}
                />
            </div>

            {/* Secondary Stats */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">

                {/* Portfolio Value */}
                <div className="bg-white rounded-xl shadow-md p-6 border border-gray-100">
                    <div className="flex items-center justify-between mb-4">
                        <h3 className="text-lg font-semibold text-gray-800">Toplam Portföy Değeri</h3>
                        <TrendingUp className="w-6 h-6 text-blue-600" />
                    </div>
                    <p className="text-3xl font-bold text-gray-900">
                        ₺{(stats?.totalPortfolioValue || 0).toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                    </p>
                    <p className="text-sm text-gray-500 mt-2">İlk yatırım tutarları toplamı</p>
                </div>

                {/* Watchlist Items */}
                <div className="bg-white rounded-xl shadow-md p-6 border border-gray-100">
                    <div className="flex items-center justify-between mb-4">
                        <h3 className="text-lg font-semibold text-gray-800">Takip Listesi</h3>
                        <Eye className="w-6 h-6 text-indigo-600" />
                    </div>
                    <p className="text-3xl font-bold text-gray-900">
                        {stats?.totalWatchlistItems || 0}
                    </p>
                    <p className="text-sm text-gray-500 mt-2">Toplam takip edilen enstrüman</p>
                </div>

                {/* User Activity */}
                <div className="bg-white rounded-xl shadow-md p-6 border border-gray-100">
                    <div className="flex items-center justify-between mb-4">
                        <h3 className="text-lg font-semibold text-gray-800">Kullanıcı Oranı</h3>
                        <UserCheck className="w-6 h-6 text-green-600" />
                    </div>
                    <p className="text-3xl font-bold text-gray-900">
                        {stats?.totalUsers > 0
                            ? Math.round((stats?.activeUsers / stats?.totalUsers) * 100)
                            : 0}%
                    </p>
                    <p className="text-sm text-gray-500 mt-2">Aktif kullanıcı oranı</p>
                </div>
            </div>

            {/* Hızlı İşlemler */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">

                <button
                    onClick={() => navigate('/admin/users')}
                    className="flex items-center gap-3 p-4 border border-gray-200 rounded-lg hover:bg-blue-50 hover:border-blue-300 transition"
                >
                    <Users className="w-6 h-6 text-blue-600" />
                    <div className="text-left">
                        <p className="font-semibold text-gray-800">Kullanıcı Yönetimi</p>
                        <p className="text-sm text-gray-500">Kullanıcıları görüntüle ve yönet</p>
                    </div>
                </button>

                <button
                    onClick={() => navigate('/admin/instruments')}
                    className="flex items-center gap-3 p-4 border border-gray-200 rounded-lg hover:bg-orange-50 hover:border-orange-300 transition"
                >
                    <RefreshCw className="w-6 h-6 text-orange-600" />
                    <div className="text-left">
                        <p className="font-semibold text-gray-800">Fiyat Güncellemesi</p>
                        <p className="text-sm text-gray-500">Enstrüman fiyatlarını güncelle</p>
                    </div>
                </button>

                <button
                    onClick={() => navigate('/admin/news')}
                    className="flex items-center gap-3 p-4 border border-gray-200 rounded-lg hover:bg-purple-50 hover:border-purple-300 transition"
                >
                    <Newspaper className="w-6 h-6 text-purple-600" />
                    <div className="text-left">
                        <p className="font-semibold text-gray-800">Haber Yönetimi</p>
                        <p className="text-sm text-gray-500">Haberleri yönet ve güncelle</p>
                    </div>
                </button>

                <button
                    onClick={fetchStats}
                    className="flex items-center gap-3 p-4 border border-gray-200 rounded-lg hover:bg-green-50 hover:border-green-300 transition"
                >
                    <Activity className="w-6 h-6 text-green-600" />
                    <div className="text-left">
                        <p className="font-semibold text-gray-800">İstatistikleri Yenile</p>
                        <p className="text-sm text-gray-500">En güncel verileri getir</p>
                    </div>
                </button>
            </div>
        </div>
    );
};

// Stat Card Component
const StatCard = ({ title, value, icon, iconBg, iconColor, subtitle, trend, onClick }) => {
    return (
        <div
            className={`bg-white rounded-xl shadow-md p-6 border border-gray-100 ${
                onClick ? 'cursor-pointer hover:shadow-lg transition' : ''
            }`}
            onClick={onClick}
        >
            <div className="flex items-center justify-between mb-4">
                <div className={`${iconBg} ${iconColor} p-3 rounded-lg`}>
                    {icon}
                </div>
                {trend && (
                    <div className={`flex items-center gap-1 ${
                        trend > 0 ? 'text-green-600' : 'text-red-600'
                    }`}>
                        {trend > 0 ? <ArrowUpRight className="w-4 h-4" /> : <ArrowDownRight className="w-4 h-4" />}
                        <span className="text-sm font-semibold">{Math.abs(trend)}%</span>
                    </div>
                )}
            </div>

            <h3 className="text-sm font-medium text-gray-600 mb-1">{title}</h3>
            <p className="text-3xl font-bold text-gray-900">{value.toLocaleString('tr-TR')}</p>

            {subtitle && (
                <p className="text-sm text-gray-500 mt-2">{subtitle}</p>
            )}
        </div>
    );
};

export default AdminDashboard;