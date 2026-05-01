import React, { useState, useEffect } from 'react';
import { Bell, Trash2, Loader2, CheckCircle, XCircle } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../UI/Card';
import { Button } from '../UI/Button';
import { getUserAlerts, deletePriceAlert } from '../../API/priceAlertApi';
import { useNavigate } from 'react-router-dom';

export default function PriceAlertsPage() {
    const navigate = useNavigate();
    const [alerts, setAlerts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedCategory, setSelectedCategory] = useState('ALL');

    useEffect(() => {
        fetchAlerts();
    }, []);

    const fetchAlerts = async () => {
        setLoading(true);
        try {
            const data = await getUserAlerts();
            setAlerts(data || []);
        } catch (e) {
            console.error('Alert fetch error:', e);
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (id) => {
        try {
            await deletePriceAlert(id);
            setAlerts(alerts.filter(a => a.id !== id));
        } catch (e) {
            console.error('Delete error:', e);
        }
    };

    const categories = [
        { id: 'ALL', label: 'Tümü' },
        { id: 'ACTIVE', label: 'Aktif' },
        { id: 'TRIGGERED', label: 'Tetiklendi' },
    ];

    const filteredAlerts = alerts.filter(a => {
        if (selectedCategory === 'ACTIVE') return a.active && !a.triggered;
        if (selectedCategory === 'TRIGGERED') return a.triggered;
        return true;
    });

    const formatPrice = (price) => {
        if (!price && price !== 0) return '-';
        if (price > 1000) return price.toLocaleString('tr-TR', { minimumFractionDigits: 2 });
        if (price > 1) return price.toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 4 });
        return price.toLocaleString('tr-TR', { minimumFractionDigits: 4, maximumFractionDigits: 6 });
    };

    const formatDate = (dateStr) => {
        if (!dateStr) return '-';
        return new Date(dateStr).toLocaleDateString('tr-TR', {
            day: '2-digit', month: 'long', year: 'numeric',
            hour: '2-digit', minute: '2-digit'
        });
    };

    return (
        <div className="min-h-screen bg-gray-50 dark:bg-gray-950 p-8">
            <div className="max-w-4xl mx-auto">

                {/* Header */}
                <div className="mb-6">
                    <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">Fiyat Alarmları</h1>
                    <p className="text-gray-600 dark:text-gray-400">
                        {alerts.filter(a => a.active && !a.triggered).length} aktif alarm
                    </p>
                </div>

                {/* Kategori Filtreleri */}
                <div className="mb-6 flex gap-2">
                    {categories.map(cat => (
                        <button
                            key={cat.id}
                            onClick={() => setSelectedCategory(cat.id)}
                            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                                selectedCategory === cat.id
                                    ? 'bg-blue-600 text-white'
                                    : 'bg-white dark:bg-gray-800 text-gray-600 dark:text-gray-300 border border-gray-200 dark:border-gray-700 hover:bg-gray-50'
                            }`}
                        >
                            {cat.label}
                        </button>
                    ))}
                </div>

                {/* Alarm Listesi */}
                <Card>
                    <CardContent className="p-0">
                        {loading ? (
                            <div className="flex items-center justify-center py-12">
                                <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
                            </div>
                        ) : filteredAlerts.length === 0 ? (
                            <div className="text-center py-12 text-gray-400">
                                <Bell className="w-12 h-12 mx-auto mb-3 opacity-50" />
                                <p className="text-lg font-medium">Alarm yok</p>
                                <p className="text-sm mt-1">Enstrüman detay sayfasından alarm ekleyebilirsiniz</p>
                                <Button
                                    variant="outline"
                                    className="mt-4"
                                    onClick={() => navigate('/instruments')}
                                >
                                    Enstrümanlara Git
                                </Button>
                            </div>
                        ) : (
                            filteredAlerts.map((alert, index) => (
                                <div
                                    key={alert.id}
                                    className={`flex items-center gap-4 px-6 py-4 transition-colors hover:bg-gray-50 dark:hover:bg-gray-800 ${
                                        index !== filteredAlerts.length - 1 ? 'border-b border-gray-100 dark:border-gray-800' : ''
                                    }`}
                                >
                                    {/* İkon */}
                                    <div className={`w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 ${
                                        alert.triggered ? 'bg-gray-100' : 'bg-blue-100'
                                    }`}>
                                        {alert.triggered
                                            ? <CheckCircle className="w-5 h-5 text-gray-500" />
                                            : <Bell className="w-5 h-5 text-blue-600" />
                                        }
                                    </div>

                                    {/* Bilgi */}
                                    <div className="flex-1 min-w-0">
                                        <div className="flex items-center gap-2 mb-1">
                                            <p className="font-semibold text-gray-900 dark:text-white">
                                                {alert.instrumentName}
                                            </p>
                                            <span className="text-xs text-gray-400 font-mono bg-gray-100 dark:bg-gray-700 px-2 py-0.5 rounded">
                                                {alert.instrumentSymbol}
                                            </span>
                                            {/* Durum badge */}
                                            {alert.triggered ? (
                                                <span className="text-xs bg-gray-100 text-gray-500 px-2 py-0.5 rounded-full">
                                                    Tetiklendi
                                                </span>
                                            ) : (
                                                <span className="text-xs bg-green-100 text-green-600 px-2 py-0.5 rounded-full">
                                                    Aktif
                                                </span>
                                            )}
                                        </div>
                                        <p className="text-sm text-gray-600 dark:text-gray-400">
                                            {alert.condition === 'ABOVE' ? '↑ Üzerine çıkınca' : '↓ Altına düşünce'}
                                            {' — '}
                                            <span className="font-semibold text-gray-900 dark:text-white">
                                                {formatPrice(alert.targetPrice)}
                                            </span>
                                        </p>
                                        <p className="text-xs text-gray-400 mt-1">
                                            {alert.triggered
                                                ? `Tetiklenme: ${formatDate(alert.triggeredAt)}`
                                                : `Oluşturulma: ${formatDate(alert.createdAt)}`
                                            }
                                        </p>
                                    </div>

                                    {/* Sil butonu */}
                                    <button
                                        onClick={() => handleDelete(alert.id)}
                                        className="text-red-400 hover:text-red-600 flex-shrink-0 p-1"
                                    >
                                        <Trash2 className="w-5 h-5" />
                                    </button>
                                </div>
                            ))
                        )}
                    </CardContent>
                </Card>
            </div>
        </div>
    );
}