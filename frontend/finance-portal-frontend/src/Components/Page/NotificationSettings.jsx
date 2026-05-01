import React, { useState, useEffect } from 'react';
import { Bell, TrendingUp, Newspaper, Activity, Loader2, CheckCircle, XCircle } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '../UI/Card';
import { useAuth } from '../../context/AuthContext';
import { updatePreferences } from '../../API/userApi';

export default function NotificationSettings() {
    const { user, refreshUser } = useAuth();

    const [settings, setSettings] = useState({
        notifyTransaction: true,
        notifyPortfolioChange: true,
        notifyPriceAlert: true,
        notifyNews: true,
    });
    const [loading, setLoading] = useState(false);
    const [toast, setToast] = useState(null);

    useEffect(() => {
        if (user) {
            setSettings({
                notifyTransaction: user.notifyTransaction ?? true,
                notifyPortfolioChange: user.notifyPortfolioChange ?? true,
                notifyPriceAlert: user.notifyPriceAlert ?? true,
                notifyNews: user.notifyNews ?? true,
            });
        }
    }, [user]);

    const showToast = (message, type = 'success') => {
        setToast({ message, type });
        setTimeout(() => setToast(null), 3000);
    };

    const handleToggle = async (key) => {
        const newSettings = { ...settings, [key]: !settings[key] };
        setSettings(newSettings);

        setLoading(true);
        try {
            await updatePreferences(
                user?.theme,
                newSettings.notifyTransaction,
                newSettings.notifyPortfolioChange,
                newSettings.notifyPriceAlert,
                newSettings.notifyNews
            );
            await refreshUser();
            showToast('Bildirim tercihi güncellendi');
        } catch{
            // Hata olursa eski değere dön
            setSettings(settings);
            showToast('Güncelleme başarısız', 'error');
        } finally {
            setLoading(false);
        }
    };

    const notifications = [
        {
            key: 'notifyTransaction',
            icon: <Activity className="w-5 h-5 text-green-600" />,
            iconBg: 'bg-green-100',
            title: 'İşlem Bildirimleri',
            description: 'Alış ve satış işlemleriniz gerçekleştiğinde bildirim alın',
        },
        {
            key: 'notifyPortfolioChange',
            icon: <TrendingUp className="w-5 h-5 text-blue-600" />,
            iconBg: 'bg-blue-100',
            title: 'Portföy Değer Değişimi',
            description: 'Portföyünüz %5\'ten fazla değiştiğinde bildirim alın',
        },
        {
            key: 'notifyPriceAlert',
            icon: <Bell className="w-5 h-5 text-yellow-600" />,
            iconBg: 'bg-yellow-100',
            title: 'Fiyat Alarmları',
            description: 'Takip ettiğiniz enstrümanların fiyat değişimlerinde bildirim alın',
        },
        {
            key: 'notifyNews',
            icon: <Newspaper className="w-5 h-5 text-purple-600" />,
            iconBg: 'bg-purple-100',
            title: 'Haber Bildirimleri',
            description: 'Yeni finansal haberler geldiğinde bildirim alın',
        },
    ];

    return (
        <div className="space-y-6">

            {/* Toast */}
            {toast && (
                <div className={`fixed top-6 right-6 z-50 flex items-center gap-3 px-6 py-4 rounded-lg shadow-lg ${
                    toast.type === 'success'
                        ? 'bg-green-50 text-green-800 border border-green-200'
                        : 'bg-red-50 text-red-800 border border-red-200'
                }`}>
                    {toast.type === 'success' ? <CheckCircle className="w-5 h-5" /> : <XCircle className="w-5 h-5" />}
                    <span>{toast.message}</span>
                </div>
            )}

            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Bell className="w-5 h-5 text-blue-600" />
                        Bildirim Tercihleri
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="space-y-4">
                        {notifications.map((item) => (
                            <div
                                key={item.key}
                                className="flex items-center justify-between p-4 border border-gray-200 dark:border-gray-700 rounded-xl"
                            >
                                <div className="flex items-center gap-4">
                                    <div className={`w-10 h-10 ${item.iconBg} rounded-lg flex items-center justify-center`}>
                                        {item.icon}
                                    </div>
                                    <div>
                                        <p className="font-medium text-gray-900 dark:text-white">{item.title}</p>
                                        <p className="text-sm text-gray-500 dark:text-gray-400">{item.description}</p>
                                    </div>
                                </div>

                                {/* Toggle Switch */}
                                <button
                                    onClick={() => handleToggle(item.key)}
                                    disabled={loading}
                                    className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none ${
                                        settings[item.key] ? 'bg-blue-600' : 'bg-gray-300 dark:bg-gray-600'
                                    } ${loading ? 'opacity-50 cursor-not-allowed' : ''}`}
                                >
                                    <span
                                        className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                                            settings[item.key] ? 'translate-x-6' : 'translate-x-1'
                                        }`}
                                    />
                                </button>
                            </div>
                        ))}
                    </div>

                    {loading && (
                        <div className="flex items-center gap-2 mt-4 text-sm text-gray-500">
                            <Loader2 className="w-4 h-4 animate-spin" />
                            Kaydediliyor...
                        </div>
                    )}
                </CardContent>
            </Card>
        </div>
    );
}