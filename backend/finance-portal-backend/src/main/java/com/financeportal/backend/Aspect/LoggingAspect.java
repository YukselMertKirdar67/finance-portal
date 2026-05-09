package com.financeportal.backend.Aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Log4j2
public class LoggingAspect {

    /**
     * Tüm controller metodlarının HTTP istek/yanıt bilgilerini ve
     * çalışma sürelerini otomatik olarak loglar.
     * Hata durumunda ERROR seviyesinde loglar.
     */
    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {

        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getRequest();

        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String requestUri = request.getRequestURI();
        String httpMethod = request.getMethod();

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            long duration = System.currentTimeMillis() - startTime;

            // Yavaş istekleri uyar (500ms üzeri)
            if (duration > 500) {
                log.warn("⚠️ SLOW REQUEST: {} {} | {}.{} | {}ms",
                        httpMethod, requestUri, className, methodName, duration);
            } else {
                log.info("← {} {} | {}.{} | {}ms | OK",
                        httpMethod, requestUri, className, methodName, duration);
            }

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ {} {} | {}.{} | {}ms | ERROR: {}",
                    httpMethod, requestUri, className, methodName, duration, e.getMessage());
            throw e;
        }
    }
}