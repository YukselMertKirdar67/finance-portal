import React, { useState } from 'react';
import { ChevronDown, ChevronUp, Info, HelpCircle } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '../UI/Card';

const faqs = [
    {
        question: 'Portföy nasıl oluşturulur?',
        answer: 'Sol menüden "Portföyler" sayfasına gidin. Sağ üstteki "Yeni Portföy" butonuna tıklayın. Portföy adı, açıklama ve para birimi seçerek oluşturabilirsiniz.'
    },
    {
        question: 'Portföye enstrüman nasıl eklenir?',
        answer: 'Portföy detay sayfasında "İşlem Ekle" butonuna tıklayın. Enstrüman arayın, miktar ve fiyat girerek alış veya satış işlemi ekleyebilirsiniz.'
    },
    {
        question: 'Hangi para birimlerini kullanabilirim?',
        answer: 'Portföy oluştururken TRY, USD veya EUR seçebilirsiniz. Tüm hesaplamalar seçtiğiniz para birimine göre otomatik dönüştürülür.'
    },
    {
        question: 'Enstrüman karşılaştırma nasıl yapılır?',
        answer: 'Sol menüden "Karşılaştırma" sayfasına gidin. İki enstrüman seçin ve zaman aralığını belirleyin. Grafik ve performans metrikleri otomatik hesaplanır.'
    },
    {
        question: 'Takip listesi ne işe yarar?',
        answer: 'Enstrüman detay sayfasındaki "Takip Et" butonuyla enstrümanları takip listenize ekleyebilirsiniz. Sol menüdeki Watchlist sayfasından takip ettiğiniz enstrümanları görüntüleyebilirsiniz.'
    },
    {
        question: 'Geçmiş fiyat verileri nereden geliyor?',
        answer: 'Geçmiş fiyat verileri Yahoo Finance\'den çekilmektedir. Döviz kurları için TCMB arşiv verileri kullanılmaktadır. 1 yıllık geçmiş veri mevcuttur.'
    },
    {
        question: 'İşlem silersem ne olur?',
        answer: 'İşlemler soft delete ile silinir, yani veri kaybolmaz ancak hesaplamalara dahil edilmez. Holding miktarları buna göre güncellenmez.'
    },
];

const appInfo = [
    { label: 'Versiyon', value: '1.0.0' },
    { label: 'Döviz Veri Kaynağı', value: 'TCMB (Türkiye Cumhuriyet Merkez Bankası)' },
    { label: 'Hisse & Kripto Veri Kaynağı', value: 'Yahoo Finance' },
    { label: 'Tahvil/Bono Veri Kaynağı', value: 'TCMB EVDS' },
    { label: 'Backend', value: 'Spring Boot (Java)' },
    { label: 'Frontend', value: 'React + Tailwind CSS' },
    { label: 'Veritabanı', value: 'PostgreSQL' },
    { label: 'Cache', value: 'Redis' },
];

export default function HelpSettings() {
    const [openFaq, setOpenFaq] = useState(null);

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
                        Sık Sorulan Sorular
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
                        Uygulama Bilgisi
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