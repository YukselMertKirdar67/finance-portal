import React, { useState } from 'react';
import { AlertCircle, X, Send, CheckCircle } from 'lucide-react';
import api from '../../API/instrumentsApi';

const EmailVerificationBanner = ({ email, onClose }) => {
    const [sending, setSending] = useState(false);
    const [sent, setSent] = useState(false);
    const [error, setError] = useState('');

    const handleResendEmail = async () => {
        setSending(true);
        setError('');

        try {
            await api.post('/auth/send-verification-email', { email });
            setSent(true);

            // 5 saniye sonra success mesajını kapat
            setTimeout(() => {
                setSent(false);
            }, 5000);

        } catch (err) {
            setError('Email gönderilemedi');
            console.error('Error sending verification email:', err);
        } finally {
            setSending(false);
        }
    };

    if (sent) {
        return (
            <div className="bg-green-50 border-b border-green-200 px-6 py-3">
                <div className="max-w-7xl mx-auto flex items-center justify-between">
                    <div className="flex items-center gap-3">
                        <CheckCircle className="w-5 h-5 text-green-600 flex-shrink-0" />
                        <p className="text-sm text-green-800">
                            <strong>Başarılı!</strong> Doğrulama emaili gönderildi. Email kutunuzu kontrol edin.
                        </p>
                    </div>
                    <button
                        onClick={onClose}
                        className="text-green-600 hover:text-green-800 transition"
                    >
                        <X className="w-5 h-5" />
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="bg-orange-50 border-b border-orange-200 px-6 py-3">
            <div className="max-w-7xl mx-auto flex items-center justify-between">
                <div className="flex items-center gap-3">
                    <AlertCircle className="w-5 h-5 text-orange-600 flex-shrink-0" />
                    <div>
                        <p className="text-sm text-orange-800">
                            <strong>Email adresiniz doğrulanmadı.</strong> Hesabınızın tüm özelliklerinden
                            yararlanmak için email adresinizi doğrulayın.
                        </p>
                        {error && (
                            <p className="text-xs text-red-600 mt-1">{error}</p>
                        )}
                    </div>
                </div>

                <div className="flex items-center gap-2">
                    <button
                        onClick={handleResendEmail}
                        disabled={sending}
                        className="flex items-center gap-2 px-4 py-2 bg-orange-600 hover:bg-orange-700 disabled:bg-orange-400 text-white text-sm font-medium rounded-lg transition"
                    >
                        {sending ? (
                            <>
                                <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                                Gönderiliyor...
                            </>
                        ) : (
                            <>
                                <Send className="w-4 h-4" />
                                Email Gönder
                            </>
                        )}
                    </button>

                    <button
                        onClick={onClose}
                        className="text-orange-600 hover:text-orange-800 transition"
                    >
                        <X className="w-5 h-5" />
                    </button>
                </div>
            </div>
        </div>
    );
};

export default EmailVerificationBanner;