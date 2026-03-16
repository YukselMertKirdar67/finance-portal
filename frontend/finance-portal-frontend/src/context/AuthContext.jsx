/* eslint-disable react-refresh/only-export-components */
import React, { createContext, useContext, useState } from 'react';
import { logoutUser } from '../API/authApi';

const AuthContext = createContext();

// localStorage'dan initial state'i oku (useEffect yok!)
const getInitialAuthState = () => {
    const token = localStorage.getItem('token');
    const userStr = localStorage.getItem('user');

    if (token && userStr) {
        try {
            const userData = JSON.parse(userStr);
            return {
                authenticated: true,
                user: {
                    ...userData,
                    isAdmin: userData.roles?.includes('ADMIN')
                },
                loading: false
            };
        } catch (error) {
            console.error('Error parsing user data:', error);
            localStorage.removeItem('token');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('user');
        }
    }

    return {
        authenticated: false,
        user: null,
        loading: false
    };
};

export const AuthProvider = ({ children }) => {
    // Lazy initialization - sadece ilk render'da çalışır
    const [authState, setAuthState] = useState(getInitialAuthState);

    const login = (tokenData) => {
        localStorage.setItem('token', tokenData.accessToken);
        localStorage.setItem('refreshToken', tokenData.refreshToken);
        localStorage.setItem('user', JSON.stringify({
            username: tokenData.username,
            email: tokenData.email,
            roles: tokenData.roles
        }));

        setAuthState({
            authenticated: true,
            user: {
                username: tokenData.username,
                email: tokenData.email,
                roles: tokenData.roles,
                isAdmin: tokenData.roles?.includes('ADMIN')
            },
            loading: false
        });
    };

    const logout = async () => {
        try {
            const refreshToken = localStorage.getItem('refreshToken');

            // Backend'e logout isteği at (Keycloak session sonlandır)
            if (refreshToken) {
                console.log('🚪 Terminating Keycloak session...');
                await logoutUser(refreshToken);
                console.log('✅ Keycloak session terminated');
            }

        } catch (error) {
            console.error('❌ Logout error:', error);
            // Hata olsa bile devam et
        } finally {
            // Her durumda localStorage temizle ve login'e yönlendir
            console.log('🧹 Cleaning up local storage...');
            localStorage.removeItem('token');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('user');

            setAuthState({
                authenticated: false,
                user: null,
                loading: false
            });

            window.location.href = '/login';
        }
    };

    const hasRole = (role) => {
        return authState.user?.roles?.includes(role) || false;
    };

    const value = {
        authenticated: authState.authenticated,
        loading: authState.loading,
        user: authState.user,
        login,
        logout,
        hasRole,
        isAdmin: authState.user?.roles?.includes('ADMIN') || false,
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within AuthProvider');
    }
    return context;
};