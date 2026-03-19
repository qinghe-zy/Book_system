package com.library.system.service.impl;

import com.library.system.dto.auth.AuthResponse;
import com.library.system.dto.auth.LoginRequest;
import com.library.system.dto.auth.RegisterRequest;
import com.library.system.entity.User;
import com.library.system.entity.enums.Role;
import com.library.system.exception.BusinessException;
import com.library.system.repository.UserRepository;
import com.library.system.security.JwtTokenProvider;
import com.library.system.security.LoginUser;
import com.library.system.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("邮箱已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRegisterTime(LocalDateTime.now());
        user.setRole(Role.USER);
        userRepository.save(user);

        return toAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getPassword(), user.getRole());
        String token = jwtTokenProvider.createToken(loginUser);

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole().name());
        return response;
    }
}
