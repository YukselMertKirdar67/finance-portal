import React, { useState } from 'react';
import { useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { User, Lock, Bell, Eye, Database, HelpCircle } from 'lucide-react';
import { Card } from '../UI/Card';
import AccountSettings from './AccountSettings';
import AppearanceSettings from './AppearanceSettings';
import HelpSettings from './HelpSettings';
import DataPrivacySettings from './DataPrivacySettings';
import NotificationSettings from './NotificationSettings';
import SecuritySettings from './SecuritySettings';

export default function SettingsPage() {
    const location = useLocation();
    const { t } = useTranslation();
    const [activeTab, setActiveTab] = useState(location.state?.tab || 'account');

    const tabs = [
        { id: 'account',       label: t('settings.account'),       icon: <User className="w-5 h-5" /> },
        { id: 'security',      label: t('settings.security'),      icon: <Lock className="w-5 h-5" /> },
        { id: 'notifications', label: t('settings.notifications'), icon: <Bell className="w-5 h-5" /> },
        { id: 'appearance',    label: t('settings.appearance'),    icon: <Eye className="w-5 h-5" /> },
        { id: 'data',          label: t('settings.data'),          icon: <Database className="w-5 h-5" /> },
        { id: 'help',          label: t('settings.help'),          icon: <HelpCircle className="w-5 h-5" /> }
    ];

    return (
        <div className="min-h-screen bg-gray-50 dark:bg-gray-950 p-8">
            <div className="max-w-7xl mx-auto">

                {/* Header */}
                <div className="mb-8">
                    <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">
                        {t('nav.settings')}
                    </h1>
                    <p className="text-gray-600 dark:text-gray-400">{t('settings.subtitle')}</p>
                </div>

                <div className="grid lg:grid-cols-4 gap-6">

                    {/* Sidebar */}
                    <Card className="lg:col-span-1 h-fit">
                        <nav className="p-2">
                            {tabs.map(tab => (
                                <button
                                    key={tab.id}
                                    onClick={() => setActiveTab(tab.id)}
                                    className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors mb-1 ${
                                        activeTab === tab.id
                                            ? 'bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400'
                                            : 'text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-800'
                                    }`}
                                >
                                    {tab.icon}
                                    <span className="font-medium">{tab.label}</span>
                                </button>
                            ))}
                        </nav>
                    </Card>

                    {/* Content */}
                    <div className="lg:col-span-3">
                        {activeTab === 'account'       && <AccountSettings onNavigateToSecurity={() => setActiveTab('security')} />}
                        {activeTab === 'security'      && <SecuritySettings />}
                        {activeTab === 'notifications' && <NotificationSettings />}
                        {activeTab === 'appearance'    && <AppearanceSettings />}
                        {activeTab === 'data'          && <DataPrivacySettings />}
                        {activeTab === 'help'          && <HelpSettings />}
                    </div>
                </div>
            </div>
        </div>
    );
}