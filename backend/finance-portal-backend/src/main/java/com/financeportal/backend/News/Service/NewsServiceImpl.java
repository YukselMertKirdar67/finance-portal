package com.financeportal.backend.News.Service;

import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.News.DTO.NewsRequestDTO;
import com.financeportal.backend.News.DTO.NewsResponseDTO;
import com.financeportal.backend.News.DTO.PageResponseDTO;
import com.financeportal.backend.News.Entity.News;
import com.financeportal.backend.News.Mapper.NewsMapper;
import com.financeportal.backend.News.Repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;

    @Override
    @Cacheable(
            value = "newsByCategory",
            key = "'cat:' + #category + ':page:' + #page + ':size:' + #size"
    )
    public PageResponseDTO<NewsResponseDTO> getNewsByCategory(
            String category, int page, int size
    ) {
        Pageable pageable = PageRequest.of(
                page, size,
                Sort.by(Sort.Order.desc("publishDate"), Sort.Order.desc("id"))
        );

        Page<News> newsPage = newsRepository.findByCategoryIgnoreCase(category, pageable);

        List<NewsResponseDTO> content = newsPage.getContent().stream()
                .map(newsMapper::toResponseDto)
                .toList();

        return new PageResponseDTO<>(
                content,
                newsPage.getNumber(),
                newsPage.getSize(),
                newsPage.getTotalElements(),
                newsPage.getTotalPages(),
                newsPage.isLast()
        );
    }

    @Override
    @Cacheable(
            value = "allNews",
            key = "'page:' + #page + ':size:' + #size"
    )
    public PageResponseDTO<NewsResponseDTO> getAllNews(int page, int size) {
        Pageable pageable = PageRequest.of(
                page, size,
                Sort.by(Sort.Order.desc("publishDate"), Sort.Order.desc("id"))
        );

        Page<News> newsPage = newsRepository.findAll(pageable);

        List<NewsResponseDTO> content = newsPage.getContent().stream()
                .map(newsMapper::toResponseDto)
                .toList();

        return new PageResponseDTO<>(
                content,
                newsPage.getNumber(),
                newsPage.getSize(),
                newsPage.getTotalElements(),
                newsPage.getTotalPages(),
                newsPage.isLast()
        );
    }

    @Override
    @Cacheable(value = "news", key = "'by-id:' + #id")
    public NewsResponseDTO getNewsById(Long id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Haber bulunamadı. ID: " + id
                ));
        return newsMapper.toResponseDto(news);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "news", allEntries = true),
            @CacheEvict(value = "allNews", allEntries = true),
            @CacheEvict(value = "newsByCategory", allEntries = true)
    })
    public NewsResponseDTO createNews(NewsRequestDTO requestDTO) {
        News news = newsMapper.toEntity(requestDTO);
        validateNews(news);
        news.setId(null);
        return newsMapper.toResponseDto(newsRepository.save(news));
    }

    @Override
    @Caching(
            put = { @CachePut(value = "news", key = "'by-id:' + #id") },
            evict = {
                    @CacheEvict(value = "allNews", allEntries = true),
                    @CacheEvict(value = "newsByCategory", allEntries = true)
            }
    )
    public NewsResponseDTO updateNews(Long id, NewsRequestDTO requestDTO) {
        News existing = newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Haber bulunamadı. ID: " + id
                ));

        News updated = newsMapper.toEntity(requestDTO);
        validateNews(updated);

        existing.setTitle(updated.getTitle());
        existing.setContent(updated.getContent());
        existing.setSource(updated.getSource());
        existing.setCategory(updated.getCategory());
        existing.setImageUrl(updated.getImageUrl());

        return newsMapper.toResponseDto(newsRepository.save(existing));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "news", key = "'by-id:' + #id"),
            @CacheEvict(value = "allNews", allEntries = true),
            @CacheEvict(value = "newsByCategory", allEntries = true)
    })
    public void deleteNews(Long id) {
        News existing = newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Haber bulunamadı. ID: " + id
                ));
        newsRepository.delete(existing);
    }

    private void validateNews(News news) {
        if (news.getTitle() == null || news.getTitle().isBlank()) {
            throw new IllegalArgumentException("Haber başlığı boş olamaz.");
        }
        if (news.getContent() == null || news.getContent().isBlank()) {
            throw new IllegalArgumentException("Haber içeriği boş olamaz.");
        }
    }
}
