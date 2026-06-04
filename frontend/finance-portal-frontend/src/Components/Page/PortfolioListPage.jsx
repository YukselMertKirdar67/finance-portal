import React, { useState, useEffect } from 'react';
import { Plus, TrendingUp, TrendingDown, ArrowRight, Briefcase, PiggyBank, Building2, Wallet } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../UI/Card';
import { Button } from '../UI/Button';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { getAllPortfolios, createPortfolio } from '../../API/portfolioApi';

export default function PortfolioListPage() {
    const navigate = useNavigate();
    const { t } = useTranslation();
    const [portfolios, setPortfolios] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [error, setError] = useState(null);

    const [formData, setFormData] = useState({
        name: '',
        description: '',
        portfolioType: 'PERSONAL',
        currency: 'TRY'
    });
    const [submitting, setSubmitting] = useState(false);

    useEffect(() => {
        loadPortfolios();
    }, []);

    const loadPortfolios = async () => {
        try {
            setLoading(true);
            setError(null);
            const data = await getAllPortfolios();
            setPortfolios(data || []);
        } catch (err) {
            console.error('Error loading portfolios:', err);
            setError(err.response?.data?.message || t('portfolioList.loadError'));
        } finally {
            setLoading(false);
        }
    };

    const handleCreatePortfolio = async () => {
        if (!formData.name) {
            alert(t('portfolioList.nameRequired'));
            return;
        }

        try {
            setSubmitting(true);
            const portfolioData = {
                name: formData.name,
                description: formData.description || undefined,
                portfolioType: formData.portfolioType,
                currency: formData.currency
            };
            const newPortfolio = await createPortfolio(portfolioData);
            setFormData({ name: '', description: '', portfolioType: 'PERSONAL', currency: 'TRY' });
            setShowCreateModal(false);
            await loadPortfolios();
            navigate(`/portfolios/${newPortfolio.id}`);
        } catch (err) {
            console.error('Error creating portfolio:', err);
            alert(err.response?.data?.message || t('portfolioList.createError'));
        } finally {
            setSubmitting(false);
        }
    };

    const getPortfolioTypeIcon = (type) => {
        const icons = {
            'PERSONAL': <Wallet className="w-6 h-6" />,
            'BUSINESS': <Briefcase className="w-6 h-6" />,
            'RETIREMENT': <PiggyBank className="w-6 h-6" />,
            'SAVINGS': <Building2 className="w-6 h-6" />
        };
        return icons[type] || <Wallet className="w-6 h-6" />;
    };

    const getPortfolioTypeColor = (type) => {
        const colors = {
            'PERSONAL': 'bg-blue-100 text-blue-700',
            'BUSINESS': 'bg-purple-100 text-purple-700',
            'RETIREMENT': 'bg-green-100 text-green-700',
            'SAVINGS': 'bg-orange-100 text-orange-700'
        };
        return colors[type] || 'bg-gray-100 text-gray-700';
    };

    const getPortfolioTypeLabel = (type) => {
        const labels = {
            'PERSONAL': t('portfolioList.type.personal'),
            'BUSINESS': t('portfolioList.type.business'),
            'RETIREMENT': t('portfolioList.type.retirement'),
            'SAVINGS': t('portfolioList.type.savings')
        };
        return labels[type] || type;
    };

    if (loading) {
        return (
            <div className="p-8 flex items-center justify-center h-screen">
                <div className="text-center">
                    <div className="animate-spin rounded-full h-16 w-16 border-b-4 border-blue-600 mx-auto mb-4"></div>
                    <p className="text-lg text-gray-700 font-medium">{t('portfolio.loading')}</p>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="p-8">
                <div className="bg-red-50 border-2 border-red-200 rounded-lg p-8 text-center max-w-md mx-auto">
                    <div className="text-red-600 text-5xl mb-4">⚠️</div>
                    <p className="text-red-800 font-semibold text-xl mb-2">{t('common.error')}</p>
                    <p className="text-red-600 mb-6">{error}</p>
                    <Button className="bg-red-600 hover:bg-red-700" onClick={loadPortfolios}>
                        {t('common.refresh')}
                    </Button>
                </div>
            </div>
        );
    }

    return (
        <div className="p-8">

            {/* Header */}
            <div className="mb-8 flex items-center justify-between">
                <div>
                    <h1 className="text-3xl font-bold mb-2">{t('portfolio.title')}</h1>
                    <p className="text-gray-600">{t('portfolioList.subtitle')}</p>
                </div>
                <Button onClick={() => setShowCreateModal(true)} className="bg-[#0066FF] hover:bg-[#0052CC]">
                    <Plus className="w-5 h-5 mr-2" />
                    {t('portfolioList.newPortfolio')}
                </Button>
            </div>

            {/* Empty State */}
            {portfolios.length === 0 ? (
                <div className="text-center py-16">
                    <div className="inline-flex items-center justify-center w-20 h-20 bg-blue-100 rounded-full mb-6">
                        <Wallet className="w-10 h-10 text-blue-600" />
                    </div>
                    <h2 className="text-2xl font-bold text-gray-900 mb-2">{t('portfolioList.emptyTitle')}</h2>
                    <p className="text-gray-600 mb-6 max-w-md mx-auto">{t('portfolioList.emptyDesc')}</p>
                    <Button onClick={() => setShowCreateModal(true)} size="lg" className="bg-[#0066FF] hover:bg-[#0052CC]">
                        <Plus className="w-5 h-5 mr-2" />
                        {t('portfolioList.createFirst')}
                    </Button>
                </div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {portfolios.map((portfolio) => (
                        <Card
                            key={portfolio.id}
                            className="hover:shadow-lg transition-shadow cursor-pointer"
                            onClick={() => navigate(`/portfolios/${portfolio.id}`)}
                        >
                            <CardHeader>
                                <div className="flex items-start justify-between">
                                    <div className="flex items-center gap-3">
                                        <div className={`p-3 rounded-lg ${getPortfolioTypeColor(portfolio.portfolioType)}`}>
                                            {getPortfolioTypeIcon(portfolio.portfolioType)}
                                        </div>
                                        <div>
                                            <div className="flex items-center gap-2 mb-1">
                                                <CardTitle className="text-xl">{portfolio.name}</CardTitle>
                                                {portfolio.active ? (
                                                    <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-700 border border-green-200">
                                                        {t('profile.active')}
                                                    </span>
                                                ) : (
                                                    <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-600 border border-gray-200">
                                                        {t('portfolio.passive')}
                                                    </span>
                                                )}
                                            </div>
                                            <p className="text-sm text-gray-500">
                                                {getPortfolioTypeLabel(portfolio.portfolioType)}
                                            </p>
                                        </div>
                                    </div>
                                    <ArrowRight className="w-5 h-5 text-gray-400" />
                                </div>
                            </CardHeader>
                            <CardContent>
                                {portfolio.description && (
                                    <p className="text-sm text-gray-600 mb-4">{portfolio.description}</p>
                                )}
                                <div className="space-y-3">
                                    <div>
                                        <p className="text-xs text-gray-500 mb-1">{t('portfolio.totalValue')}</p>
                                        <p className="text-2xl font-bold text-gray-900">
                                            {portfolio.currency === 'TRY' ? '₺' : '$'}
                                            {portfolio.totalValue?.toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                                        </p>
                                    </div>
                                    <div className="flex items-center justify-between">
                                        <div>
                                            <p className="text-xs text-gray-500 mb-1">{t('portfolio.unrealizedPnL')}</p>
                                            <p className={`text-lg font-semibold flex items-center gap-1 ${
                                                portfolio.unrealizedPnL === 0 ? 'text-gray-500' :
                                                    portfolio.unrealizedPnL > 0 ? 'text-green-600' : 'text-red-600'
                                            }`}>
                                                {portfolio.unrealizedPnL === 0 ? <span>—</span> :
                                                    portfolio.unrealizedPnL > 0 ? <TrendingUp className="w-4 h-4" /> : <TrendingDown className="w-4 h-4" />}
                                                {portfolio.unrealizedPnL > 0 ? '+' : ''}
                                                {portfolio.currency === 'TRY' ? '₺' : '$'}
                                                {portfolio.unrealizedPnL?.toLocaleString('tr-TR', { minimumFractionDigits: 2 })}
                                            </p>
                                        </div>
                                        <div className="text-right">
                                            <p className="text-xs text-gray-500 mb-1">{t('portfolioList.return')}</p>
                                            <p className={`text-lg font-semibold ${
                                                portfolio.pnlPercent === 0 ? 'text-gray-500' :
                                                    portfolio.pnlPercent > 0 ? 'text-green-600' : 'text-red-600'
                                            }`}>
                                                {portfolio.pnlPercent > 0 ? '+' : ''}{portfolio.pnlPercent?.toFixed(2)}%
                                            </p>
                                        </div>
                                    </div>
                                    <div className="pt-3 border-t border-gray-200">
                                        <p className="text-sm text-gray-600">
                                            {t('portfolioList.holdingCount', { count: portfolio.holdingCount || 0 })}
                                        </p>
                                    </div>
                                </div>
                            </CardContent>
                        </Card>
                    ))}
                </div>
            )}

            {/* Create Portfolio Modal */}
            {showCreateModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-lg shadow-2xl w-full max-w-lg">
                        <div className="p-6 border-b border-gray-200">
                            <h2 className="text-2xl font-bold text-gray-900">{t('portfolioList.createTitle')}</h2>
                        </div>
                        <div className="p-6 space-y-5">

                            {/* Portfolio Name */}
                            <div>
                                <label className="block text-sm font-semibold text-gray-700 mb-2">
                                    {t('portfolio.name')} *
                                </label>
                                <input
                                    type="text"
                                    className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#0066FF]"
                                    placeholder={t('portfolioList.namePlaceholder')}
                                    value={formData.name}
                                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                                    disabled={submitting}
                                />
                            </div>

                            {/* Description */}
                            <div>
                                <label className="block text-sm font-semibold text-gray-700 mb-2">
                                    {t('portfolio.description')}
                                </label>
                                <textarea
                                    className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#0066FF] resize-none"
                                    placeholder={t('portfolioList.descPlaceholder')}
                                    rows={3}
                                    value={formData.description}
                                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                                    disabled={submitting}
                                />
                            </div>

                            {/* Portfolio Type */}
                            <div>
                                <label className="block text-sm font-semibold text-gray-700 mb-2">
                                    {t('portfolioList.typeLabel')} *
                                </label>
                                <div className="grid grid-cols-2 gap-3">
                                    {[
                                        { value: 'PERSONAL',   label: t('portfolioList.type.personal'),   icon: <Wallet className="w-5 h-5" /> },
                                        { value: 'BUSINESS',   label: t('portfolioList.type.business'),   icon: <Briefcase className="w-5 h-5" /> },
                                        { value: 'RETIREMENT', label: t('portfolioList.type.retirement'), icon: <PiggyBank className="w-5 h-5" /> },
                                        { value: 'SAVINGS',    label: t('portfolioList.type.savings'),    icon: <Building2 className="w-5 h-5" /> }
                                    ].map((type) => (
                                        <button
                                            key={type.value}
                                            type="button"
                                            className={`flex items-center gap-2 px-4 py-3 border-2 rounded-lg font-medium transition-colors ${
                                                formData.portfolioType === type.value
                                                    ? 'border-[#0066FF] bg-blue-50 text-[#0066FF]'
                                                    : 'border-gray-300 text-gray-700 hover:border-gray-400'
                                            }`}
                                            onClick={() => setFormData({ ...formData, portfolioType: type.value })}
                                            disabled={submitting}
                                        >
                                            {type.icon}
                                            {type.label}
                                        </button>
                                    ))}
                                </div>
                            </div>

                            {/* Currency */}
                            <div>
                                <label className="block text-sm font-semibold text-gray-700 mb-2">
                                    {t('portfolioList.currencyLabel')} *
                                </label>
                                <div className="grid grid-cols-4 gap-2">
                                    {['TRY', 'USD', 'EUR', 'GBP'].map((curr) => (
                                        <button
                                            key={curr}
                                            type="button"
                                            className={`px-4 py-3 border-2 rounded-lg font-semibold transition-colors ${
                                                formData.currency === curr
                                                    ? 'border-[#0066FF] bg-blue-50 text-[#0066FF]'
                                                    : 'border-gray-300 text-gray-700 hover:border-gray-400'
                                            }`}
                                            onClick={() => setFormData({ ...formData, currency: curr })}
                                            disabled={submitting}
                                        >
                                            {curr}
                                        </button>
                                    ))}
                                </div>
                            </div>
                        </div>

                        <div className="p-6 border-t border-gray-200 flex gap-3">
                            <Button
                                variant="outline"
                                className="flex-1 h-12 font-semibold border-2"
                                onClick={() => {
                                    setShowCreateModal(false);
                                    setFormData({ name: '', description: '', portfolioType: 'PERSONAL', currency: 'TRY' });
                                }}
                                disabled={submitting}
                            >
                                {t('common.cancel')}
                            </Button>
                            <Button
                                className="flex-1 h-12 font-semibold bg-[#0066FF] hover:bg-[#0052CC]"
                                onClick={handleCreatePortfolio}
                                disabled={!formData.name || submitting}
                            >
                                {submitting ? t('portfolioList.creating') : t('portfolioList.createBtn')}
                            </Button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}