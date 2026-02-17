package com.financeportal.backend.News.Service;

import com.financeportal.backend.News.DTO.NewsRequestDTO;
import com.financeportal.backend.News.DTO.NewsResponseDTO;
import com.financeportal.backend.News.DTO.PageResponseDTO;


public interface NewsService {

    PageResponseDTO<NewsResponseDTO> getAllNews(int page, int size);

    PageResponseDTO<NewsResponseDTO> getNewsByCategory(String category, int page, int size);

    NewsResponseDTO getNewsById(Long id);

    NewsResponseDTO createNews(NewsRequestDTO requestDTO);

    NewsResponseDTO updateNews(Long id, NewsRequestDTO requestDTO);

    void deleteNews(Long id);
}