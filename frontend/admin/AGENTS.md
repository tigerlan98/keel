# AGENTS.md — Keel Admin

管理端工程约定。仓库级约束见根目录 `AGENTS.md` 与 `../../CLAUDE.md`。

## 1. 技术栈与结构

- Vue 3 + TypeScript（strict）+ Vite + pnpm。
- UI：shadcn-vue（Reka UI v2 + Tailwind CSS v4）；表格 TanStack Table；表单 VeeValidate + Zod；图标 Lucide。
- `src/api/`：Axios 薄封装与接口模块；`src/views/`：页面；`src/router/`：静态路由与守卫；`src/stores/`：Pinia。
- `src/components/ui/` 为 shadcn-vue 生成组件，不手工修改；业务封装放 `src/components/app/`。

## 2. API 契约与类型

- API 类型必须从 `openapi/openapi.json` 生成，禁止手写或手改 `src/api/generated.ts`。
- 契约以快照为准；后端端点变更后由后端导出快照，再执行 `pnpm openapi:generate`。
- 业务封装类型在 `src/api/types.ts` 中从生成类型派生。
- 提交前执行 `pnpm openapi:check`，确保生成物与快照一致。

## 3. 认证与权限

- Web 登录态使用后端 `HttpOnly` Cookie；认证状态只放 Pinia，禁止把 token 写入 `localStorage`。
- 写请求必须回传 `X-XSRF-TOKEN`；统一由 `src/api/http.ts` 处理。
- 必须同时判断 HTTP 状态与响应体 `code`，不得只看 HTTP 200。
- `401`：清理登录态并跳转登录页；`403`：只提示无权限，不退出登录。
- 路由权限使用 `meta.permission`；按钮权限使用 `v-permission`；侧边栏由后端菜单驱动。
- `/auth/me` 用于启动时恢复会话；首次改密由服务端强制拦截。

## 4. 页面约定

- 页面结构必须符合已确认的原型与业务设计，不擅自改动核心功能与布局。
- 列表页必须处理加载中、空数据、请求失败与重试；查询条件变化时回到第 1 页。
- 表单必须做前端校验，并提供提交中状态与成功/失败反馈。
- 保持既有设计语言与窄屏适配；组件命名清晰表达业务意图。

## 5. 测试

- 仅使用 Playwright；用例位于 `e2e/*.spec.ts`，接口由用例内 route mock 提供，不依赖本地 MySQL/Redis。
- 公共 mock 放 `e2e/support/api-mocks.ts`；`mockSession` 可覆盖权限与菜单。
- 运行：`pnpm test:e2e`（自动启动 Vite），失败时保留 trace/video。

## 6. 常用命令

```powershell
pnpm install
pnpm dev          # http://localhost:5173，/api 代理到 localhost:8080
pnpm lint
pnpm typecheck
pnpm build
pnpm test:e2e
pnpm openapi:generate
pnpm openapi:check
```

## 7. 约束

- 引入新依赖或组件库前先确认，不擅自替换已选型的技术栈。
- 禁止无理由使用 `any`；禁止在页面中重复编写请求逻辑。
- 不修改任务范围外的页面与共享组件。
