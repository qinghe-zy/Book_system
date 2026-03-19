package com.library.system.service;

import com.library.system.dto.auth.AuthResponse;
import com.library.system.dto.auth.LoginRequest;
import com.library.system.dto.auth.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
