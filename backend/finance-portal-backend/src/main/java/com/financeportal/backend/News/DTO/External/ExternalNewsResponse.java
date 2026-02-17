package com.financeportal.backend.News.DTO.External;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ExternalNewsResponse {

    @Getter
    @Setter
    public static class Article {
        private Long id;
        private String category;
        private Long datetime;
        private String headline;
        private String image;
        private String related;
        private String source;
        private String summary;
        private String url;
    }
}