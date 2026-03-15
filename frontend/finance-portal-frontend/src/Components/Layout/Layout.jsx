import React, { useState, useEffect } from 'react';
import Sidebar from './Sidebar';
import Header from './Header';
import EmailVerificationBanner from '../UI/EmailVerificationBanner';
import api from '../../API/instrumentsApi';

export default function Layout({
                                   children,
                                   isLoggedIn = false,
                                   onLogout,
                                   user = null
                               }) {
    const [showBanner, setShowBanner] = useState(false);
    const [emailVerified, setEmailVerified] = useState(true);

    useEffect(() => {
        const checkEmailVerification = async () => {
            if (!user?.email) return;

            try {
                const response = await api.get(`/auth/check-email-verification?email=${user.email}`);
                setEmailVerified(response.data.emailVerified);
                setShowBanner(!response.data.emailVerified);
            } catch (err) {
                console.error('Error checking email verification:', err);
            }
        };

        if (isLoggedIn && user?.email) {
            checkEmailVerification();
        }
    }, [isLoggedIn, user?.email]);

    return (
        <div className="flex h-screen bg-gray-50">

            {/* SIDEBAR */}
            <Sidebar
                isLoggedIn={isLoggedIn}
                onLogout={onLogout}
                user={user}
            />

            {/* MAIN CONTENT */}
            <div className="flex-1 flex flex-col overflow-hidden">

                {/* HEADER */}
                <Header
                    isLoggedIn={isLoggedIn}
                    onLogout={onLogout}
                    user={user}
                />

                {/* EMAIL VERIFICATION BANNER */}
                {showBanner && !emailVerified && (
                    <EmailVerificationBanner
                        email={user?.email}
                        onClose={() => setShowBanner(false)}
                    />
                )}

                {/* PAGE CONTENT */}
                <main className="flex-1 overflow-y-auto">
                    {children}
                </main>
            </div>
        </div>
    );
}