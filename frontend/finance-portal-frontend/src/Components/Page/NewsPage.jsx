import React, { useState, useEffect } from 'react';
import { Clock, Newspaper, RefreshCw } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { Card, CardContent } from '../UI/Card';
import { Button } from '../UI/Button';
import Pagination from '../UI/Pagination';
import { getAllNews, getNewsByCategory, fetchNewsFromAPI } from '../../API/newsApi';

export default function NewsPage() {
    const navigate = useNavigate();
    const { category: urlCategory } = useParams();

    const [selectedCategory, setSelectedCategory] = useState(urlCategory || 'all');
    const [news, setNews] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const pageSize = 20;

    const categories = [
        { id: 'all',      name: 'Tümü' },
        { id: 'FINANS',   name: 'Finans' },
        { id: 'DOVIZ',    name: 'Döviz' },
        { id: 'KRIPTO',   name: 'Kripto' },
        { id: 'BIRLESME', name: 'Birleşme' },
    ];

    useEffect(() => {
        let cancelled = false;

        const fetchNews = async () => {
            setLoading(true);
            setError(null);

            try {
                let response;
                if (selectedCategory === 'all') {
                    response = await getAllNews(currentPage, pageSize);
                } else {
                    response = await getNewsByCategory(selectedCategory, currentPage, pageSize);
                }

                if (cancelled) return;

                if (response && response.content !== undefined) {
                    setNews(response.content ?? []);
                    setTotalPages(response.totalPages ?? 0);
                    setTotalElements(response.totalElements ?? 0);
                } else if (Array.isArray(response)) {
                    setNews(response);
                    setTotalElements(response.length);
                    setTotalPages(Math.ceil(response.length / pageSize));
                } else {
                    setNews([]);
                    setTotalPages(0);
                    setTotalElements(0);
                }
            } catch (err) {
                if (cancelled) return;
                console.error('Load news error:', err);
                setNews([]);
                setTotalPages(0);
                setTotalElements(0);
                if (err.response?.status !== 404) {
                    setError('Haberler yüklenirken bir hata oluştu.');
                }
            } finally {
                if (!cancelled) setLoading(false);
            }
        };

        fetchNews();
        return () => { cancelled = true; };

    }, [selectedCategory, currentPage]);

    const handleFetchNews = async () => {
        setLoading(true);
        try {
            const result = await fetchNewsFromAPI();
            const saved = result.stats?.saved ?? result.totalSaved ?? 0;
            alert(`Başarılı! ${saved} haber kaydedildi.`);
            setSelectedCategory('all');
            setCurrentPage(0);
            navigate('/news');
        } catch (err) {
            console.error('Fetch news error:', err);
            alert('Haberler çekilirken bir hata oluştu.');
            setLoading(false);
        }
    };

    const handleCategoryChange = (categoryId) => {
        if (categoryId === selectedCategory) return;
        setCurrentPage(0);
        setSelectedCategory(categoryId);
    };

    const handlePageChange = (page) => {
        if (page === currentPage) return;
        if (page < 0 || page >= totalPages) return;
        setCurrentPage(page);
        window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    const handleNewsClick = (id) => {
        navigate(`/news/detail/${id}`);
    };

    const formatTime = (dateString) => {
        if (!dateString) return 'Bilinmiyor';
        const date = new Date(dateString);
        const now = new Date();
        const diffHours = Math.floor((now - date) / (1000 * 60 * 60));
        if (diffHours < 1) return 'Az önce';
        if (diffHours < 24) return `${diffHours} saat önce`;
        return `${Math.floor(diffHours / 24)} gün önce`;
    };

    return (
        <div className="min-h-screen bg-gray-50 p-8">
            <div className="max-w-7xl mx-auto">

                {/* Header */}
                <div className="mb-6 flex justify-between items-center">
                    <div>
                        <h1 className="text-3xl font-bold mb-2">Haberler</h1>
                        <p className="text-gray-600">Finansal piyasalardan son dakika haberleri</p>
                    </div>
                    <Button onClick={handleFetchNews} disabled={loading}>
                        <RefreshCw className={`w-4 h-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
                        Haberleri Güncelle
                    </Button>
                </div>

                {/* Category Filter */}
                <div className="mb-6 flex gap-2 flex-wrap">
                    {categories.map((category) => (
                        <Button
                            key={category.id}
                            variant={selectedCategory === category.id ? 'default' : 'outline'}
                            onClick={() => handleCategoryChange(category.id)}
                            disabled={loading}
                        >
                            {category.name}
                        </Button>
                    ))}
                </div>

                {/* Loading */}
                {loading && (
                    <div className="text-center py-12">
                        <RefreshCw className="w-8 h-8 animate-spin mx-auto text-blue-600" />
                        <p className="mt-4 text-gray-600">Haberler yükleniyor...</p>
                    </div>
                )}

                {/* Error */}
                {!loading && error && (
                    <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-6">
                        {error}
                    </div>
                )}

                {/* News Grid */}
                {!loading && !error && (
                    <>
                        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
                            {news.length === 0 ? (
                                <div className="col-span-2 text-center py-12 text-gray-500">
                                    Henüz haber bulunmuyor. "Haberleri Güncelle" butonuna tıklayın.
                                </div>
                            ) : (
                                news.map((item) => (
                                    <Card
                                        key={item.id}
                                        className="hover:shadow-lg transition-shadow cursor-pointer overflow-hidden"
                                        onClick={() => handleNewsClick(item.id)}
                                    >
                                        <div
                                            className="h-48 bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center"
                                            style={item.imageUrl ? {
                                                backgroundImage: `url(${item.imageUrl})`,
                                                backgroundSize: 'cover',
                                                backgroundPosition: 'center'
                                            } : {}}
                                        >
                                            {!item.imageUrl && <Newspaper className="w-16 h-16 text-white opacity-50" />}
                                        </div>

                                        <CardContent className="pt-6">
                                            <div className="flex items-center gap-2 mb-3">
                                                <span className="px-2 py-1 bg-blue-100 text-blue-700 rounded text-xs font-medium">
                                                    {item.category || 'Genel'}
                                                </span>
                                                <div className="flex items-center gap-1 text-sm text-gray-500">
                                                    <Clock className="w-4 h-4" />
                                                    <span>{formatTime(item.publishDate)}</span>
                                                </div>
                                            </div>

                                            <h3 className="text-xl font-semibold text-gray-900 mb-3 line-clamp-2">
                                                {item.title}
                                            </h3>

                                            <p className="text-gray-600 mb-4 line-clamp-3">
                                                {item.content}
                                            </p>

                                            <div className="flex items-center justify-between">
                                                <span className="text-sm text-gray-500">
                                                    {item.source || 'Kaynak Belirtilmemiş'}
                                                </span>
                                                <Button variant="ghost" size="sm">
                                                    Devamını Oku
                                                </Button>
                                            </div>
                                        </CardContent>
                                    </Card>
                                ))
                            )}
                        </div>

                        {news.length > 0 && totalPages > 1 && (
                            <Pagination
                                currentPage={currentPage}
                                totalPages={totalPages}
                                totalElements={totalElements}
                                pageSize={pageSize}
                                onPageChange={handlePageChange}
                            />
                        )}
                    </>
                )}
            </div>
        </div>
    );
}