# Keel

Keel 项目工作区。后端与前端工程位于独立子目录，研发阶段共用当前 Git 仓库；各工程独立安装依赖、独立构建与部署。

## 目录结构

```
keel/
├── backend/            后端：Spring Boot 多模块 Maven 工程
├── frontend/
│   ├── admin/          管理端：Vue 3 + TypeScript + Vite + shadcn-vue
│   └── portal/         门户：Vue 3 + TypeScript + Vite + Tailwind
├── docs/               需求、设计、规则与交接文档
├── scripts/            数据库脚本等辅助资源
├── deploy/             部署资源：Docker Compose 与环境变量模板
└── CLAUDE.md           项目协作与开发规范
```

面向 AI Agent 与新人：根目录 `AGENTS.md`（仓库与后端约定）、`frontend/admin/AGENTS.md`、`frontend/portal/AGENTS.md`。

## 环境要求

| 工具 | 版本 |
|---|---|
| JDK | 25（本机：`D:\env\jdks\openjdk-25.0.2`） |
| Maven | 3.9+（本机：3.9.13） |
| Node.js | 24 |
| pnpm | 10 |

本机用户级 `JAVA_HOME` 已指向 JDK 25（`D:\env\jdks\openjdk-25.0.2`），Maven 会使用它。系统 `PATH` 中的 `java` 仍可能指向 JDK 17，直接运行 Java 应用时使用 JDK 25 的完整路径，或调整系统 `PATH`。

项目不提交 Maven Wrapper，开发环境与 CI 需预装 Maven 3.9+。执行 `mvn -v` 应显示 Maven 3.9+ 且运行在 Java 25 上。

## 后端

```powershell
cd backend
mvn verify
```

`mvn verify` 会通过 Testcontainers 启动真实 MySQL 8.4 与 Redis 7.4，需要 Docker 可用；Flyway 迁移与 MyBatis-Plus 集成测试会在其中执行。

若当前终端未加载新的用户环境变量，先执行：

```powershell
$env:JAVA_HOME = 'D:\env\jdks\openjdk-25.0.2'
```

构建产物：`backend/keel-bootstrap/target/keel-bootstrap-1.0.0.jar`。

本地启动前先启动 MySQL / Redis（见“部署资源”），并设置对应密码与初始管理员：

```powershell
$env:DB_PASSWORD = '<MYSQL_PASSWORD>'
$env:REDIS_PASSWORD = '<REDIS_PASSWORD>'
$env:KEEL_ADMIN_USERNAME = 'admin'
$env:KEEL_ADMIN_PASSWORD = '<强密码>'
& 'D:\env\jdks\openjdk-25.0.2\bin\java.exe' -jar .\keel-bootstrap\target\keel-bootstrap-1.0.0.jar
```

初始管理员仅在不存在时创建，创建后 `mustChangePassword = true`。

应用启动后仅暴露 `GET /actuator/health`；Flyway 会在启动时自动执行 `iam`、`system`、`audit`、`seed` 四个位置的迁移。

API 文档：

- `/v3/api-docs`：默认文档
- `/v3/api-docs/admin`：管理端分组
- `/v3/api-docs/public`：公开接口分组
- `/swagger-ui.html`：Swagger UI

Maven 模块：

| 模块 | 职责 |
|---|---|
| `keel-common-core` | 统一返回、错误码、异常、分页结构 |
| `keel-common-web` | 全局异常处理、校验错误结构、请求 ID |
| `keel-common-security` | Security / token / 当前用户 / CSRF |
| `keel-common-persistence` | MyBatis-Plus、Flyway、基础实体与审计字段填充 |
| `keel-common-test` | 测试基座（仅 test 作用域） |
| `keel-iam` | 用户、角色、资源、菜单、认证与授权 |
| `keel-system` | 系统参数 |
| `keel-audit` | 操作日志、登录日志 |
| `keel-bootstrap` | 启动、装配、全局配置与 ArchUnit 测试 |

## 前端

Admin：

```powershell
cd frontend/admin
pnpm install
pnpm dev          # http://localhost:5173
pnpm build
```

Portal：

```powershell
cd frontend/portal
pnpm install
pnpm dev          # http://localhost:5174
pnpm build
```

两个工程均启用 TypeScript strict、ESLint、Prettier 与 `vue-tsc`。开发服务器将 `/api` 代理到 `http://localhost:8080`。

当前前端状态：Admin 已接入登录、`/auth/me` 会话恢复、路由守卫、401/403、菜单驱动侧边栏与按钮权限，并完成用户、角色、资源、系统参数、操作日志、登录日志页面；API 类型由 OpenAPI 快照生成。Portal 已接入公开配置接口，首页展示系统名称与 Logo，内容仍为骨架。

## 部署资源

`deploy/` 存放本地依赖服务的 Docker Compose 配置。

```powershell
cd deploy
Copy-Item docker-compose.env.example docker-compose.env   # 首次使用，填写真实密码
docker compose --env-file docker-compose.env up -d
```

`deploy/docker-compose.env` 已被 `.gitignore` 忽略，禁止提交真实密码。

## 设计文档

- 文档索引：`docs/design/00-文档索引.md`
- 技术选型与架构：`docs/design/02-技术选型与整体架构.md`
- 实施计划与工程规范：`docs/design/05-实施计划与工程规范.md`
- 数据库规范：`docs/rules/database-rules.md`
- API 规范：`docs/rules/api-rules.md`

## 当前阶段

Phase 1A / 1B / 2 / 3 已完成；Phase 4 Portal 进行中（公开配置已接入，待同域部署验证）。以下为各阶段交付摘要。

Phase 1A 已完成：

