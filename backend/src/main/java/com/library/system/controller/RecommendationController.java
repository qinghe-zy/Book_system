package com.library.system.controller;

import com.library.system.dto.common.ApiResponse;
import com.library.system.dto.recommendation.RecommendationResponse;
import com.library.system.dto.recommendation.RecommendationStatsResponse;
import com.library.system.security.SecurityUtils;
import com.library.system.service.RecommendationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/my")
    public ApiResponse<List<RecommendationResponse>> my(@RequestParam(required = false, defaultValue = "10") int topN) {
        return ApiResponse.success(recommendationService.myRecommendations(topN));
    }

    @PostMapping("/refresh/me")
    public ApiResponse<List<RecommendationResponse>> refreshMe(@RequestParam(required = false, defaultValue = "10") int topN) {
        Long userId = SecurityUtils.currentUser().getId();
        return ApiResponse.success("刷新成功", recommendationService.refreshForUser(userId, topN));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/refresh/{userId}")
    public ApiResponse<List<RecommendationResponse>> refreshForUser(@PathVariable Long userId,
                                                                    @RequestParam(required = false, defaultValue = "10") int topN) {
        return ApiResponse.success("刷新成功", recommendationService.refreshForUser(userId, topN));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/stats")
    public ApiResponse<RecommendationStatsResponse> stats() {
        return ApiResponse.success(recommendationService.recommendationStats());
    }
}
