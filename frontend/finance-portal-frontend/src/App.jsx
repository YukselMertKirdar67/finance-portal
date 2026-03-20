import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Layout from './Components/Layout/Layout';
import LoginPage from './Components/Page/LoginPage';
import RegisterPage from './Components/Page/RegisterPage';
import ForgotPasswordPage from './Components/Page/ForgotPasswordPage';
import HomePage from './Components/Page/HomePage';
import NewsPage from './Components/Page/NewsPage';
import NewsDetailPage from './Components/Page/NewsDetailPage';
import InstrumentsPage from './Components/Page/InstrumentsPage';
import CategoryDetailPage from './Components/Page/CategoryDetailPage';
import InstrumentDetailPage from './Components/Page/InstrumentDetailPage';
import ComparisonPage from './Components/Page/ComparisonPage';
import WatchlistPage from './Components/Page/WatchlistPage';
import DashboardPage from './Components/Page/DashboardPage';
import PortfolioListPage from './Components/Page/PortfolioListPage';
import PortfolioPage from './Components/Page/PortfolioPage';
import TransactionPage from './Components/Page/TransactionPage';
import UserProfilePage from './Components/Page/UserProfilePage';
import AdminDashboard from './Components/Page/AdminDashboard';
import AdminUsersPage from './Components/Page/AdminUsersPage';
import AdminUserDetailPage from './Components/Page/AdminUserDetailPage';
import AuthCallbackPage from './Components/Page/AuthCallbackPage';

// Protected Route Component
const ProtectedRoute = ({ children, adminOnly = false }) => {
    const { authenticated, loading, isAdmin } = useAuth();

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="text-center">
                    <div className="w-16 h-16 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
                    <p className="text-gray-600">Yükleniyor...</p>
                </div>
            </div>
        );
    }

    if (!authenticated) {
        return <Navigate to="/login" replace />;
    }

    if (adminOnly && !isAdmin) {
        return <Navigate to="/home" replace />;
    }

    return children;
};

// App Routes Component
const AppRoutes = () => {
    const { authenticated, loading, logout, user } = useAuth();

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="w-16 h-16 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
                <div className="text-xl text-gray-600">Yükleniyor...</div>
            </div>
        );
    }

    return (
        <Routes>
            {/* PUBLIC ROUTES */}
            <Route
                path="/login"
                element={authenticated ? <Navigate to="/home" replace /> : <LoginPage />}
            />
            <Route
                path="/register"
                element={authenticated ? <Navigate to="/home" replace /> : <RegisterPage />}
            />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
            <Route path="/auth/callback" element={<AuthCallbackPage />} />

            {/* PROTECTED ROUTES - USER & ADMIN */}
            <Route
                path="/*"
                element={
                    <ProtectedRoute>
                        <Layout
                            isLoggedIn={authenticated}
                            onLogout={logout}
                            user={user}
                        >
                            <Routes>
                                {/* Root redirect */}
                                <Route path="/" element={<Navigate to="/home" replace />} />

                                {/* USER ROUTES */}
                                <Route path="/home" element={<HomePage />} />
                                <Route path="/news" element={<NewsPage />} />
                                <Route path="/news/detail/:id" element={<NewsDetailPage />} />
                                <Route path="/instruments" element={<InstrumentsPage />} />
                                <Route path="/instruments/:type" element={<CategoryDetailPage />} />
                                <Route path="/instruments/detail/:id" element={<InstrumentDetailPage />} />
                                <Route path="/comparison" element={<ComparisonPage />} />
                                <Route path="/watchlist" element={<WatchlistPage />} />
                                <Route path="/dashboard" element={<DashboardPage />} />
                                <Route path="/portfolios" element={<PortfolioListPage />} />
                                <Route path="/portfolios/:id" element={<PortfolioPage />} />
                                <Route path="/portfolios/:id/transactions" element={<TransactionPage />} />
                                <Route path="/profile" element={<UserProfilePage />} />

                                {/* ADMIN ROUTES */}
                                <Route
                                    path="/admin/dashboard"
                                    element={
                                        <ProtectedRoute adminOnly>
                                            <AdminDashboard />
                                        </ProtectedRoute>
                                    }
                                />
                                <Route
                                    path="/admin/users"
                                    element={
                                        <ProtectedRoute adminOnly>
                                            <AdminUsersPage />
                                        </ProtectedRoute>
                                    }
                                />

                                <Route
                                    path="/admin/users/:id"
                                    element={
                                        <ProtectedRoute adminOnly>
                                                <AdminUserDetailPage />
                                        </ProtectedRoute>
                                    }
                                />

                                {/* 404 Redirect */}
                                <Route path="*" element={<Navigate to="/home" replace />} />
                            </Routes>
                        </Layout>
                    </ProtectedRoute>
                }
            />
        </Routes>
    );
};

// Main App Component
function App() {
    return (
        <AuthProvider>
            <BrowserRouter>
                <AppRoutes />
            </BrowserRouter>
        </AuthProvider>
    );
}

export default App;