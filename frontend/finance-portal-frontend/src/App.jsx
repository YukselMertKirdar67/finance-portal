import React, { useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './Components/Layout/Layout';
import NewsPage from './Components/Page/NewsPage';
import NewsDetailPage from './Components/Page/NewsDetailPage';
import InstrumentsPage from './Components/Page/InstrumentsPage';
import CategoryDetailPage from './Components/Page/CategoryDetailPage';
import InstrumentDetailPage from './Components/Page/InstrumentDetailPage';

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
                    <Route path="/" element={<Navigate to="/news" replace />} />
                    <Route path="/news" element={<NewsPage />} />
                    <Route path="/news/detail/:id" element={<NewsDetailPage />} />
                    <Route path="/home" element={<div className="p-8">Anasayfa - Yakında</div>} />
                    <Route path="/instruments" element={<InstrumentsPage />} />
                    <Route path="/instruments/:type" element={<CategoryDetailPage />} />
                    <Route path="/instruments/detail/:id" element={<InstrumentDetailPage />} />
                    <Route path="/compare" element={<div className="p-8">Karşılaştır</div>} />
                    <Route path="/portfolio" element={<div className="p-8">Portföyüm</div>} />
                    <Route path="/watchlist" element={<div className="p-8">Takip Listesi</div>} />
                </Routes>
            </Layout>
        </BrowserRouter>
    );
}

export default App;