- 9 个 Flyway 迁移（`iam` 5 个、`system` 1 个、核心种子 3 个）在空库上按序执行。
- MyBatis-Plus CRUD、分页、乐观锁与审计字段填充通过集成测试。
- Testcontainers 真实 MySQL 8.4 / Redis 7.4 集成测试通过。
- SpringDoc 分组文档 `/v3/api-docs`、`/v3/api-docs/admin`、`/v3/api-docs/public` 可访问。
- Actuator 仅暴露 `/actuator/health`，其他端点被安全层拦截。
- 认证链路通过：Web 登录使用 CSRF + `HttpOnly` Cookie，App / Mini 使用 Bearer token，同一设备再次登录会踢掉旧 token。
- ArchUnit 模块与分层规则通过。

认证接口：

- `GET /api/v1/admin/auth/csrf`
- `POST /api/v1/admin/auth/login`
- `POST /api/v1/admin/auth/logout`
- `GET /api/v1/admin/auth/me`
- `POST /api/v1/admin/auth/change-password`

管理接口（Phase 1B / 2 / 3）：

- 用户：分页、详情、创建、编辑、启停、重置密码、查询/替换角色、按用户或设备强制下线
- 角色：分页、详情、创建、编辑、删除、查询/替换资源授权
- 资源：树、详情、更新展示属性、启停；资源的创建/删除仅由 Flyway 迁移维护
- 系统参数：分页、详情、更新参数值；参数创建/删除仅由 Flyway 迁移维护
- 操作日志 / 登录日志：分页与详情，只读
- 公开接口：`GET /api/v1/public/system/config`，仅返回 `public_flag = 1` 且属于 `system.*` 的参数

登录行为同步写入 `aud_login_log`，记录 `LOGIN`、`LOGOUT`、`LOGIN_FAILED`。

首次启动可通过 `KEEL_ADMIN_USERNAME` / `KEEL_ADMIN_PASSWORD` 创建超级管理员，创建后要求首次登录改密。

Phase 1B 前后端闭环已完成：

- 前端启动时调用 `/auth/me` 恢复登录态，接入 Pinia 与路由守卫。
- 未登录或会话失效时返回 401，前端清理登录态并跳转登录页。
- 用户列表页面支持用户名前缀、状态筛选、排序与分页。
- 登录、登出、登录失败同步写入 `aud_login_log`。
- OpenAPI 快照（`frontend/admin/openapi/openapi.json`）由后端导出，Admin 类型由快照生成。
- Playwright 覆盖登录、登出、登录失败、401 跳转、用户列表筛选/分页/错误重试。

Phase 2 RBAC 已完成：

- 权限码与菜单树按用户查询并缓存于 Redis（`iam:perm:{userId}`，TTL 30 分钟）；角色、资源、用户-角色与用户状态变更后按影响范围失效。
- `PermissionChecker` 先判断 `SUPER_ADMIN`，再校验权限码；普通管理员只看到被授权菜单与按钮。
- 角色保护：`SUPER_ADMIN` 不可删除/禁用/改 code；被用户引用时禁止删除。
- 资源保护：停用存在启用子资源的目录/菜单返回 409；父级非法或成环返回 400；`type/code/path/component` 不可修改。
- 用户禁用、重置密码与强制下线会立即吊销 token；`mustChangePassword` 由服务端强制拦截，仅允许改密、登出、`/me` 与 CSRF。
- Admin 侧边栏由后端菜单驱动，登录落在第一个有权限菜单；新增角色、资源、用户页面与 `v-permission` 按钮权限；403 只提示不退出登录。
- Playwright 覆盖菜单可见性、403、角色创建、资源停用冲突与强制改密，共 14 个用例。
- 登录失败锁定：按用户名 + IP 在 Redis 计数，连续 5 次失败锁定 15 分钟，返回 429 `AUTH_TOO_MANY_ATTEMPTS`；成功登录清零。

Phase 3 系统参数与审计日志已完成：

- 系统参数查询/更新与公开配置接口可用；参数读取走 Redis 缓存（TTL 30 分钟，更新后立即失效）。
- `ConfigProvider` 已接入 token 超时与审计日志保留期等运行参数。
- 操作日志通过 `@OperationLog` 切面在接口成功后发布事件并异步落库；请求参数脱敏并截断 2 KB，事务回滚不产生日志。
- 登录日志保持同步写入，记录 `LOGIN`、`LOGOUT`、`LOGIN_FAILED` 与失败原因。
- 操作日志与登录日志支持分页/详情查询；每日 02:00 按保留期分批清理（单批 1000 条），默认操作日志 180 天、登录日志 365 天，可通过系统参数调整。
- Admin 新增系统参数、操作日志、登录日志页面；OpenAPI 快照与前端类型已同步。
- Playwright 新增系统参数、操作日志、登录日志 8 个用例，并补充登录锁定 429 提示用例（当前合计 23 个）。

Phase 4 Portal（进行中）：

- Portal 已接入 `GET /api/v1/public/system/config`，首页展示系统名称与 Logo，并处理加载、失败与重试状态。
- 匿名客户端，不引入认证逻辑。
- 待办：同域部署验证；内容信息架构仍保持骨架级。

契约与 E2E 命令：

```powershell
# 后端导出 OpenAPI 快照到 frontend/admin/openapi/openapi.json（需要 Docker）
powershell -File scripts/openapi/export-openapi.ps1

cd frontend/admin
pnpm openapi:generate   # 重新生成 src/api/generated.ts
pnpm openapi:check      # 校验生成物与已提交内容一致
pnpm test:e2e           # Playwright（自动启动 Vite，接口由用例 mock）
```

后端 OpenAPI 包含 `cookieAuth` / `bearerAuth` 安全方案、端点摘要、参数说明与响应示例；DTO 中必填字段通过 `@Schema(requiredMode = REQUIRED)` 写入契约。
