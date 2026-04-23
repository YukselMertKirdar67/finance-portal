import React from 'react';
import { LayoutDashboard, Newspaper, Home, GitCompare, Bell, LogOut } from 'lucide-react';
import { NavLink } from 'react-router-dom';
import { Button } from '../UI/Button';

export default function Header({
                                   isLoggedIn = false,
                                   onLogout,
                                   user = null
                               }) {

    const tabClass = ({ isActive }) =>
        `px-4 py-2 rounded-lg transition-colors flex items-center gap-2 ${
            isActive
                ? 'bg-blue-600 text-white'
                : 'text-gray-700 hover:bg-gray-100'
        }`;

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
                    <Button variant="ghost" size="icon">
                        <Bell className="w-5 h-5 text-gray-600" />
                    </Button>

                    {isLoggedIn && user ? (
                        <div className="flex items-center gap-3">
                            {/* User Info */}
                            <div className="flex flex-col items-end">
                                <span className="text-sm font-medium text-gray-900">
                                    {user.username}
                                </span>
                                {user.isAdmin && (
                                    <span className="text-xs bg-red-100 text-red-600 px-2 py-0.5 rounded">
                                        ADMIN
                                    </span>
                                )}
                            </div>

                            {/* Logout Button */}
                            <Button
                                onClick={onLogout}
                                variant="outline"
                                size="sm"
                                className="flex items-center gap-2"
                            >
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