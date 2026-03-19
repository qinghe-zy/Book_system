package com.library.system.dto.recommendation;

public class RecommendationBookStat {

    private Long bookId;
    private String title;
    private Long recommendedCount;
    private Long hitCount;
    private Double hitRate;

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getRecommendedCount() {
        return recommendedCount;
    }

    public void setRecommendedCount(Long recommendedCount) {
        this.recommendedCount = recommendedCount;
    }

    public Long getHitCount() {
        return hitCount;
    }

    public void setHitCount(Long hitCount) {
        this.hitCount = hitCount;
    }

    public Double getHitRate() {
        return hitRate;
    }

    public void setHitRate(Double hitRate) {
        this.hitRate = hitRate;
    }
}
