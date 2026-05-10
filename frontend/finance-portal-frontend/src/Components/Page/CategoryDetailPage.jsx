import React, { useState, useEffect } from 'react';
import { TrendingUp, TrendingDown, ArrowLeft, Star, RefreshCw, Search } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { Card, CardContent } from '../UI/Card';
import { Button } from '../UI/Button';
import Pagination from '../UI/Pagination';
import { getInstrumentsByType, searchInstruments } from '../../API/instrumentsApi';
import { addToWatchlist, removeFromWatchlist, isInWatchlist } from '../../API/watchlistApi';
import { useWebSocket } from '../../Hooks/useWebSocket';

const TYPE_META = {
    FOREX:    { title: 'Döviz Piyasası',       description: 'Majör ve minör döviz çiftlerinin anlık fiyatları' },
    STOCK:    { title: 'Hisse Senetleri',       description: 'BIST ve ABD hisse senetleri' },
    BOND:     { title: 'Tahvil ve Bonolar',     description: 'Devlet tahvilleri ve hazine bonoları' },
    FUND:     { title: 'Fon ve EFTler',         description: 'Global fonler ve EFTler' },
    PRECIOUS: { title: 'Altın ve Gümüş',        description: 'Kıymetli metaller ve emtia piyasaları' },
    CRYPTO:   { title: 'Kripto Paralar',        description: 'Bitcoin, Ethereum ve altcoinler' },
};

