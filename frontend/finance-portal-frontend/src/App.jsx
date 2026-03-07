import React, { useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './Components/Layout/Layout';
import NewsPage from './Components/Page/NewsPage';
import NewsDetailPage from './Components/Page/NewsDetailPage';
import InstrumentsPage from './Components/Page/InstrumentsPage';
import CategoryDetailPage from './Components/Page/CategoryDetailPage';
import InstrumentDetailPage from './Components/Page/InstrumentDetailPage';
import ComparisonPage from './Components/Page/ComparisonPage';
import HomePage from './Components/Page/HomePage';
import WatchlistPage from './Components/Page/WatchlistPage';
import PortfolioPage from './Components/Page/PortfolioPage';

function App() {
    const [isLoggedIn, setIsLoggedIn] = useState(false);

    return (
        <BrowserRouter>
            <Layout
                isLoggedIn={isLoggedIn}
                onLogin={() => setIsLoggedIn(true)}
                onLogout={() => setIsLoggedIn(false)}
                onRegister={() => alert('Kayıt sayfası yakında!')}
            >
                <Routes>
                    <Route path="/" element={<Navigate to="/home" replace />} />
                    <Route path="/news" element={<NewsPage />} />
                    <Route path="/news/detail/:id" element={<NewsDetailPage />} />
                    <Route path="/home" element={<HomePage/> } />
                    <Route path="/instruments" element={<InstrumentsPage />} />
                    <Route path="/instruments/:type" element={<CategoryDetailPage />} />
                    <Route path="/instruments/detail/:id" element={<InstrumentDetailPage />} />
                    <Route path="/comparison" element={<ComparisonPage />} />
                    <Route path="/portfolio" element={< PortfolioPage/>} />
                    <Route path="/watchlist" element={<WatchlistPage/>} />
                </Routes>
            </Layout>
        </BrowserRouter>
    );
}

export default App;