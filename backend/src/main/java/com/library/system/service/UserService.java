package com.library.system.service;

import com.library.system.dto.user.AdminUserUpdateRequest;
import com.library.system.dto.user.UpdateProfileRequest;
import com.library.system.dto.user.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse getCurrentUser();
    UserResponse updateProfile(UpdateProfileRequest request);
    List<UserResponse> listUsers();
    UserResponse adminUpdateUser(Long userId, AdminUserUpdateRequest request);
}
