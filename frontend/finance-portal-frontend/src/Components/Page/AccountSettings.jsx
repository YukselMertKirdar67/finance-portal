import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useTranslation } from 'react-i18next';
import { User, Mail, Lock, Edit2, Loader2, CheckCircle, XCircle } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '../UI/Card';
import { Button } from '../UI/Button';
import { updateUsername, updateEmail, getPasswordLastChanged } from '../../API/userApi';

export default function AccountSettings({ onNavigateToSecurity }) {
    const { user, refreshUser } = useAuth();
    const { t, i18n } = useTranslation();

    const [passwordLastChanged, setPasswordLastChanged] = useState(null);
    const [loading, setLoading] = useState(true);
    const [showUsernameModal, setShowUsernameModal] = useState(false);
    const [showEmailModal, setShowEmailModal] = useState(false);
    const [newUsername, setNewUsername] = useState('');
    const [usernameLoading, setUsernameLoading] = useState(false);
    const [usernameError, setUsernameError] = useState('');
    const [newEmail, setNewEmail] = useState('');
    const [emailPassword, setEmailPassword] = useState('');
    const [emailLoading, setEmailLoading] = useState(false);
    const [emailError, setEmailError] = useState('');
    const [toast, setToast] = useState(null);

    useEffect(() => { fetchData(); }, []);

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
        if (!newUsername.trim()) { setUsernameError(t('account.usernameRequired')); return; }
        if (newUsername.length < 3 || newUsername.length > 20) { setUsernameError(t('account.usernameLength')); return; }
        setUsernameLoading(true);
        setUsernameError('');
        try {
            await updateUsername(newUsername);
            showToast(t('account.usernameUpdated'));
            setShowUsernameModal(false);
            setNewUsername('');
            await refreshUser();
        } catch (error) {
            setUsernameError(error.response?.data?.message || t('account.usernameUpdateError'));
        } finally {
            setUsernameLoading(false);
        }
    };

    const handleEmailUpdate = async () => {
        if (!newEmail.trim()) { setEmailError(t('account.emailRequired')); return; }
        if (!emailPassword.trim()) { setEmailError(t('account.passwordRequired')); return; }
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(newEmail)) { setEmailError(t('register.validation.emailInvalid')); return; }
        setEmailLoading(true);
        setEmailError('');
        try {
            await updateEmail(newEmail, emailPassword);
            showToast(t('account.emailVerificationSent'), 'info');
            setShowEmailModal(false);
            setNewEmail('');
            setEmailPassword('');
        } catch (error) {
            setEmailError(error.response?.data?.message || t('account.emailUpdateError'));
        } finally {
            setEmailLoading(false);
        }
    };

    const formatDate = (dateString) => {
        if (!dateString) return t('account.neverChanged');
        const locale = i18n.language === 'en' ? 'en-US' : 'tr-TR';
        return new Intl.DateTimeFormat(locale, {
            day: '2-digit', month: 'long', year: 'numeric',
            hour: '2-digit', minute: '2-digit'
        }).format(new Date(dateString));
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
                    toast.type === 'success'
                        ? 'bg-green-50 dark:bg-green-900/30 text-green-800 dark:text-green-300 border border-green-200 dark:border-green-800'
                        : toast.type === 'error'
                            ? 'bg-red-50 dark:bg-red-900/30 text-red-800 dark:text-red-300 border border-red-200 dark:border-red-800'
                            : 'bg-blue-50 dark:bg-blue-900/30 text-blue-800 dark:text-blue-300 border border-blue-200 dark:border-blue-800'
                }`}>
                    {toast.type === 'success' && <CheckCircle className="w-5 h-5" />}
                    {toast.type === 'error' && <XCircle className="w-5 h-5" />}
                    <span>{toast.message}</span>
                </div>
            )}

            {/* Profil Bilgileri */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('account.profileInfo')}</CardTitle>
                </CardHeader>
                <CardContent className="space-y-6">

                    {/* Kullanıcı Adı */}
                    <div className="flex items-center justify-between pb-4 border-b border-gray-200 dark:border-gray-700">
                        <div className="flex items-center gap-3">
                            <div className="w-12 h-12 bg-blue-100 dark:bg-blue-900/30 rounded-full flex items-center justify-center">
                                <User className="w-6 h-6 text-blue-600 dark:text-blue-400" />
                            </div>
                            <div>
                                <p className="text-sm text-gray-500 dark:text-gray-400">{t('auth.username')}</p>
                                <p className="font-semibold text-gray-900 dark:text-white">{user?.username}</p>
                            </div>
                        </div>
                        <Button
                            variant="outline" size="sm"
                            onClick={() => { setNewUsername(user?.username || ''); setShowUsernameModal(true); }}
                            className="dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
                        >
                            <Edit2 className="w-4 h-4 mr-2" />{t('common.edit')}
                        </Button>
                    </div>

                    {/* E-posta */}
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <div className="w-12 h-12 bg-purple-100 dark:bg-purple-900/30 rounded-full flex items-center justify-center">
                                <Mail className="w-6 h-6 text-purple-600 dark:text-purple-400" />
                            </div>
                            <div>
                                <p className="text-sm text-gray-500 dark:text-gray-400">{t('auth.email')}</p>
                                <p className="font-semibold text-gray-900 dark:text-white">{user?.email}</p>
                            </div>
                        </div>
                        <Button
                            variant="outline" size="sm"
                            onClick={() => setShowEmailModal(true)}
                            className="dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
                        >
                            <Edit2 className="w-4 h-4 mr-2" />{t('common.edit')}
                        </Button>
                    </div>
                </CardContent>
            </Card>

            {/* Şifre Yönetimi */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('account.passwordManagement')}</CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <div className="w-12 h-12 bg-orange-100 dark:bg-orange-900/30 rounded-full flex items-center justify-center">
                                <Lock className="w-6 h-6 text-orange-600 dark:text-orange-400" />
                            </div>
                            <div>
                                <p className="text-sm text-gray-500 dark:text-gray-400">{t('account.lastChanged')}</p>
                                <p className="font-semibold text-gray-900 dark:text-white">{formatDate(passwordLastChanged)}</p>
                            </div>
                        </div>
                        <Button onClick={onNavigateToSecurity}>{t('profile.changePassword')}</Button>
                    </div>
                </CardContent>
            </Card>

            {/* Username Modal */}
            {showUsernameModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
                    <div className="bg-white dark:bg-gray-800 rounded-xl shadow-xl max-w-md w-full p-6">
                        <h3 className="text-xl font-bold text-gray-900 dark:text-white mb-4">{t('account.changeUsername')}</h3>
                        <div className="mb-4">
                            <p className="text-sm text-gray-500 dark:text-gray-400 mb-2">
                                {t('account.current')}: <span className="font-semibold text-gray-900 dark:text-white">{user?.username}</span>
                            </p>
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                                {t('account.newUsername')}
                            </label>
                            <input
                                type="text"
                                value={newUsername}
                                onChange={(e) => setNewUsername(e.target.value)}
                                className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                                placeholder={t('account.usernamePlaceholder')}
                            />
                            {usernameError && <p className="text-sm text-red-600 dark:text-red-400 mt-2">{usernameError}</p>}
                            <p className="text-xs text-gray-500 dark:text-gray-400 mt-2">ⓘ {t('account.usernameHint')}</p>
                        </div>
                        <div className="flex gap-3">
                            <Button
                                variant="outline" className="flex-1 dark:border-gray-600 dark:text-gray-300"
                                onClick={() => { setShowUsernameModal(false); setNewUsername(''); setUsernameError(''); }}
                                disabled={usernameLoading}
                            >
                                {t('common.cancel')}
                            </Button>
                            <Button className="flex-1" onClick={handleUsernameUpdate} disabled={usernameLoading}>
                                {usernameLoading
                                    ? <><Loader2 className="w-4 h-4 mr-2 animate-spin" />{t('common.saving')}</>
                                    : t('common.save')}
                            </Button>
                        </div>
                    </div>
                </div>
            )}

            {/* Email Modal */}
            {showEmailModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
                    <div className="bg-white dark:bg-gray-800 rounded-xl shadow-xl max-w-md w-full p-6">
                        <h3 className="text-xl font-bold text-gray-900 dark:text-white mb-4">{t('account.changeEmail')}</h3>
                        <div className="mb-4">
                            <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
                                {t('account.current')}: <span className="font-semibold text-gray-900 dark:text-white">{user?.email}</span>
                            </p>
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                                {t('account.newEmail')}
                            </label>
                            <input
                                type="email"
                                value={newEmail}
                                onChange={(e) => setNewEmail(e.target.value)}
                                className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 mb-4"
                                placeholder="yeni@email.com"
                            />
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                                {t('account.passwordConfirm')}
                            </label>
                            <input
                                type="password"
                                value={emailPassword}
                                onChange={(e) => setEmailPassword(e.target.value)}
                                className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                                placeholder="••••••••"
                            />
                            {emailError && <p className="text-sm text-red-600 dark:text-red-400 mt-2">{emailError}</p>}
                            <div className="mt-4 p-3 bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg">
                                <p className="text-xs text-yellow-800 dark:text-yellow-300">⚠️ {t('account.emailVerificationNote')}</p>
                            </div>
                        </div>
                        <div className="flex gap-3">
                            <Button
                                variant="outline" className="flex-1 dark:border-gray-600 dark:text-gray-300"
                                onClick={() => { setShowEmailModal(false); setNewEmail(''); setEmailPassword(''); setEmailError(''); }}
                                disabled={emailLoading}
                            >
                                {t('common.cancel')}
                            </Button>
                            <Button className="flex-1" onClick={handleEmailUpdate} disabled={emailLoading}>
                                {emailLoading
                                    ? <><Loader2 className="w-4 h-4 mr-2 animate-spin" />{t('common.saving')}</>
                                    : t('common.save')}
                            </Button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}