/* eslint-disable react-refresh/only-export-components */
import React, { createContext, useContext, useState, useEffect } from 'react';
import { logoutUser } from '../API/authApi';
import api from '../API/instrumentsApi';

const AuthContext = createContext();

// Storage helper (rememberMe'ye göre localStorage veya sessionStorage)
const getStorage = (rememberMe) => {
    return rememberMe ? localStorage : sessionStorage;
};

// Initial state'i oku (sessionStorage öncelikli, sonra localStorage)
const getInitialAuthState = () => {
    // Önce sessionStorage kontrol et (Remember Me kapalı ise burda olur)
    let token = sessionStorage.getItem('token');
    let userStr = sessionStorage.getItem('user');

    // sessionStorage'da yoksa localStorage'a bak (Remember Me açık ise burda olur)
    if (!token) {
        token = localStorage.getItem('token');
        userStr = localStorage.getItem('user');
    }

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
            console.error('❌ Error parsing user data:', error);
            // Hata varsa tüm storage'ı temizle
            localStorage.clear();
            sessionStorage.clear();
        }
    }

    return {
        authenticated: false,
        user: null,
        loading: false
    };
};

export const AuthProvider = ({ children }) => {
    const [authState, setAuthState] = useState(getInitialAuthState);

    // Component mount olduğunda user bilgilerini API'den çek
    useEffect(() => {
        const fetchUserProfile = async () => {
            const token = sessionStorage.getItem('token') || localStorage.getItem('token');

            if (token && authState.authenticated) {
                try {
                    const response = await api.get('/me/profile');
                    setAuthState(prev => ({
                        ...prev,
                        user: {
                            ...response.data,
                            isAdmin: response.data.roles?.includes('ADMIN')
                        }
                    }));
                } catch (error) {
                    console.error('Failed to fetch user profile:', error);
                }
            }
        };

        fetchUserProfile();
    }, []);

    const login = async (tokenData, rememberMe = false) => {
        const storage = getStorage(rememberMe);

        // Token'ları seçilen storage'a kaydet
        storage.setItem('token', tokenData.accessToken);
        storage.setItem('refreshToken', tokenData.refreshToken);
        storage.setItem('user', JSON.stringify({
            username: tokenData.username,
            email: tokenData.email,
            roles: tokenData.roles
        }));

        if (!rememberMe) {
            localStorage.removeItem('token');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('user');
        }

        // API'den güncel user bilgilerini çek
        try {
            const response = await api.get('/me/profile');
            setAuthState({
                authenticated: true,
                user: {
                    ...response.data,
                    isAdmin: response.data.roles?.includes('ADMIN')
                },
                loading: false
            });
        } catch (error) {
            console.error('Failed to fetch user profile after login:', error);
            // Fallback: tokenData'yı kullan
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
        }

        console.log(`✅ Login successful (Remember Me: ${rememberMe ? 'ON' : 'OFF'})`);
    };

    const logout = async () => {
        try {
            // Refresh token'ı bul (sessionStorage veya localStorage'dan)
            const refreshToken = sessionStorage.getItem('refreshToken') || localStorage.getItem('refreshToken');

            if (refreshToken) {
                console.log('🚪 Terminating Keycloak session...');
                await logoutUser(refreshToken);
                console.log('✅ Keycloak session terminated');
            }

        } catch (error) {
            console.error('❌ Logout error:', error);
            // Hata olsa bile devam et (logout tamamlanmalı)
        } finally {
            console.log('🧹 Cleaning up storage...');

            // Her iki storage'ı da temizle
            localStorage.removeItem('token');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('user');
            sessionStorage.removeItem('token');
            sessionStorage.removeItem('refreshToken');
            sessionStorage.removeItem('user');

            // Auth state'i sıfırla
            setAuthState({
                authenticated: false,
                user: null,
                loading: false
            });

            // Login sayfasına yönlendir
            window.location.href = '/login';
        }
    };

    // User bilgilerini refresh et
    const refreshUser = async () => {
        try {
            const response = await api.get('/me/profile');
            setAuthState(prev => ({
                ...prev,
                user: {
                    ...response.data,
                    isAdmin: response.data.roles?.includes('ADMIN')
                }
            }));
        } catch (error) {
            console.error('Failed to refresh user:', error);
            throw error;
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
        refreshUser,
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