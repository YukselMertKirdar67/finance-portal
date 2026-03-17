import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
    User,
    Mail,
    Calendar,
    Shield,
    CheckCircle,
    AlertCircle,
    ArrowLeft,
    UserCog,
    Briefcase,
    TrendingUp,
    Eye
} from 'lucide-react';
import { getUserDetail, assignRole, removeRole } from '../../API/adminApi';

const AdminUserDetailPage = () => {
    const { id } = useParams();
    const navigate = useNavigate();

    const [userDetail, setUserDetail] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [roleLoading, setRoleLoading] = useState(false);
    const [successMessage, setSuccessMessage] = useState('');

    useEffect(() => {
        fetchUserDetail();
    }, [id]);

    const fetchUserDetail = async () => {
        setLoading(true);
        setError('');

        try {
            const data = await getUserDetail(id);
            setUserDetail(data);
        } catch (err) {
            setError('Kullanıcı bilgileri yüklenirken bir hata oluştu');
            console.error('Error fetching user detail:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleAssignRole = async () => {
        setRoleLoading(true);
        setError('');
        setSuccessMessage('');

        try {
            const response = await assignRole(id, 'ADMIN');

            if (response.success) {
                setSuccessMessage('ADMIN rolü başarıyla atandı');
                setTimeout(() => {
                    setSuccessMessage('');
                    fetchUserDetail();
                }, 2000);
            } else {
                setError(response.message || 'Rol atanamadı');
            }

        } catch (err) {
            setError('Rol atama başarısız');
            console.error('Error assigning role:', err);
        } finally {
            setRoleLoading(false);
        }
    };

    const handleRemoveRole = async () => {
        setRoleLoading(true);
        setError('');
        setSuccessMessage('');

        try {
            const response = await removeRole(id, 'ADMIN');

            if (response.success) {
                setSuccessMessage('ADMIN rolü başarıyla kaldırıldı');
                setTimeout(() => {
                    setSuccessMessage('');
                    fetchUserDetail();
                }, 2000);
            } else {
                setError(response.message || 'Rol kaldırılamadı');
            }

        } catch (err) {
            setError('Rol kaldırma başarısız');
            console.error('Error removing role:', err);
        } finally {
            setRoleLoading(false);
        }
    };

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="text-center">
                    <div className="w-16 h-16 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
                    <p className="text-gray-600">Kullanıcı bilgileri yükleniyor...</p>
                </div>
            </div>
        );
    }

    const isAdmin = userDetail?.roles?.includes('ADMIN');

    return (
        <div className="p-6 bg-gray-50 min-h-screen">

            {/* Header */}
            <div className="mb-6 flex items-center gap-4">
                <button
                    onClick={() => navigate('/admin/users')}
                    className="p-2 hover:bg-gray-200 rounded-lg transition"
                >
                    <ArrowLeft className="w-5 h-5" />
                </button>
                <div>
                    <h1 className="text-3xl font-bold text-gray-800">Kullanıcı Detayı</h1>
                    <p className="text-gray-600">Kullanıcı bilgilerini görüntüle ve yönet</p>
                </div>
            </div>

            {/* Error Message */}
            {error && (
                <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
                    <AlertCircle className="w-5 h-5 text-red-600 mt-0.5 flex-shrink-0" />
                    <p className="text-red-800">{error}</p>
                </div>
            )}

            {/* Success Message */}
            {successMessage && (
                <div className="mb-6 p-4 bg-green-50 border border-green-200 rounded-lg flex items-start gap-3">
                    <CheckCircle className="w-5 h-5 text-green-600 mt-0.5 flex-shrink-0" />
                    <p className="text-green-800">{successMessage}</p>
                </div>
            )}

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

                {/* Profile Card */}
                <div className="lg:col-span-1">
                    <div className="bg-white rounded-xl shadow-md p-6 border border-gray-100">

                        {/* Avatar */}
                        <div className="text-center mb-6">
                            <div className="w-24 h-24 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-full mx-auto flex items-center justify-center text-white text-3xl font-bold shadow-lg">
                                {userDetail?.username?.substring(0, 2).toUpperCase()}
                            </div>
                        </div>

                        {/* Username */}
                        <div className="text-center mb-6">
                            <h2 className="text-2xl font-bold text-gray-900 mb-1">
                                {userDetail?.username}
                            </h2>
                            <p className="text-gray-500 text-sm">@{userDetail?.username}</p>
                        </div>

                        {/* Status */}
                        <div className="mb-6 text-center">
                            {userDetail?.enabled ? (
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
                        </div>

                        {/* Roles */}
                        <div className="space-y-2">
                            <p className="text-sm font-medium text-gray-600 mb-2">Roller:</p>
                            <div className="flex flex-wrap gap-2">
                                {userDetail?.roles?.map((role) => (
                                    <span
                                        key={role}
                                        className={`px-3 py-1 rounded-full text-sm font-medium ${
                                            role === 'ADMIN'
                                                ? 'bg-red-100 text-red-700'
                                                : 'bg-blue-100 text-blue-700'
                                        }`}
                                    >
                                        {role}
                                    </span>
                                ))}
                            </div>
                        </div>
                    </div>

                    {/* Stats Card */}
                    <div className="mt-6 bg-white rounded-xl shadow-md p-6 border border-gray-100">
                        <h3 className="text-lg font-bold text-gray-800 mb-4">İstatistikler</h3>

                        <div className="space-y-4">
                            <div className="flex items-center justify-between">
                                <div className="flex items-center gap-2">
                                    <Briefcase className="w-5 h-5 text-blue-600" />
                                    <span className="text-gray-600">Portföyler</span>
                                </div>
                                <span className="font-bold text-gray-900">{userDetail?.portfolioCount || 0}</span>
                            </div>

                            <div className="flex items-center justify-between">
                                <div className="flex items-center gap-2">
                                    <TrendingUp className="w-5 h-5 text-green-600" />
                                    <span className="text-gray-600">İşlemler</span>
                                </div>
                                <span className="font-bold text-gray-900">{userDetail?.transactionCount || 0}</span>
                            </div>

                            <div className="flex items-center justify-between">
                                <div className="flex items-center gap-2">
                                    <Eye className="w-5 h-5 text-purple-600" />
                                    <span className="text-gray-600">Takip Listesi</span>
                                </div>
                                <span className="font-bold text-gray-900">{userDetail?.watchlistCount || 0}</span>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Details Card */}
                <div className="lg:col-span-2">
                    <div className="bg-white rounded-xl shadow-md p-6 border border-gray-100">
                        <h3 className="text-xl font-bold text-gray-800 mb-6">Hesap Bilgileri</h3>

                        <div className="space-y-6">

                            {/* Username */}
                            <div className="flex items-start gap-4 pb-4 border-b border-gray-100">
                                <div className="bg-blue-100 text-blue-600 p-3 rounded-lg">
                                    <User className="w-5 h-5" />
                                </div>
                                <div className="flex-1">
                                    <p className="text-sm text-gray-500 mb-1">Kullanıcı Adı</p>
                                    <p className="text-gray-900 font-semibold">{userDetail?.username}</p>
                                </div>
                            </div>

                            {/* Email */}
                            <div className="flex items-start gap-4 pb-4 border-b border-gray-100">
                                <div className="bg-green-100 text-green-600 p-3 rounded-lg">
                                    <Mail className="w-5 h-5" />
                                </div>
                                <div className="flex-1">
                                    <p className="text-sm text-gray-500 mb-1">Email</p>
                                    <p className="text-gray-900 font-semibold">{userDetail?.email}</p>
                                    <div className="mt-2">
                                        {userDetail?.emailVerified ? (
                                            <span className="inline-flex items-center gap-1 text-sm text-green-600">
                                                <CheckCircle className="w-4 h-4" />
                                                Email doğrulandı
                                            </span>
                                        ) : (
                                            <span className="inline-flex items-center gap-1 text-sm text-orange-600">
                                                <AlertCircle className="w-4 h-4" />
                                                Email doğrulanmadı
                                            </span>
                                        )}
                                    </div>
                                </div>
                            </div>

                            {/* Account Status */}
                            <div className="flex items-start gap-4 pb-4 border-b border-gray-100">
                                <div className="bg-purple-100 text-purple-600 p-3 rounded-lg">
                                    <Shield className="w-5 h-5" />
                                </div>
                                <div className="flex-1">
                                    <p className="text-sm text-gray-500 mb-1">Hesap Durumu</p>
                                    {userDetail?.enabled ? (
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
                                </div>
                            </div>

                            {/* Created At */}
                            <div className="flex items-start gap-4">
                                <div className="bg-indigo-100 text-indigo-600 p-3 rounded-lg">
                                    <Calendar className="w-5 h-5" />
                                </div>
                                <div className="flex-1">
                                    <p className="text-sm text-gray-500 mb-1">Kayıt Tarihi</p>
                                    <p className="text-gray-900 font-semibold">
                                        {userDetail?.createdAt ? new Date(userDetail.createdAt).toLocaleDateString('tr-TR', {
                                            year: 'numeric',
                                            month: 'long',
                                            day: 'numeric'
                                        }) : 'Bilinmiyor'}
                                    </p>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Role Management Card */}
                    <div className="mt-6 bg-white rounded-xl shadow-md p-6 border border-gray-100">
                        <div className="flex items-center gap-2 mb-4">
                            <UserCog className="w-6 h-6 text-blue-600" />
                            <h3 className="text-xl font-bold text-gray-800">Rol Yönetimi</h3>
                        </div>

                        <p className="text-gray-600 mb-4">
                            Bu kullanıcıya ADMIN rolü atayabilir veya kaldırabilirsiniz.
                        </p>

                        {isAdmin ? (
                            <button
                                onClick={handleRemoveRole}
                                disabled={roleLoading}
                                className="flex items-center gap-2 px-4 py-2 bg-red-600 hover:bg-red-700 disabled:bg-red-400 text-white font-semibold rounded-lg transition"
                            >
                                {roleLoading ? (
                                    <>
                                        <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                                        İşleniyor...
                                    </>
                                ) : (
                                    <>
                                        <Shield className="w-4 h-4" />
                                        ADMIN Rolünü Kaldır
                                    </>
                                )}
                            </button>
                        ) : (
                            <button
                                onClick={handleAssignRole}
                                disabled={roleLoading}
                                className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-semibold rounded-lg transition"
                            >
                                {roleLoading ? (
                                    <>
                                        <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                                        İşleniyor...
                                    </>
                                ) : (
                                    <>
                                        <Shield className="w-4 h-4" />
                                        ADMIN Rolü Ata
                                    </>
                                )}
                            </button>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default AdminUserDetailPage;