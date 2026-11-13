# AGENTS.md — Keel 仓库协作指南

面向 AI Agent 与新人。若本文件与 `CLAUDE.md`、`docs/design/`、`docs/rules/` 冲突，以后者为准。

## 1. 开工前阅读顺序

1. `CLAUDE.md`：阶段规则、编码规范与工作方式。
2. `docs/design/00-文档索引.md`，再按 `01 → 02 → 03 → 04 → 05 → 06` 阅读。
3. `docs/rules/database-rules.md`、`docs/rules/api-rules.md`：数据库与 API 的强制约束。
4. 前端工程各自的 `AGENTS.md`：`frontend/admin/AGENTS.md`、`frontend/portal/AGENTS.md`。

## 2. 仓库结构

| 目录 | 内容 |
|---|---|
| `backend/` | Spring Boot 多模块 Maven 工程，模块名 `keel-*` |
| `frontend/admin/` | 管理端：Vue 3 + TypeScript + Vite + shadcn-vue |
| `frontend/portal/` | 门户：Vue 3 + TypeScript + Vite + Tailwind |
| `docs/` | 需求、设计、规则与交接文档 |
| `scripts/` | OpenAPI 导出、数据库等辅助脚本 |
| `deploy/` | Docker Compose 与环境变量模板 |

三个工程独立安装依赖、独立构建与部署；研发阶段共用当前 Git 仓库。

## 3. 后端模块与边界

- 模块：`common-core`、`common-web`、`common-security`、`common-persistence`、`common-test`、`iam`、`system`、`audit`、`bootstrap`。
- 包结构：`io.github.keel.{module}.{interfaces|application|domain|infrastructure}`。
- 业务模块之间禁止直接依赖；跨模块协作只通过 `common` 中的契约（如 `PermissionProvider`、`ConfigProvider`、审计事件）。
- `common` 中禁止编写业务逻辑；`bootstrap` 只做装配与全局配置。
- 分层与依赖由 ArchUnit 校验，修改模块关系后必须运行架构测试。
- 禁止使用 MyBatis-Plus `IService` / `ServiceImpl`，只使用 `BaseMapper` 与显式 SQL；SQL 仅允许出现在 `infrastructure/persistence`。
- 生产代码依赖注入统一使用 Lombok `@RequiredArgsConstructor` + `private final`。

## 4. 数据库与迁移

- MySQL 8.x、InnoDB、`utf8mb4`；主键自增 `BIGINT`；时间 `DATETIME(3)`；统一 `Asia/Shanghai` 本地时间。
- 禁止物理外键、禁止 `TIMESTAMP`、禁止数据库 `ENUM`、禁止全局逻辑删除。
- 表名单数 + 模块前缀：`iam_`、`sys_`、`aud_`；索引命名 `uk_*` / `idx_*`。
- 所有结构变更通过 Flyway，只前进，不修改已执行脚本。
- 版本段：`1000–1999` iam、`2000–2999` system、`3000–3999` audit、`9000–9999` 跨模块种子（bootstrap）。
- 模块迁移只操作本模块表；种子数据按业务 `code` 幂等写入，禁止依赖固定自增 ID。
- `scripts/sql/schema.sql` 是结构参考基线，实际执行以 Flyway 为准。

## 5. API 契约

- 路径：`/api/v1/admin/**`、`/api/v1/public/**`；多词段 kebab-case；资源名复数（单例资源需在文档说明）。
- 响应固定 `{ code, message, data }`；成功 `code = "OK"`；错误码按模块注册。
- 分页 `page` 从 1 开始，`size` 默认 20、上限 100；`sort` 字段必须白名单。
- 变更端点前先更新 `docs/design/04-API接口设计.md`。
- 后端通过 `scripts/openapi/export-openapi.ps1` 导出快照到 `frontend/admin/openapi/openapi.json`，前端再生成 TypeScript 类型。
- 破坏性变更进入 `/api/v2`，v1 只做加法。

## 6. 常用命令

```powershell
# JDK 25（首次或新终端）
$env:JAVA_HOME = 'D:\env\jdks\openjdk-25.0.2'

# 后端定向测试（需要 Docker，Testcontainers）
cd backend
mvn -pl keel-bootstrap -am test

# 后端全量验证
mvn verify

# 导出 OpenAPI 快照（需要 Docker）
powershell -File ..\scripts\openapi\export-openapi.ps1

# 管理端
cd ..\frontend\admin
pnpm install
pnpm lint
pnpm typecheck
pnpm build
pnpm test:e2e
pnpm openapi:generate
pnpm openapi:check
```

## 7. 工作约束

- 未经授权不启动应用、Docker、长期运行的服务，不执行全量构建或集成测试。
- 不修改任务范围外的代码；不擅自增加需求未定义的功能或依赖。
- 数据库、API、模块职责与核心流程变更时，同步更新受影响的文档与 OpenAPI 快照。
- 代码与文档不一致视为缺陷。
