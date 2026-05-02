import React, { useState } from 'react';
import { Lock, Shield, CheckCircle, AlertCircle, Loader2 } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '../UI/Card';
import { Button } from '../UI/Button';
import { changePassword } from '../../API/userApi';

export default function SecuritySettings() {
    const [passwordForm, setPasswordForm] = useState({
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
    });
    const [passwordErrors, setPasswordErrors] = useState({});
    const [passwordLoading, setPasswordLoading] = useState(false);
    const [showOTPInput, setShowOTPInput] = useState(false);
    const [otpCode, setOtpCode] = useState('');
    const [toast, setToast] = useState(null);

    const showToast = (message, type = 'success') => {
        setToast({ message, type });
        setTimeout(() => setToast(null), 4000);
    };

    const handlePasswordChange = (e) => {
        const { name, value } = e.target;
        setPasswordForm(prev => ({ ...prev, [name]: value }));
        if (passwordErrors[name]) {
            setPasswordErrors(prev => ({ ...prev, [name]: '' }));
        }
    };

    const validatePasswordForm = () => {
        const errors = {};

        if (!passwordForm.currentPassword) errors.currentPassword = 'Mevcut şifre gerekli';
        if (!passwordForm.newPassword) {
            errors.newPassword = 'Yeni şifre gerekli';
        } else if (passwordForm.newPassword.length < 6) {
            errors.newPassword = 'Yeni şifre en az 6 karakter olmalı';
        }
        if (passwordForm.newPassword !== passwordForm.confirmPassword) {
            errors.confirmPassword = 'Şifreler eşleşmiyor';
        }
        if (showOTPInput && (!otpCode || otpCode.length !== 6)) {
            errors.otpCode = '6 haneli OTP kodu gerekli';
        }

        setPasswordErrors(errors);
        return Object.keys(errors).length === 0;
    };

    const handlePasswordSubmit = async (e) => {
        e.preventDefault();
        if (!validatePasswordForm()) return;

        setPasswordLoading(true);
        setPasswordErrors({});

        try {
            const response = await changePassword({
                currentPassword: passwordForm.currentPassword,
                newPassword: passwordForm.newPassword,
                confirmPassword: passwordForm.confirmPassword,
                otpCode: showOTPInput ? otpCode : null
            });

            if (response.success) {
                setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
                setOtpCode('');
                setShowOTPInput(false);
                showToast('Şifreniz başarıyla değiştirildi');
            } else if (response.message === 'OTP_REQUIRED') {
                setShowOTPInput(true);
            } else {
                setPasswordErrors({ submit: response.message || 'Şifre değiştirme başarısız' });
            }
        } catch (error) {
            const errorMsg = error.response?.data?.message || 'Şifre değiştirme başarısız';
            if (errorMsg === 'OTP_REQUIRED') {
                setShowOTPInput(true);
            } else {
                setPasswordErrors({ submit: errorMsg });
            }
        } finally {
            setPasswordLoading(false);
        }
    };

    return (
        <div className="space-y-6">

            {/* Toast */}
            {toast && (
                <div className={`fixed top-6 right-6 z-50 flex items-center gap-3 px-6 py-4 rounded-lg shadow-lg ${
                    toast.type === 'success'
                        ? 'bg-green-50 text-green-800 border border-green-200'
                        : 'bg-red-50 text-red-800 border border-red-200'
                }`}>
                    {toast.type === 'success' ? <CheckCircle className="w-5 h-5" /> : <AlertCircle className="w-5 h-5" />}
                    <span>{toast.message}</span>
                </div>
            )}

            {/* Şifre Değiştirme */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Lock className="w-5 h-5 text-blue-600" />
                        Şifre Değiştir
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    {passwordErrors.submit && (
                        <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
                            <AlertCircle className="w-5 h-5 text-red-600 mt-0.5 flex-shrink-0" />
                            <p className="text-red-800">{passwordErrors.submit}</p>
                        </div>
                    )}

                    <form onSubmit={handlePasswordSubmit} className="space-y-4">
                        {/* Mevcut Şifre */}
                        <div>
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                                Mevcut Şifre
                            </label>
                            <div className="relative">
                                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                                <input
                                    type="password"
                                    name="currentPassword"
                                    value={passwordForm.currentPassword}
                                    onChange={handlePasswordChange}
                                    disabled={showOTPInput}
                                    className={`w-full pl-10 pr-4 py-2 border rounded-lg focus:outline-none focus:ring-2 transition disabled:bg-gray-100 dark:disabled:bg-gray-700 dark:bg-gray-800 dark:text-white ${
                                        passwordErrors.currentPassword
                                            ? 'border-red-300 focus:ring-red-500'
                                            : 'border-gray-300 dark:border-gray-600 focus:ring-blue-500'
                                    }`}
                                    placeholder="••••••••"
                                />
                            </div>
                            {passwordErrors.currentPassword && (
                                <p className="text-red-600 text-sm mt-1">{passwordErrors.currentPassword}</p>
                            )}
                        </div>

                        {/* Yeni Şifre */}
                        <div>
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                                Yeni Şifre
                            </label>
                            <div className="relative">
                                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                                <input
                                    type="password"
                                    name="newPassword"
                                    value={passwordForm.newPassword}
                                    onChange={handlePasswordChange}
                                    disabled={showOTPInput}
                                    className={`w-full pl-10 pr-4 py-2 border rounded-lg focus:outline-none focus:ring-2 transition disabled:bg-gray-100 dark:disabled:bg-gray-700 dark:bg-gray-800 dark:text-white ${
                                        passwordErrors.newPassword
                                            ? 'border-red-300 focus:ring-red-500'
                                            : 'border-gray-300 dark:border-gray-600 focus:ring-blue-500'
                                    }`}
                                    placeholder="••••••••"
                                />
                            </div>
                            {passwordErrors.newPassword && (
                                <p className="text-red-600 text-sm mt-1">{passwordErrors.newPassword}</p>
                            )}
                        </div>

                        {/* Yeni Şifre Tekrar */}
                        <div>
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                                Yeni Şifre Tekrar
                            </label>
                            <div className="relative">
                                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                                <input
                                    type="password"
                                    name="confirmPassword"
                                    value={passwordForm.confirmPassword}
                                    onChange={handlePasswordChange}
                                    disabled={showOTPInput}
                                    className={`w-full pl-10 pr-4 py-2 border rounded-lg focus:outline-none focus:ring-2 transition disabled:bg-gray-100 dark:disabled:bg-gray-700 dark:bg-gray-800 dark:text-white ${
                                        passwordErrors.confirmPassword
                                            ? 'border-red-300 focus:ring-red-500'
                                            : 'border-gray-300 dark:border-gray-600 focus:ring-blue-500'
                                    }`}
                                    placeholder="••••••••"
                                />
                            </div>
                            {passwordErrors.confirmPassword && (
                                <p className="text-red-600 text-sm mt-1">{passwordErrors.confirmPassword}</p>
                            )}
                        </div>

                        {/* OTP Input */}
                        {showOTPInput && (
                            <div className="border-t dark:border-gray-700 pt-4">
                                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                                    2FA Kodu
                                </label>
                                <input
                                    type="text"
                                    value={otpCode}
                                    onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                                    className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 text-center text-2xl tracking-widest dark:bg-gray-800 dark:text-white"
                                    placeholder="000000"
                                    maxLength={6}
                                    autoFocus
                                />
                                <p className="text-sm text-gray-500 mt-2 text-center">
                                    Authenticator uygulamanızdan 6 haneli kodu girin
                                </p>
                                {passwordErrors.otpCode && (
                                    <p className="text-red-600 text-sm mt-1 text-center">{passwordErrors.otpCode}</p>
                                )}
                            </div>
                        )}

                        <div className="flex justify-end pt-2">
                            <Button type="submit" disabled={passwordLoading}>
                                {passwordLoading ? (
                                    <><Loader2 className="w-4 h-4 mr-2 animate-spin" />
                                        {showOTPInput ? 'Doğrulanıyor...' : 'Değiştiriliyor...'}
                                    </>
                                ) : (
                                    <><Lock className="w-4 h-4 mr-2" />
                                        {showOTPInput ? '2FA Doğrula' : 'Şifreyi Değiştir'}
                                    </>
                                )}
                            </Button>
                        </div>
                    </form>
                </CardContent>
            </Card>

            {/* Güvenlik Notu */}
            <div className="p-4 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg">
                <div className="flex items-start gap-3">
                    <Shield className="w-5 h-5 text-blue-600 dark:text-blue-400 flex-shrink-0 mt-0.5" />
                    <p className="text-sm text-blue-800 dark:text-blue-300">
                        Hesabınızı korumak için güçlü bir şifre kullanın ve şifrenizi düzenli olarak değiştirin.
                        2FA aktifse şifre değişikliği için authenticator kodunuz istenecektir.
                    </p>
                </div>
            </div>
        </div>
    );
}