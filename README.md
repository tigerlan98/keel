# Keel

![verify](https://github.com/tigerlan98/keel/actions/workflows/verify.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-brightgreen)
![Vue](https://img.shields.io/badge/Vue-3-42b883)
![License](https://img.shields.io/badge/License-MIT-yellow)

Keel 是一个面向管理后台的通用脚手架：Spring Boot 多模块后端、Vue 3 管理端与匿名门户，内置认证、RBAC、系统参数、审计日志与同域部署。

## 特性

- **认证与会话**：Web 使用 `HttpOnly` Cookie + CSRF，App / 小程序使用 Bearer token；按设备单会话，支持强制下线与登录失败锁定。
- **RBAC**：统一资源树、多角色、`SUPER_ADMIN` 绕过；权限码与菜单树缓存于 Redis，变更按范围失效。
- **系统参数**：由 Flyway 迁移维护，后台只更新参数值；读取走 Redis 缓存，支持公开配置接口。
- **审计日志**：操作日志异步写入、登录日志同步写入；分页查询与保留期分批清理。
- **契约驱动**：SpringDoc 分组文档、OpenAPI 快照、前端 TypeScript 类型自动生成。
- **工程质量**：Maven 多模块 + ArchUnit 边界校验、Testcontainers 集成测试、Flyway 版本化迁移、Playwright E2E。
- **同域部署**：Docker Compose + Nginx，统一入口 `/admin/`、`/portal/`、`/api/v1`。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 25、Spring Boot 4.1、Spring Security、MyBatis-Plus、Flyway、SpringDoc |
| 数据 | MySQL 8、Redis 7 |
| 管理端 | Vue 3、TypeScript、Vite、Pinia、Vue Router、shadcn-vue、Tailwind CSS v4 |
| 表单与表格 | VeeValidate + Zod、TanStack Table、Lucide |
| 测试 | JUnit 5、Testcontainers、ArchUnit、Playwright |
| 构建 | Maven 3.9+、pnpm 10、Docker |

## 项目结构

```text
keel/
├── backend/            Spring Boot 多模块 Maven 工程
├── frontend/
│   ├── admin/          管理端（Vue 3 + TypeScript + Vite + shadcn-vue）
│   └── portal/         门户（Vue 3 + TypeScript + Vite + Tailwind）
├── docs/               需求、设计、规则与项目文档
├── deploy/             Docker Compose、Nginx 与环境变量模板
├── scripts/            OpenAPI 导出、数据库脚本与验证脚本
└── .github/            GitHub Actions
```

## 快速开始

### 环境要求

| 工具 | 版本 |
|---|---|
| JDK | 25 |
| Maven | 3.9+ |
| Node.js | 24 |
| pnpm | 10 |
| Docker | 用于 Testcontainers 与本地部署 |

项目不提交 Maven Wrapper，请确保 `mvn -v` 运行在 Java 25 上。

> Windows PowerShell 中可将示例里的 `cp` 替换为 `Copy-Item`，`export` 替换为 `$env:NAME = 'value'`。

### 1. 启动依赖服务

```bash
cd deploy
cp docker-compose.env.example docker-compose.env
# 编辑 docker-compose.env，填写 MySQL / Redis 密码
docker compose --env-file docker-compose.env up -d
```

### 2. 启动后端

```bash
cd backend
mvn verify        # 需要 Docker：Testcontainers 会启动真实 MySQL 与 Redis
java -jar keel-bootstrap/target/keel-bootstrap-1.0.0.jar
```

本地运行需提供数据库与 Redis 连接信息：

```bash
export DB_PASSWORD=<mysql-password>
export REDIS_PASSWORD=<redis-password>
```

初始管理员由 Flyway 种子创建，用户名为 `admin`。默认密码为本地开发固定值，首次登录后请立即修改。

启动后可访问：

- `http://localhost:8080/actuator/health`：健康检查
- `http://localhost:8080/swagger-ui.html`：Swagger UI
- `/v3/api-docs`、`/v3/api-docs/admin`、`/v3/api-docs/public`：OpenAPI 分组文档

### 3. 启动管理端

```bash
cd frontend/admin
pnpm install
pnpm dev          # http://localhost:5173
```

### 4. 启动门户

```bash
cd frontend/portal
pnpm install
pnpm dev          # http://localhost:5174
```

开发服务器会将 `/api` 代理到 `http://localhost:8080`。

### 5. 完整同域部署

```bash
cd backend && mvn verify
cd ../deploy
docker compose --env-file docker-compose.env --profile full up -d --build
```

访问：

| 路径 | 内容 |
|---|---|
| `http://localhost/admin/` | 管理端 |
| `http://localhost/portal/` | 门户 |
| `http://localhost/api/v1/public/system/config` | 公开配置 |
| `http://localhost/health` | 健康检查 |

更多部署说明见 [`deploy/README.md`](deploy/README.md)。

## 后端模块

| 模块 | 职责 |
|---|---|
| `keel-common-core` | 统一返回、错误码、异常与分页结构 |
| `keel-common-web` | 全局异常处理、校验错误、请求 ID |
| `keel-common-security` | Security、token、当前用户、CSRF、登录锁定 |
| `keel-common-persistence` | MyBatis-Plus、Flyway、审计字段填充 |
| `keel-common-test` | 测试基座（test 作用域） |
| `keel-iam` | 用户、角色、资源、菜单、认证与授权 |
| `keel-system` | 系统参数 |
| `keel-audit` | 操作日志、登录日志 |
| `keel-bootstrap` | 启动、装配、全局配置与 ArchUnit 测试 |

## API 与契约

- 管理端接口：`/api/v1/admin/**`
- 公开接口：`/api/v1/public/**`
- 统一响应：`{ code, message, data }`
- 分页参数：`page` 从 1 开始，`size` 默认 20、上限 100

更新接口后需同步契约：

```bash
pwsh -File scripts/openapi/export-openapi.ps1

cd frontend/admin
pnpm openapi:generate   # 生成 src/api/generated.ts
pnpm openapi:check      # 校验生成物与快照一致
```

## 测试与 CI

```bash
# 后端：单元、集成、API 与架构测试（需要 Docker）
cd backend && mvn verify

# 管理端 E2E
cd frontend/admin && pnpm test:e2e

# 全量验证
pwsh -File scripts/verify.ps1
```

GitHub Actions：

- `.github/workflows/verify.yml`：PR 与 `main` 推送时执行全量验证。
- `.github/workflows/release.yml`：`v*` tag 触发验证。

## 配置

后端通过 Spring Profile 区分环境，敏感配置使用环境变量注入：

| 变量 | 说明 |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` / `test` / `prod` |
| `DB_HOST` / `DB_PORT` / `DB_DATABASE` | MySQL 连接 |
| `DB_USERNAME` / `DB_PASSWORD` | MySQL 账号 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 连接 |
| `TZ` / `JAVA_TOOL_OPTIONS` | 时区配置 |
| `VITE_API_BASE_URL` | 前端构建期 API 前缀，默认 `/api/v1` |

## 文档

- [文档索引](docs/design/00-文档索引.md)
- [需求与功能模块](docs/design/01-需求与功能模块梳理.md)
- [技术选型与整体架构](docs/design/02-技术选型与整体架构.md)
- [数据库设计](docs/design/03-数据库设计.md)
- [API 接口设计](docs/design/04-API接口设计.md)
- [实施计划与工程规范](docs/design/05-实施计划与工程规范.md)
- [数据库规范](docs/rules/database-rules.md)
- [API 规范](docs/rules/api-rules.md)

面向 Agent 与贡献者的约定见根目录 [`AGENTS.md`](AGENTS.md)、[`frontend/admin/AGENTS.md`](frontend/admin/AGENTS.md) 与 [`frontend/portal/AGENTS.md`](frontend/portal/AGENTS.md)。

## 状态

已完成：认证与会话、RBAC、用户/角色/资源管理、系统参数、操作日志、登录日志、登录失败锁定、管理端页面、Portal 公开配置、同域部署与 CI 验证。

计划中：生产 TLS 与网关配置、多实例任务锁（ShedLock）、Portal 内容扩展与 E2E。

## 贡献

1. 阅读 `AGENTS.md` 与相关设计/规则文档。
2. 修改后运行 `pwsh -File scripts/verify.ps1`。
3. 数据库与 API 变更需同步设计文档与 OpenAPI 快照。
4. 提交 PR 前确保 CI 通过。

## License

本项目采用 [MIT License](LICENSE)。
