import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Mail, Lock, LogIn, AlertCircle } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../context/AuthContext';
import api from '../../API/instrumentsApi';

const LoginPage = () => {
    const navigate = useNavigate();
    const { authenticated, login } = useAuth();
    const { t } = useTranslation();

    const [formData, setFormData] = useState({
        username: '',
        password: ''
    });

    const [rememberMe, setRememberMe] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        if (authenticated) {
            navigate('/home', { replace: true });
        }
    }, [authenticated, navigate]);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!formData.username || !formData.password) {
            setError(t('login.requiredFields'));
            return;
        }

        setLoading(true);
        setError('');

        try {
            const loginResponse = await api.post('/auth/login', {
                ...formData,
                rememberMe: rememberMe
            });

            if (loginResponse.data.success) {
                login(loginResponse.data, rememberMe);

                const token = loginResponse.data.accessToken;
                const totpStatus = await api.get('/totp/status', {
                    headers: { Authorization: `Bearer ${token}` }
                });

                if (!totpStatus.data.enabled) {
                    window.location.href = '/setup-2fa';
                } else {
                    window.location.href = '/home';
                }
            } else if (loginResponse.data.message === '2FA_REQUIRED') {
                navigate('/verify-2fa', {
                    state: {
                        keycloakId: loginResponse.data.keycloakId,
                        username: formData.username,
                        password: formData.password,
                        rememberMe: rememberMe
                    }
                });
            } else {
                setError(loginResponse.data.message || t('auth.loginFailed'));
            }

        } catch (err) {
            const errorMsg = err.response?.data?.message || t('login.retryError');
            setError(errorMsg);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 px-4">
            <div className="bg-white rounded-2xl shadow-2xl p-8 w-full max-w-md">

                {/* Header */}
                <div className="text-center mb-8">
                    <div className="w-16 h-16 bg-blue-600 rounded-full flex items-center justify-center mx-auto mb-4">
                        <LogIn className="w-8 h-8 text-white" />
                    </div>
                    <h1 className="text-3xl font-bold text-gray-900 mb-2">{t('login.welcome')}</h1>
                    <p className="text-gray-600">{t('login.subtitle')}</p>
                </div>

                {/* Error Message */}
                {error && (
                    <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
                        <AlertCircle className="w-5 h-5 text-red-600 mt-0.5 flex-shrink-0" />
                        <p className="text-red-800 text-sm">{error}</p>
                    </div>
                )}

                {/* Form */}
                <form onSubmit={handleSubmit} className="space-y-6">

                    {/* Username */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            {t('auth.username')}
                        </label>
                        <div className="relative">
                            <Mail className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
                            <input
                                type="text"
                                name="username"
                                value={formData.username}
                                onChange={handleChange}
                                className="w-full pl-11 pr-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition"
                                placeholder={t('login.usernamePlaceholder')}
                                required
                            />
                        </div>
                    </div>

                    {/* Password */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            {t('auth.password')}
                        </label>
                        <div className="relative">
                            <Lock className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
                            <input
                                type="password"
                                name="password"
                                value={formData.password}
                                onChange={handleChange}
                                className="w-full pl-11 pr-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition"
                                placeholder="••••••••"
                                required
                            />
                        </div>
                    </div>

                    {/* Remember Me & Forgot Password */}
                    <div className="flex items-center justify-between">
                        <div className="flex items-center">
                            <input
                                type="checkbox"
                                id="rememberMe"
                                checked={rememberMe}
                                onChange={(e) => setRememberMe(e.target.checked)}
                                className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                            />
                            <label htmlFor="rememberMe" className="ml-2 text-sm text-gray-700">
                                {t('login.rememberMe')}
                            </label>
                        </div>

                        <Link
                            to="/forgot-password"
                            className="text-sm text-blue-600 hover:text-blue-800 font-medium"
                        >
                            {t('auth.forgotPassword')}
                        </Link>
                    </div>

                    {/* Submit Button */}
                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-semibold py-3 rounded-lg transition flex items-center justify-center gap-2"
                    >
                        {loading ? (
                            <>
                                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                                {t('login.loggingIn')}
                            </>
                        ) : (
                            <>
                                <LogIn className="w-5 h-5" />
                                {t('auth.login')}
                            </>
                        )}
                    </button>
                </form>

                {/* Register Link */}
                <div className="mt-6 text-center">
                    <p className="text-gray-600">
                        {t('auth.noAccount')}{' '}
                        <Link to="/register" className="text-blue-600 hover:text-blue-800 font-semibold">
                            {t('auth.register')}
                        </Link>
                    </p>
                </div>
            </div>
        </div>
    );
};

export default LoginPage;