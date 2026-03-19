package com.library.system.dto.dashboard;

/**
 * Dashboard 指标卡片。
 */
public class DashboardStatCard {

    private String label;
    private String value;
    private String hint;

    public DashboardStatCard() {
    }

    public DashboardStatCard(String label, String value, String hint) {
        this.label = label;
        this.value = value;
        this.hint = hint;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }
}
