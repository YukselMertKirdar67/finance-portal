package com.financeportal.backend.News.Mapper;

import com.financeportal.backend.News.DTO.NewsRequestDTO;
import com.financeportal.backend.News.DTO.NewsResponseDTO;
import com.financeportal.backend.News.Entity.News;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class NewsMapper {

    public NewsResponseDTO toResponseDto(News news) {
        return new NewsResponseDTO(
                news.getId(),
                news.getTitle(),
                news.getContent(),
                news.getSource(),
                news.getCategory(),
                news.getImageUrl(),
                news.getPublishDate() != null ? news.getPublishDate() : LocalDateTime.now()
        );
    }

    public News toEntity(NewsRequestDTO dto) {
        News news = new News();
        news.setTitle(dto.getTitle());
        news.setContent(dto.getContent());
        news.setSource(dto.getSource());
        news.setCategory(dto.getCategory());
        news.setImageUrl(dto.getImageUrl());
        news.setPublishDate(LocalDateTime.now());
        return news;
    }
}

