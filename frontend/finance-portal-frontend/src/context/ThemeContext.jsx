/* eslint-disable react-refresh/only-export-components */
/* eslint-disable react-hooks/exhaustive-deps */
import React, { createContext, useContext, useState, useLayoutEffect } from 'react';
import { useAuth } from './AuthContext';

const ThemeContext = createContext();

export const ThemeProvider = ({ children }) => {
    const { user } = useAuth();

    const getInitialTheme = () => {
        return localStorage.getItem('theme') || 'light';
    };

    const [theme, setTheme] = useState(getInitialTheme);

    // Tema DOM'a uygula
    useLayoutEffect(() => {
        if (theme === 'dark') {
            document.documentElement.classList.add('dark');
        } else {
            document.documentElement.classList.remove('dark');
        }
        localStorage.setItem('theme', theme);
    }, [theme]);

    // User sync
    useLayoutEffect(() => {
        if (user?.theme) {
            // Giriş yapıldı, DB'deki temayı uygula
            setTheme(user.theme);
            localStorage.setItem('theme', user.theme);
        }
    }, [user?.theme]);

   // Çıkış yapılınca light'a dön
    useLayoutEffect(() => {
        if (!user) {
            setTheme('light');
            localStorage.setItem('theme', 'light');
            document.documentElement.classList.remove('dark');
        }
    }, [user]);

    const applyTheme = (newTheme) => {
        setTheme(newTheme);
    };

    const toggleTheme = () => {
        const newTheme = theme === 'light' ? 'dark' : 'light';
        setTheme(newTheme);
    };

    return (
        <ThemeContext.Provider value={{ theme, setTheme: applyTheme, toggleTheme }}>
            {children}
        </ThemeContext.Provider>
    );
};

export const useTheme = () => {
    const context = useContext(ThemeContext);
    if (!context) {
        throw new Error('useTheme must be used within ThemeProvider');
    }
    return context;
};