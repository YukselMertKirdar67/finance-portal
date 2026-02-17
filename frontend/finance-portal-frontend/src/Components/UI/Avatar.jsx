import React from 'react';

export const Avatar = ({ children, className = '' }) => {
    return (
        <div className={`relative inline-block ${className}`}>
            {children}
        </div>
    );
};

export const AvatarFallback = ({ children, className = '' }) => {
    return (
        <div className={`flex items-center justify-center w-full h-full rounded-full ${className}`}>
            {children}
        </div>
    );
};