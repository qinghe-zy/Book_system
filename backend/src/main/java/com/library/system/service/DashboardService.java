package com.library.system.service;

import com.library.system.dto.dashboard.DashboardOverviewResponse;

public interface DashboardService {

    /**
     * 获取当前登录用户的首页聚合数据。
     */
    DashboardOverviewResponse currentOverview();
}
