package com.financeportal.backend.News.Repository;

import com.financeportal.backend.News.Entity.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {

    Page<News> findByCategory(String category, Pageable pageable);
    // Duplicate kontrolü için
    boolean existsByTitleAndSource(String title, String source);

    List<News> findByCategory(String category);

    // Kategoriye göre sıralı liste
    List<News> findByCategoryOrderByPublishDateDesc(String category);

    // Tüm haberleri tarih sıralı getir
    List<News> findAllByOrderByPublishDateDesc();

    // Kategorileri listele
    @Query("SELECT DISTINCT n.category FROM News n")
    List<String> findDistinctCategories();

    Page<News> findByCategoryIgnoreCase(String category, Pageable pageable);

}
