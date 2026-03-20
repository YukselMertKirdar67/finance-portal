import React, { useEffect } from 'react';
import api from '../../API/instrumentsApi';

const AuthCallbackPage = () => {
    useEffect(() => {
        const handleCallback = async () => {
            // URL'den authorization code al
            const urlParams = new URLSearchParams(window.location.search);
            const code = urlParams.get('code');

            if (!code) {
                console.error('❌ No authorization code found');

                if (window.opener) {
                    window.close();
                } else {
                    window.location.href = '/login';
                }
                return;
            }

            try {
                console.log('🔄 Exchanging code for token...');

                // Backend'e code gönder, token al
                const response = await api.post('/auth/token-exchange', { code });

                if (response.data.success) {
                    const { accessToken, refreshToken, username, email, roles } = response.data;

                    console.log('✅ Token exchange successful');

                    // Parent window'a token gönder (postMessage)
                    if (window.opener) {
                        window.opener.postMessage({
                            type: 'KEYCLOAK_AUTH_SUCCESS',
                            accessToken,
                            refreshToken,
                            username,
                            email,
                            roles
                        }, window.location.origin);

                        console.log('✅ Token sent to parent window');

                        // Popup'ı kapat
                        setTimeout(() => {
                            window.close();
                        }, 500);

                    } else {
                        // Popup değil, normal window - direkt login
                        console.log('⚠️ Not a popup, using direct storage');

                        localStorage.setItem('token', accessToken);
                        localStorage.setItem('refreshToken', refreshToken);
                        localStorage.setItem('user', JSON.stringify({ username, email, roles }));

                        window.location.href = '/home';
                    }
                }

            } catch (error) {
                console.error('❌ Token exchange failed:', error);

                if (window.opener) {
                    window.close();
                } else {
                    window.location.href = '/login';
                }
            }
        };

        handleCallback();
    }, []);  //  navigate dependency kaldırıldı

    return (
        <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100">
            <div className="text-center">
                <div className="w-16 h-16 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
                <p className="text-gray-600 font-medium">Authenticating...</p>
                <p className="text-gray-500 text-sm mt-2">Please wait</p>
            </div>
        </div>
    );
};

export default AuthCallbackPage;