import React, { useState } from 'react';
import { ChevronDown, ChevronUp, Info, HelpCircle } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Card, CardHeader, CardTitle, CardContent } from '../UI/Card';

export default function HelpSettings() {
    const { t } = useTranslation();
    const [openFaq, setOpenFaq] = useState(null);

    const faqs = [
        { question: t('help.faq.q1'), answer: t('help.faq.a1') },
        { question: t('help.faq.q2'), answer: t('help.faq.a2') },
        { question: t('help.faq.q3'), answer: t('help.faq.a3') },
        { question: t('help.faq.q4'), answer: t('help.faq.a4') },
        { question: t('help.faq.q5'), answer: t('help.faq.a5') },
        { question: t('help.faq.q6'), answer: t('help.faq.a6') },
        { question: t('help.faq.q7'), answer: t('help.faq.a7') },
    ];

    const appInfo = [
        { label: t('help.appInfo.version'),        value: '1.0.0' },
        { label: t('help.appInfo.forexSource'),    value: 'TCMB (Türkiye Cumhuriyet Merkez Bankası)' },
        { label: t('help.appInfo.stockSource'),    value: 'Yahoo Finance' },
        { label: t('help.appInfo.bondSource'),     value: 'TCMB EVDS' },
        { label: t('help.appInfo.viopSource'),     value: 'İş Yatırım' },
        { label: t('help.appInfo.backend'),        value: 'Spring Boot (Java)' },
        { label: t('help.appInfo.frontend'),       value: 'React + Tailwind CSS' },
        { label: t('help.appInfo.database'),       value: 'PostgreSQL' },
        { label: t('help.appInfo.cache'),          value: 'Redis' },
    ];

    const toggleFaq = (index) => {
        setOpenFaq(openFaq === index ? null : index);
    };

    return (
        <div className="space-y-6">

            {/* SSS */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <HelpCircle className="w-5 h-5 text-blue-600" />
                        {t('help.faqTitle')}
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="space-y-2">
                        {faqs.map((faq, index) => (
                            <div
                                key={index}
                                className="border border-gray-200 dark:border-gray-700 rounded-lg overflow-hidden"
                            >
                                <button
                                    onClick={() => toggleFaq(index)}
                                    className="w-full flex items-center justify-between px-4 py-4 text-left hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors"
                                >
                                    <span className="font-medium text-gray-900 dark:text-white">
                                        {faq.question}
                                    </span>
                                    {openFaq === index
                                        ? <ChevronUp className="w-5 h-5 text-gray-400 flex-shrink-0" />
                                        : <ChevronDown className="w-5 h-5 text-gray-400 flex-shrink-0" />
                                    }
                                </button>
                                {openFaq === index && (
                                    <div className="px-4 pb-4 text-gray-600 dark:text-gray-400 text-sm leading-relaxed border-t border-gray-100 dark:border-gray-700 pt-3">
                                        {faq.answer}
                                    </div>
                                )}
                            </div>
                        ))}
                    </div>
                </CardContent>
            </Card>

            {/* Uygulama Bilgisi */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Info className="w-5 h-5 text-blue-600" />
                        {t('help.appInfoTitle')}
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="space-y-3">
                        {appInfo.map((item, index) => (
                            <div
                                key={index}
                                className="flex items-center justify-between py-3 border-b border-gray-100 dark:border-gray-700 last:border-0"
                            >
                                <span className="text-sm text-gray-500 dark:text-gray-400">{item.label}</span>
                                <span className="text-sm font-medium text-gray-900 dark:text-white">{item.value}</span>
                            </div>
                        ))}
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}