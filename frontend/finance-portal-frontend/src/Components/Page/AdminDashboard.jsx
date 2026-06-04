import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
    Users, Briefcase, TrendingUp, Activity,
    UserCheck, ArrowUpRight, ArrowDownRight,
    Eye, RefreshCw, Newspaper
} from 'lucide-react';
import { getAdminStats } from '../../API/adminApi';
import { useAuth } from '../../context/AuthContext';

const AdminDashboard = () => {
    const navigate = useNavigate();
    const { isAdmin } = useAuth();
    const { t } = useTranslation();

    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        if (!isAdmin) { navigate('/dashboard'); return; }
        fetchStats();
    }, [isAdmin, navigate]);

    const fetchStats = async () => {
        setLoading(true);
        setError('');
        try {
            const data = await getAdminStats();
            setStats(data);
        } catch (err) {
            setError(t('admin.dashboard.loadError'));
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
                    <p className="text-gray-600">{t('admin.dashboard.loading')}</p>
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
                        {t('common.refresh')}
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="p-6 bg-gray-50 min-h-screen">

            {/* Header */}
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-gray-800 mb-2">{t('admin.dashboard.title')}</h1>
                <p className="text-gray-600">{t('admin.dashboard.subtitle')}</p>
            </div>

            {/* Stats Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                <StatCard
                    title={t('admin.users.totalUsers')}
                    value={stats?.totalUsers || 0}
                    icon={<Users className="w-8 h-8" />}
                    iconBg="bg-blue-100" iconColor="text-blue-600"
                    onClick={() => navigate('/admin/users')}
                />
                <StatCard
                    title={t('admin.users.activeUsers')}
                    value={stats?.activeUsers || 0}
                    icon={<UserCheck className="w-8 h-8" />}
                    iconBg="bg-green-100" iconColor="text-green-600"
                    subtitle={t('admin.dashboard.disabledCount', { count: stats?.disabledUsers || 0 })}
                />
                <StatCard
                    title={t('admin.portfolio.totalPortfolios')}
                    value={stats?.totalPortfolios || 0}
                    icon={<Briefcase className="w-8 h-8" />}
                    iconBg="bg-purple-100" iconColor="text-purple-600"
                    subtitle={t('admin.dashboard.activeCount', { count: stats?.activePortfolios || 0 })}
                />
                <StatCard
                    title={t('admin.dashboard.totalTransactions')}
                    value={stats?.totalTransactions || 0}
                    icon={<Activity className="w-8 h-8" />}
                    iconBg="bg-orange-100" iconColor="text-orange-600"
                    subtitle={t('admin.dashboard.buySell', {
                        buy: stats?.buyTransactions || 0,
                        sell: stats?.sellTransactions || 0
                    })}
                />
            </div>

            {/* Secondary Stats */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
                <div className="bg-white rounded-xl shadow-md p-6 border border-gray-100">
                    <div className="flex items-center justify-between mb-4">
                        <h3 className="text-lg font-semibold text-gray-800">{t('admin.dashboard.totalPortfolioValue')}</h3>
                        <TrendingUp className="w-6 h-6 text-blue-600" />
                    </div>
                    <p className="text-3xl font-bold text-gray-900">
                        ₺{(stats?.totalPortfolioValue || 0).toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                    </p>
                    <p className="text-sm text-gray-500 mt-2">{t('admin.dashboard.totalInvestedDesc')}</p>
                </div>

                <div className="bg-white rounded-xl shadow-md p-6 border border-gray-100">
                    <div className="flex items-center justify-between mb-4">
                        <h3 className="text-lg font-semibold text-gray-800">{t('watchlist.title')}</h3>
                        <Eye className="w-6 h-6 text-indigo-600" />
                    </div>
                    <p className="text-3xl font-bold text-gray-900">{stats?.totalWatchlistItems || 0}</p>
                    <p className="text-sm text-gray-500 mt-2">{t('admin.dashboard.totalWatchlistDesc')}</p>
                </div>

                <div className="bg-white rounded-xl shadow-md p-6 border border-gray-100">
                    <div className="flex items-center justify-between mb-4">
                        <h3 className="text-lg font-semibold text-gray-800">{t('admin.dashboard.userRate')}</h3>
                        <UserCheck className="w-6 h-6 text-green-600" />
                    </div>
                    <p className="text-3xl font-bold text-gray-900">
                        {stats?.totalUsers > 0
                            ? Math.round((stats?.activeUsers / stats?.totalUsers) * 100)
                            : 0}%
                    </p>
                    <p className="text-sm text-gray-500 mt-2">{t('admin.dashboard.activeUserRate')}</p>
                </div>
            </div>

            {/* Quick Actions */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <button
                    onClick={() => navigate('/admin/users')}
                    className="flex items-center gap-3 p-4 border border-gray-200 rounded-lg hover:bg-blue-50 hover:border-blue-300 transition"
                >
                    <Users className="w-6 h-6 text-blue-600" />
                    <div className="text-left">
                        <p className="font-semibold text-gray-800">{t('admin.users.title')}</p>
                        <p className="text-sm text-gray-500">{t('admin.users.subtitle')}</p>
                    </div>
                </button>

                <button
                    onClick={() => navigate('/admin/instruments')}
                    className="flex items-center gap-3 p-4 border border-gray-200 rounded-lg hover:bg-orange-50 hover:border-orange-300 transition"
                >
                    <RefreshCw className="w-6 h-6 text-orange-600" />
                    <div className="text-left">
                        <p className="font-semibold text-gray-800">{t('admin.instruments.title')}</p>
                        <p className="text-sm text-gray-500">{t('admin.instruments.subtitle')}</p>
                    </div>
                </button>

                <button
                    onClick={() => navigate('/admin/news')}
                    className="flex items-center gap-3 p-4 border border-gray-200 rounded-lg hover:bg-purple-50 hover:border-purple-300 transition"
                >
                    <Newspaper className="w-6 h-6 text-purple-600" />
                    <div className="text-left">
                        <p className="font-semibold text-gray-800">{t('admin.news.title')}</p>
                        <p className="text-sm text-gray-500">{t('admin.news.subtitle')}</p>
                    </div>
                </button>

                <button
                    onClick={fetchStats}
                    className="flex items-center gap-3 p-4 border border-gray-200 rounded-lg hover:bg-green-50 hover:border-green-300 transition"
                >
                    <Activity className="w-6 h-6 text-green-600" />
                    <div className="text-left">
                        <p className="font-semibold text-gray-800">{t('admin.dashboard.refreshStats')}</p>
                        <p className="text-sm text-gray-500">{t('admin.dashboard.refreshStatsDesc')}</p>
                    </div>
                </button>
            </div>
        </div>
    );
};

const StatCard = ({ title, value, icon, iconBg, iconColor, subtitle, trend, onClick }) => (
    <div
        className={`bg-white rounded-xl shadow-md p-6 border border-gray-100 ${onClick ? 'cursor-pointer hover:shadow-lg transition' : ''}`}
        onClick={onClick}
    >
        <div className="flex items-center justify-between mb-4">
            <div className={`${iconBg} ${iconColor} p-3 rounded-lg`}>{icon}</div>
            {trend && (
                <div className={`flex items-center gap-1 ${trend > 0 ? 'text-green-600' : 'text-red-600'}`}>
                    {trend > 0 ? <ArrowUpRight className="w-4 h-4" /> : <ArrowDownRight className="w-4 h-4" />}
                    <span className="text-sm font-semibold">{Math.abs(trend)}%</span>
                </div>
            )}
        </div>
        <h3 className="text-sm font-medium text-gray-600 mb-1">{title}</h3>
        <p className="text-3xl font-bold text-gray-900">{value.toLocaleString('tr-TR')}</p>
        {subtitle && <p className="text-sm text-gray-500 mt-2">{subtitle}</p>}
    </div>
);

export default AdminDashboard;