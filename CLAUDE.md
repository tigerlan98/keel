# CLAUDE.md

## 需求阶段规则

适用范围：需求梳理、需求探索、产出需求文档阶段。

1. 有任何不确定的需求或问题，与我进行交互确认；等我回答后再继续追问，不要一次性把所有问题都抛出来。
2. 一轮一轮推进，直到关键需求逐步清晰、没有阻塞性问题为止。
3. 每轮结束时，输出一份简短的“当前需求快照”：已确认结论、待确认清单、本轮新增结论。
4. 问题必须具体、可回答，不要太宽泛；尽量给出候选选项和倾向建议。
5. 每个问题都必须简要说明“为什么问”，即影响到哪些设计决策。
6. 优先提出最影响系统架构与开发落地的问题。
7. 禁止一次性抛出所有问题。
8. 禁止提出“你想要什么”这类空泛的问题。

## 项目设计阶段规则

适用范围：总体设计、技术方案、数据库设计、API 设计阶段。

1. 总体设计阶段以分析、方案讨论和设计文档编写为主。
2. 未经用户明确要求，不得提前编写或修改业务代码。
3. 设计方案应以已确认的需求和现有页面原型为依据，不得脱离项目实际进行设计。
4. 数据库、API、模块职责和核心业务流程之间必须保持一致。
5. 设计发生变更时，应检查并同步更新所有受影响的文档。
6. 对重大业务规则、架构调整、数据库模型变更和接口契约变更，应先提出方案并获得用户确认。
7. 遇到需求歧义、设计冲突或信息缺失时，应明确指出问题，不得将未经确认的推断当作既定事实。
8. 不得擅自增加需求文档中未定义的业务功能；发现必要的补充项时，应先提出建议。
9. 按合理的任务粒度推进工作，不得为了追求一次性完成而跳过必要的分析、评审和验证。

## 数据库设计规则

1. 涉及数据库设计或修改时，必须阅读 `docs/rules/database-rules.md`，并遵守其规则。

## API 接口设计规则

1. 涉及 API 接口设计或修改时，必须阅读 `docs/rules/api-rules.md`，并遵守其规则。

## 后端开发规范

> 技术栈以 `docs/design/02-技术选型与整体架构.md` 为准；数据库与 API 细则分别以 `docs/rules/database-rules.md`、`docs/rules/api-rules.md` 为准。编码前必须阅读对应文档。

### 技术栈与架构

1. 使用 Java 25 LTS、Spring Boot 4.1.x、Spring MVC、Spring Security（STATELESS）。
2. 持久层使用 MyBatis-Plus（`mybatis-plus-spring-boot4-starter` 3.5.15+），数据库 MySQL 8.x，结构迁移使用 Flyway。
3. 缓存与会话使用 Redis（Lettuce）；API 文档使用 SpringDoc；构建与版本管理遵守 `05-实施计划与工程规范.md`。
4. 采用单体多模块结构，模块依赖、分层与包边界必须遵守 `02` 文档与 ArchUnit 规则。
5. 当前不引入 MinIO 等文件存储；如后续确有需求，先补充设计与依赖评审，不得擅自增加。
6. 技术选型变更属于架构变更，必须先更新设计文档与 ADR 并获得确认。

### 命名与代码结构

1. 类名使用 UpperCamelCase，例如 `QuestionService`；方法名、变量名使用 lowerCamelCase，例如 `getQuestionDetail`；常量使用大写下划线形式，例如 `DEFAULT_PAGE_SIZE`。
2. 命名应清晰表达业务意图，避免含义模糊的缩写。
3. 包结构遵循 `io.github.keel.{module}.{interfaces|application|domain|infrastructure}`，业务模块之间禁止直接依赖。
4. PO 使用 `XxxPO` 后缀并位于 `infrastructure/persistence`；应用层 DTO 使用 `XxxDTO`，接口响应使用 `XxxVO`；DTO/VO 使用 record，PO 使用 Lombok。
5. Controller 只负责接收、校验、调用应用服务、返回 VO，禁止编写业务逻辑；事务边界放在 application 层。
6. 跨模块协作只通过 `common` 中的契约，禁止依赖其他业务模块的实现；`common` 中不得编写业务逻辑。
7. 禁止使用 MyBatis-Plus 的 `IService` / `ServiceImpl`，只使用 `BaseMapper` 与显式 SQL。
8. 生产代码中 Spring 组件的依赖注入统一使用 Lombok `@RequiredArgsConstructor` 配合 `private final` 字段，禁止手写仅用于依赖注入的构造器，禁止使用 `@Autowired` / `@Resource` 字段注入；非依赖注入的配置值（如 `@Value`）可保留字段注入。测试类不受此限制，按 Spring Boot 测试惯例使用 `@Autowired`。

### 参数校验与持久化

1. 所有请求参数必须在服务端校验（Jakarta Bean Validation），不得只依赖前端；字符串长度、数值范围、枚举值、日期格式与数组大小必须有合理限制。
2. 数据库操作优先使用 MyBatis-Plus `BaseMapper` 与显式 SQL；复杂查询编写 XML，SQL 只允许出现在 `infrastructure/persistence`。
3. 禁止在循环中无必要地执行大量单条查询；关联数据使用批量查询或 JOIN，避免 N+1 查询问题。
4. 列表查询必须分页，禁止无界查询；排序字段与筛选条件必须使用白名单。
5. 请求与响应必须使用显式 DTO，禁止暴露 PO 或领域实体，禁止用 `Map` 承载业务请求体。

### 异常、日志与安全

