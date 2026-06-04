package com.financeportal.backend.News.Mapper;

import com.financeportal.backend.News.DTO.NewsRequestDTO;
import com.financeportal.backend.News.DTO.NewsResponseDTO;
import com.financeportal.backend.News.Entity.News;
import org.mapstruct.*;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface NewsMapper {

    @Mapping(target = "publishDate", expression = "java(news.getPublishDate() != null ? news.getPublishDate() : java.time.LocalDateTime.now())")
    @Mapping(source = "titleEn", target = "titleEn")
    @Mapping(source = "contentEn", target = "contentEn")
    NewsResponseDTO toResponseDto(News news);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publishDate", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "titleEn", ignore = true)
    @Mapping(target = "contentEn", ignore = true)
    News toEntity(NewsRequestDTO dto);
}