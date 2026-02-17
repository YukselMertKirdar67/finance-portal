import React from 'react';
import Sidebar from './Sidebar';
import Header from './Header';

export default function Layout({
                                   children,
                                   isLoggedIn = false,
                                   onLogin,
                                   onLogout,
                                   onRegister
                               }) {
    return (
        <div className="flex h-screen bg-gray-50">

            {/* SIDEBAR */}
            <Sidebar
                isLoggedIn={isLoggedIn}
                onLogin={onLogin}
                onLogout={onLogout}
                onRegister={onRegister}
            />

            {/* MAIN CONTENT */}
            <div className="flex-1 flex flex-col overflow-hidden">

                {/* HEADER */}
                <Header
                    isLoggedIn={isLoggedIn}
                    onLogin={onLogin}
                    onRegister={onRegister}
                />

                {/* PAGE CONTENT */}
                <main className="flex-1 overflow-y-auto">
                    {children}
                </main>
            </div>
        </div>
    );
}