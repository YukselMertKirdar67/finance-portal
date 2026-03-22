import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { User, Mail, Lock, Edit2, Loader2, CheckCircle, XCircle } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '../UI/Card';
import { Button } from '../UI/Button';
import { updateUsername, updateEmail, getPasswordLastChanged } from '../../API/userApi';


export default function AccountSettings() {
    const { user, refreshUser } = useAuth();
    const [passwordLastChanged, setPasswordLastChanged] = useState(null);
    const [loading, setLoading] = useState(true);

    // Modals
    const [showUsernameModal, setShowUsernameModal] = useState(false);
    const [showEmailModal, setShowEmailModal] = useState(false);

    // Username modal states
    const [newUsername, setNewUsername] = useState('');
    const [usernameLoading, setUsernameLoading] = useState(false);
    const [usernameError, setUsernameError] = useState('');

    // Email modal states
    const [newEmail, setNewEmail] = useState('');
    const [emailPassword, setEmailPassword] = useState('');
    const [emailLoading, setEmailLoading] = useState(false);
    const [emailError, setEmailError] = useState('');

    // Toast
    const [toast, setToast] = useState(null);

    useEffect(() => {
        fetchData();
    }, []);

    const fetchData = async () => {
        try {
            await refreshUser();

            const passwordData = await getPasswordLastChanged();
            setPasswordLastChanged(passwordData.lastChanged);
        } catch (error) {
            console.error('Error fetching data:', error);
        } finally {
            setLoading(false);
        }
    };

    const showToast = (message, type = 'success') => {
        setToast({ message, type });
        setTimeout(() => setToast(null), 5000);
    };

    const handleUsernameUpdate = async () => {
        if (!newUsername.trim()) {
            setUsernameError('Kullanıcı adı boş olamaz');
            return;
        }

        if (newUsername.length < 3 || newUsername.length > 20) {
            setUsernameError('Kullanıcı adı 3-20 karakter arasında olmalıdır');
            return;
        }

        setUsernameLoading(true);
        setUsernameError('');

        try {
            await updateUsername(newUsername);
            showToast('Kullanıcı adınız başarıyla güncellendi');
            setShowUsernameModal(false);
            setNewUsername('');
            await refreshUser();
        } catch (error) {
            setUsernameError(error.response?.data?.message || 'Kullanıcı adı güncellenemedi');
        } finally {
            setUsernameLoading(false);
        }
    };

    const handleEmailUpdate = async () => {
        if (!newEmail.trim()) {
            setEmailError('E-posta adresi boş olamaz');
            return;
        }

        if (!emailPassword.trim()) {
            setEmailError('Şifre gereklidir');
            return;
        }

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(newEmail)) {
            setEmailError('Geçerli bir e-posta adresi giriniz');
            return;
        }

        setEmailLoading(true);
        setEmailError('');

        try {
            await updateEmail(newEmail, emailPassword);
            showToast('Doğrulama e-postası gönderildi. Lütfen e-postanızı kontrol edin.', 'info');
            setShowEmailModal(false);
            setNewEmail('');
            setEmailPassword('');
        } catch (error) {
            setEmailError(error.response?.data?.message || 'E-posta güncellenemedi');
        } finally {
            setEmailLoading(false);
        }
    };

    const formatDate = (dateString) => {
        if (!dateString) return 'Henüz değiştirilmedi';

        const date = new Date(dateString);
        return new Intl.DateTimeFormat('tr-TR', {
            day: '2-digit',
            month: 'long',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        }).format(date);
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center h-64">
                <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
            </div>
        );
    }

    return (
        <div className="space-y-6">

            {/* Toast */}
            {toast && (
                <div className={`fixed top-6 right-6 z-50 flex items-center gap-3 px-6 py-4 rounded-lg shadow-lg ${
                    toast.type === 'success' ? 'bg-green-50 text-green-800 border border-green-200' :
                        toast.type === 'error' ? 'bg-red-50 text-red-800 border border-red-200' :
                            'bg-blue-50 text-blue-800 border border-blue-200'
                }`}>
                    {toast.type === 'success' && <CheckCircle className="w-5 h-5" />}
                    {toast.type === 'error' && <XCircle className="w-5 h-5" />}
                    <span>{toast.message}</span>
                </div>
            )}

            {/* Profil Bilgileri */}
            <Card>
                <CardHeader>
                    <CardTitle>Profil Bilgileri</CardTitle>
                </CardHeader>
                <CardContent className="space-y-6">

                    {/* Kullanıcı Adı */}
                    <div className="flex items-center justify-between pb-4 border-b">
                        <div className="flex items-center gap-3">
                            <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center">
                                <User className="w-6 h-6 text-blue-600" />
                            </div>
                            <div>
                                <p className="text-sm text-gray-500">Kullanıcı Adı</p>
                                <p className="font-semibold text-gray-900">{user?.username}</p>
                            </div>
                        </div>
                        <Button
                            variant="outline"
                            size="sm"
                            onClick={() => {
                                setNewUsername(user?.username || '');
                                setShowUsernameModal(true);
                            }}
                        >
                            <Edit2 className="w-4 h-4 mr-2" />
                            Düzenle
                        </Button>
                    </div>

                    {/* E-posta */}
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <div className="w-12 h-12 bg-purple-100 rounded-full flex items-center justify-center">
                                <Mail className="w-6 h-6 text-purple-600" />
                            </div>
                            <div>
                                <p className="text-sm text-gray-500">E-posta Adresi</p>
                                <p className="font-semibold text-gray-900">{user?.email}</p>
                            </div>
                        </div>
                        <Button
                            variant="outline"
                            size="sm"
                            onClick={() => setShowEmailModal(true)}
                        >
                            <Edit2 className="w-4 h-4 mr-2" />
                            Düzenle
                        </Button>
                    </div>
                </CardContent>
            </Card>

            {/* Şifre Yönetimi */}
            <Card>
                <CardHeader>
                    <CardTitle>Şifre Yönetimi</CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <div className="w-12 h-12 bg-orange-100 rounded-full flex items-center justify-center">
                                <Lock className="w-6 h-6 text-orange-600" />
                            </div>
                            <div>
                                <p className="text-sm text-gray-500">Son Değiştirilme</p>
                                <p className="font-semibold text-gray-900">{formatDate(passwordLastChanged)}</p>
                            </div>
                        </div>
                        <Button
                            onClick={() => {
                                // UserProfilePage'deki şifre değiştir modal'ını tetikle
                                // Alternatif: navigate('/profile') veya modal state'i global yap
                                window.alert('Şifre değiştirme özelliği için Profil sayfasına gidin');
                            }}
                        >
                            Şifre Değiştir
                        </Button>
                    </div>
                </CardContent>
            </Card>

            {/* Username Modal */}
            {showUsernameModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-xl shadow-xl max-w-md w-full p-6">
                        <h3 className="text-xl font-bold text-gray-900 mb-4">Kullanıcı Adı Değiştir</h3>

                        <div className="mb-4">
                            <p className="text-sm text-gray-500 mb-2">Mevcut: <span className="font-semibold">{user?.username}</span></p>

                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                Yeni Kullanıcı Adı
                            </label>
                            <input
                                type="text"
                                value={newUsername}
                                onChange={(e) => setNewUsername(e.target.value)}
                                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                placeholder="yeni_kullanici_adi"
                            />

                            {usernameError && (
                                <p className="text-sm text-red-600 mt-2">{usernameError}</p>
                            )}

                            <p className="text-xs text-gray-500 mt-2">ⓘ 3-20 karakter arası</p>
                        </div>

                        <div className="flex gap-3">
                            <Button
                                variant="outline"
                                className="flex-1"
                                onClick={() => {
                                    setShowUsernameModal(false);
                                    setNewUsername('');
                                    setUsernameError('');
                                }}
                                disabled={usernameLoading}
                            >
                                İptal
                            </Button>
                            <Button
                                className="flex-1"
                                onClick={handleUsernameUpdate}
                                disabled={usernameLoading}
                            >
                                {usernameLoading ? (
                                    <>
                                        <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                                        Kaydediliyor...
                                    </>
                                ) : (
                                    'Kaydet'
                                )}
                            </Button>
                        </div>
                    </div>
                </div>
            )}

            {/* Email Modal */}
            {showEmailModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-xl shadow-xl max-w-md w-full p-6">
                        <h3 className="text-xl font-bold text-gray-900 mb-4">E-posta Adresi Değiştir</h3>

                        <div className="mb-4">
                            <p className="text-sm text-gray-500 mb-4">Mevcut: <span className="font-semibold">{user?.email}</span></p>

                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                Yeni E-posta
                            </label>
                            <input
                                type="email"
                                value={newEmail}
                                onChange={(e) => setNewEmail(e.target.value)}
                                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 mb-4"
                                placeholder="yeni@email.com"
                            />

                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                Şifreniz (onay için)
                            </label>
                            <input
                                type="password"
                                value={emailPassword}
                                onChange={(e) => setEmailPassword(e.target.value)}
                                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                placeholder="••••••••"
                            />

                            {emailError && (
                                <p className="text-sm text-red-600 mt-2">{emailError}</p>
                            )}

                            <div className="mt-4 p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
                                <p className="text-xs text-yellow-800">⚠️ Doğrulama e-postası gönderilecek</p>
                            </div>
                        </div>

                        <div className="flex gap-3">
                            <Button
                                variant="outline"
                                className="flex-1"
                                onClick={() => {
                                    setShowEmailModal(false);
                                    setNewEmail('');
                                    setEmailPassword('');
                                    setEmailError('');
                                }}
                                disabled={emailLoading}
                            >
                                İptal
                            </Button>
                            <Button
                                className="flex-1"
                                onClick={handleEmailUpdate}
                                disabled={emailLoading}
                            >
                                {emailLoading ? (
                                    <>
                                        <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                                        Kaydediliyor...
                                    </>
                                ) : (
                                    'Kaydet'
                                )}
                            </Button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}