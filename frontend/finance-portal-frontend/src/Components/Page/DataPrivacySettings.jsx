import React, { useState } from 'react';
import { Download, Trash2, Loader2, CheckCircle, XCircle, AlertTriangle } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '../UI/Card';
import { Button } from '../UI/Button';
import { exportUserData, deleteAccount } from '../../API/userApi';
import { useAuth } from '../../context/AuthContext';

export default function DataPrivacySettings() {
    const { logout } = useAuth();

    const [exporting, setExporting] = useState(false);
    const [showDeleteModal, setShowDeleteModal] = useState(false);
    const [deleteConfirmText, setDeleteConfirmText] = useState('');
    const [deleting, setDeleting] = useState(false);
    const [toast, setToast] = useState(null);

    const showToast = (message, type = 'success') => {
        setToast({ message, type });
        setTimeout(() => setToast(null), 5000);
    };

    const handleExport = async () => {
        setExporting(true);
        try {
            const blob = await exportUserData();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'finance-portal-data.json';
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);
            showToast('Verileriniz başarıyla indirildi');
        } catch {
            showToast('Veri dışa aktarma başarısız', 'error');
        } finally {
            setExporting(false);
        }
    };

    const handleDeleteAccount = async () => {
        if (deleteConfirmText !== 'HESABIMI SİL') return;

        setDeleting(true);
        try {
            await deleteAccount();
            showToast('Hesabınız silindi. Çıkış yapılıyor...');
            setTimeout(() => logout(), 2000);
        } catch {
            showToast('Hesap silinemedi', 'error');
            setDeleting(false);
        }
    };

    return (
        <div className="space-y-6">

            {/* Toast */}
            {toast && (
                <div className={`fixed top-6 right-6 z-50 flex items-center gap-3 px-6 py-4 rounded-lg shadow-lg ${
                    toast.type === 'success'
                        ? 'bg-green-50 dark:bg-green-900/30 text-green-800 dark:text-green-300 border border-green-200 dark:border-green-800'
                        : 'bg-red-50 dark:bg-red-900/30 text-red-800 dark:text-red-300 border border-red-200 dark:border-red-800'
                }`}>
                    {toast.type === 'success' ? <CheckCircle className="w-5 h-5" /> : <XCircle className="w-5 h-5" />}
                    <span>{toast.message}</span>
                </div>
            )}

            {/* Veri Dışa Aktarma */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Download className="w-5 h-5 text-blue-600" />
                        Verilerimi Dışa Aktar
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">
                        Tüm portföy ve işlem verilerinizi JSON formatında indirin. Bu dosya; portföylerinizi, işlem geçmişinizi ve hesap bilgilerinizi içerir.
                    </p>
                    <div className="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-4 mb-4">
                        <p className="text-sm text-blue-800 dark:text-blue-300 font-medium mb-2">Dışa aktarılan veriler:</p>
                        <ul className="text-sm text-blue-700 dark:text-blue-400 space-y-1">
                            <li>• Hesap bilgileri (kullanıcı adı, e-posta)</li>
                            <li>• Tüm portföyler ve detayları</li>
                            <li>• İşlem geçmişi (alış/satış)</li>
                            <li>• Dışa aktarma tarihi</li>
                        </ul>
                    </div>
                    <Button onClick={handleExport} disabled={exporting} variant="outline">
                        {exporting ? (
                            <><Loader2 className="w-4 h-4 mr-2 animate-spin" />Hazırlanıyor...</>
                        ) : (
                            <><Download className="w-4 h-4 mr-2" />Verileri İndir (JSON)</>
                        )}
                    </Button>
                </CardContent>
            </Card>

            {/* Hesap Silme */}
            <Card className="border-red-200 dark:border-red-800">
                <CardHeader>
                    <CardTitle className="flex items-center gap-2 text-red-600 dark:text-red-400">
                        <Trash2 className="w-5 h-5" />
                        Hesabı Sil
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4 mb-4">
                        <div className="flex items-start gap-3">
                            <AlertTriangle className="w-5 h-5 text-red-600 dark:text-red-400 flex-shrink-0 mt-0.5" />
                            <div>
                                <p className="text-sm font-semibold text-red-800 dark:text-red-300 mb-1">
                                    Bu işlem geri alınamaz!
                                </p>
                                <ul className="text-sm text-red-700 dark:text-red-400 space-y-1">
                                    <li>• Tüm portföyleriniz silinecek</li>
                                    <li>• Tüm işlem geçmişiniz silinecek</li>
                                    <li>• Takip listeniz silinecek</li>
                                    <li>• Hesabınız kalıcı olarak kapatılacak</li>
                                </ul>
                            </div>
                        </div>
                    </div>
                    <Button
                        variant="outline"
                        className="border-red-300 text-red-600 hover:bg-red-50 dark:border-red-700 dark:text-red-400 dark:hover:bg-red-900/20"
                        onClick={() => setShowDeleteModal(true)}
                    >
                        <Trash2 className="w-4 h-4 mr-2" />
                        Hesabımı Sil
                    </Button>
                </CardContent>
            </Card>

            {/* Hesap Silme Modal */}
            {showDeleteModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
                    <div className="bg-white dark:bg-gray-800 rounded-xl shadow-xl max-w-md w-full p-6">
                        <div className="flex items-center gap-3 mb-4">
                            <div className="w-12 h-12 bg-red-100 dark:bg-red-900/30 rounded-full flex items-center justify-center">
                                <AlertTriangle className="w-6 h-6 text-red-600 dark:text-red-400" />
                            </div>
                            <div>
                                <h3 className="text-xl font-bold text-gray-900 dark:text-white">Hesabı Sil</h3>
                                <p className="text-sm text-gray-500 dark:text-gray-400">Bu işlem geri alınamaz</p>
                            </div>
                        </div>

                        <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">
                            Hesabınızı silmek istediğinizden emin misiniz? Devam etmek için aşağıya
                            <span className="font-bold text-red-600 dark:text-red-400"> HESABIMI SİL </span>
                            yazın.
                        </p>

                        <input
                            type="text"
                            value={deleteConfirmText}
                            onChange={(e) => setDeleteConfirmText(e.target.value)}
                            className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white mb-4 focus:outline-none focus:ring-2 focus:ring-red-500"
                            placeholder="HESABIMI SİL"
                        />

                        <div className="flex gap-3">
                            <Button
                                variant="outline"
                                className="flex-1 dark:border-gray-600 dark:text-gray-300"
                                onClick={() => {
                                    setShowDeleteModal(false);
                                    setDeleteConfirmText('');
                                }}
                                disabled={deleting}
                            >
                                İptal
                            </Button>
                            <Button
                                className="flex-1 bg-red-600 hover:bg-red-700 text-white"
                                onClick={handleDeleteAccount}
                                disabled={deleteConfirmText !== 'HESABIMI SİL' || deleting}
                            >
                                {deleting ? (
                                    <><Loader2 className="w-4 h-4 mr-2 animate-spin" />Siliniyor...</>
                                ) : (
                                    <><Trash2 className="w-4 h-4 mr-2" />Hesabı Kalıcı Sil</>
                                )}
                            </Button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}