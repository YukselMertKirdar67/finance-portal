import React, { useState, useEffect } from 'react';
import {
    DollarSign, Building2, FileText,
    Landmark, Coins, Bitcoin, ChevronRight, RefreshCw
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent } from '../UI/Card';
import { getInstrumentsByType } from '../../API/instrumentsApi';

const CATEGORIES = [
    {
        title: 'Döviz',
        apiType: 'FOREX',
        icon: DollarSign,
        color: 'bg-blue-500',
        description: 'USD, EUR, GBP ve diğer döviz çiftleri',
    },
    {
        title: 'Borsa',
        apiType: 'STOCK',
        icon: Building2,
        color: 'bg-purple-500',
        description: 'BIST ve ABD hisse senetleri',
    },
    {
        title: 'Tahvil/Bono',
        apiType: 'BOND',
        icon: FileText,
        color: 'bg-orange-500',
        description: 'Devlet tahvilleri ve hazine bonoları',
    },
    {
        title: 'EuroBond',
        apiType: 'EUROBOND',
        icon: Landmark,
        color: 'bg-indigo-500',
        description: 'Uluslararası tahviller',
    },
    {
        title: 'Altın/Gümüş',
        apiType: 'PRECIOUS',
        icon: Coins,
        color: 'bg-yellow-500',
        description: 'Kıymetli metaller ve emtia',
    },
    {
        title: 'Kripto',
        apiType: 'CRYPTO',
        icon: Bitcoin,
        color: 'bg-pink-500',
        description: 'Bitcoin, Ethereum ve altcoinler',
    },
];

export default function InstrumentsPage() {
    const navigate = useNavigate();
    const [counts, setCounts] = useState({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        let cancelled = false;

        const fetchCounts = async () => {
            setLoading(true);
            setError(null);

            try {
                const results = await Promise.allSettled(
                    CATEGORIES.map(cat =>
                        getInstrumentsByType(cat.apiType, 0, 1)
                            .then(data => ({
                                type: cat.apiType,
                                count: data.totalElements || 0
                            }))
                    )
                );

                if (cancelled) return;

                const countMap = {};
                results.forEach(result => {
                    if (result.status === 'fulfilled') {
                        countMap[result.value.type] = result.value.count;
                    }
                });

                setCounts(countMap);
            } catch (err) {
                if (cancelled) return;
                console.error('Count fetch error:', err);
                setError('Veriler yüklenirken hata oluştu.');
            } finally {
                if (!cancelled) setLoading(false);
            }
        };

        fetchCounts();
        return () => { cancelled = true; };

    }, []);

    const handleCategoryClick = (apiType) => {
        navigate(`/instruments/${apiType}`);
    };

    return (
        <div className="min-h-screen bg-gray-50 p-8">
            <div className="max-w-7xl mx-auto">

                {/* Header */}
                <div className="mb-8">
                    <h1 className="text-3xl font-bold mb-2">Finansal Enstrümanlar</h1>
                    <p className="text-gray-600">
                        Tüm piyasalara göz atın ve yatırım fırsatlarını keşfedin
                    </p>
                </div>

                {/* Loading */}
                {loading && (
                    <div className="text-center py-12">
                        <RefreshCw className="w-8 h-8 animate-spin mx-auto text-blue-600" />
                        <p className="mt-4 text-gray-600">Yükleniyor...</p>
                    </div>
                )}

                {/* Error */}
                {!loading && error && (
                    <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-6">
                        {error}
                    </div>
                )}

                {/* Grid */}
                {!loading && !error && (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                        {CATEGORIES.map((category) => {
                            const Icon = category.icon;
                            const count = counts[category.apiType];

                            return (
                                <Card
                                    key={category.title}
                                    className="overflow-hidden cursor-pointer hover:shadow-lg transition-all hover:scale-105"
                                    onClick={() => handleCategoryClick(category.apiType)}
                                >
                                    <CardContent className="p-6">
                                        {/* Icon */}
                                        <div className={`${category.color} w-12 h-12 rounded-lg flex items-center justify-center mb-4`}>
                                            <Icon className="w-6 h-6 text-white" />
                                        </div>

                                        {/* Title */}
                                        <h2 className="text-xl font-semibold text-gray-900 mb-2">
                                            {category.title}
                                        </h2>

                                        {/* Description */}
                                        <p className="text-sm text-gray-600 mb-3">
                                            {category.description}
                                        </p>

                                        {/* Footer */}
                                        <div className="flex items-center justify-between">
                                            <span className="text-xs text-gray-500">
                                                {count !== undefined
                                                    ? `${count} Enstrüman`
                                                    : '-'
                                                }
                                            </span>
                                            <ChevronRight className="w-5 h-5 text-gray-400" />
                                        </div>
                                    </CardContent>
                                </Card>
                            );
                        })}
                    </div>
                )}
            </div>
        </div>
    );
}