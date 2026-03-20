import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Mail, Lock, LogIn, AlertCircle } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import api from '../../API/instrumentsApi';

const LoginPage = () => {
    const navigate = useNavigate();
    const { authenticated, login } = useAuth();

    const [formData, setFormData] = useState({
        username: '',
        password: ''
    });

    const [rememberMe, setRememberMe] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    // Zaten login ise home'a yönlendir
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
            setError('Kullanıcı adı ve şifre gerekli');
            return;
        }

        setLoading(true);
        setError('');

        try {
            //  Pre-auth: Credentials check + OTP kontrolü
            const preAuthResponse = await api.post('/auth/pre-auth', {
                username: formData.username,
                password: formData.password,
                rememberMe: rememberMe
            });

            if (preAuthResponse.data.success && !preAuthResponse.data.requiresOTP) {
                // OTP yok, direkt login
                const loginResponse = await api.post('/auth/login', {
                    ...formData,
                    rememberMe: rememberMe
                });

                if (loginResponse.data.success) {
                    //  AuthContext'e rememberMe parametresi ile token kaydet
                    login(loginResponse.data, rememberMe);
                    window.location.href = '/home';
                }

            } else if (preAuthResponse.data.requiresOTP) {
                //  OTP gerekli - Keycloak popup aç
                openKeycloakOTPPopup(preAuthResponse.data.keycloakAuthUrl);

            } else {
                setError(preAuthResponse.data.message || 'Giriş başarısız');
            }

        } catch (err) {
            const errorMsg = err.response?.data?.message || 'Giriş başarısız. Lütfen tekrar deneyin.';
            setError(errorMsg);
        } finally {
            setLoading(false);
        }
    };

    // Keycloak OTP Popup
    const openKeycloakOTPPopup = (authUrl) => {
        const width = 500;
        const height = 600;
        const left = (window.screen.width / 2) - (width / 2);
        const top = (window.screen.height / 2) - (height / 2);

        const popup = window.open(
            authUrl,
            'Keycloak 2FA',
            `width=${width},height=${height},left=${left},top=${top},toolbar=no,menubar=no,scrollbars=yes`
        );

        // Popup'tan mesaj dinle (postMessage)
        const messageHandler = (event) => {
            // Güvenlik: Origin kontrolü
            if (event.origin !== window.location.origin) return;

            if (event.data.type === 'KEYCLOAK_AUTH_SUCCESS') {
                // Token alındı!
                const { accessToken, refreshToken, username, email, roles } = event.data;

                //  AuthContext'e rememberMe ile kaydet
                login(
                    { accessToken, refreshToken, username, email, roles },
                    rememberMe
                );

                // Popup'ı kapat
                if (popup && !popup.closed) popup.close();

                // Home'a yönlendir
                navigate('/home');

                // Event listener'ı temizle
                window.removeEventListener('message', messageHandler);
            }
        };

        window.addEventListener('message', messageHandler);

        // Popup kapandığında cleanup
        const checkPopupClosed = setInterval(() => {
            if (popup && popup.closed) {
                clearInterval(checkPopupClosed);
                window.removeEventListener('message', messageHandler);
                setLoading(false);
            }
        }, 500);
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 px-4">
            <div className="bg-white rounded-2xl shadow-2xl p-8 w-full max-w-md">

                {/* Header */}
                <div className="text-center mb-8">
                    <div className="w-16 h-16 bg-blue-600 rounded-full flex items-center justify-center mx-auto mb-4">
                        <LogIn className="w-8 h-8 text-white" />
                    </div>
                    <h1 className="text-3xl font-bold text-gray-900 mb-2">Hoş Geldiniz</h1>
                    <p className="text-gray-600">Hesabınıza giriş yapın</p>
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
                            Kullanıcı Adı
                        </label>
                        <div className="relative">
                            <Mail className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
                            <input
                                type="text"
                                name="username"
                                value={formData.username}
                                onChange={handleChange}
                                className="w-full pl-11 pr-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition"
                                placeholder="Kullanıcı adınız"
                                required
                            />
                        </div>
                    </div>

                    {/* Password */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            Şifre
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
                                Beni Hatırla (30 gün)
                            </label>
                        </div>

                        <Link
                            to="/forgot-password"
                            className="text-sm text-blue-600 hover:text-blue-800 font-medium"
                        >
                            Şifremi Unuttum?
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
                                Giriş Yapılıyor...
                            </>
                        ) : (
                            <>
                                <LogIn className="w-5 h-5" />
                                Giriş Yap
                            </>
                        )}
                    </button>
                </form>

                {/* Register Link */}
                <div className="mt-6 text-center">
                    <p className="text-gray-600">
                        Hesabınız yok mu?{' '}
                        <Link to="/register" className="text-blue-600 hover:text-blue-800 font-semibold">
                            Kayıt Olun
                        </Link>
                    </p>
                </div>
            </div>
        </div>
    );
};

export default LoginPage;