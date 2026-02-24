package com.financeportal.backend.Home;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
@Tag(name = "Home API", description = "Anasayfa verileri")
public class HomeController {

    private final HomeService homeService;

    /**
     * Anasayfa için tüm verileri getir
     */
    @GetMapping
    @Operation(summary = "Anasayfa verileri")
    public ResponseEntity<HomePageDTO> getHomePageData() {
        return ResponseEntity.ok(homeService.getHomePageData());
    }
}
