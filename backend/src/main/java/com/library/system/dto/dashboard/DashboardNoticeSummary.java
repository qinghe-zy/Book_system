package com.library.system.dto.dashboard;

/**
 * Dashboard 提醒区摘要。
 */
public class DashboardNoticeSummary {

    private String title;
    private String content;
    private long primaryCount;
    private long secondaryCount;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getPrimaryCount() {
        return primaryCount;
    }

    public void setPrimaryCount(long primaryCount) {
        this.primaryCount = primaryCount;
    }

    public long getSecondaryCount() {
        return secondaryCount;
    }

    public void setSecondaryCount(long secondaryCount) {
        this.secondaryCount = secondaryCount;
    }
}
