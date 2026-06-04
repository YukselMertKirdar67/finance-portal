import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { CheckCircle, XCircle, Loader2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button } from '../UI/Button';
import axios from 'axios';

export default function EmailVerifiedPage() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const { t } = useTranslation();
    const [status, setStatus] = useState('loading');
    const [countdown, setCountdown] = useState(5);

    useEffect(() => {
        const token = searchParams.get('token');
        if (!token) {
            setStatus('error');
            return;
        }

        axios.get(`http://localhost:8080/api/auth/verify-email?token=${token}`)
            .then(() => {
                setStatus('success');
                const timer = setInterval(() => {
                    setCountdown(prev => {
                        if (prev <= 1) {
                            clearInterval(timer);
                            navigate('/setup-2fa');
                            return 0;
                        }
                        return prev - 1;
                    });
                }, 1000);
                return () => clearInterval(timer);
            })
            .catch(() => {
                setStatus('error');
            });
    }, []);

    if (status === 'loading') {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-50">
                <div className="text-center">
                    <Loader2 className="w-16 h-16 text-blue-600 animate-spin mx-auto mb-4" />
                    <p className="text-gray-600">{t('emailVerified.verifying')}</p>
                </div>
            </div>
        );
    }

    if (status === 'error') {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-50">
                <div className="bg-white rounded-2xl shadow-lg p-10 max-w-md w-full text-center">
                    <div className="flex justify-center mb-6">
                        <div className="bg-red-100 p-4 rounded-full">
                            <XCircle className="w-12 h-12 text-red-600" />
                        </div>
                    </div>
                    <h1 className="text-2xl font-bold text-gray-900 mb-2">
                        {t('emailVerified.error.title')}
                    </h1>
                    <p className="text-gray-500 mb-6">
                        {t('emailVerified.error.description')}
                    </p>
                    <Button className="w-full" onClick={() => navigate('/login')}>
                        {t('verify2fa.backToLogin')}
                    </Button>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50">
            <div className="bg-white rounded-2xl shadow-lg p-10 max-w-md w-full text-center">
                <div className="flex justify-center mb-6">
                    <div className="bg-green-100 p-4 rounded-full">
                        <CheckCircle className="w-12 h-12 text-green-600" />
                    </div>
                </div>
                <h1 className="text-2xl font-bold text-gray-900 mb-2">
                    {t('emailVerified.success.title')}
                </h1>
                <p className="text-gray-500 mb-6">
                    {t('emailVerified.success.description')}
                </p>
                <p className="text-sm text-gray-400 mb-6">
                    {t('emailVerified.success.redirect', { count: countdown })}
                </p>
                <Button className="w-full" onClick={() => navigate('/setup-2fa')}>
                    {t('emailVerified.success.goTo2FA')}
                </Button>
            </div>
        </div>
    );
}