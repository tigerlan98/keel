# AGENTS.md — Keel Portal

门户工程约定。仓库级约束见根目录 `AGENTS.md` 与 `../../CLAUDE.md`。

## 1. 定位与技术栈

- 匿名公开站点，不建立 C 端用户体系，不引入认证逻辑。
- Vue 3 + TypeScript（strict）+ Vite + pnpm + Tailwind CSS v4 + shadcn-vue，与 Admin 保持同一设计语言。
- `src/api/http.ts` 为 Axios 薄封装（统一 `{ code, message, data }` 与错误归一化）；`src/views/` 页面；`src/router/` 路由。

## 2. 接口

- 只调用 `/api/v1/public/**` 匿名只读接口；禁止调用管理端接口。
- 当前使用 `GET /api/v1/public/system/config` 展示系统名称与 Logo。
- 当前类型为手写接口（`src/api/types.ts`），因为 Portal 尚未接入 OpenAPI 快照生成；新增接口时应先确认是否引入生成流程。
- 必须处理加载中、失败与空数据状态，失败时提供重试。

## 3. 页面约定

- 公共页面保持与 Admin 一致的设计语言与窄屏适配。
- 不展示管理端数据，不放置登录/权限入口。
- 不擅自扩展页面信息架构；内容形态变更需先确认。

## 4. 常用命令

```powershell
pnpm install
pnpm dev          # http://localhost:5174，/api 代理到 localhost:8080
pnpm lint
pnpm typecheck
pnpm build
```

## 5. 测试与约束

- 当前未配置 Playwright；引入测试依赖前先确认。
- 引入新依赖前先确认，不擅自替换已选型的技术栈。
- 禁止在前端代码或构建产物中暴露密钥等敏感信息。
