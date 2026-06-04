import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Shield, Loader2, XCircle, ArrowLeft } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../context/AuthContext';
import { verifyTotpLogin } from '../../API/totpApi';
import api from '../../API/instrumentsApi';

const Verify2FAPage = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const { login } = useAuth();
    const { t } = useTranslation();

    const { keycloakId, username, password, rememberMe } = location.state || {};

    const [code, setCode] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    if (!keycloakId) {
        navigate('/login');
        return null;
    }

    const handleVerify = async () => {
        if (code.length !== 6) {
            setError(t('setup2fa.codeLength'));
            return;
        }

        setLoading(true);
        setError('');

        try {
            const totpResponse = await verifyTotpLogin(keycloakId, code);

            if (!totpResponse.success) {
                setError(t('setup2fa.invalidCode'));
                return;
            }

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
                setError(t('verify2fa.loginFailed'));
            }

        } catch {
            setError(t('setup2fa.verifyError'));
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
                        {t('auth.twoFactor')}
                    </h1>
                    <p className="text-gray-500 text-sm">
                        {t('verify2fa.subtitle')}
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
                        {t('auth.enterCode')}
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
                        {t('verify2fa.codeRefresh')}
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
                            {t('setup2fa.verifying')}
                        </>
                    ) : (
                        <>
                            <Shield className="w-5 h-5" />
                            {t('verify2fa.verifyAndLogin')}
                        </>
                    )}
                </button>

                {/* Back Button */}
                <button
                    onClick={() => navigate('/login')}
                    className="w-full flex items-center justify-center gap-2 text-gray-500 hover:text-gray-700 text-sm"
                >
                    <ArrowLeft className="w-4 h-4" />
                    {t('verify2fa.backToLogin')}
                </button>
            </div>
        </div>
    );
};

export default Verify2FAPage;