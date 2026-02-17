package com.financeportal.backend.Aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Aspect
@Component
@Log4j2
public class LoggingAspect {

    /**
     * Tüm controller metodlarını logla
     */
    @Around("execution(* com.financeportal.backend.Controller..*(..))")
    public Object logControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {

        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getRequest();

        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String requestUri = request.getRequestURI();
        String httpMethod = request.getMethod();

        // Request log
        log.info("→ REQUEST: {} {} | Method: {}.{} | Args: {}",
                httpMethod,
                requestUri,
                className,
                methodName,
                Arrays.toString(joinPoint.getArgs()));

        long startTime = System.currentTimeMillis();
        Object result = null;

        try {
            result = joinPoint.proceed();

            long duration = System.currentTimeMillis() - startTime;

            // Success response log
            log.info("← RESPONSE: {} {} | Duration: {}ms | Status: SUCCESS",
                    httpMethod,
                    requestUri,
                    duration);

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            // Error response log
            log.error("← RESPONSE: {} {} | Duration: {}ms | Status: ERROR | Error: {}",
                    httpMethod,
                    requestUri,
                    duration,
                    e.getMessage(),
                    e);

            throw e;
        }
    }

    /**
     * Service katmanı metodlarını logla
     */
    @Around("execution(* com.financeportal.backend.Service..*(..))")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {

        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        log.debug("Service: {}.{} started", className, methodName);

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            long duration = System.currentTimeMillis() - startTime;
            log.debug("Service: {}.{} completed in {}ms", className, methodName, duration);

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Service: {}.{} failed after {}ms: {}",
                    className, methodName, duration, e.getMessage(), e);
            throw e;
        }
    }
}
