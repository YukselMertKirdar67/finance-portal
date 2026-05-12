import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Shield, Loader2, XCircle, ArrowLeft } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { verifyTotpLogin } from '../../API/totpApi';
import api from '../../API/instrumentsApi';

const Verify2FAPage = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const { login } = useAuth();

    const { keycloakId, username, password, rememberMe } = location.state || {};

    const [code, setCode] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    // State yoksa login'e yönlendir
    if (!keycloakId) {
        navigate('/login');
        return null;
    }

    const handleVerify = async () => {
        if (code.length !== 6) {
            setError('Lütfen 6 haneli kodu girin');
            return;
        }

        setLoading(true);
        setError('');

        try {
            // 1. TOTP kodunu doğrula
            const totpResponse = await verifyTotpLogin(keycloakId, code);

            if (!totpResponse.success) {
                setError('Geçersiz kod. Lütfen tekrar deneyin.');
                return;
            }

            // 2. Kod doğrulandı, login yap
            const loginResponse = await api.post('/auth/login', {
                username,
                password,
                rememberMe,
                totpVerified: true
            });

            if (loginResponse.data.success) {
                login(loginResponse.data, rememberMe);
                window.location.href = '/home';
            } else {
                setError('Giriş başarısız. Lütfen tekrar deneyin.');
            }

        } catch {
            setError('Doğrulama başarısız. Lütfen tekrar deneyin.');
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
                        <Shield className="w-8 h-8 text-white" />
                    </div>
                    <h1 className="text-2xl font-bold text-gray-900 mb-2">
                        İki Faktörlü Doğrulama
                    </h1>
                    <p className="text-gray-500 text-sm">
                        Google Authenticator uygulamasındaki 6 haneli kodu girin
                    </p>
                </div>

                {/* Error */}
                {error && (
                    <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
                        <XCircle className="w-5 h-5 text-red-600 mt-0.5 flex-shrink-0" />
                        <p className="text-red-800 text-sm">{error}</p>
                    </div>
                )}

                {/* Code Input */}
                <div className="mb-6">
                    <label className="block text-sm font-medium text-gray-700 mb-2 text-center">
                        Doğrulama Kodu
                    </label>
                    <input
                        type="text"
                        value={code}
                        onChange={(e) => {
                            const val = e.target.value.replace(/\D/g, '').slice(0, 6);
                            setCode(val);
                            setError('');
                        }}
                        className="w-full px-4 py-4 border border-gray-300 rounded-xl text-center text-3xl font-mono tracking-widest focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="000000"
                        maxLength={6}
                        autoFocus
                        onKeyDown={(e) => {
                            if (e.key === 'Enter' && code.length === 6) handleVerify();
                        }}
                    />
                    <p className="text-xs text-gray-400 text-center mt-2">
                        Kod her 30 saniyede bir yenilenir
                    </p>
                </div>

                {/* Verify Button */}
                <button
                    onClick={handleVerify}
                    disabled={loading || code.length !== 6}
                    className="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-semibold py-3 rounded-lg transition flex items-center justify-center gap-2 mb-4"
                >
                    {loading ? (
                        <>
                            <Loader2 className="w-5 h-5 animate-spin" />
                            Doğrulanıyor...
                        </>
                    ) : (
                        <>
                            <Shield className="w-5 h-5" />
                            Doğrula ve Giriş Yap
                        </>
                    )}
                </button>

                {/* Back Button */}
                <button
                    onClick={() => navigate('/login')}
                    className="w-full flex items-center justify-center gap-2 text-gray-500 hover:text-gray-700 text-sm"
                >
                    <ArrowLeft className="w-4 h-4" />
                    Giriş Sayfasına Dön
                </button>
            </div>
        </div>
    );
};

export default Verify2FAPage;