1. 使用 `common-web` 统一异常处理机制，禁止在 Controller 中重复编写异常捕获代码。
2. 业务异常必须携带清晰、可识别的错误信息与稳定错误码，错误码按 `docs/rules/api-rules.md` 注册。
3. 不得向前端返回 SQL 详情、堆栈信息、内部路径、类名、密钥等敏感信息；日志中不得输出密码、token、Cookie。
4. 接口鉴权统一使用 `@PreAuthorize("@perm.has('module:resource:action')")`，权限码格式遵守 API 规范。
5. 代码注释应简洁、准确，重点说明业务意图、复杂逻辑和关键设计原因，避免冗余注释。

## 前端开发规范

> 前端技术栈与工程约定以 `docs/design/02-技术选型与整体架构.md`、`05-实施计划与工程规范.md` 为准；Admin 与 Portal 为独立仓库。

### 技术栈与工程

1. 使用 Vue 3 + TypeScript + Vite + pnpm；管理端 UI 使用 shadcn-vue（Reka UI v2 + Tailwind CSS v4），不使用 Element Plus。
2. 表单使用 VeeValidate + Zod；表格使用 TanStack Table；日期与图标使用 date-fns、Lucide。
3. 启用 TypeScript strict 与 `vue-tsc`；禁止无理由使用 `any`；CI 执行 `pnpm lint`、`pnpm typecheck`、`pnpm build`。
4. `components/ui` 为 shadcn-vue 生成组件，不手工修改；业务封装放 `components/app`。
5. 引入新依赖或组件库前先确认，不得擅自替换已选型的技术栈。

### 编码与组件

1. 组件开发优先采用 Composition API 与 `<script setup>`。
2. 组件、方法及变量命名应清晰表达业务意图，避免含义模糊的缩写。
3. 页面结构与交互必须符合已确认的页面原型和业务设计，不得擅自修改核心功能和布局。
4. 页面应保持整体风格统一，布局清晰、层次分明、视觉美观，并兼顾常用屏幕尺寸与窄屏适配。
5. 表单必须进行前端校验，并提供清晰的错误提示、提交中状态和操作反馈。
6. 必须合理处理数据加载中、请求失败、数据为空等状态，避免页面出现异常或无反馈的情况。
7. 代码注释应简洁、准确，重点说明业务意图、复杂逻辑和关键设计原因，避免冗余注释。

### 接口、认证与状态

1. 后端接口调用必须统一封装和管理（Axios 薄封装），集中处理 CSRF、401/403 与错误归一化，禁止在页面中重复编写请求逻辑。
2. API 类型从后端 OpenAPI 快照生成，不手写、不手改生成物；接口契约以 `openapi.json` 为准。
3. 前端必须同时判断 HTTP 状态与响应体 `code`，不得只看 HTTP 200。
4. 路由守卫只负责前端导航控制；真正的身份认证和接口权限校验必须由后端执行。
5. Web 端使用 HttpOnly Cookie 登录态，认证状态只放 Pinia，不得把 token 写入 `localStorage`；写请求必须回传 `X-XSRF-TOKEN`。
6. `401` 时清理登录态并跳转登录页；`403` 只提示无权限，不得退出登录。
7. 按钮权限使用 `v-permission`，路由权限使用 `meta.permission`；侧边栏由后端菜单驱动。

### 安全

1. 不得在前端代码、环境变量或构建产物中暴露 API 密钥、服务端凭证等敏感信息；前端环境变量只允许公开配置。
2. 敏感信息不得写入日志、URL 查询参数或持久化存储。

## 工程结构

1. `backend/`：后端 Spring Boot 多模块 Maven 工程，模块名保留 `keel-*`。
2. `frontend/admin/`：管理端 Vue 3 + TypeScript + Vite + shadcn-vue。
3. `frontend/portal/`：门户 Vue 3 + TypeScript + Vite + Tailwind。
4. `docs/`：需求、设计、规则与交接文档；`scripts/`：数据库脚本等辅助资源；`deploy/`：Docker Compose 与部署资源。
5. 三个工程独立安装依赖、独立构建与部署，研发阶段共用当前 Git 仓库。
6. 后端使用 JDK 25 构建；本机已配置用户级 `JAVA_HOME`，系统 `PATH` 中的 `java` 可能仍为 17，命令见 `README.md`。

## 工作方式

采用轻量级 **SDD（Spec-Driven Development）+ Agentic Engineering** 工作流。

### 流程分级

| 任务类型 | 流程 |
|---|---|
| 简单任务 | 理解需求 → 直接实现 → 定向验证 |
| 普通功能 | 需求澄清 → 简要规格 → 实现 → 验证 |
| 复杂功能 / 架构改造 | 需求澄清 → 规格设计 → 技术方案与风险评估 → 任务拆解 → 迭代实现 → 代码审查 |

### 开发原则

- 优先代码清晰、模块解耦、职责单一和长期可维护性。
- 遵循项目现有架构与编码规范。
- 优先最小必要改动，避免过度设计、重复抽象和无意义的防御性代码。
- 重要设计决策说明理由与权衡。
- 不为简单任务生成冗长文档；规格文档必须与实际实现保持一致。

### 执行与验证

- 复杂任务先输出方案、风险和验收标准，确认后实施。
- 已批准的实施任务允许自主连续执行，不必逐文件请求确认。
- 默认使用单 Agent；只有高风险任务或明确要求时才引入独立 Reviewer。
- 优先定向静态检查、编译和相关测试。
- 未经授权，不启动应用、Docker、长期运行的服务，也不执行全量构建或集成测试。
- 不修改任务范围外的代码。

### 交付汇报

完成任务后简要说明：

1. 实现内容与关键设计决策；
2. 修改的主要文件；
3. 已执行的验证及结果；
4. 未解决的问题和剩余风险。
