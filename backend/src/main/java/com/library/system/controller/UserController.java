package com.library.system.controller;

import com.library.system.dto.common.ApiResponse;
import com.library.system.dto.user.AdminUserUpdateRequest;
import com.library.system.dto.user.UpdateProfileRequest;
import com.library.system.dto.user.UserResponse;
import com.library.system.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users/me")
    public ApiResponse<UserResponse> me() {
        return ApiResponse.success(userService.getCurrentUser());
    }

    @PutMapping("/users/me")
    public ApiResponse<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success("更新成功", userService.updateProfile(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users")
    public ApiResponse<List<UserResponse>> listUsers() {
        return ApiResponse.success(userService.listUsers());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/users/{userId}")
    public ApiResponse<UserResponse> adminUpdateUser(@PathVariable Long userId,
                                                     @Valid @RequestBody AdminUserUpdateRequest request) {
        return ApiResponse.success("更新成功", userService.adminUpdateUser(userId, request));
    }
}
