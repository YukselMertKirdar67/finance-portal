import React, { useState, useEffect } from 'react';
import { useNavigate,Link } from 'react-router-dom';
import {
    Users,
    Search,
    UserCheck,
    UserX,
    Mail,
    Calendar,
    AlertCircle,
    CheckCircle,
    RefreshCw
} from 'lucide-react';
import { getAllUsers, searchUsers, disableUser, enableUser } from '../../API/adminApi';
import { useAuth } from '../../context/AuthContext';

const AdminUsersPage = () => {
    const navigate = useNavigate();
    const { isAdmin } = useAuth();

    const [users, setUsers] = useState([]);
    const [filteredUsers, setFilteredUsers] = useState([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [actionLoading, setActionLoading] = useState(null);

    useEffect(() => {
        // Admin değilse dashboard'a yönlendir
        if (!isAdmin) {
            navigate('/dashboard');
            return;
        }

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
            setError('Kullanıcılar yüklenirken bir hata oluştu');
            console.error('Error fetching users:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleSearch = async (query) => {
        setSearchQuery(query);

        if (!query.trim()) {
            setFilteredUsers(users);
            return;
        }

        try {
            const data = await searchUsers(query);
            setFilteredUsers(data);
        } catch (err) {
            console.error('Error searching users:', err);
            // Fallback to client-side filtering
            const filtered = users.filter(user =>
                user.username.toLowerCase().includes(query.toLowerCase()) ||
                user.email.toLowerCase().includes(query.toLowerCase())
            );
            setFilteredUsers(filtered);
        }
    };

    const handleToggleUser = async (userId, currentStatus) => {
        setActionLoading(userId);

        try {
            if (currentStatus) {
                await disableUser(userId);
            } else {
                await enableUser(userId);
            }

            // Kullanıcı listesini yenile
            await fetchUsers();

        } catch (err) {
            console.error('Error toggling user:', err);
            alert('İşlem sırasında bir hata oluştu');
        } finally {
            setActionLoading(null);
        }
    };

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="text-center">
                    <div className="w-16 h-16 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
                    <p className="text-gray-600">Kullanıcılar yükleniyor...</p>
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
                        Tekrar Dene
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
                        <h1 className="text-3xl font-bold text-gray-800 mb-2">Kullanıcı Yönetimi</h1>
                        <p className="text-gray-600">Tüm kullanıcıları görüntüle ve yönet</p>
                    </div>
                    <button
                        onClick={fetchUsers}
                        className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition"
                    >
                        <RefreshCw className="w-4 h-4" />
                        Yenile
                    </button>
                </div>

                {/* Stats Summary */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
                    <div className="bg-white rounded-lg shadow-md p-4 border border-gray-100">
                        <div className="flex items-center gap-3">
                            <div className="bg-blue-100 text-blue-600 p-3 rounded-lg">
                                <Users className="w-6 h-6" />
                            </div>
                            <div>
                                <p className="text-sm text-gray-600">Toplam Kullanıcı</p>
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
                                <p className="text-sm text-gray-600">Aktif Kullanıcı</p>
                                <p className="text-2xl font-bold text-gray-900">
                                    {users.filter(u => u.enabled).length}
                                </p>
                            </div>
                        </div>
                    </div>

                    <div className="bg-white rounded-lg shadow-md p-4 border border-gray-100">
                        <div className="flex items-center gap-3">
                            <div className="bg-red-100 text-red-600 p-3 rounded-lg">
                                <UserX className="w-6 h-6" />
                            </div>
                            <div>
                                <p className="text-sm text-gray-600">Devre Dışı</p>
                                <p className="text-2xl font-bold text-gray-900">
                                    {users.filter(u => !u.enabled).length}
                                </p>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Search Bar */}
                <div className="bg-white rounded-lg shadow-md p-4 border border-gray-100">
                    <div className="relative">
                        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
                        <input
                            type="text"
                            value={searchQuery}
                            onChange={(e) => handleSearch(e.target.value)}
                            placeholder="Kullanıcı adı veya email ile ara..."
                            className="w-full pl-11 pr-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition"
                        />
                    </div>
                </div>
            </div>

            {/* Users Table */}
            <div className="bg-white rounded-xl shadow-md border border-gray-100 overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full">
                        <thead className="bg-gray-50 border-b border-gray-200">
                        <tr>
                            <th className="px-6 py-4 text-left text-sm font-semibold text-gray-700">Kullanıcı</th>
                            <th className="px-6 py-4 text-left text-sm font-semibold text-gray-700">Email</th>
                            <th className="px-6 py-4 text-left text-sm font-semibold text-gray-700">Kayıt Tarihi</th>
                            <th className="px-6 py-4 text-left text-sm font-semibold text-gray-700">Durum</th>
                            <th className="px-6 py-4 text-left text-sm font-semibold text-gray-700">İşlemler</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200">
                        {filteredUsers.length === 0 ? (
                            <tr>
                                <td colSpan="5" className="px-6 py-12 text-center">
                                    <Users className="w-12 h-12 text-gray-300 mx-auto mb-3" />
                                    <p className="text-gray-500">
                                        {searchQuery ? 'Kullanıcı bulunamadı' : 'Henüz kullanıcı yok'}
                                    </p>
                                </td>
                            </tr>
                        ) : (
                            filteredUsers.map((user) => (
                                <tr key={user.id} className="hover:bg-gray-50 transition">
                                    {/* User Info */}
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

                                    {/* Email */}
                                    <td className="px-6 py-4">
                                        <div className="flex items-center gap-2 text-gray-700">
                                            <Mail className="w-4 h-4 text-gray-400" />
                                            <span>{user.email}</span>
                                        </div>
                                    </td>

                                    {/* Created At */}
                                    <td className="px-6 py-4">
                                        <div className="flex items-center gap-2 text-gray-700">
                                            <Calendar className="w-4 h-4 text-gray-400" />
                                            <span>{new Date(user.createdAt).toLocaleDateString('tr-TR')}</span>
                                        </div>
                                    </td>

                                    {/* Status */}
                                    <td className="px-6 py-4">
                                        {user.enabled ? (
                                            <span className="inline-flex items-center gap-1 px-3 py-1 bg-green-100 text-green-700 rounded-full text-sm font-medium">
                                                    <CheckCircle className="w-4 h-4" />
                                                    Aktif
                                                </span>
                                        ) : (
                                            <span className="inline-flex items-center gap-1 px-3 py-1 bg-red-100 text-red-700 rounded-full text-sm font-medium">
                                                    <AlertCircle className="w-4 h-4" />
                                                    Devre Dışı
                                                </span>
                                        )}
                                    </td>

                                    {/* Actions */}
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
                                                    İşleniyor...
                                                </>
                                            ) : user.enabled ? (
                                                <>
                                                    <UserX className="w-4 h-4" />
                                                    Devre Dışı Bırak
                                                </>
                                            ) : (
                                                <>
                                                    <UserCheck className="w-4 h-4" />
                                                    Aktif Et
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