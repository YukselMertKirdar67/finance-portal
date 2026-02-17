package com.financeportal.backend.News.Service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ImageService {

    private static final Map<String, String> CATEGORY_PLACEHOLDERS = Map.of(
            "FINANS", "https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?w=800",
            "KRIPTO", "https://images.unsplash.com/photo-1518546305927-5a555bb7020d?w=800",
            "DOVIZ", "https://images.unsplash.com/photo-1526304640581-d334cdbbf45e?w=800",
            "BIRLESME", "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=800"
    );

    public String getImageUrl(String apiImage, String category) {
        if (apiImage != null && !apiImage.isEmpty()) {
            return apiImage;
        }

        return CATEGORY_PLACEHOLDERS.getOrDefault(
                category,
                "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=800"
        );
    }
}
