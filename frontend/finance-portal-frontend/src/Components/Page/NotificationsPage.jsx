import React, { useState, useEffect } from 'react';
import { Bell, Check, CheckCheck, Loader2, Activity, TrendingUp, Newspaper, DollarSign } from 'lucide-react';
import { Card, CardContent } from '../UI/Card';
import { Button } from '../UI/Button';
import { useTranslation } from 'react-i18next';
import { getNotifications, markAllAsRead, markAsRead } from '../../API/notificationApi';

export default function NotificationsPage() {
    const { t } = useTranslation();
    const [notifications, setNotifications] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedCategory, setSelectedCategory] = useState('ALL');
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const pageSize = 20;

    const categories = [
        { id: 'ALL',              label: t('notifications.all') },
        { id: 'TRANSACTION',      label: t('notifications.transactions') },
        { id: 'PORTFOLIO_CHANGE', label: t('notifications.portfolio') },
        { id: 'PRICE_ALERT',      label: t('notifications.priceAlert') },
        { id: 'NEWS',             label: t('nav.news') },
    ];

    useEffect(() => {
        fetchNotifications();
    }, [currentPage]);

    const fetchNotifications = async () => {
        setLoading(true);
        try {
            const data = await getNotifications(currentPage, pageSize);
            setNotifications(data.content || []);
            setTotalPages(data.totalPages || 0);
            setTotalElements(data.totalElements || 0);
        } catch (e) {
            console.error('Error fetching notifications:', e);
        } finally {
            setLoading(false);
        }
    };

    const handleMarkAllRead = async () => {
        try {
            await markAllAsRead();
            setNotifications(notifications.map(n => ({ ...n, read: true })));
        } catch (e) {
            console.error('Error marking all as read:', e);
        }
    };

    const handleMarkRead = async (id) => {
        try {
            await markAsRead(id);
            setNotifications(notifications.map(n => n.id === id ? { ...n, read: true } : n));
        } catch (e) {
            console.error('Error marking as read:', e);
        }
    };

    const getNotificationIcon = (type) => {
        switch (type) {
            case 'TRANSACTION':      return <Activity className="w-5 h-5 text-green-600" />;
            case 'PORTFOLIO_CHANGE': return <TrendingUp className="w-5 h-5 text-blue-600" />;
            case 'PRICE_ALERT':      return <DollarSign className="w-5 h-5 text-yellow-600" />;
            case 'NEWS':             return <Newspaper className="w-5 h-5 text-purple-600" />;
            default:                 return <Bell className="w-5 h-5 text-gray-600" />;
        }
    };

    const getNotificationIconBg = (type) => {
        switch (type) {
            case 'TRANSACTION':      return 'bg-green-100';
            case 'PORTFOLIO_CHANGE': return 'bg-blue-100';
            case 'PRICE_ALERT':      return 'bg-yellow-100';
            case 'NEWS':             return 'bg-purple-100';
            default:                 return 'bg-gray-100';
        }
    };

    const formatTime = (dateStr) => {
        if (!dateStr) return '';
        const diff = Math.floor((new Date() - new Date(dateStr)) / 60000);
        if (diff < 1) return t('common.justNow');
        if (diff < 60) return `${diff}${t('common.minutesAgo')}`;
        if (diff < 1440) return `${Math.floor(diff / 60)}${t('common.hoursAgo')}`;
        return `${Math.floor(diff / 1440)}${t('common.daysAgo')}`;
    };

    const filteredNotifications = selectedCategory === 'ALL'
        ? notifications
        : notifications.filter(n => n.type === selectedCategory);

    const unreadCount = filteredNotifications.filter(n => !n.read).length;

    return (
        <div className="min-h-screen bg-gray-50 dark:bg-gray-950 p-8">
            <div className="max-w-4xl mx-auto">

                {/* Header */}
                <div className="mb-6 flex items-center justify-between">
                    <div>
                        <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">
                            {t('common.notifications')}
                        </h1>
                        <p className="text-gray-600 dark:text-gray-400">
                            {t('notifications.summary', { total: totalElements, unread: unreadCount })}
                        </p>
                    </div>
                    {unreadCount > 0 && (
                        <Button variant="outline" onClick={handleMarkAllRead}>
                            <CheckCheck className="w-4 h-4 mr-2" />
                            {t('common.markAllRead')}
                        </Button>
                    )}
                </div>

                {/* Kategori Filtreleri */}
                <div className="mb-6 flex gap-2 flex-wrap">
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

                {/* Bildirimler */}
                <Card>
                    <CardContent className="p-0">
                        {loading ? (
                            <div className="flex items-center justify-center py-12">
                                <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
                            </div>
                        ) : filteredNotifications.length === 0 ? (
                            <div className="text-center py-12 text-gray-400">
                                <Bell className="w-12 h-12 mx-auto mb-3 opacity-50" />
                                <p className="text-lg font-medium">{t('common.noNotifications')}</p>
                                <p className="text-sm mt-1">{t('notifications.noCategoryNotif')}</p>
                            </div>
                        ) : (
                            filteredNotifications.map((n, index) => (
                                <div
                                    key={n.id}
                                    className={`flex items-start gap-4 px-6 py-4 transition-colors ${
                                        !n.read ? 'bg-blue-50 dark:bg-blue-900/10' : 'hover:bg-gray-50 dark:hover:bg-gray-800'
                                    } ${index !== filteredNotifications.length - 1 ? 'border-b border-gray-100 dark:border-gray-800' : ''}`}
                                >
                                    <div className={`w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 ${getNotificationIconBg(n.type)}`}>
                                        {getNotificationIcon(n.type)}
                                    </div>

                                    <div className="flex-1 min-w-0">
                                        <div className="flex items-center gap-2 mb-1">
                                            <p className="font-semibold text-gray-900 dark:text-white">{n.title}</p>
                                            {!n.read && (
                                                <span className="w-2 h-2 bg-blue-600 rounded-full flex-shrink-0" />
                                            )}
                                        </div>
                                        <p className="text-sm text-gray-600 dark:text-gray-400">{n.message}</p>
                                        <p className="text-xs text-gray-400 dark:text-gray-500 mt-1">{formatTime(n.createdAt)}</p>
                                    </div>

                                    {!n.read && (
                                        <button
                                            onClick={() => handleMarkRead(n.id)}
                                            className="text-blue-500 hover:text-blue-600 flex-shrink-0 p-1"
                                            title={t('notifications.markRead')}
                                        >
                                            <Check className="w-5 h-5" />
                                        </button>
                                    )}
                                </div>
                            ))
                        )}
                    </CardContent>
                </Card>

                {/* Pagination */}
                {totalPages > 1 && (
                    <div className="flex items-center justify-center gap-2 mt-6">
                        <Button
                            variant="outline"
                            disabled={currentPage === 0}
                            onClick={() => setCurrentPage(p => p - 1)}
                        >
                            {t('common.back')}
                        </Button>
                        <span className="text-sm text-gray-600 dark:text-gray-400">
                            {currentPage + 1} / {totalPages}
                        </span>
                        <Button
                            variant="outline"
                            disabled={currentPage === totalPages - 1}
                            onClick={() => setCurrentPage(p => p + 1)}
                        >
                            {t('common.next')}
                        </Button>
                    </div>
                )}
            </div>
        </div>
    );
}