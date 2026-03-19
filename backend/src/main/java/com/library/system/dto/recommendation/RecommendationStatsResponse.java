package com.library.system.dto.recommendation;

import java.util.List;

public class RecommendationStatsResponse {

    private Long totalRecommendations;
    private Long hitRecommendations;
    private Double overallHitRate;
    private Long coveredUsers;
    private List<RecommendationBookStat> topBooks;

    public Long getTotalRecommendations() {
        return totalRecommendations;
    }

    public void setTotalRecommendations(Long totalRecommendations) {
        this.totalRecommendations = totalRecommendations;
    }

    public Long getHitRecommendations() {
        return hitRecommendations;
    }

    public void setHitRecommendations(Long hitRecommendations) {
        this.hitRecommendations = hitRecommendations;
    }

    public Double getOverallHitRate() {
        return overallHitRate;
    }

    public void setOverallHitRate(Double overallHitRate) {
        this.overallHitRate = overallHitRate;
    }

    public Long getCoveredUsers() {
        return coveredUsers;
    }

    public void setCoveredUsers(Long coveredUsers) {
        this.coveredUsers = coveredUsers;
    }

    public List<RecommendationBookStat> getTopBooks() {
        return topBooks;
    }

    public void setTopBooks(List<RecommendationBookStat> topBooks) {
        this.topBooks = topBooks;
    }
}
