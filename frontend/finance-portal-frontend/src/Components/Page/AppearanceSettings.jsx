import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { Sun, Moon, Monitor, Loader2, CheckCircle, XCircle } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '../UI/Card';
import { Button } from '../UI/Button';
import { updatePreferences } from '../../API/userApi';

export default function AppearanceSettings() {
    const { user, refreshUser } = useAuth();
    const { setTheme } = useTheme();

    const [selectedTheme, setSelectedTheme] = useState(user?.theme || 'light');
    const [loading, setLoading] = useState(false);
    const [toast, setToast] = useState(null);

    useEffect(() => {
        if (user?.theme) {
            setSelectedTheme(user.theme);
        }
    }, [user?.theme]);

    const themes = [
        { value: 'light', label: 'Açık Mod', icon: <Sun className="w-5 h-5" />, desc: 'Klasik beyaz tema' },
        { value: 'dark', label: 'Koyu Mod', icon: <Moon className="w-5 h-5" />, desc: 'Göz dostu koyu tema' },
    ];

    const showToast = (message, type = 'success') => {
        setToast({ message, type });
        setTimeout(() => setToast(null), 3000);
    };

    const handleSave = async () => {
        setLoading(true);
        try {
            await updatePreferences(selectedTheme);
            await refreshUser();
            setTheme(selectedTheme);
            showToast('Tercihleriniz kaydedildi');
        } catch (error) {
            showToast(error.response?.data?.message || 'Tercihler kaydedilemedi', 'error');
        } finally {
            setLoading(false);
        }
    };

    const hasChanges = selectedTheme !== user?.theme;

    return (
        <div className="space-y-6">

            {/* Toast */}
            {toast && (
                <div className={`fixed top-6 right-6 z-50 flex items-center gap-3 px-6 py-4 rounded-lg shadow-lg ${
                    toast.type === 'success'
                        ? 'bg-green-50 dark:bg-green-900/30 text-green-800 dark:text-green-300 border border-green-200 dark:border-green-800'
                        : 'bg-red-50 dark:bg-red-900/30 text-red-800 dark:text-red-300 border border-red-200 dark:border-red-800'
                }`}>
                    {toast.type === 'success' && <CheckCircle className="w-5 h-5" />}
                    {toast.type === 'error' && <XCircle className="w-5 h-5" />}
                    <span>{toast.message}</span>
                </div>
            )}

            {/* Tema Seçimi */}
            <Card>
                <CardHeader>
                    <CardTitle>Tema</CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        {themes.map(theme => (
                            <button
                                key={theme.value}
                                onClick={() => setSelectedTheme(theme.value)}
                                className={`p-4 rounded-xl border-2 transition-all ${
                                    selectedTheme === theme.value
                                        ? 'border-blue-600 bg-blue-50 dark:bg-blue-900/20'
                                        : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-500'
                                }`}
                            >
                                <div className="flex items-center gap-3 mb-2">
                                    <div className={`p-2 rounded-lg ${
                                        selectedTheme === theme.value
                                            ? 'bg-blue-100 dark:bg-blue-800 text-blue-600 dark:text-blue-300'
                                            : 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400'
                                    }`}>
                                        {theme.icon}
                                    </div>
                                    <div className="text-left">
                                        <p className="font-semibold text-gray-900 dark:text-white">{theme.label}</p>
                                        <p className="text-sm text-gray-500 dark:text-gray-400">{theme.desc}</p>
                                    </div>
                                </div>
                            </button>
                        ))}
                    </div>
                </CardContent>
            </Card>


            {/* Kaydet Butonu */}
            {hasChanges && (
                <div className="flex justify-end">
                    <Button onClick={handleSave} disabled={loading} className="px-6">
                        {loading ? (
                            <>
                                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                                Kaydediliyor...
                            </>
                        ) : (
                            'Değişiklikleri Kaydet'
                        )}
                    </Button>
                </div>
            )}
        </div>
    );
}