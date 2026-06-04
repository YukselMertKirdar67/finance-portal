import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
    TrendingUp, Briefcase, Star, BarChart3, Shield,
    Zap, ArrowRight, CheckCircle, Users, Globe,
    Lock, LineChart
} from 'lucide-react';
import { Button } from '../UI/Button';

export default function LandingPage() {
    const navigate = useNavigate();
    const { t, i18n } = useTranslation();

    const toggleLanguage = () => {
        const newLang = i18n.language === 'tr' ? 'en' : 'tr';
        i18n.changeLanguage(newLang);
        localStorage.setItem('i18nextLng', newLang);
    };

    return (
        <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white">

            {/* Hero Section */}
            <div className="relative overflow-hidden">
                <div className="absolute inset-0 bg-gradient-to-br from-blue-50 via-white to-purple-50 opacity-70" />
                <div className="absolute inset-0" style={{
                    backgroundImage: 'radial-gradient(circle at 1px 1px, rgb(203 213 225 / 0.3) 1px, transparent 0)',
                    backgroundSize: '40px 40px'
                }} />

                <div className="relative max-w-7xl mx-auto px-6 pt-20 pb-24">

                    {/* Logo + Dil Butonu */}
                    <div className="flex items-center justify-between mb-12">
                        <div className="flex items-center gap-3">
                            <div className="w-12 h-12 bg-gradient-to-br from-blue-600 to-indigo-600 rounded-xl flex items-center justify-center shadow-lg">
                                <TrendingUp className="w-7 h-7 text-white" />
                            </div>
                            <span className="text-2xl font-bold bg-gradient-to-r from-blue-600 to-indigo-600 bg-clip-text text-transparent">
                                FinansApp
                            </span>
                        </div>

                        {/* Dil Değiştirme Butonu */}
                        <button
                            onClick={toggleLanguage}
                            className="flex items-center gap-2 px-4 py-2 rounded-xl border-2 border-gray-200 hover:border-blue-400 bg-white hover:bg-blue-50 text-gray-700 hover:text-blue-600 font-semibold transition-all shadow-sm"
                        >
                            <Globe className="w-4 h-4" />
                            <span className="text-sm">
                                {i18n.language === 'tr' ? '🇹🇷 TR' : '🇬🇧 EN'}
                            </span>
                            <span className="text-gray-400 text-xs">→</span>
                            <span className="text-sm text-gray-400">
                                {i18n.language === 'tr' ? 'EN' : 'TR'}
                            </span>
                        </button>
                    </div>

                    {/* ... geri kalan JSX aynı kalıyor */}
                    <div className="grid lg:grid-cols-2 gap-12 items-center">
                        <div>
                            <h1 className="text-5xl md:text-6xl font-bold text-gray-900 mb-6 leading-tight">
                                {t('landing.hero.title')}
                                <span className="block bg-gradient-to-r from-blue-600 to-indigo-600 bg-clip-text text-transparent">
                                    {t('landing.hero.titleHighlight')}
                                </span>
                            </h1>
                            <p className="text-xl text-gray-600 mb-8 leading-relaxed">
                                {t('landing.hero.description')}
                            </p>
                            <div className="flex flex-col sm:flex-row gap-4 mb-12">
                                <Button
                                    onClick={() => navigate('/register')}
                                    className="px-8 py-4 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white font-semibold rounded-xl shadow-lg hover:shadow-xl transition-all text-lg"
                                >
                                    {t('landing.hero.startFree')}
                                    <ArrowRight className="w-5 h-5 ml-2" />
                                </Button>
                                <Button
                                    onClick={() => navigate('/login')}
                                    variant="outline"
                                    className="px-8 py-4 border-2 border-gray-300 hover:border-blue-600 text-gray-700 hover:text-blue-600 font-semibold rounded-xl transition-all text-lg"
                                >
                                    {t('auth.login')}
                                </Button>
                            </div>
                        </div>

                        <div className="relative">
                            <div className="bg-white rounded-2xl shadow-2xl p-8 border border-gray-100">
                                <div className="space-y-4">
                                    <div className="flex items-center justify-between pb-4 border-b">
                                        <div>
                                            <p className="text-sm text-gray-500">{t('landing.demo.totalValue')}</p>
                                            <p className="text-3xl font-bold text-gray-900">₺248,567</p>
                                        </div>
                                        <div className="flex items-center gap-1 text-emerald-600">
                                            <TrendingUp className="w-5 h-5" />
                                            <span className="font-semibold">+12.4%</span>
                                        </div>
                                    </div>
                                    <div className="space-y-3">
                                        {[
                                            { name: 'AAPL', amount: '₺85,230', change: '+8.2%', positive: true },
                                            { name: 'GARAN.IS', amount: '₺52,100', change: '+5.1%', positive: true },
                                            { name: 'BTC/USD', amount: '₺68,450', change: '-2.3%', positive: false },
                                            { name: 'XAU/USD', amount: '₺42,787', change: '+3.7%', positive: true }
                                        ].map((item, i) => (
                                            <div key={i} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                                                <div>
                                                    <p className="font-semibold text-gray-900">{item.name}</p>
                                                    <p className="text-sm text-gray-500">{item.amount}</p>
                                                </div>
                                                <span className={`font-semibold ${item.positive ? 'text-emerald-600' : 'text-red-500'}`}>
                                                    {item.change}
                                                </span>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Features Section */}
            <div className="max-w-7xl mx-auto px-6 py-24">
                <div className="text-center mb-16">
                    <h2 className="text-4xl font-bold text-gray-900 mb-4">{t('landing.features.title')}</h2>
                    <p className="text-xl text-gray-600">{t('landing.features.subtitle')}</p>
                </div>
                <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
                    {[
                        { icon: <Briefcase className="w-8 h-8" />, title: t('landing.features.portfolio.title'), description: t('landing.features.portfolio.description'), color: 'blue' },
                        { icon: <BarChart3 className="w-8 h-8" />, title: t('landing.features.analysis.title'), description: t('landing.features.analysis.description'), color: 'purple' },
                        { icon: <Star className="w-8 h-8" />, title: t('landing.features.watchlist.title'), description: t('landing.features.watchlist.description'), color: 'yellow' },
                        { icon: <LineChart className="w-8 h-8" />, title: t('landing.features.comparison.title'), description: t('landing.features.comparison.description'), color: 'green' },
                        { icon: <Lock className="w-8 h-8" />, title: t('landing.features.security.title'), description: t('landing.features.security.description'), color: 'red' },
                        { icon: <Globe className="w-8 h-8" />, title: t('landing.features.markets.title'), description: t('landing.features.markets.description'), color: 'indigo' }
                    ].map((feature, i) => (
                        <div key={i} className="bg-white rounded-2xl p-8 shadow-md hover:shadow-xl transition-all border border-gray-100 group">
                            <div className={`w-16 h-16 bg-${feature.color}-100 rounded-xl flex items-center justify-center mb-6 text-${feature.color}-600 group-hover:scale-110 transition-transform`}>
                                {feature.icon}
                            </div>
                            <h3 className="text-xl font-bold text-gray-900 mb-3">{feature.title}</h3>
                            <p className="text-gray-600 leading-relaxed">{feature.description}</p>
                        </div>
                    ))}
                </div>
            </div>

            {/* How It Works Section */}
            <div className="bg-gradient-to-b from-blue-50 to-white py-24">
                <div className="max-w-7xl mx-auto px-6">
                    <div className="text-center mb-16">
                        <h2 className="text-4xl font-bold text-gray-900 mb-4">{t('landing.howItWorks.title')}</h2>
                        <p className="text-xl text-gray-600">{t('landing.howItWorks.subtitle')}</p>
                    </div>
                    <div className="grid md:grid-cols-3 gap-12">
                        {[
                            { step: '01', title: t('landing.howItWorks.step1.title'), description: t('landing.howItWorks.step1.description') },
                            { step: '02', title: t('landing.howItWorks.step2.title'), description: t('landing.howItWorks.step2.description') },
                            { step: '03', title: t('landing.howItWorks.step3.title'), description: t('landing.howItWorks.step3.description') }
                        ].map((step, i) => (
                            <div key={i} className="relative">
                                <div className="text-7xl font-bold text-blue-100 mb-4">{step.step}</div>
                                <h3 className="text-2xl font-bold text-gray-900 mb-3">{step.title}</h3>
                                <p className="text-gray-600 leading-relaxed">{step.description}</p>
                                {i < 2 && (
                                    <div className="hidden md:block absolute top-12 -right-6 text-blue-300">
                                        <ArrowRight className="w-12 h-12" />
                                    </div>
                                )}
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            {/* CTA Section */}
            <div className="max-w-7xl mx-auto px-6 py-24">
                <div className="bg-gradient-to-r from-blue-600 to-indigo-600 rounded-3xl p-12 text-center shadow-2xl">
                    <h2 className="text-4xl font-bold text-white mb-6">{t('landing.cta.title')}</h2>
                    <p className="text-xl text-blue-100 mb-8 max-w-2xl mx-auto">{t('landing.cta.description')}</p>
                    <div className="flex flex-col sm:flex-row gap-4 justify-center items-center mb-8">
                        <Button
                            onClick={() => navigate('/register')}
                            variant="outline"
                            className="px-8 py-4 bg-white text-blue-600 hover:bg-blue-50 font-semibold rounded-xl shadow-lg hover:shadow-xl transition-all text-lg"
                        >
                            {t('landing.cta.button')}
                            <ArrowRight className="w-5 h-5 ml-2" />
                        </Button>
                    </div>
                    <div className="flex flex-wrap justify-center gap-8 pt-8 border-t border-blue-400">
                        {[
                            { icon: <CheckCircle className="w-5 h-5" />, text: t('landing.cta.freeSignup') },
                            { icon: <Shield className="w-5 h-5" />, text: t('landing.cta.securePlatform') },
                        ].map((item, i) => (
                            <div key={i} className="flex items-center gap-2 text-white">
                                {item.icon}
                                <span className="font-semibold">{item.text}</span>
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            {/* Footer */}
            <footer className="bg-gray-900 text-gray-300 py-12">
                <div className="max-w-7xl mx-auto px-6">
                    <div className="grid md:grid-cols-4 gap-8 mb-8">
                        <div>
                            <div className="flex items-center gap-2 mb-4">
                                <div className="w-10 h-10 bg-gradient-to-br from-blue-600 to-indigo-600 rounded-lg flex items-center justify-center">
                                    <TrendingUp className="w-6 h-6 text-white" />
                                </div>
                                <span className="text-xl font-bold text-white">FinansApp</span>
                            </div>
                            <p className="text-sm">{t('landing.footer.description')}</p>
                        </div>
                    </div>
                    <div className="border-t border-gray-800 pt-8 text-center text-sm">
                        <p>{t('landing.footer.copyright')}</p>
                    </div>
                </div>
            </footer>
        </div>
    );
}