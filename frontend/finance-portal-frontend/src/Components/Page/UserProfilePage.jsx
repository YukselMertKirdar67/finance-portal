import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import {
    User,
    Mail,
    Calendar,
    Shield,
    CheckCircle,
    AlertCircle,
    Send,
} from 'lucide-react';
import api from '../../API/instrumentsApi';
import { getCurrentUser } from '../../API/userApi';

const UserProfilePage = () => {
    const { user } = useAuth();

    const [profileData, setProfileData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [sendingEmail, setSendingEmail] = useState(false);
    const [emailSent, setEmailSent] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        fetchProfile();
    }, []);

    const fetchProfile = async () => {
        setLoading(true);
        setError('');
        try {
            const data = await getCurrentUser();
            setProfileData(data);
        } catch (err) {
            setError('Profil bilgileri yüklenirken bir hata oluştu');
            console.error('Error fetching profile:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleResendVerificationEmail = async () => {
        if (!profileData?.email) return;
        setSendingEmail(true);
        setEmailSent(false);
        setError('');
        try {
            await api.post('/auth/send-verification-email', { email: profileData.email });
            setEmailSent(true);
            setTimeout(() => setEmailSent(false), 5000);
        } catch (err) {
            setError('Email gönderilemedi. Lütfen tekrar deneyin.');
            console.error('Error sending verification email:', err);
        } finally {
            setSendingEmail(false);
        }
    };

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="text-center">
                    <div className="w-16 h-16 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
                    <p className="text-gray-600">Profil yükleniyor...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="p-6 bg-gray-50 min-h-screen">

            {/* Header */}
            <div className="mb-6">
                <h1 className="text-3xl font-bold text-gray-800 mb-2">Profilim</h1>
                <p className="text-gray-600">Hesap bilgilerinizi görüntüleyin</p>
            </div>

            {/* Error Message */}
            {error && (
                <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
                    <AlertCircle className="w-5 h-5 text-red-600 mt-0.5 flex-shrink-0" />
                    <p className="text-red-800">{error}</p>
                </div>
            )}

            {/* Success Message */}
            {emailSent && (
                <div className="mb-6 p-4 bg-green-50 border border-green-200 rounded-lg flex items-start gap-3">
                    <CheckCircle className="w-5 h-5 text-green-600 mt-0.5 flex-shrink-0" />
                    <div>
                        <p className="text-green-800 font-medium">Doğrulama emaili gönderildi!</p>
                        <p className="text-green-600 text-sm mt-1">Email kutunuzu kontrol edin.</p>
                    </div>
                </div>
            )}

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

                {/* Profile Card */}
                <div className="lg:col-span-1">
                    <div className="bg-white rounded-xl shadow-md p-6 border border-gray-100">
                        <div className="text-center mb-6">
                            <div className="w-24 h-24 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-full mx-auto flex items-center justify-center text-white text-3xl font-bold shadow-lg">
                                {user?.username?.substring(0, 2).toUpperCase()}
                            </div>
                        </div>
                        <div className="text-center mb-6">
                            <h2 className="text-2xl font-bold text-gray-900 mb-1">
                                {profileData?.username}
                            </h2>
                            <p className="text-gray-500 text-sm">@{profileData?.username}</p>
                        </div>
                        <div className="space-y-2">
                            <p className="text-sm font-medium text-gray-600 mb-2">Roller:</p>
                            <div className="flex flex-wrap gap-2">
                                {profileData?.roles?.map((role) => (
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
                </div>

                {/* Details Card */}
                <div className="lg:col-span-2">
                    <div className="bg-white rounded-xl shadow-md p-6 border border-gray-100">
                        <h3 className="text-xl font-bold text-gray-800 mb-6">Hesap Bilgileri</h3>

                        <div className="space-y-6">
                            <div className="flex items-start gap-4 pb-4 border-b border-gray-100">
                                <div className="bg-blue-100 text-blue-600 p-3 rounded-lg">
                                    <User className="w-5 h-5" />
                                </div>
                                <div className="flex-1">
                                    <p className="text-sm text-gray-500 mb-1">Kullanıcı Adı</p>
                                    <p className="text-gray-900 font-semibold">{profileData?.username}</p>
                                </div>
                            </div>

                            <div className="flex items-start gap-4 pb-4 border-b border-gray-100">
                                <div className="bg-green-100 text-green-600 p-3 rounded-lg">
                                    <Mail className="w-5 h-5" />
                                </div>
                                <div className="flex-1">
                                    <p className="text-sm text-gray-500 mb-1">Email</p>
                                    <p className="text-gray-900 font-semibold">{profileData?.email}</p>
                                    <div className="mt-2">
                                        {profileData?.emailVerified ? (
                                            <span className="inline-flex items-center gap-1 text-sm text-green-600">
                                                <CheckCircle className="w-4 h-4" />
                                                Email doğrulandı
                                            </span>
                                        ) : (
                                            <div className="space-y-2">
                                                <span className="inline-flex items-center gap-1 text-sm text-orange-600">
                                                    <AlertCircle className="w-4 h-4" />
                                                    Email doğrulanmadı
                                                </span>
                                                <button
                                                    onClick={handleResendVerificationEmail}
                                                    disabled={sendingEmail}
                                                    className="flex items-center gap-2 text-sm text-blue-600 hover:text-blue-700 font-medium disabled:opacity-50"
                                                >
                                                    {sendingEmail ? (
                                                        <>
                                                            <div className="w-4 h-4 border-2 border-blue-600 border-t-transparent rounded-full animate-spin" />
                                                            Gönderiliyor...
                                                        </>
                                                    ) : (
                                                        <>
                                                            <Send className="w-4 h-4" />
                                                            Doğrulama emaili gönder
                                                        </>
                                                    )}
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </div>

                            <div className="flex items-start gap-4 pb-4 border-b border-gray-100">
                                <div className="bg-purple-100 text-purple-600 p-3 rounded-lg">
                                    <Shield className="w-5 h-5" />
                                </div>
                                <div className="flex-1">
                                    <p className="text-sm text-gray-500 mb-1">Hesap Durumu</p>
                                    <span className="inline-flex items-center gap-1 px-3 py-1 bg-green-100 text-green-700 rounded-full text-sm font-medium">
                                        <CheckCircle className="w-4 h-4" />
                                        Aktif
                                    </span>
                                </div>
                            </div>

                            <div className="flex items-start gap-4">
                                <div className="bg-indigo-100 text-indigo-600 p-3 rounded-lg">
                                    <Calendar className="w-5 h-5" />
                                </div>
                                <div className="flex-1">
                                    <p className="text-sm text-gray-500 mb-1">Kayıt Tarihi</p>
                                    <p className="text-gray-900 font-semibold">
                                        {profileData?.createdAt ? new Date(profileData.createdAt).toLocaleDateString('tr-TR', {
                                            year: 'numeric',
                                            month: 'long',
                                            day: 'numeric'
                                        }) : 'Bilinmiyor'}
                                    </p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default UserProfilePage;