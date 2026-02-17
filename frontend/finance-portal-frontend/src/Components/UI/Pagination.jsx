import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from './Button';

export default function Pagination({
                                       currentPage,
                                       totalPages,
                                       onPageChange,
                                       totalElements = 0,
                                       pageSize = 20
                                   }) {

    const startItem = currentPage * pageSize + 1;
    const endItem = Math.min((currentPage + 1) * pageSize, totalElements);

    const getPageNumbers = () => {
        const pages = [];
        const maxVisible = 5;

        let start = Math.max(0, currentPage - Math.floor(maxVisible / 2));
        let end = Math.min(totalPages - 1, start + maxVisible - 1);

        if (end - start < maxVisible - 1) {
            start = Math.max(0, end - maxVisible + 1);
        }

        for (let i = start; i <= end; i++) {
            pages.push(i);
        }

        return pages;
    };

    if (totalPages <= 1) return null;

    return (
        <div className="flex items-center justify-between border-t border-gray-200 bg-white px-4 py-3 sm:px-6 rounded-b-lg">

            {/* Info */}
            <div className="flex flex-1 justify-between sm:hidden">
                <Button
                    onClick={() => onPageChange(currentPage - 1)}
                    disabled={currentPage === 0}
                    variant="outline"
                    size="sm"
                >
                    Önceki
                </Button>
                <Button
                    onClick={() => onPageChange(currentPage + 1)}
                    disabled={currentPage === totalPages - 1}
                    variant="outline"
                    size="sm"
                >
                    Sonraki
                </Button>
            </div>

            <div className="hidden sm:flex sm:flex-1 sm:items-center sm:justify-between">
                <div>
                    <p className="text-sm text-gray-700">
                        <span className="font-medium">{startItem}</span>
                        {' - '}
                        <span className="font-medium">{endItem}</span>
                        {' / '}
                        <span className="font-medium">{totalElements}</span>
                        {' sonuç'}
                    </p>
                </div>

                <div>
                    <nav className="isolate inline-flex -space-x-px rounded-md shadow-sm" aria-label="Pagination">

                        {/* Previous Button */}
                        <button
                            onClick={() => onPageChange(currentPage - 1)}
                            disabled={currentPage === 0}
                            className="relative inline-flex items-center rounded-l-md px-2 py-2 text-gray-400 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            <ChevronLeft className="h-5 w-5" />
                        </button>

                        {/* Page Numbers */}
                        {getPageNumbers().map((page) => (
                            <button
                                key={page}
                                onClick={() => onPageChange(page)}
                                className={`relative inline-flex items-center px-4 py-2 text-sm font-semibold ${
                                    page === currentPage
                                        ? 'z-10 bg-blue-600 text-white focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600'
                                        : 'text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50'
                                }`}
                            >
                                {page + 1}
                            </button>
                        ))}

                        {/* Next Button */}
                        <button
                            onClick={() => onPageChange(currentPage + 1)}
                            disabled={currentPage === totalPages - 1}
                            className="relative inline-flex items-center rounded-r-md px-2 py-2 text-gray-400 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            <ChevronRight className="h-5 w-5" />
                        </button>
                    </nav>
                </div>
            </div>
        </div>
    );
}