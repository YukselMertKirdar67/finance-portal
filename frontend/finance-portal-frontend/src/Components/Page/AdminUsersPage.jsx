import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
    Users, Search, UserCheck, UserX, Mail,
    Calendar, AlertCircle, CheckCircle, RefreshCw
} from 'lucide-react';
import { getAllUsers, searchUsers, disableUser, enableUser } from '../../API/adminApi';
import { useAuth } from '../../context/AuthContext';

const AdminUsersPage = () => {
    const navigate = useNavigate();
    const { isAdmin } = useAuth();
    const { t, i18n } = useTranslation();

    const [users, setUsers] = useState([]);
    const [filteredUsers, setFilteredUsers] = useState([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [actionLoading, setActionLoading] = useState(null);

    useEffect(() => {
        if (!isAdmin) { navigate('/dashboard'); return; }
        fetchUsers();
    }, [isAdmin, navigate]);

    const fetchUsers = async () => {
        setLoading(true);
        setError('');
        try {
            const data = await getAllUsers();
            setUsers(data);
            setFilteredUsers(data);
        } catch (err) {
            setError(t('admin.users.loadError'));
            console.error('Error fetching users:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleSearch = async (query) => {
        setSearchQuery(query);
        if (!query.trim()) { setFilteredUsers(users); return; }
        try {
            const data = await searchUsers(query);
            setFilteredUsers(data);
        } catch (err) {
            setFilteredUsers(users.filter(user =>
                user.username.toLowerCase().includes(query.toLowerCase()) ||
                user.email.toLowerCase().includes(query.toLowerCase())
            ));
        }
    };

    const handleToggleUser = async (userId, currentStatus) => {
        setActionLoading(userId);
        try {
            if (currentStatus) { await disableUser(userId); }
            else { await enableUser(userId); }
            await fetchUsers();
        } catch (err) {
            alert(t('admin.users.toggleError'));
        } finally {
            setActionLoading(null);
        }
    };

    const formatDate = (dateStr) => {
        const locale = i18n.language === 'en' ? 'en-US' : 'tr-TR';
        return new Date(dateStr).toLocaleDateString(locale);
    };

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="text-center">
                    <div className="w-16 h-16 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
                    <p className="text-gray-600">{t('admin.users.loading')}</p>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="bg-red-50 border border-red-200 rounded-lg p-6 max-w-md">
                    <AlertCircle className="w-12 h-12 text-red-600 mx-auto mb-4" />
                    <p className="text-red-800 text-center">{error}</p>
                    <button
                        onClick={fetchUsers}
                        className="mt-4 w-full px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg transition"
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
            <div className="mb-6">
                <div className="flex items-center justify-between mb-4">
                    <div>
                        <h1 className="text-3xl font-bold text-gray-800 mb-2">{t('admin.users.title')}</h1>
                        <p className="text-gray-600">{t('admin.users.subtitle')}</p>
                    </div>
                    <button
                        onClick={fetchUsers}
                        className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition"
                    >
                        <RefreshCw className="w-4 h-4" />
                        {t('common.refresh')}
                    </button>
                </div>

                {/* Stats */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
                    <div className="bg-white rounded-lg shadow-md p-4 border border-gray-100">
                        <div className="flex items-center gap-3">
                            <div className="bg-blue-100 text-blue-600 p-3 rounded-lg">
                                <Users className="w-6 h-6" />
                            </div>
                            <div>
                                <p className="text-sm text-gray-600">{t('admin.users.totalUsers')}</p>
                                <p className="text-2xl font-bold text-gray-900">{users.length}</p>
                            </div>
                        </div>
                    </div>
                    <div className="bg-white rounded-lg shadow-md p-4 border border-gray-100">
                        <div className="flex items-center gap-3">
                            <div className="bg-green-100 text-green-600 p-3 rounded-lg">
                                <UserCheck className="w-6 h-6" />
                            </div>
                            <div>
                                <p className="text-sm text-gray-600">{t('admin.users.activeUsers')}</p>
                                <p className="text-2xl font-bold text-gray-900">{users.filter(u => u.enabled).length}</p>
                            </div>
                        </div>
                    </div>
                    <div className="bg-white rounded-lg shadow-md p-4 border border-gray-100">
                        <div className="flex items-center gap-3">
                            <div className="bg-red-100 text-red-600 p-3 rounded-lg">
                                <UserX className="w-6 h-6" />
                            </div>
                            <div>
                                <p className="text-sm text-gray-600">{t('admin.users.disabledUsers')}</p>
                                <p className="text-2xl font-bold text-gray-900">{users.filter(u => !u.enabled).length}</p>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Search */}
                <div className="bg-white rounded-lg shadow-md p-4 border border-gray-100">
                    <div className="relative">
                        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
                        <input
                            type="text"
                            value={searchQuery}
                            onChange={(e) => handleSearch(e.target.value)}
                            placeholder={t('admin.users.searchPlaceholder')}
                            className="w-full pl-11 pr-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition"
                        />
                    </div>
                </div>
            </div>

            {/* Table */}
            <div className="bg-white rounded-xl shadow-md border border-gray-100 overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full">
                        <thead className="bg-gray-50 border-b border-gray-200">
                        <tr>
                            <th className="px-6 py-4 text-left text-sm font-semibold text-gray-700">{t('admin.users.colUser')}</th>
                            <th className="px-6 py-4 text-left text-sm font-semibold text-gray-700">{t('auth.email')}</th>
                            <th className="px-6 py-4 text-left text-sm font-semibold text-gray-700">{t('profile.registerDate')}</th>
                            <th className="px-6 py-4 text-left text-sm font-semibold text-gray-700">{t('admin.users.colStatus')}</th>
                            <th className="px-6 py-4 text-left text-sm font-semibold text-gray-700">{t('admin.users.colActions')}</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200">
                        {filteredUsers.length === 0 ? (
                            <tr>
                                <td colSpan="5" className="px-6 py-12 text-center">
                                    <Users className="w-12 h-12 text-gray-300 mx-auto mb-3" />
                                    <p className="text-gray-500">
                                        {searchQuery ? t('admin.users.notFound') : t('admin.users.noUsers')}
                                    </p>
                                </td>
                            </tr>
                        ) : (
                            filteredUsers.map((user) => (
                                <tr key={user.id} className="hover:bg-gray-50 transition">
                                    <td className="px-6 py-4">
                                        <div className="flex items-center gap-3">
                                            <div className="w-10 h-10 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center font-semibold">
                                                {user.username.substring(0, 2).toUpperCase()}
                                            </div>
                                            <div>
                                                <Link
                                                    to={`/admin/users/${user.id}`}
                                                    className="font-semibold text-blue-600 hover:text-blue-800 hover:underline"
                                                >
                                                    {user.username}
                                                </Link>
                                                <p className="text-sm text-gray-500">ID: {user.id}</p>
                                            </div>
                                        </div>
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="flex items-center gap-2 text-gray-700">
                                            <Mail className="w-4 h-4 text-gray-400" />
                                            <span>{user.email}</span>
                                        </div>
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="flex items-center gap-2 text-gray-700">
                                            <Calendar className="w-4 h-4 text-gray-400" />
                                            <span>{formatDate(user.createdAt)}</span>
                                        </div>
                                    </td>
                                    <td className="px-6 py-4">
                                        {user.enabled ? (
                                            <span className="inline-flex items-center gap-1 px-3 py-1 bg-green-100 text-green-700 rounded-full text-sm font-medium">
                                                    <CheckCircle className="w-4 h-4" />
                                                {t('profile.active')}
                                                </span>
                                        ) : (
                                            <span className="inline-flex items-center gap-1 px-3 py-1 bg-red-100 text-red-700 rounded-full text-sm font-medium">
                                                    <AlertCircle className="w-4 h-4" />
                                                {t('admin.users.disabled')}
                                                </span>
                                        )}
                                    </td>
                                    <td className="px-6 py-4">
                                        <button
                                            onClick={() => handleToggleUser(user.id, user.enabled)}
                                            disabled={actionLoading === user.id}
                                            className={`px-4 py-2 rounded-lg font-medium transition flex items-center gap-2 ${
                                                user.enabled
                                                    ? 'bg-red-100 text-red-700 hover:bg-red-200'
                                                    : 'bg-green-100 text-green-700 hover:bg-green-200'
                                            }`}
                                        >
                                            {actionLoading === user.id ? (
                                                <>
                                                    <div className="w-4 h-4 border-2 border-current border-t-transparent rounded-full animate-spin" />
                                                    {t('instrumentDetail.processing')}
                                                </>
                                            ) : user.enabled ? (
                                                <>
                                                    <UserX className="w-4 h-4" />
                                                    {t('admin.users.disable')}
                                                </>
                                            ) : (
                                                <>
                                                    <UserCheck className="w-4 h-4" />
                                                    {t('admin.users.enable')}
                                                </>
                                            )}
                                        </button>
                                    </td>
                                </tr>
                            ))
                        )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
};

export default AdminUsersPage;