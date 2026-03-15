package com.financeportal.backend.User.Service;

import com.financeportal.backend.User.DTO.*;

public interface AuthService {

    RegisterResponseDTO registerUser(RegisterRequestDTO request);
    PasswordResetResponseDTO sendPasswordResetEmail(ForgotPasswordRequestDTO request);

    PasswordResetResponseDTO resetPassword(ResetPasswordRequestDTO request);
}
