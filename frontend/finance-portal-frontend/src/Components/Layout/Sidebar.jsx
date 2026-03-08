import React from 'react';
import { TrendingUp, Star, Settings, LogOut } from 'lucide-react';
import { NavLink, useNavigate } from 'react-router-dom';
import { Button } from '../UI/Button';
import { Avatar, AvatarFallback } from '../UI/Avatar';

export default function Sidebar({
                                    isLoggedIn = false,
                                    onLogout,
                                    onLogin,
                                    onRegister
                                }) {
    const navigate = useNavigate();

    const handleProtectedNavigation = (path) => {
        if (!isLoggedIn && (path === '/portfolio' || path === '/watchlist')) {
            alert('Bu sayfayı görüntülemek için giriş yapmalısınız!');
            return;
        }
        navigate(path);
    };

    const navClass = ({ isActive }) =>
        `w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${
            isActive
                ? 'bg-blue-50 text-blue-600'
                : 'text-gray-700 hover:bg-gray-50'
        }`;

    return (
        <div className="w-64 bg-white border-r border-gray-200 flex flex-col h-screen sticky top-0">

            {/* LOGO */}
            <div className="p-6 border-b border-gray-200">
                <NavLink to="/news" className="flex items-center gap-2">
                    <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center">
                        <TrendingUp className="w-5 h-5 text-white" />
                    </div>
                    <span className="text-xl font-semibold text-gray-900">FinansApp</span>
                </NavLink>
            </div>

            {/* NAV */}
            <nav className="flex-1 p-4">
                <ul className="space-y-1">
                    <li>
                        <button
                            onClick={() => handleProtectedNavigation('/dashboard')}
                            className={navClass({ isActive: location.pathname === '/dashboard' })}
                        >
                            <Star className="w-5 h-5" />
                            <span>Dashboard</span>
                        </button>
                    </li>
                    <li>
                        <button
                            onClick={() => handleProtectedNavigation('/portfolio')}
                            className={navClass({ isActive: location.pathname === '/portfolio' })}
                        >
                            <TrendingUp className="w-5 h-5" />
                            <span>Portföyüm</span>
                        </button>
                    </li>

                    <li>
                        <button
                            onClick={() => handleProtectedNavigation('/watchlist')}
                            className={navClass({ isActive: location.pathname === '/watchlist' })}
                        >
                            <Star className="w-5 h-5" />
                            <span>Takip Listesi</span>
                        </button>
                    </li>
                </ul>
            </nav>

            {/* USER */}
            <div className="p-4 border-t border-gray-200">
                {isLoggedIn ? (
                    <>
                        <div className="flex items-center gap-3 mb-3">
                            <Avatar className="w-10 h-10">
                                <AvatarFallback className="bg-blue-600 text-white">
                                    YK
                                </AvatarFallback>
                            </Avatar>
                            <div className="flex-1 min-w-0">
                                <p className="text-sm font-medium text-gray-900 truncate">Kullanıcı</p>
                                <p className="text-xs text-gray-500 truncate">user@email.com</p>
                            </div>
                        </div>

                        <div className="flex gap-2">
                            <Button variant="outline" size="sm" className="flex-1">
                                <Settings className="w-4 h-4 mr-1" />
                                Ayarlar
                            </Button>
                            <Button variant="outline" size="sm" onClick={onLogout}>
                                <LogOut className="w-4 h-4" />
                            </Button>
                        </div>
                    </>
                ) : (
                    <div className="space-y-2">
                        <Button
                            onClick={onLogin}
                            className="w-full bg-blue-600 hover:bg-blue-700 text-white"
                        >
                            Giriş Yap
                        </Button>
                        <Button
                            onClick={onRegister}
                            variant="outline"
                            className="w-full"
                        >
                            Kayıt Ol
                        </Button>
                    </div>
                )}
            </div>
        </div>
    );
}