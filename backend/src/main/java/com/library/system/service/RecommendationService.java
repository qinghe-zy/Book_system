package com.library.system.service;

import com.library.system.dto.recommendation.RecommendationResponse;
import com.library.system.dto.recommendation.RecommendationStatsResponse;

import java.util.List;

public interface RecommendationService {
    List<RecommendationResponse> myRecommendations(int topN);
    List<RecommendationResponse> refreshForUser(Long userId, int topN);
    void refreshAllBySchedule();
    RecommendationStatsResponse recommendationStats();
}
