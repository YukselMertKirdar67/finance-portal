package com.financeportal.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableCaching
@SpringBootApplication
@EnableScheduling
public class FinancePortalBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinancePortalBackendApplication.class, args);
    }

}
