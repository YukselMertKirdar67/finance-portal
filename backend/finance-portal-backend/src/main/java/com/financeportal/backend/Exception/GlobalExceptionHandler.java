package com.financeportal.backend.Exception;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final MessageSource messageSource;

  // 404 - Kayıt bulunamadı
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleResourceNotFound(
          ResourceNotFoundException ex) {

    ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage()
    );

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  // 400 - Validation / Hatalı istek
  @ExceptionHandler({
          IllegalArgumentException.class,
          MethodArgumentNotValidException.class
  })
  public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {

    String message;

    if (ex instanceof MethodArgumentNotValidException e) {
      message = e.getBindingResult()
              .getFieldError()
              .getDefaultMessage();
    } else {
      message = ex.getMessage();
    }

    ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            message
    );

    return ResponseEntity.badRequest().body(error);
  }

  // 401 - Kimlik doğrulama hatası
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthenticationException(
          AuthenticationException ex, Locale locale) {

    ErrorResponse error = new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            messageSource.getMessage("error.unauthorized", null, locale)
    );

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
  }

  // 403 - Yetkisiz erişim
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(
          AccessDeniedException ex, Locale locale) {

    ErrorResponse error = new ErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            messageSource.getMessage("error.forbidden", null, locale)
    );

    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
  }

  // 400 - İş kuralı ihlali
  @ExceptionHandler(BusinessRuleException.class)
  public ResponseEntity<ErrorResponse> handleBusinessRuleException(
          BusinessRuleException ex) {

    ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage()
    );

    return ResponseEntity.badRequest().body(error);
  }

  // 500 - Beklenmeyen hatalar
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneralException(
          Exception ex, Locale locale) {

    ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            messageSource.getMessage("error.internal", null, locale)
    );

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }
}
