import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Shield, Loader2, CheckCircle, XCircle, Copy, Check } from 'lucide-react';
import { Button } from '../UI/Button';
import { setupTotp, verifyTotpSetup } from '../../API/totpApi';

export default function Setup2FAPage() {
    const navigate = useNavigate();

    const [step, setStep] = useState(1); // 1: QR göster, 2: Kod doğrula, 3: Başarılı
    const [qrCode, setQrCode] = useState('');
    const [secret, setSecret] = useState('');
    const [code, setCode] = useState('');
    const [loading, setLoading] = useState(true);
    const [verifying, setVerifying] = useState(false);
    const [error, setError] = useState('');
    const [copied, setCopied] = useState(false);

    useEffect(() => {
        fetchQrCode();
    }, []);

    const fetchQrCode = async () => {
        setLoading(true);
        try {
            const data = await setupTotp();
            setQrCode(data.qrCode);
            setSecret(data.secret);
        } catch {
            setError('QR kod oluşturulamadı. Lütfen tekrar deneyin.');
        } finally {
            setLoading(false);
        }
    };

    const handleVerify = async () => {
        if (code.length !== 6) {
            setError('Lütfen 6 haneli kodu girin');
            return;
        }
        setVerifying(true);
        setError('');
        try {
            const result = await verifyTotpSetup(code);
            if (result.success) {
                setStep(3);
            } else {
                setError('Geçersiz kod. Lütfen tekrar deneyin.');
            }
        } catch {
            setError('Doğrulama başarısız. Lütfen tekrar deneyin.');
        } finally {
            setVerifying(false);
        }
    };

    const handleCopySecret = () => {
        navigator.clipboard.writeText(secret);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-50">
                <div className="text-center">
                    <Loader2 className="w-16 h-16 text-blue-600 animate-spin mx-auto mb-4" />
                    <p className="text-gray-600">QR kod oluşturuluyor...</p>
                </div>
            </div>
        );
    }

    // Başarılı
    if (step === 3) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-50">
                <div className="bg-white rounded-2xl shadow-lg p-10 max-w-md w-full text-center">
                    <div className="flex justify-center mb-6">
                        <div className="bg-green-100 p-4 rounded-full">
                            <CheckCircle className="w-12 h-12 text-green-600" />
                        </div>
                    </div>
                    <h1 className="text-2xl font-bold text-gray-900 mb-2">
                        2FA Aktif Edildi!
                    </h1>
                    <p className="text-gray-500 mb-6">
                        İki faktörlü doğrulama başarıyla aktif edildi. Artık her girişte Google Authenticator kodunuzu girmeniz gerekecek.
                    </p>
                    <Button className="w-full" onClick={() => navigate('/settings')}>
                        Ayarlara Dön
                    </Button>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50 p-4">
            <div className="bg-white rounded-2xl shadow-lg p-8 max-w-md w-full">

                {/* Header */}
                <div className="text-center mb-8">
                    <div className="flex justify-center mb-4">
                        <div className="bg-blue-100 p-4 rounded-full">
                            <Shield className="w-10 h-10 text-blue-600" />
                        </div>
                    </div>
                    <h1 className="text-2xl font-bold text-gray-900 mb-2">
                        İki Faktörlü Doğrulama Kurulumu
                    </h1>
                    <p className="text-gray-500 text-sm">
                        Hesabınızı daha güvenli hale getirmek için 2FA kurun
                    </p>
                </div>

                {/* Steps */}
                <div className="flex items-center justify-center gap-2 mb-8">
                    <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold ${step >= 1 ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-500'}`}>1</div>
                    <div className={`h-1 w-12 ${step >= 2 ? 'bg-blue-600' : 'bg-gray-200'}`} />
                    <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold ${step >= 2 ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-500'}`}>2</div>
                </div>

                {step === 1 && (
                    <>
                        <div className="space-y-4 mb-6">
                            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                                <p className="text-sm text-blue-800 font-semibold mb-2">📱 Adım 1: Uygulamayı İndirin</p>
                                <p className="text-sm text-blue-700">Google Authenticator veya Authy uygulamasını telefonunuza indirin.</p>
                            </div>

                            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                                <p className="text-sm text-blue-800 font-semibold mb-2">📷 Adım 2: QR Kodu Tarayın</p>
                                <p className="text-sm text-blue-700">Uygulamayı açıp aşağıdaki QR kodu tarayın.</p>
                            </div>
                        </div>

                        {/* QR Code */}
                        {qrCode && (
                            <div className="flex justify-center mb-6">
                                <div className="border-4 border-gray-100 rounded-xl p-2">
                                    <img src={qrCode} alt="QR Code" className="w-48 h-48" />
                                </div>
                            </div>
                        )}

                        {/* Secret Key */}
                        <div className="bg-gray-50 rounded-lg p-4 mb-6">
                            <p className="text-xs text-gray-500 mb-2">QR kod çalışmıyorsa bu kodu manuel girin:</p>
                            <div className="flex items-center gap-2">
                                <code className="flex-1 text-sm font-mono text-gray-800 break-all">{secret}</code>
                                <button
                                    onClick={handleCopySecret}
                                    className="text-blue-600 hover:text-blue-800 flex-shrink-0"
                                >
                                    {copied ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
                                </button>
                            </div>
                        </div>

                        <Button className="w-full" onClick={() => setStep(2)}>
                            QR Kodu Taradım →
                        </Button>
                    </>
                )}

                {step === 2 && (
                    <>
                        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
                            <p className="text-sm text-blue-800 font-semibold mb-1">✅ Adım 3: Kodu Doğrulayın</p>
                            <p className="text-sm text-blue-700">Uygulamadaki 6 haneli kodu girin.</p>
                        </div>

                        <div className="mb-6">
                            <label className="block text-sm font-medium text-gray-700 mb-2">
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
                                className="w-full px-4 py-3 border border-gray-300 rounded-lg text-center text-2xl font-mono tracking-widest focus:outline-none focus:ring-2 focus:ring-blue-500"
                                placeholder="000000"
                                maxLength={6}
                                autoFocus
                            />
                            {error && (
                                <p className="text-red-500 text-sm mt-2 flex items-center gap-1">
                                    <XCircle className="w-4 h-4" />
                                    {error}
                                </p>
                            )}
                        </div>

                        <div className="flex gap-3">
                            <Button
                                variant="outline"
                                className="flex-1"
                                onClick={() => { setStep(1); setCode(''); setError(''); }}
                                disabled={verifying}
                            >
                                Geri
                            </Button>
                            <Button
                                className="flex-1"
                                onClick={handleVerify}
                                disabled={verifying || code.length !== 6}
                            >
                                {verifying ? (
                                    <><Loader2 className="w-4 h-4 mr-2 animate-spin" />Doğrulanıyor...</>
                                ) : (
                                    'Doğrula'
                                )}
                            </Button>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
}