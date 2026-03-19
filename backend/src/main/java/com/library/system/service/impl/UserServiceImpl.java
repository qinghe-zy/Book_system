package com.library.system.service.impl;

import com.library.system.dto.user.AdminUserUpdateRequest;
import com.library.system.dto.user.UpdateProfileRequest;
import com.library.system.dto.user.UserResponse;
import com.library.system.entity.User;
import com.library.system.entity.enums.Role;
import com.library.system.exception.BusinessException;
import com.library.system.repository.UserRepository;
import com.library.system.security.LoginUser;
import com.library.system.security.SecurityUtils;
import com.library.system.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse getCurrentUser() {
        LoginUser loginUser = SecurityUtils.currentUser();
        User user = userRepository.findById(loginUser.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        return DtoMapper.toUserResponse(user);
    }

    @Override
    public UserResponse updateProfile(UpdateProfileRequest request) {
        LoginUser loginUser = SecurityUtils.currentUser();
        User user = userRepository.findById(loginUser.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        user.setEmail(request.getEmail());
        user.setInterestTags(request.getInterestTags());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        userRepository.save(user);
        return DtoMapper.toUserResponse(user);
    }

    @Override
    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream().map(DtoMapper::toUserResponse).toList();
    }

    @Override
    public UserResponse adminUpdateUser(Long userId, AdminUserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        user.setEmail(request.getEmail());
        try {
            user.setRole(Role.valueOf(request.getRole().toUpperCase()));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("角色必须是 USER 或 ADMIN");
        }

        userRepository.save(user);
        return DtoMapper.toUserResponse(user);
    }
}
