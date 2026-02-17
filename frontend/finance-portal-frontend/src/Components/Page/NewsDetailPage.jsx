import React, { useState, useEffect } from 'react';
import { ArrowLeft, Clock, ExternalLink, Share2 } from 'lucide-react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, CardContent } from '../UI/Card';
import { Button } from '../UI/Button';
import { getNewsById } from '../../API/newsApi';

export default function NewsDetailPage() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [news, setNews] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (!id) return;

        let cancelled = false;

        const loadDetail = async () => {
            setLoading(true);
            setError(null);
            try {
                const data = await getNewsById(id);
                if (!cancelled) setNews(data);
            } catch (err) {
                if (!cancelled) setError('Haber detayı yüklenirken bir hata oluştu.');
                console.error(err);
            } finally {
                if (!cancelled) setLoading(false);
            }
        };

        loadDetail();
        return () => { cancelled = true; };

    }, [id]);

    const formatDate = (dateString) => {
        if (!dateString) return 'Tarih belirtilmemiş';
        return new Date(dateString).toLocaleDateString('tr-TR', {
            year: 'numeric', month: 'long', day: 'numeric',
            hour: '2-digit', minute: '2-digit'
        });
    };

    const handleShare = () => {
        if (navigator.share) {
            navigator.share({
                title: news?.title,
                text: news?.content?.substring(0, 200),
                url: window.location.href,
            }).catch(() => {});
        } else {
            alert('Paylaşma özelliği bu tarayıcıda desteklenmiyor.');
        }
    };

    if (loading) {
        return (
            <div className="min-h-screen bg-gray-50 p-8">
                <div className="max-w-4xl mx-auto animate-pulse">
                    <div className="h-8 bg-gray-200 rounded w-1/4 mb-6"></div>
                    <div className="h-96 bg-gray-200 rounded mb-6"></div>
                    <div className="h-4 bg-gray-200 rounded w-3/4 mb-4"></div>
                    <div className="h-4 bg-gray-200 rounded w-full mb-4"></div>
                </div>
            </div>
        );
    }

    if (error || !news) {
        return (
            <div className="min-h-screen bg-gray-50 p-8">
                <div className="max-w-4xl mx-auto">
                    <Button onClick={() => navigate('/news')} variant="outline" className="mb-6">
                        <ArrowLeft className="w-4 h-4 mr-2" />
                        Geri Dön
                    </Button>
                    <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
                        {error || 'Haber bulunamadı'}
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gray-50 p-8">
            <div className="max-w-4xl mx-auto">

                <Button onClick={() => navigate('/news')} variant="outline" className="mb-6">
                    <ArrowLeft className="w-4 h-4 mr-2" />
                    Haberlere Dön
                </Button>

                <Card>
                    {news.imageUrl && (
                        <div
                            className="h-96 bg-cover bg-center rounded-t-lg"
                            style={{ backgroundImage: `url(${news.imageUrl})` }}
                        />
                    )}

                    <CardContent className="p-8">
                        <div className="flex items-center gap-4 mb-6">
                            <span className="px-3 py-1 bg-blue-100 text-blue-700 rounded-full text-sm font-medium">
                                {news.category || 'Genel'}
                            </span>
                            <div className="flex items-center gap-2 text-gray-500 text-sm">
                                <Clock className="w-4 h-4" />
                                <span>{formatDate(news.publishDate)}</span>
                            </div>
                        </div>

                        <h1 className="text-4xl font-bold text-gray-900 mb-4">
                            {news.title}
                        </h1>

                        {news.source && (
                            <p className="text-gray-500 mb-6 flex items-center gap-2">
                                <ExternalLink className="w-4 h-4" />
                                Kaynak: {news.source}
                            </p>
                        )}

                        <hr className="mb-6" />

                        <p className="text-lg text-gray-700 leading-relaxed whitespace-pre-wrap">
                            {news.content}
                        </p>

                        <div className="mt-8 pt-6 border-t flex gap-3">
                            <Button onClick={handleShare} variant="outline">
                                <Share2 className="w-4 h-4 mr-2" />
                                Paylaş
                            </Button>
                        </div>
                    </CardContent>
                </Card>
            </div>
        </div>
    );
}