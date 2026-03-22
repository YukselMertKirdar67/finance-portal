import React from 'react';
import {
    TrendingUp,
    Star,
    Settings,
    LogOut,
    LayoutDashboard,
    Briefcase,
    Users,
    User,
    Home,
    RefreshCw
} from 'lucide-react';
import { NavLink } from 'react-router-dom';
import { Button } from '../UI/Button';
import { Avatar, AvatarFallback } from '../UI/Avatar';

export default function Sidebar({
                                    isLoggedIn = false,
                                    onLogout,
                                    user = null
                                }) {

    const navClass = ({ isActive }) =>
        `w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${
            isActive
                ? 'bg-blue-50 text-blue-600'
                : 'text-gray-700 hover:bg-gray-50'
        }`;

    // User initials for avatar
    const getUserInitials = () => {
        if (!user?.username) return 'U';
        return user.username.substring(0, 2).toUpperCase();
    };

    return (
        <div className="w-64 bg-white border-r border-gray-200 flex flex-col h-screen sticky top-0">

            {/* LOGO */}
            <div className="p-6 border-b border-gray-200">
                <NavLink to="/home" className="flex items-center gap-2">
                    <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center">
                        <TrendingUp className="w-5 h-5 text-white" />
                    </div>
                    <span className="text-xl font-semibold text-gray-900">FinansApp</span>
                </NavLink>
            </div>

            {/* NAV */}
            <nav className="flex-1 p-4 overflow-y-auto">
                <ul className="space-y-1">
                    {/* Home */}
                    <li>
                        <NavLink to="/home" className={navClass}>
                            <Home className="w-5 h-5" />
                            <span>Ana Sayfa</span>
                        </NavLink>
                    </li>

                    {/* Dashboard */}
                    <li>
                        <NavLink to="/dashboard" className={navClass}>
                            <LayoutDashboard className="w-5 h-5" />
                            <span>Dashboard</span>
                        </NavLink>
                    </li>

                    {/* Portfolios */}
                    <li>
                        <NavLink to="/portfolios" className={navClass}>
                            <Briefcase className="w-5 h-5" />
                            <span>Portföylerim</span>
                        </NavLink>
                    </li>

                    {/* Watchlist */}
                    <li>
                        <NavLink to="/watchlist" className={navClass}>
                            <Star className="w-5 h-5" />
                            <span>Takip Listesi</span>
                        </NavLink>
                    </li>

                    <li>
                        <NavLink to="/settings" className={navClass}>
                            <Settings className="w-5 h-5" />
                            <span>Ayarlar</span>
                        </NavLink>
                    </li>

                    {/* ADMIN MENU */}
                    {user?.isAdmin && (
                        <>
                            <li className="pt-4 mt-4 border-t border-gray-200">
                                <p className="px-4 text-xs font-semibold text-gray-500 uppercase mb-2">
                                    Admin
                                </p>
                            </li>
                            <li>
                                <NavLink to="/admin/dashboard" className={navClass}>
                                    <LayoutDashboard className="w-5 h-5" />
                                    <span>Admin Dashboard</span>
                                </NavLink>
                            </li>
                            <li>
                                <NavLink to="/admin/users" className={navClass}>
                                    <Users className="w-5 h-5" />
                                    <span>Kullanıcı Yönetimi</span>
                                </NavLink>
                            </li>
                            {/* ⭐ YENİ: Fiyat Güncellemesi */}
                            <li>
                                <NavLink to="/admin/instruments" className={navClass}>
                                    <RefreshCw className="w-5 h-5" />
                                    <span>Fiyat Güncellemesi</span>
                                </NavLink>
                            </li>
                        </>
                    )}
                </ul>
            </nav>

            {/* USER */}
            <div className="p-4 border-t border-gray-200">
                {isLoggedIn && user ? (
                    <>
                        <div className="flex items-center gap-3 mb-3">
                            <Avatar className="w-10 h-10">
                                <AvatarFallback className="bg-blue-600 text-white font-semibold">
                                    {getUserInitials()}
                                </AvatarFallback>
                            </Avatar>
                            <div className="flex-1 min-w-0">
                                <p className="text-sm font-medium text-gray-900 truncate">
                                    {user.username}
                                </p>
                                <p className="text-xs text-gray-500 truncate">
                                    {user.email}
                                </p>
                                {user.isAdmin && (
                                    <span className="inline-block text-xs bg-red-100 text-red-600 px-2 py-0.5 rounded mt-1">
                                        ADMIN
                                    </span>
                                )}
                            </div>
                        </div>

                        <div className="flex flex-col gap-2">
                            {/* Profile Button */}
                            <NavLink to="/profile">
                                <Button variant="outline" size="sm" className="w-full">
                                    <User className="w-4 h-4 mr-2" />
                                    Profilim
                                </Button>
                            </NavLink>

                            {/* Logout Button */}
                            <Button
                                variant="outline"
                                size="sm"
                                onClick={onLogout}
                                className="w-full text-red-600 hover:bg-red-50 hover:border-red-200"
                            >
                                <LogOut className="w-4 h-4 mr-2" />
                                Çıkış Yap
                            </Button>
                        </div>
                    </>
                ) : null}
            </div>
        </div>
    );
}