export default function CategoryDetailPage() {
    const navigate = useNavigate();
    const { type } = useParams();

    const [instruments, setInstruments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const [searchQuery, setSearchQuery] = useState('');
    const [watchlistStatus, setWatchlistStatus] = useState({});
    const [livePrices, setLivePrices] = useState({});
    const pageSize = 20;

    const meta = TYPE_META[type] || { title: type, description: '' };

    useEffect(() => {
        setCurrentPage(0);
        setSearchQuery('');
    }, [type]);

    useEffect(() => {
        let cancelled = false;

        const fetchInstruments = async () => {
            setLoading(true);
            setError(null);

            try {
                let response;

                if (searchQuery.trim().length > 0) {
                    response = await searchInstruments(searchQuery, currentPage, pageSize);
                    if (response && response.content) {
                        response = {
                            ...response,
                            content: response.content.filter(
                                item => item !== null && item.type === type
                            )
                        };
                    }
                } else {
                    response = await getInstrumentsByType(type, currentPage, pageSize);
                }

                if (cancelled) return;

                if (response && response.content !== undefined) {
                    const validInstruments = response.content.filter(item => item !== null) ?? [];
                    setInstruments(validInstruments);
                    setTotalPages(response.totalPages ?? 0);
                    setTotalElements(response.totalElements ?? 0);

                    // Watchlist durumunu kontrol et
                    checkWatchlistStatus(validInstruments);
                } else {
                    setInstruments([]);
                    setTotalPages(0);
                    setTotalElements(0);
                }
            } catch (err) {
                if (cancelled) return;
                console.error('Load instruments error:', err);
                setInstruments([]);
                if (err.response?.status !== 404) {
                    setError('Veriler yüklenirken bir hata oluştu.');
                }
            } finally {
                if (!cancelled) setLoading(false);
            }
        };

        fetchInstruments();
        return () => { cancelled = true; };

    }, [type, currentPage, searchQuery]);

    useWebSocket((priceUpdate) => {
        setLivePrices(prev => ({
            ...prev,
            [priceUpdate.instrumentId]: priceUpdate
        }));
    });

    const checkWatchlistStatus = async (instrumentsList) => {
        const status = {};
        for (const inst of instrumentsList) {
            try {
                const inList = await isInWatchlist(inst.id);
                status[inst.id] = inList;
            } catch (error) {
                console.error('Watchlist check error:', error);
            }
        }
        setWatchlistStatus(status);
    };

    const handleToggleWatchlist = async (e, instrumentId) => {
        e.stopPropagation();

        try {
            if (watchlistStatus[instrumentId]) {
                await removeFromWatchlist(instrumentId);
            } else {
                await addToWatchlist(instrumentId);
            }

            setWatchlistStatus(prev => ({
                ...prev,
                [instrumentId]: !prev[instrumentId]
            }));
        } catch (error) {
            console.error('Watchlist toggle error:', error);
            alert('İşlem başarısız');
        }
    };

    const handlePageChange = (page) => {
        if (page === currentPage) return;
        if (page < 0 || page >= totalPages) return;
        setCurrentPage(page);
        window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    const handleSearch = (e) => {
        setSearchQuery(e.target.value);
        setCurrentPage(0);
    };

    const handleDetailClick = (id) => {
        navigate(`/instruments/detail/${id}`);
    };

    const formatPrice = (price) => {
        if (!price && price !== 0) return '-';
        if (price > 1000) return price.toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        if (price > 1)    return price.toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 4 });
        return price.toLocaleString('tr-TR', { minimumFractionDigits: 4, maximumFractionDigits: 6 });
    };

    const getSubtitle = (instrument) => {
        if (instrument.sector)       return instrument.sector;
        if (instrument.blockchain)   return instrument.blockchain;
        if (instrument.baseCurrency && instrument.quoteCurrency)
            return `${instrument.baseCurrency} / ${instrument.quoteCurrency}`;
        if (instrument.issuer)       return instrument.issuer;
        if (instrument.metalType)    return instrument.metalType;
        return instrument.exchange || '';
    };

    return (
        <div className="min-h-screen bg-gray-50 p-8">
            <div className="max-w-7xl mx-auto">

                {/* Header */}
                <div className="mb-6">
                    <Button
                        variant="ghost"
                        onClick={() => navigate('/instruments')}
                        className="mb-4"
                    >
                        <ArrowLeft className="w-4 h-4 mr-2" />
                        Geri Dön
                    </Button>

                    <div className="flex items-start justify-between">
                        <div>
                            <h1 className="text-3xl font-bold mb-2">{meta.title}</h1>
                            <p className="text-gray-600">{meta.description}</p>
                        </div>
                        <div className="text-right">
                            <p className="text-2xl font-bold text-gray-900">{totalElements}</p>
                            <p className="text-sm text-gray-500">Toplam Enstrüman</p>
                        </div>
                    </div>
                </div>

                {/* Search */}
                <div className="mb-6 relative max-w-md">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                    <input
                        type="text"
                        placeholder="Sembol veya isim ara..."
                        value={searchQuery}
                        onChange={handleSearch}
                        disabled={loading}
                        className="w-full pl-10 pr-4 py-2.5 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
                    />
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

                {/* Table */}
                {!loading && !error && (
                    <>
                        <Card>
                            <CardContent className="p-0">
                                <div className="overflow-x-auto">
                                    <table className="w-full">
                                        <thead className="bg-gray-50 border-b border-gray-200">
                                        <tr>
                                            <th className="text-left py-4 px-6 text-sm font-medium text-gray-600">Enstrüman</th>
                                            <th className="text-left py-4 px-6 text-sm font-medium text-gray-600">Sembol</th>
                                            <th className="text-right py-4 px-6 text-sm font-medium text-gray-600">Fiyat</th>
                                            <th className="text-right py-4 px-6 text-sm font-medium text-gray-600">Değişim</th>
                                            <th className="text-right py-4 px-6 text-sm font-medium text-gray-600">Değişim %</th>
                                            <th className="text-right py-4 px-6 text-sm font-medium text-gray-600">Yüksek</th>
                                            <th className="text-right py-4 px-6 text-sm font-medium text-gray-600">Düşük</th>
                                            <th className="text-center py-4 px-6 text-sm font-medium text-gray-600">Takip</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {instruments.length === 0 ? (
                                            <tr>
                                                <td colSpan={8} className="text-center py-12 text-gray-500">
                                                    {searchQuery
                                                        ? 'Arama sonucu bulunamadı.'
                                                        : 'Henüz veri yok. Admin panelinden güncelleme yapın.'
                                                    }
                                                </td>
                                            </tr>
                                        ) : (
                                            instruments.map((instrument, index) => {
                                                const livePrice = livePrices[instrument.id];
                                                const price = livePrice ? {
                                                    current: livePrice.currentPrice,
                                                    changeAmount: livePrice.changeAmount,
                                                    changePercent: livePrice.changePercent,
                                                    high: instrument.currentPrice?.high,
                                                    low: instrument.currentPrice?.low,
                                                } : instrument.currentPrice;
                                                const isPositive = (price?.changePercent ?? 0) > 0;
                                                const isNeutral = (price?.changePercent ?? 0) === 0;

                                                return (
                                                    <tr
                                                        key={instrument.id}
                                                        className={`border-b border-gray-100 hover:bg-gray-50 transition-colors cursor-pointer ${
                                                            index % 2 === 0 ? 'bg-white' : 'bg-gray-50/50'
                                                        }`}
                                                        onClick={() => handleDetailClick(instrument.id)}
                                                    >
                                                        <td className="py-4 px-6">
                                                            <p className="font-medium text-gray-900">{instrument.name}</p>
                                                            <p className="text-xs text-gray-400 mt-0.5">
                                                                {getSubtitle(instrument)}
                                                            </p>
                                                        </td>

                                                        <td className="py-4 px-6">
                                                            <span className="text-sm font-mono bg-gray-100 text-gray-600 px-2 py-1 rounded">
                                                                {instrument.symbol}
                                                            </span>
                                                        </td>

                                                        <td className="py-4 px-6 text-right">
                                                            {price ? (
                                                                <span className="font-semibold text-gray-900">
                                                                    {formatPrice(price.current)}
                                                                </span>
                                                            ) : (
                                                                <span className="text-gray-400 text-sm">-</span>
                                                            )}
                                                        </td>

                                                        <td className="py-4 px-6 text-right">
                                                            {price ? (
                                                                <span className={isNeutral ? 'text-gray-500' : isPositive ? 'text-green-600' : 'text-red-600'}>
                                                                      {isPositive ? '+' : ''}{formatPrice(price.changeAmount)}
                                                                </span>

                                                            ) : '-'}
                                                        </td>

                                                        <td className="py-4 px-6 text-right">
                                                            {price ? (
                                                                <div className={`flex items-center justify-end gap-1 ${
                                                                    isNeutral ? 'text-gray-500' :
                                                                        isPositive ? 'text-green-600' : 'text-red-600'
                                                                }`}>
                                                                    {isNeutral ? (
                                                                        <span>—</span>
                                                                    ) : isPositive ? (
                                                                        <TrendingUp className="w-4 h-4" />
                                                                    ) : (
                                                                        <TrendingDown className="w-4 h-4" />
                                                                    )}
                                                                    <span className="font-medium">
                                                                         {Math.abs(price.changePercent).toFixed(2)}%
                                                                    </span>
                                                                </div>
                                                            ) : '-'}
                                                        </td>

                                                        <td className="py-4 px-6 text-right">
                                                            <span className="text-sm text-green-600">
                                                                {price ? formatPrice(price.high) : '-'}
                                                            </span>
                                                        </td>

                                                        <td className="py-4 px-6 text-right">
                                                            <span className="text-sm text-red-500">
                                                                {price ? formatPrice(price.low) : '-'}
                                                            </span>
                                                        </td>

                                                        <td className="py-4 px-6 text-center">
                                                            <button
                                                                onClick={(e) => handleToggleWatchlist(e, instrument.id)}
                                                                className={`p-2 rounded-lg transition-all ${
                                                                    watchlistStatus[instrument.id]
                                                                        ? 'text-yellow-500 hover:bg-yellow-50'
                                                                        : 'text-gray-400 hover:bg-gray-100'
                                                                }`}
                                                                title={watchlistStatus[instrument.id] ? 'Takipten çıkar' : 'Takip listesine ekle'}
                                                            >
                                                                <Star
                                                                    className={`w-5 h-5 ${
                                                                        watchlistStatus[instrument.id] ? 'fill-yellow-500' : ''
                                                                    }`}
                                                                />
                                                            </button>
                                                        </td>
                                                    </tr>
                                                );
                                            })
                                        )}
                                        </tbody>
                                    </table>
                                </div>
                            </CardContent>
                        </Card>

                        {/* Pagination */}
                        {totalPages > 1 && (
                            <div className="mt-6">
                                <Pagination
                                    currentPage={currentPage}
                                    totalPages={totalPages}
                                    totalElements={totalElements}
                                    pageSize={pageSize}
                                    onPageChange={handlePageChange}
                                />
                            </div>
                        )}
                    </>
                )}
            </div>
        </div>
    );
}