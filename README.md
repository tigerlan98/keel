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

若当前终端未加载新的用户环境变量，先执行：

```powershell
$env:JAVA_HOME = 'D:\env\jdks\openjdk-25.0.2'
```

构建产物：`backend/keel-bootstrap/target/keel-bootstrap-1.0.0.jar`。

```powershell
& 'D:\env\jdks\openjdk-25.0.2\bin\java.exe' -jar .\keel-bootstrap\target\keel-bootstrap-1.0.0.jar
```

启动后仅暴露 `GET /actuator/health`。当前骨架未接入数据库、Redis、认证与业务接口。

Maven 模块：

| 模块 | 职责 |
|---|---|
| `keel-common-core` | 统一返回、错误码、异常、分页结构 |
| `keel-common-web` | 全局异常处理、校验错误结构、请求 ID |
| `keel-common-security` | Security / token / 当前用户（Phase 1A 实现） |
| `keel-common-persistence` | 持久层基础实体与配置（Phase 1A 启用） |
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

当前前端为可构建骨架：Admin 含登录页占位、基础布局与 Axios 封装；Portal 含首页占位与匿名 API 客户端。尚未生成 shadcn-vue 业务组件，也尚未接入真实接口与 OpenAPI 类型。

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

Phase 1A 骨架已完成：9 个后端模块可构建、应用上下文可启动、ArchUnit 基础规则通过；Admin 与 Portal 可安装、类型检查、Lint 与构建。MyBatis-Plus 运行配置、Flyway、SpringDoc、Security token 过滤器与 Testcontainers 集成测试留待 Phase 1A 验证任务。
