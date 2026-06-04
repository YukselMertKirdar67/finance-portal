package com.financeportal.backend.News.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NewsResponseDTO implements Serializable {
    private Long id;
    private String title;
    private String content;
    private String source;
    private String category;
    private String imageUrl;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishDate;
    private String titleEn;
    private String contentEn;
}
