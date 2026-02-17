package com.financeportal.backend.News.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class NewsRequestDTO {
    @NotBlank
    private String title;

    @NotBlank
    private String content;

    private String source;
    private String category;

}
