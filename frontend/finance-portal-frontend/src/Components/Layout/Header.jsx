import React, { useState, useEffect, useRef } from 'react';
import { LayoutDashboard, Newspaper, Home, GitCompare, Bell, LogOut, Check, CheckCheck } from 'lucide-react';
import { NavLink, useNavigate } from 'react-router-dom';
import { Button } from '../UI/Button';
import { getUnreadNotifications, markAllAsRead, markAsRead, getUnreadCount } from '../../API/notificationApi';

export default function Header({
                                   isLoggedIn = false,
                                   onLogout,
                                   user = null
                               }) {
    const navigate = useNavigate();
    const [showNotifications, setShowNotifications] = useState(false);
    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const dropdownRef = useRef(null);

    const tabClass = ({ isActive }) =>
        `px-4 py-2 rounded-lg transition-colors flex items-center gap-2 ${
            isActive ? 'bg-blue-600 text-white' : 'text-gray-700 hover:bg-gray-100'
        }`;

    // fetchUnreadCount'tan sonra useEffect
    useEffect(() => {
        if (!isLoggedIn) return;

        const loadCount = async () => {
            const data = await getUnreadCount().catch(() => ({ count: 0 }));
            setUnreadCount(data.count || 0);
        };

        loadCount();
        const interval = setInterval(loadCount, 30000);
        return () => clearInterval(interval);
    }, [isLoggedIn]);

    useEffect(() => {
        const handleClickOutside = (e) => {
            if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
                setShowNotifications(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleBellClick = async () => {
        setShowNotifications(!showNotifications);
        if (!showNotifications) {
            try {
                const data = await getUnreadNotifications();
                setNotifications(data || []);
            } catch (e) {
                console.error('Notifications error:', e);
            }
        }
    };

    const handleMarkAllRead = async () => {
        try {
            await markAllAsRead();
            setNotifications(notifications.map(n => ({ ...n, read: true })));
            setUnreadCount(0);
        } catch (e) {
            console.error('Mark all read error:', e);
        }
    };

    const handleMarkRead = async (id) => {
        try {
            await markAsRead(id);
            setNotifications(notifications.map(n => n.id === id ? { ...n, read: true } : n));
            setUnreadCount(prev => Math.max(0, prev - 1));
        } catch (e) {
            console.error('Mark read error:', e);
        }
    };

    const getNotificationIcon = (type) => {
        switch (type) {
            case 'PORTFOLIO_CHANGE': return '📊';
            case 'PRICE_ALERT': return '💰';
            case 'TRANSACTION': return '✅';
            case 'NEWS': return '📰';
            default: return '🔔';
        }
    };

    const formatTime = (dateStr) => {
        if (!dateStr) return '';
        const diff = Math.floor((new Date() - new Date(dateStr)) / 60000);
        if (diff < 1) return 'Az önce';
        if (diff < 60) return `${diff}dk önce`;
        if (diff < 1440) return `${Math.floor(diff / 60)}sa önce`;
        return `${Math.floor(diff / 1440)}g önce`;
    };

    return (
        <header className="bg-white border-b border-gray-200 px-6 py-4 sticky top-0 z-10">
            <div className="flex items-center justify-between">

                {/* TABS */}
                <div className="flex gap-2">
                    <NavLink to="/home" className={tabClass}>
                        <LayoutDashboard className="w-4 h-4" />
                        <span>Anasayfa</span>
                    </NavLink>
                    <NavLink to="/news" className={tabClass}>
                        <Newspaper className="w-4 h-4" />
                        <span>Haber</span>
                    </NavLink>
                    <NavLink to="/instruments" className={tabClass}>
                        <Home className="w-4 h-4" />
                        <span>Finansal Enstrümanlar</span>
                    </NavLink>
                    <NavLink to="/comparison" className={tabClass}>
                        <GitCompare className="w-4 h-4" />
                        <span>Karşılaştır</span>
                    </NavLink>
                </div>

                {/* RIGHT */}
                <div className="flex items-center gap-4">

                    {/* Bildirim Butonu */}
                    <div className="relative" ref={dropdownRef}>
                        <Button
                            variant="ghost"
                            size="icon"
                            onClick={handleBellClick}
                            className="relative"
                        >
                            <Bell className="w-5 h-5 text-gray-600" />
                            {unreadCount > 0 && (
                                <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center font-bold">
                                    {unreadCount > 9 ? '9+' : unreadCount}
                                </span>
                            )}
                        </Button>

                        {/* Bildirim Dropdown */}
                        {showNotifications && (
                            <div className="absolute right-0 top-12 w-96 bg-white border border-gray-200 rounded-xl shadow-xl z-50">
                                <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100">
                                    <h3 className="font-semibold text-gray-900">Bildirimler</h3>
                                    {unreadCount > 0 && (
                                        <button
                                            onClick={handleMarkAllRead}
                                            className="text-xs text-blue-600 hover:text-blue-700 flex items-center gap-1"
                                        >
                                            <CheckCheck className="w-3 h-3" />
                                            Tümünü Okundu İşaretle
                                        </button>
                                    )}
                                </div>

                                <div className="max-h-80 overflow-y-auto">
                                    {notifications.length === 0 ? (
                                        <div className="text-center py-8 text-gray-400">
                                            <Bell className="w-8 h-8 mx-auto mb-2 opacity-50" />
                                            <p className="text-sm">Bildirim yok</p>
                                        </div>
                                    ) : (
                                        notifications.map(n => (
                                            <div
                                                key={n.id}
                                                className={`flex items-start gap-3 px-4 py-3 hover:bg-gray-50 transition-colors border-b border-gray-50 last:border-0 ${
                                                    !n.read ? 'bg-blue-50' : ''
                                                }`}
                                            >
                                                <span className="text-xl flex-shrink-0">{getNotificationIcon(n.type)}</span>
                                                <div className="flex-1 min-w-0">
                                                    <p className="text-sm font-medium text-gray-900">{n.title}</p>
                                                    <p className="text-xs text-gray-600 mt-0.5 truncate">{n.message}</p>
                                                    <p className="text-xs text-gray-400 mt-1">{formatTime(n.createdAt)}</p>
                                                </div>
                                                {!n.read && (
                                                    <button
                                                        onClick={() => handleMarkRead(n.id)}
                                                        className="text-blue-500 hover:text-blue-600 flex-shrink-0"
                                                    >
                                                        <Check className="w-4 h-4" />
                                                    </button>
                                                )}
                                            </div>
                                        ))
                                    )}
                                </div>

                                <div className="px-4 py-3 border-t border-gray-100">
                                    <button
                                        onClick={() => {
                                            setShowNotifications(false);
                                            navigate('/notifications');
                                        }}
                                        className="text-xs text-blue-600 hover:text-blue-700 w-full text-center"
                                    >
                                        Tüm Bildirimleri Gör
                                    </button>
                                </div>
                            </div>
                        )}
                    </div>

                    {isLoggedIn && user ? (
                        <div className="flex items-center gap-3">
                            <div className="flex flex-col items-end">
                                <span className="text-sm font-medium text-gray-900">{user.username}</span>
                                {user.isAdmin && (
                                    <span className="text-xs bg-red-100 text-red-600 px-2 py-0.5 rounded">ADMIN</span>
                                )}
                            </div>
                            <Button onClick={onLogout} variant="outline" size="sm" className="flex items-center gap-2">
                                <LogOut className="w-4 h-4" />
                                Çıkış
                            </Button>
                        </div>
                    ) : null}
                </div>
            </div>
        </header>
    );
}