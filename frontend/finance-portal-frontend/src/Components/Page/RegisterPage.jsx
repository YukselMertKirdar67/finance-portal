import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { registerUser } from '../../API/registerApi';
import { Mail, User, Lock, UserCircle, AlertCircle, CheckCircle } from 'lucide-react';
import { useTranslation } from 'react-i18next';

const RegisterPage = () => {
    const navigate = useNavigate();
    const { t } = useTranslation();

    const [formData, setFormData] = useState({
        username: '',
        email: '',
        password: '',
        confirmPassword: '',
        firstName: '',
        lastName: ''
    });

    const [errors, setErrors] = useState({});
    const [loading, setLoading] = useState(false);
    const [successMessage, setSuccessMessage] = useState('');
    const [countdown, setCountdown] = useState(5);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        if (errors[name]) {
            setErrors(prev => ({ ...prev, [name]: '' }));
        }
    };

    const validateForm = () => {
        const newErrors = {};

        if (!formData.username.trim()) {
            newErrors.username = t('register.validation.usernameRequired');
        } else if (formData.username.length < 3) {
            newErrors.username = t('register.validation.usernameMin');
        }

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!formData.email.trim()) {
            newErrors.email = t('register.validation.emailRequired');
        } else if (!emailRegex.test(formData.email)) {
            newErrors.email = t('register.validation.emailInvalid');
        }

        if (!formData.password) {
            newErrors.password = t('register.validation.passwordRequired');
        } else if (formData.password.length < 6) {
            newErrors.password = t('register.validation.passwordMin');
        }

        if (formData.password !== formData.confirmPassword) {
            newErrors.confirmPassword = t('register.validation.passwordMismatch');
        }

        if (!formData.firstName.trim()) {
            newErrors.firstName = t('register.validation.firstNameRequired');
        }

        if (!formData.lastName.trim()) {
            newErrors.lastName = t('register.validation.lastNameRequired');
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!validateForm()) return;

        setLoading(true);
        setSuccessMessage('');

        try {
            const registerData = {
                username: formData.username,
                email: formData.email,
                password: formData.password,
                firstName: formData.firstName,
                lastName: formData.lastName
            };

            const response = await registerUser(registerData);

            if (response.success) {
                setSuccessMessage(response.message);
                setCountdown(5);

                const timer = setInterval(() => {
                    setCountdown((prev) => {
                        if (prev <= 1) {
                            clearInterval(timer);
                            navigate('/login');
                            return 0;
                        }
                        return prev - 1;
                    });
                }, 1000);
            }

        } catch (error) {
            const errorMessage = error.response?.data?.message || t('register.submitError');
            setErrors({ submit: errorMessage });
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center p-6">
            <div className="bg-white rounded-xl shadow-2xl max-w-2xl w-full p-8">

                {/* Header */}
                <div className="text-center mb-8">
                    <div className="inline-flex items-center justify-center w-16 h-16 bg-blue-600 rounded-full mb-4">
                        <UserCircle className="w-10 h-10 text-white" />
                    </div>
                    <h1 className="text-3xl font-bold text-gray-800 mb-2">{t('register.title')}</h1>
                    <p className="text-gray-600">{t('register.subtitle')}</p>
                </div>

                {/* Success Message */}
                {successMessage && (
                    <div className="mb-6 p-4 bg-green-50 border border-green-200 rounded-lg">
                        <div className="flex items-start gap-3 mb-3">
                            <CheckCircle className="w-5 h-5 text-green-600 mt-0.5 flex-shrink-0" />
                            <div>
                                <p className="text-green-800 font-medium">{t('register.success.title')}</p>
                                <p className="text-green-700 text-sm mt-1">{t('register.success.description')}</p>
                            </div>
                        </div>

                        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mt-3">
                            <div className="flex items-start gap-3">
                                <Mail className="w-5 h-5 text-blue-600 mt-0.5 flex-shrink-0" />
                                <div>
                                    <p className="text-blue-800 font-medium">{t('register.success.emailVerification')}</p>
                                    <p className="text-blue-700 text-sm mt-1">
                                        {t('register.success.emailSent')}
                                    </p>
                                    <p className="text-blue-600 text-xs mt-2">
                                        {t('register.success.spamNote')}
                                    </p>
                                </div>
                            </div>
                        </div>

                        <p className="text-green-600 text-sm mt-3 text-center">
                            {t('register.success.redirect', { count: countdown })}
                        </p>
                    </div>
                )}

                {/* Error Message */}
                {errors.submit && (
                    <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
                        <AlertCircle className="w-5 h-5 text-red-600 mt-0.5 flex-shrink-0" />
                        <p className="text-red-800">{errors.submit}</p>
                    </div>
                )}

                {/* Form */}
                <form onSubmit={handleSubmit} className="space-y-5">

                    {/* Username */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            {t('auth.username')} *
                        </label>
                        <div className="relative">
                            <User className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
                            <input
                                type="text"
                                name="username"
                                value={formData.username}
                                onChange={handleChange}
                                className={`w-full pl-11 pr-4 py-3 border rounded-lg focus:outline-none focus:ring-2 transition ${
                                    errors.username ? 'border-red-300 focus:ring-red-500' : 'border-gray-300 focus:ring-blue-500'
                                }`}
                                placeholder="johndoe"
                            />
                        </div>
                        {errors.username && <p className="text-red-600 text-sm mt-1">{errors.username}</p>}
                    </div>

                    {/* Email */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            {t('auth.email')} *
                        </label>
                        <div className="relative">
                            <Mail className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
                            <input
                                type="email"
                                name="email"
                                value={formData.email}
                                onChange={handleChange}
                                className={`w-full pl-11 pr-4 py-3 border rounded-lg focus:outline-none focus:ring-2 transition ${
                                    errors.email ? 'border-red-300 focus:ring-red-500' : 'border-gray-300 focus:ring-blue-500'
                                }`}
                                placeholder="john@example.com"
                            />
                        </div>
                        {errors.email && <p className="text-red-600 text-sm mt-1">{errors.email}</p>}
                    </div>

                    {/* First Name & Last Name */}
                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                {t('auth.firstName')} *
                            </label>
                            <input
                                type="text"
                                name="firstName"
                                value={formData.firstName}
                                onChange={handleChange}
                                className={`w-full px-4 py-3 border rounded-lg focus:outline-none focus:ring-2 transition ${
                                    errors.firstName ? 'border-red-300 focus:ring-red-500' : 'border-gray-300 focus:ring-blue-500'
                                }`}
                                placeholder="John"
                            />
                            {errors.firstName && <p className="text-red-600 text-sm mt-1">{errors.firstName}</p>}
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                {t('auth.lastName')} *
                            </label>
                            <input
                                type="text"
                                name="lastName"
                                value={formData.lastName}
                                onChange={handleChange}
                                className={`w-full px-4 py-3 border rounded-lg focus:outline-none focus:ring-2 transition ${
                                    errors.lastName ? 'border-red-300 focus:ring-red-500' : 'border-gray-300 focus:ring-blue-500'
                                }`}
                                placeholder="Doe"
                            />
                            {errors.lastName && <p className="text-red-600 text-sm mt-1">{errors.lastName}</p>}
                        </div>
                    </div>

                    {/* Password */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            {t('auth.password')} *
                        </label>
                        <div className="relative">
                            <Lock className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
                            <input
                                type="password"
                                name="password"
                                value={formData.password}
                                onChange={handleChange}
                                className={`w-full pl-11 pr-4 py-3 border rounded-lg focus:outline-none focus:ring-2 transition ${
                                    errors.password ? 'border-red-300 focus:ring-red-500' : 'border-gray-300 focus:ring-blue-500'
                                }`}
                                placeholder="••••••••"
                            />
                        </div>
                        {errors.password && <p className="text-red-600 text-sm mt-1">{errors.password}</p>}
                    </div>

                    {/* Confirm Password */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            {t('register.confirmPassword')} *
                        </label>
                        <div className="relative">
                            <Lock className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
                            <input
                                type="password"
                                name="confirmPassword"
                                value={formData.confirmPassword}
                                onChange={handleChange}
                                className={`w-full pl-11 pr-4 py-3 border rounded-lg focus:outline-none focus:ring-2 transition ${
                                    errors.confirmPassword ? 'border-red-300 focus:ring-red-500' : 'border-gray-300 focus:ring-blue-500'
                                }`}
                                placeholder="••••••••"
                            />
                        </div>
                        {errors.confirmPassword && <p className="text-red-600 text-sm mt-1">{errors.confirmPassword}</p>}
                    </div>

                    {/* Submit Button */}
                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full py-3 px-4 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-semibold rounded-lg shadow-md transition duration-200 flex items-center justify-center gap-2"
                    >
                        {loading ? (
                            <>
                                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                                {t('register.registering')}
                            </>
                        ) : (
                            t('auth.register')
                        )}
                    </button>
                </form>

                {/* Login Link */}
                <div className="mt-6 text-center">
                    <p className="text-gray-600">
                        {t('auth.hasAccount')}{' '}
                        <button
                            onClick={() => navigate('/login')}
                            className="text-blue-600 hover:text-blue-700 font-semibold"
                        >
                            {t('auth.login')}
                        </button>
                    </p>
                </div>
            </div>
        </div>
    );
};

export default RegisterPage;