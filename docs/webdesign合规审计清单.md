# webdesign 合规审计清单

## 已落实
- skip link 已保留
- icon/button 关键按钮补充 `aria-label`
- 表单控件补充 `label`、`name`、`autocomplete`
- 图片补充 `alt`、`width`、`height`、`loading=lazy`
- 焦点态统一为 `:focus-visible`
- 未使用 `transition: all`
- 危险操作保留确认
- 首页 / 图书页 / 推荐页增加 URL 状态同步
- 模态层补充 `overscroll-behavior: contain`

## 已知限制
- 当前项目未引入长列表虚拟化库，后续若数据量持续增长可补充分页或虚拟滚动
- 由于采用传统 SPA 架构，不涉及 SSR hydration 问题
