package com.financeportal.backend.User.Service;

import com.financeportal.backend.User.DTO.*;

public interface AuthService {

    RegisterResponseDTO registerUser(RegisterRequestDTO request);
    PasswordResetResponseDTO sendPasswordResetEmail(ForgotPasswordRequestDTO request);

    PasswordResetResponseDTO resetPassword(ResetPasswordRequestDTO request);

    EmailVerificationResponseDTO sendVerificationEmail(EmailVerificationRequestDTO request);

    EmailVerificationResponseDTO checkEmailVerification(String email);

    LoginResponseDTO login(LoginRequestDTO request);

    RefreshTokenResponseDTO refreshToken(RefreshTokenRequestDTO request);

    ChangePasswordResponseDTO changePassword(String userId, ChangePasswordRequestDTO request);
}
