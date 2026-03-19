package com.library.system.dto.dashboard;

import com.library.system.dto.book.BookResponse;
import com.library.system.dto.borrow.BorrowRecordResponse;
import com.library.system.dto.recommendation.RecommendationResponse;
import com.library.system.dto.recommendation.RecommendationStatsResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * 首页聚合接口响应。
 * 统一承载用户端与管理员端 Dashboard 需要的摘要信息。
 */
public class DashboardOverviewResponse {

    private boolean admin;
    private List<DashboardStatCard> statCards = new ArrayList<>();
    private DashboardNoticeSummary noticeSummary;
    private List<BookResponse> highlightBooks = new ArrayList<>();
    private List<BookResponse> recentBooks = new ArrayList<>();
    private List<BorrowRecordResponse> recentBorrowRecords = new ArrayList<>();
    private List<RecommendationResponse> recommendations = new ArrayList<>();
    private RecommendationStatsResponse recommendationStats;

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public List<DashboardStatCard> getStatCards() {
        return statCards;
    }

    public void setStatCards(List<DashboardStatCard> statCards) {
        this.statCards = statCards;
    }

    public DashboardNoticeSummary getNoticeSummary() {
        return noticeSummary;
    }

    public void setNoticeSummary(DashboardNoticeSummary noticeSummary) {
        this.noticeSummary = noticeSummary;
    }

    public List<BookResponse> getHighlightBooks() {
        return highlightBooks;
    }

    public void setHighlightBooks(List<BookResponse> highlightBooks) {
        this.highlightBooks = highlightBooks;
    }

    public List<BookResponse> getRecentBooks() {
        return recentBooks;
    }

    public void setRecentBooks(List<BookResponse> recentBooks) {
        this.recentBooks = recentBooks;
    }

    public List<BorrowRecordResponse> getRecentBorrowRecords() {
        return recentBorrowRecords;
    }

    public void setRecentBorrowRecords(List<BorrowRecordResponse> recentBorrowRecords) {
        this.recentBorrowRecords = recentBorrowRecords;
    }

    public List<RecommendationResponse> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<RecommendationResponse> recommendations) {
        this.recommendations = recommendations;
    }

    public RecommendationStatsResponse getRecommendationStats() {
        return recommendationStats;
    }

    public void setRecommendationStats(RecommendationStatsResponse recommendationStats) {
        this.recommendationStats = recommendationStats;
    }
}
