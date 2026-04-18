package com.financeportal.backend.News.DTO.External;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ExternalNewsResponse {

    private String status;
    private Integer totalResults;
    private List<Article> articles;

    @Getter
    @Setter
    public static class Article {
        private Source source;
        private String author;
        private String title;
        private String description;
        private String url;
        private String urlToImage;
        private String publishedAt;
        private String content;
    }

    @Getter
    @Setter
    public static class Source {
        private String id;
        private String name;
    }
}