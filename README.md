<<<<<<< HEAD
﻿# 智能图书管理系统（前后端分离）

## 1. 项目结构

- `sql/`：数据库脚本
- `backend/`：Spring Boot 3 + Java 21 + Maven 后端
- `frontend/`：Vue 3 + TypeScript + Vue Router + Pinia 前端
- `docs/`：接口说明

## 2. 环境要求

- Java 21
- Maven 3.9.12
- MySQL 8+
- Node.js 24.13.1
- npm 11+

## 3. 数据库初始化

1. 启动 MySQL，确保有账号：`root / 123456`
2. 执行脚本：`sql/library_system.sql`
3. 执行结构升级脚本（旧库推荐执行）：`sql/alter_optimization.sql`
4. 导入 100 条测试数据（含 100 本图书）：`sql/seed_100_test_data.sql`

## 4. 后端启动

```bash
cd backend
mvn spring-boot:run
```

默认地址：`http://127.0.0.1:8080`

配置文件：`backend/src/main/resources/application.yml`

推荐配置：

- `DEEPSEEK_API_KEY`：用于生成推荐理由
- `RECOMMEND_SCORE_API_URL`：远程深度学习评分接口
- `RECOMMEND_SCORE_API_KEY`：远程评分接口密钥

回退策略：
- 未配置 `DEEPSEEK_API_KEY`：推荐理由回退本地模板
- 未配置 `RECOMMEND_SCORE_API_URL` 或接口异常：评分回退协同过滤分

## 5. 前端启动

```bash
cd frontend
npm install
npm run dev
```

默认地址：`http://127.0.0.1:5173`

生产打包：

```bash
npm run build
```

## 6. 默认账号

- 管理员：`admin`
- 密码：`123456`

管理员账号在后端启动时自动初始化。

## 7. 功能覆盖

- 用户管理：注册、登录、个人信息修改、管理员用户维护、角色权限
- 图书管理：增删改查、分类管理、位置管理、批量位置更新与批量删除
- 图书封面：管理员上传 jpg/png 封面，后端校验类型/大小并返回图片 URL
- 检索与借阅：按书名/作者/分类号检索；申请、审核、归还、续借、逾期扫描
- 个性化推荐：候选生成（借阅历史优先）+ 远程深度学习评分排序 + 冷启动策略 + 定时任务刷新
- 推荐统计：管理员查看被推荐次数、命中率、覆盖用户数

## 8. 接口文档

见：`docs/API.md`
=======
# Book_system
>>>>>>> 62ef02fb60c32f5447f3c3a51d5c4f0e6fdfbee9
