package com.financeportal.backend.Home;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
@Log4j2
@Tag(name = "Home API", description = "Anasayfa verileri")
public class HomeController {

    private final HomeService homeService;

    /**
     * Anasayfa için tüm verileri tek endpoint'ten getirir.
     * Piyasa özeti, kazananlar, kaybedenler, haberler ve kategori özetleri içerir.
     */
    @GetMapping
    @Operation(summary = "Anasayfa verileri")
    public ResponseEntity<HomePageDTO> getHomePageData() {
        log.info("Fetching home page data");
        return ResponseEntity.ok(homeService.getHomePageData());
    }
}
