package com.financeportal.backend.News.Service;

import com.financeportal.backend.News.DTO.NewsRequestDTO;
import com.financeportal.backend.News.DTO.NewsResponseDTO;
import com.financeportal.backend.News.DTO.PageResponseDTO;

import java.util.Locale;
import java.util.Map;


public interface NewsService {

    PageResponseDTO<NewsResponseDTO> getAllNews(int page, int size, Locale locale);
    PageResponseDTO<NewsResponseDTO> getNewsByCategory(String category, int page, int size, Locale locale);
    NewsResponseDTO getNewsById(Long id, Locale locale);

    long deleteAllNews();

    int deleteNewsByCategory(String category);

    Map<String, Object> refreshNews();

    Map<String, Object> getNewsStats();
}