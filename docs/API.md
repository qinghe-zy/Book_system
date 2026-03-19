# API 文档（RESTful / JSON）

## 统一返回结构

```json
{
  "success": true,
  "message": "OK",
  "data": {}
}
```

## 认证

### POST `/api/auth/register`
- 入参：`username`, `password`, `email`
- 出参：`token`, `userId`, `username`, `role`

### POST `/api/auth/login`
- 入参：`username`, `password`
- 出参：同上

## 用户

### GET `/api/users/me`
- 返回：`id`, `username`, `email`, `interestTags`, `role`, `registerTime`

### PUT `/api/users/me`
- 入参：`email`, `password(可选)`, `interestTags(可选，逗号分隔)`

### GET `/api/admin/users`（ADMIN）

### PUT `/api/admin/users/{userId}`（ADMIN）
- 入参：`email`, `role(USER/ADMIN)`

## 分类

### GET `/api/categories`

### POST `/api/categories`（ADMIN）

### PUT `/api/categories/{id}`（ADMIN）

### DELETE `/api/categories/{id}`（ADMIN）

## Dashboard

### GET `/api/dashboard/overview`
- 说明：返回当前登录用户首页所需的聚合数据。
- 用户端返回：指标卡片、提醒摘要、推荐图书、热门/新书、最近借阅。
- 管理端返回：指标卡片、待办摘要、库存紧张图书、最近新增图书、推荐统计。

## 图书

### GET `/api/books?keyword=&categoryCode=`
- 说明：按书名/作者/分类号检索
- 返回新增字段：`imageUrl`

### GET `/api/books/{id}?similarLimit=4`
- 说明：获取图书详情与相似图书
- 返回：基础图书字段 + `summary`, `availabilityText`, `similarBooks[]`

### POST `/api/books`（ADMIN）
- 入参：`title`, `author`, `categoryCode`, `stock`, `location`, `categoryId`

### PUT `/api/books/{id}`（ADMIN）

### DELETE `/api/books/{id}`（ADMIN）

### PUT `/api/books/batch/location`（ADMIN）
- 入参：`bookIds[]`, `location`

### DELETE `/api/books/batch`（ADMIN）
- 入参：`[1,2,3]`

### POST `/api/books/{id}/cover`（ADMIN）
- `multipart/form-data`，字段名 `file`
- 校验：仅 jpg/jpeg/png，最大 2MB（可配置）
- 返回：更新后的图书对象（含 `imageUrl`）

### GET `/api/files/book-covers/{filename}`
- 公开访问图片文件（跨域可访问）

## 借阅

### POST `/api/borrow/apply`
- 入参：`bookId`, `borrowType(ONLINE/OFFLINE)`

### GET `/api/borrow/my`

### PUT `/api/borrow/{recordId}/approve`（ADMIN）

### PUT `/api/borrow/{recordId}/reject`（ADMIN）

### PUT `/api/borrow/{recordId}/return`

### PUT `/api/borrow/{recordId}/renew`

### GET `/api/borrow/admin/all`（ADMIN）

### GET `/api/borrow/admin/overdue`（ADMIN）

## 推荐（优化后）

### GET `/api/recommendations/my?topN=10`
- 返回新增字段：`imageUrl`, `modelScore`, `finalScore`

### POST `/api/recommendations/refresh/me?topN=10`

### POST `/api/recommendations/refresh/{userId}?topN=10`（ADMIN）

### GET `/api/recommendations/admin/stats`（ADMIN）
- 返回：`totalRecommendations`, `hitRecommendations`, `overallHitRate`, `coveredUsers`, `topBooks[]`
- `topBooks` 每项：`bookId`, `title`, `recommendedCount`, `hitCount`, `hitRate`

## 推荐算法说明（实现）

1. 候选生成优先使用用户借阅历史（协同过滤 + 偏好分类加权）。
2. 调用远程深度学习评分接口对候选图书打分并排序：
   - 配置项：`recommendation.remote-score-url`, `recommendation.remote-score-api-key`
   - 接口不可用时自动回退协同过滤分。
3. 支持 `topN` 输出。
4. 冷启动处理：
   - 新用户：热门图书 + 兴趣标签（`interestTags`）匹配。
   - 新图书：低借阅新书冷启动加权 + 标签匹配加权。
5. 管理端可查看推荐效果统计（推荐次数/命中率）。

## 示例：上传图书图片

```bash
curl -X POST "http://127.0.0.1:8080/api/books/1/cover" \
  -H "Authorization: Bearer <JWT>" \
  -F "file=@D:/tmp/book-cover.jpg"
```

## 前端交互说明

1. 管理员在图书管理页可直接选择文件并上传封面。
2. 用户在推荐页可选择 `Top N`，刷新后查看按综合分排序的推荐结果。
3. 管理员在推荐页额外可见推荐效果统计面板。
