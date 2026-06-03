package com.financeportal.backend.Exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Unit Testleri")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("ResourceNotFoundException → 404 dönmeli")
    void handleResourceNotFound_Returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Kayıt bulunamadı");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo("Kayıt bulunamadı");
    }

    @Test
    @DisplayName("IllegalArgumentException → 400 dönmeli")
    void handleBadRequest_IllegalArgument_Returns400() {
        IllegalArgumentException ex = new IllegalArgumentException("Geçersiz parametre");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadRequest(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Geçersiz parametre");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException → 400 ve field mesajı dönmeli")
    void handleBadRequest_MethodArgumentNotValid_Returns400() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "Alan boş olamaz");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldError()).thenReturn(fieldError);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadRequest(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Alan boş olamaz");
    }

    @Test
    @DisplayName("AuthenticationException → 401 dönmeli")
    void handleAuthenticationException_Returns401() {
        BadCredentialsException ex = new BadCredentialsException("Kimlik doğrulama hatası");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAuthenticationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getMessage()).isEqualTo("Kimlik doğrulama başarısız.");
    }

    @Test
    @DisplayName("AccessDeniedException → 403 dönmeli")
    void handleAccessDenied_Returns403() {
        AccessDeniedException ex = new AccessDeniedException("Erişim reddedildi");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccessDenied(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(403);
        assertThat(response.getBody().getMessage()).isEqualTo("Bu kaynağa erişim yetkiniz yok.");
    }

    @Test
    @DisplayName("BusinessRuleException → 400 dönmeli")
    void handleBusinessRuleException_Returns400() {
        BusinessRuleException ex = new BusinessRuleException("İş kuralı ihlali");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessRuleException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("İş kuralı ihlali");
    }

    @Test
    @DisplayName("Genel Exception → 500 dönmeli")
    void handleGeneralException_Returns500() {
        Exception ex = new Exception("Beklenmeyen hata");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGeneralException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getMessage())
                .isEqualTo("Sunucu tarafında beklenmeyen bir hata oluştu.");
    }

    @Test
    @DisplayName("ResourceNotFoundException mesajı doğru aktarılmalı")
    void handleResourceNotFound_MessagePropagated() {
        String message = "Instrument not found with id: 999";
        ResourceNotFoundException ex = new ResourceNotFoundException(message);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFound(ex);

        assertThat(response.getBody().getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("BusinessRuleException mesajı doğru aktarılmalı")
    void handleBusinessRuleException_MessagePropagated() {
        String message = "Insufficient quantity. Available: 10, Requested: 20";
        BusinessRuleException ex = new BusinessRuleException(message);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessRuleException(ex);

        assertThat(response.getBody().getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("UnauthorizedException RuntimeException olmalı")
    void unauthorizedException_IsRuntimeException() {
        UnauthorizedException ex = new UnauthorizedException("Yetkisiz erişim");

        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("Yetkisiz erişim");
    }

    @Test
    @DisplayName("UnauthorizedException cause ile oluşturulabilmeli")
    void unauthorizedException_WithCause() {
        Throwable cause = new RuntimeException("Sebep");
        UnauthorizedException ex = new UnauthorizedException("Yetkisiz", cause);

        assertThat(ex.getMessage()).isEqualTo("Yetkisiz");
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}
