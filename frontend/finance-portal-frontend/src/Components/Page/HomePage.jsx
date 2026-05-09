import React, { useState, useEffect } from 'react';
import { TrendingUp, TrendingDown, ArrowRight, DollarSign, Gem, Briefcase, Globe, FileText, Bitcoin } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../UI/Card';
import { Button } from '../UI/Button';
import { useNavigate } from 'react-router-dom';
import { getHomePageData } from '../../API/homeApi';
import { useWebSocket } from '../../Hooks/useWebSocket';


export default function HomePage() {
    const navigate = useNavigate();
    const [homeData, setHomeData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [livePrices, setLivePrices] = useState({});

    useEffect(() => {
        fetchHomeData();
    }, []);

    useWebSocket((priceUpdate) => {
        setLivePrices(prev => ({
            ...prev,
            [priceUpdate.instrumentId]: priceUpdate
        }));
    });


    const fetchHomeData = async () => {
        try {
            setLoading(true);
            const data = await getHomePageData();
            setHomeData(data);
        } catch (err) {
            console.error('Home data fetch error:', err);
            setError('Veri yüklenirken hata oluştu');
        } finally {
            setLoading(false);
        }
    };

    const handleNavigateToDetail = (id) => {
        navigate(`/instruments/detail/${id}`);
    };

    const getCategoryIcon = (iconName) => {
        const icons = {
            'DollarSign': DollarSign,
            'TrendingUp': TrendingUp,
            'Bitcoin': Bitcoin,
            'FileText': FileText,
            'Globe': Globe,
            'Gem': Gem,
            'Briefcase': Briefcase
        };
        return icons[iconName] || DollarSign;
    };

    if (loading) {
        return (
            <div className="min-h-screen bg-gray-50 flex items-center justify-center">
                <div className="text-center">
                    <div className="w-16 h-16 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
                    <p className="text-gray-600">Yükleniyor...</p>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="min-h-screen bg-gray-50 flex items-center justify-center">
                <div className="text-center">
                    <p className="text-red-600 mb-4">{error}</p>
                    <Button onClick={fetchHomeData}>Tekrar Dene</Button>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gray-50 p-8">
            <div className="max-w-7xl mx-auto">
                {/* Header */}
                <div className="mb-8">
                    <h1 className="text-4xl font-bold text-gray-900 mb-2">Anasayfa</h1>
                    <p className="text-gray-600">Piyasalara genel bakış ve güncel gelişmeler</p>
                </div>

                {/* Market Overview */}
                {homeData?.marketOverview && homeData.marketOverview.length > 0 && (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                        {homeData.marketOverview.map((item) => (
                            <Card
                                key={item.id}
                                className="cursor-pointer hover:shadow-lg transition-all border-0"
                                onClick={() => handleNavigateToDetail(item.id)}
                            >
                                <CardContent className="pt-6">
                                    <div className="flex justify-between items-start mb-2">
                                        <div>
                                            <p className="text-sm text-gray-600 mb-1">{item.name}</p>
                                            <p className="text-2xl font-bold text-gray-900">
                                                {livePrices[item.id]
                                                    ? livePrices[item.id].currentPrice
                                                    : item.currentPrice}
                                            </p>
                                        </div>
                                    </div>
                                    <div className={`flex items-center gap-1 ${
                                        item.isPositive ? 'text-emerald-600' : 'text-red-500'
                                    }`}>
                                        {item.isPositive ? (
                                            <TrendingUp className="w-4 h-4" />
                                        ) : (
                                            <TrendingDown className="w-4 h-4" />
                                        )}
                                        <span className="text-sm font-semibold">
                                            {item.change} ({item.changePercent})
                                        </span>
                                    </div>
                                </CardContent>
                            </Card>
                        ))}
                    </div>
                )}

                {/* Market Stats */}
                {homeData?.marketStats && (
                    <Card className="mb-8 border-0 shadow-sm">
                        <CardHeader>
                            <CardTitle>Piyasa Durumu</CardTitle>
                        </CardHeader>
                        <CardContent>
                            <div className="grid grid-cols-3 gap-6">
                                <div className="text-center">
                                    <div className="text-3xl font-bold text-emerald-600 mb-1">
                                        {homeData.marketStats.rising}
                                    </div>
                                    <p className="text-sm text-gray-600">Yükseliş</p>
                                </div>
                                <div className="text-center">
                                    <div className="text-3xl font-bold text-red-500 mb-1">
                                        {homeData.marketStats.falling}
                                    </div>
                                    <p className="text-sm text-gray-600">Düşüş</p>
                                </div>
                                <div className="text-center">
                                    <div className="text-3xl font-bold text-gray-500 mb-1">
                                        {homeData.marketStats.unchanged}
                                    </div>
                                    <p className="text-sm text-gray-600">Değişmedi</p>
                                </div>
                            </div>
                        </CardContent>
                    </Card>
                )}

                {/* Categories */}
                {homeData?.categories && homeData.categories.length > 0 && (
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
                        {homeData.categories.map((category) => {
                            const IconComponent = getCategoryIcon(category.iconName);
                            return (
                                <Card
                                    key={category.type}
                                    className="cursor-pointer hover:shadow-lg transition-all border-0"
                                    onClick={() => navigate(`/instruments/${category.type}`)}
                                >
                                    <CardContent className="pt-6 text-center">
                                        <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-3">
                                            <IconComponent className="w-6 h-6 text-blue-600" />
                                        </div>
                                        <p className="font-semibold text-gray-900 mb-1">{category.displayName}</p>
                                        <p className="text-sm text-gray-600">{category.count} enstrüman</p>
                                    </CardContent>
                                </Card>
                            );
                        })}
                    </div>
                )}

                {/* Top Gainers & Losers */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
                    {/* Top Gainers */}
                    <Card className="border-0 shadow-sm">
                        <CardHeader className="flex flex-row items-center justify-between">
                            <CardTitle className="text-emerald-600 flex items-center gap-2">
                                <TrendingUp className="w-5 h-5" />
                                En Çok Kazananlar
                            </CardTitle>
                        </CardHeader>
                        <CardContent>
                            {homeData?.topGainers && homeData.topGainers.length > 0 ? (
                                <div className="space-y-2">
                                    {homeData.topGainers.map((item) => (
                                        <button
                                            key={item.id}
                                            onClick={() => handleNavigateToDetail(item.id)}
                                            className="w-full flex items-center justify-between p-3 hover:bg-gray-50 rounded-lg transition-colors"
                                        >
                                            <div className="text-left">
                                                <p className="font-semibold text-gray-900">{item.name}</p>
                                                <p className="text-xs text-gray-500">{item.symbol}</p>
                                            </div>
                                            <div className="text-right">
                                                <p className="font-semibold text-gray-900">{item.currentPrice}</p>
                                                <p className="text-sm text-emerald-600 font-medium">
                                                    {item.change} ({item.changePercent})
                                                </p>
                                            </div>
                                        </button>
                                    ))}
                                </div>
                            ) : (
                                <p className="text-center text-gray-400 py-8">Veri bulunamadı</p>
                            )}
                        </CardContent>
                    </Card>

                    {/* Top Losers */}
                    <Card className="border-0 shadow-sm">
                        <CardHeader className="flex flex-row items-center justify-between">
                            <CardTitle className="text-red-500 flex items-center gap-2">
                                <TrendingDown className="w-5 h-5" />
                                En Çok Kaybedenler
                            </CardTitle>
                        </CardHeader>
                        <CardContent>
                            {homeData?.topLosers && homeData.topLosers.length > 0 ? (
                                <div className="space-y-2">
                                    {homeData.topLosers.map((item) => (
                                        <button
                                            key={item.id}
                                            onClick={() => handleNavigateToDetail(item.id)}
                                            className="w-full flex items-center justify-between p-3 hover:bg-gray-50 rounded-lg transition-colors"
                                        >
                                            <div className="text-left">
                                                <p className="font-semibold text-gray-900">{item.name}</p>
                                                <p className="text-xs text-gray-500">{item.symbol}</p>
                                            </div>
                                            <div className="text-right">
                                                <p className="font-semibold text-gray-900">{item.currentPrice}</p>
                                                <p className="text-sm text-red-500 font-medium">
                                                    {item.change} ({item.changePercent})
                                                </p>
                                            </div>
                                        </button>
                                    ))}
                                </div>
                            ) : (
                                <p className="text-center text-gray-400 py-8">Veri bulunamadı</p>
                            )}
                        </CardContent>
                    </Card>
                </div>

                {/* Recent News */}
                {homeData?.recentNews && homeData.recentNews.length > 0 && (
                    <Card className="border-0 shadow-sm">
                        <CardHeader className="flex flex-row items-center justify-between">
                            <CardTitle>Son Haberler</CardTitle>
                            <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => navigate('/news')}
                                className="hover:bg-gray-100"
                            >
                                Tümünü Gör
                                <ArrowRight className="w-4 h-4 ml-2" />
                            </Button>
                        </CardHeader>
                        <CardContent>
                            <div className="space-y-3">
                                {homeData.recentNews.map((news) => (
                                    <div
                                        key={news.id}
                                        onClick={() => navigate(`/news/${news.id}`)}
                                        className="flex items-start gap-4 p-3 hover:bg-gray-50 rounded-lg transition-colors cursor-pointer"
                                    >
                                        {news.imageUrl && (
                                            <img
                                                src={news.imageUrl}
                                                alt={news.title}
                                                className="w-20 h-20 object-cover rounded-lg"
                                            />
                                        )}
                                        <div className="flex-1">
                                            <p className="font-medium text-gray-900 mb-1 line-clamp-2">
                                                {news.title}
                                            </p>
                                            <div className="flex items-center gap-2 text-sm text-gray-500">
                                                <span className="px-2 py-0.5 bg-blue-100 text-blue-700 rounded text-xs">
                                                    {news.category}
                                                </span>
                                                <span>{news.source}</span>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </CardContent>
                    </Card>
                )}
            </div>
        </div>
    );
}