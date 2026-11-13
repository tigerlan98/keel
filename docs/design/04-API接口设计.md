# 04 API 接口设计

> 状态：v0.3（评审确认定稿）
> 更新：2026-11-12
> 关联：`01-需求与功能模块梳理.md`、`02-技术选型与整体架构.md`、`03-数据库设计.md`、`06-设计方案评审结果.md`、`../rules/api-rules.md`

## 1. 概述

### 1.1 基础信息

| 项目 | 约定 |
|---|---|
| Base Path | `/api/v1` |
| 管理端 | `/api/v1/admin/**` |
| 公开端 | `/api/v1/public/**` |
| Content-Type | `application/json; charset=UTF-8`；服务端校验 |
| 请求体大小 | JSON 请求默认上限 1 MB；文件上传另定规则 |
| 响应体 | `{ code, message, data }` |
| 请求 ID | `X-Request-Id` 请求 / 响应头 |
| 认证 Cookie | `KEEL_TOKEN`，`HttpOnly` / `Secure` / `SameSite=Lax` |
| CSRF Cookie | `XSRF-TOKEN`，前端可读 |
| CSRF Header | `X-XSRF-TOKEN` |
| App 认证 | `Authorization: Bearer <opaque-token>` |

### 1.2 认证方式

- Web：登录成功后服务端设置 `KEEL_TOKEN` Cookie；浏览器自动携带。
- App / 小程序：登录响应返回 `token`，客户端通过 `Authorization: Bearer` 携带。
- Cookie 模式的写请求必须携带 `X-XSRF-TOKEN`；Bearer 模式跳过 CSRF。
- 所有管理端接口默认需要认证，除非明确标注为公开。

### 1.3 响应与状态码

成功：

```json
{
  "code": "OK",
  "message": "success",
  "data": {}
}
```

错误：

```json
{
  "code": "IAM_USER_NOT_FOUND",
  "message": "用户不存在",
  "data": null
}
```

| 状态 | 用途 |
|---|---|
| 200 | 查询、更新成功 |
| 201 | 创建成功，返回响应体 |
| 204 | 删除、动作执行成功，无响应体 |
| 400 | 参数错误、业务校验失败 |
| 401 | 未认证、token 无效或过期 |
| 403 | 无权限、CSRF 失败、必须先改密 |
| 404 | 资源不存在 |
| 409 | 唯一冲突、引用冲突、乐观锁冲突 |
| 429 | 登录锁定、请求过于频繁 |
| 500 | 内部错误 |

通用错误约定：

- 参数校验失败统一返回 `COMMON_VALIDATION_FAILED` 400，明细放 `data.fieldErrors`。
- 目标资源不存在返回对应模块的 `*_NOT_FOUND` 404。
- 乐观锁冲突统一返回 `COMMON_OPTIMISTIC_LOCK` 409。
- 未认证 401、无权限 403、限流 429、内部错误 500 的通用错误码见第 11 节。

### 1.4 分页、排序、时间

- 分页参数：`page` 从 1 开始，`size` 默认 20、上限 100。
- 排序参数：`sort=field,asc|desc`，字段按端点白名单校验。
- 分页响应：`items`、`total`、`page`、`size`。
- 时间：`yyyy-MM-dd'T'HH:mm:ss.SSS`，`Asia/Shanghai` 本地时间。
- 查询时间参数：`startTime`、`endTime`，使用上述格式。

### 1.5 DTO 命名

| 后缀 | 用途 |
|---|---|
| `XxxRequest` | 创建 / 更新请求体 |
| `XxxResponse` | 动作型响应 |
| `XxxSummary` | 列表项 |
| `XxxDetail` | 详情 |
| `XxxNode` | 树节点 |
| `XxxQuery` | 查询参数 |

所有 DTO 使用 record，字段 camelCase；禁止直接暴露 PO。

### 1.6 通用数据结构

| 类型 | 字段 | 说明 |
|---|---|---|
| `ApiResponse<T>` | `code`、`message`、`data` | 统一响应包；成功 `code = "OK"`，`message = "success"` |
| `PageResult<T>` | `items`、`total`、`page`、`size` | 分页响应 |
| `FieldError` | `field`、`message` | 校验错误项 |
| `UserBrief` | `id`、`username`、`nickname` | 用户摘要；不包含邮箱、手机号等敏感字段 |
| `RoleBrief` | `id`、`code`、`name`、`status` | 角色摘要 |
| `MenuNode` | `id`、`parentId`、`type`、`code`、`name`、`path`、`component`、`icon`、`sort`、`visible`、`children` | 当前用户可见菜单树节点 |

更新语义：

- `PUT` 为全量替换：请求体必须包含全部可更新字段；可空字段使用显式 `null` 表示清空；缺失字段按 `COMMON_VALIDATION_FAILED` 处理。
- `PATCH` 只更新请求体中显式提供的字段。
- ID 集合参数按集合语义处理：重复 ID 去重；不存在的 ID 返回对应 `*_INVALID_*_IDS` 错误。
- 请求体忽略未知字段；不得依赖未知字段承载业务含义。

## 2. 认证与会话流程

### 2.1 登录（Web）

1. 前端调用 `GET /api/v1/admin/auth/csrf`，服务端设置 `XSRF-TOKEN` Cookie。
2. 前端提交 `POST /api/v1/admin/auth/login`，携带 `X-XSRF-TOKEN`。
3. 服务端校验用户名、密码、账号状态、失败锁定。
4. 生成 opaque token，Redis 存哈希与索引，Web 设置 `KEEL_TOKEN` Cookie。
5. 写入登录日志。
6. 返回用户、权限码与菜单树。

> 失败锁定：按“用户名 + 来源 IP”计数，Redis 键为 `auth:login-fail:{username}:{ip}`；连续失败达到 `auth.max-login-failures`（默认 5）后锁定 `auth.lock-minutes`（默认 15）分钟，成功登录清零。不存在的用户名同样计数，避免通过锁定行为判断账号是否存在。

### 2.2 登录（App / 小程序）

1. 直接提交 `POST /api/v1/admin/auth/login`，`device = APP | MINI`。
2. 服务端校验后返回 `token`，不设置 Cookie。
3. 客户端后续请求携带 `Authorization: Bearer <token>`。

### 2.3 已认证请求

1. `TokenAuthenticationFilter` 按 `Authorization` → Cookie 顺序解析 token。
2. 对 token 做 SHA-256，查询 Redis；校验绝对过期时间。
3. 加载用户与权限缓存，写入 `SecurityContext`。
4. 按需续期 Redis TTL（限制写入频率）。
5. 执行 `@PreAuthorize("@perm.has('...')")`。

### 2.4 必须修改密码

- 登录响应或 `/me` 返回 `mustChangePassword = true`。
- 该状态下仅允许访问 `/auth/me`、`/auth/change-password`、`/auth/logout`。
- 其他接口返回 `403 AUTH_PASSWORD_CHANGE_REQUIRED`。
- 修改密码后吊销全部 token，用户重新登录。

### 2.5 强制下线

- 管理员调用 `DELETE /iam/users/{id}/sessions` 或 `/sessions/{device}`。
- 服务端删除对应 token 与索引；目标客户端下一次请求返回 `401 AUTH_TOKEN_INVALID`。
- 无会话时仍返回 204，保持幂等。

### 2.6 登出

- Web：删除 Redis token、清理索引、清除 `KEEL_TOKEN`。
- App：删除 Redis token；客户端丢弃本地 token。
- 写入登录日志，`action = LOGOUT`。

## 3. 端点总览

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/v1/admin/auth/login` | 公开 | 登录 |
| POST | `/api/v1/admin/auth/logout` | 登录 | 登出 |
| GET | `/api/v1/admin/auth/me` | 登录 | 当前用户、权限、菜单 |
| GET | `/api/v1/admin/auth/csrf` | 公开 | 获取 CSRF token |
| POST | `/api/v1/admin/auth/change-password` | 登录 | 修改自己的密码 |
| GET | `/api/v1/admin/iam/users` | `iam:user:list` | 用户分页 |
| GET | `/api/v1/admin/iam/users/{id}` | `iam:user:list` | 用户详情 |
| POST | `/api/v1/admin/iam/users` | `iam:user:create` | 创建用户 |
| PUT | `/api/v1/admin/iam/users/{id}` | `iam:user:update` | 更新用户资料 |
| PATCH | `/api/v1/admin/iam/users/{id}/status` | `iam:user:status` | 启用 / 禁用 |
| POST | `/api/v1/admin/iam/users/{id}/reset-password` | `iam:user:reset-password` | 重置密码 |
| GET | `/api/v1/admin/iam/users/{id}/roles` | `iam:user:list` | 查询用户角色 |
| PUT | `/api/v1/admin/iam/users/{id}/roles` | `iam:user:assign-role` | 替换用户角色 |
| DELETE | `/api/v1/admin/iam/users/{id}/sessions` | `iam:user:force-logout` | 踢出全部端 |
| DELETE | `/api/v1/admin/iam/users/{id}/sessions/{device}` | `iam:user:force-logout` | 踢出指定端 |
| GET | `/api/v1/admin/iam/roles` | `iam:role:list` | 角色分页 |
| GET | `/api/v1/admin/iam/roles/{id}` | `iam:role:list` | 角色详情 |
| POST | `/api/v1/admin/iam/roles` | `iam:role:create` | 创建角色 |
| PUT | `/api/v1/admin/iam/roles/{id}` | `iam:role:update` | 更新角色 |
| DELETE | `/api/v1/admin/iam/roles/{id}` | `iam:role:delete` | 删除角色 |
| GET | `/api/v1/admin/iam/roles/{id}/resources` | `iam:role:list` | 查询角色资源 |
| PUT | `/api/v1/admin/iam/roles/{id}/resources` | `iam:role:assign-resource` | 替换角色资源 |
| GET | `/api/v1/admin/iam/resources/tree` | `iam:resource:list` | 资源树 |
| GET | `/api/v1/admin/iam/resources/{id}` | `iam:resource:list` | 资源详情 |
| PUT | `/api/v1/admin/iam/resources/{id}` | `iam:resource:update` | 更新展示属性 |
| PATCH | `/api/v1/admin/iam/resources/{id}/status` | `iam:resource:status` | 启用 / 禁用资源 |
| GET | `/api/v1/admin/system/configs` | `sys:config:list` | 系统参数分页 |
| GET | `/api/v1/admin/system/configs/{id}` | `sys:config:list` | 系统参数详情 |
| PUT | `/api/v1/admin/system/configs/{id}` | `sys:config:update` | 更新系统参数 |
| GET | `/api/v1/admin/audit/operation-logs` | `audit:operation:list` | 操作日志分页 |
| GET | `/api/v1/admin/audit/operation-logs/{id}` | `audit:operation:list` | 操作日志详情 |
| GET | `/api/v1/admin/audit/login-logs` | `audit:login:list` | 登录日志分页 |
| GET | `/api/v1/admin/audit/login-logs/{id}` | `audit:login:list` | 登录日志详情 |
| GET | `/api/v1/public/system/config` | 公开 | 门户公开配置 |

## 4. 认证接口

### 4.1 `POST /api/v1/admin/auth/login`

**权限**：公开（Web 需要 CSRF）

**请求 `LoginRequest`**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `username` | string | 是 | 登录名，1–64 |
| `password` | string | 是 | 明文密码，1–72 |
| `device` | enum | 是 | `WEB` / `APP` / `MINI` |

**响应 `LoginResponse`**

| 字段 | 类型 | 说明 |
|---|---|---|
| `user` | `UserBrief` | 用户摘要 |
| `superAdmin` | boolean | 是否超级管理员 |
| `permissions` | string[] | 权限码集合 |
| `menus` | `MenuNode[]` | 菜单树 |
| `mustChangePassword` | boolean | 是否必须修改密码 |
| `token` | string? | 仅 `APP` / `MINI` 返回 |

超级管理员的 `permissions` 可以为空，前端必须先判断 `superAdmin`；`menus` 返回全部启用菜单。

**示例（Web）**

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "user": { "id": 2, "username": "ops", "nickname": "运维管理员" },
    "superAdmin": false,
    "permissions": ["iam:user:list", "iam:user:create"],
    "menus": [
      {
        "id": 10,
        "parentId": 0,
        "type": "MENU",
        "code": "iam:user:list",
        "name": "用户管理",
        "path": "/iam/users",
        "component": "iam/users/index",
        "icon": "users",
        "sort": 201,
        "visible": true,
        "children": []
      }
    ],
    "mustChangePassword": false
  }
}
```

**错误**

- `AUTH_BAD_CREDENTIALS` 401：用户名或密码错误、账号不存在或已禁用，统一模糊提示。
- `AUTH_TOO_MANY_ATTEMPTS` 429：连续失败锁定。
- `AUTH_CSRF_INVALID` 403：Web 请求 CSRF 校验失败。

登录成功、失败、登出均写入登录日志；失败记录 `failureReason`（`BAD_CREDENTIALS`、`DISABLED`、`TOO_MANY_ATTEMPTS`）。

### 4.2 `POST /api/v1/admin/auth/logout`

**权限**：登录

**请求**：无

**响应**：204

### 4.3 `GET /api/v1/admin/auth/me`

**权限**：登录

**响应 `MeResponse`**：字段与 `LoginResponse` 相同，不含 `token`。超级管理员的 `permissions` 可以为空，前端必须先判断 `superAdmin`；`menus` 返回全部启用菜单。

### 4.4 `GET /api/v1/admin/auth/csrf`

**权限**：公开

**响应 `CsrfResponse`**

| 字段 | 类型 | 说明 |
|---|---|---|
| `token` | string | CSRF token，同时写入 `XSRF-TOKEN` Cookie |

### 4.5 `POST /api/v1/admin/auth/change-password`

**权限**：登录

**请求 `ChangePasswordRequest`**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `oldPassword` | string | 是 | 当前密码 |
| `newPassword` | string | 是 | 新密码，8–72，字母+数字 |

**响应**：204；服务端吊销该用户全部 token，客户端需重新登录。

**错误**：`AUTH_OLD_PASSWORD_INVALID` 400、`COMMON_VALIDATION_FAILED` 400。

## 5. 用户管理

### 5.1 `GET /api/v1/admin/iam/users`

**权限**：`iam:user:list`

**查询参数**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `page` | int | 否 | 默认 1 |
| `size` | int | 否 | 默认 20，最大 100 |
| `sort` | string | 否 | 白名单：`createdAt`、`username`、`lastLoginAt` |
| `username` | string | 否 | 前缀匹配 |
| `status` | enum | 否 | `ENABLED` / `DISABLED` |

**响应**：`PageResult<UserSummary>`

`UserSummary`：`id`、`username`、`nickname`、`status`、`lastLoginAt`、`createdAt`。

### 5.2 `GET /api/v1/admin/iam/users/{id}`

**权限**：`iam:user:list`

**响应 `UserDetail`**

`id`、`username`、`nickname`、`email`、`phone`、`status`、`mustChangePassword`、`lastLoginAt`、`lastLoginIp`、`roles`（`RoleBrief[]`）、`createdAt`、`updatedAt`、`version`。

### 5.3 `POST /api/v1/admin/iam/users`

**权限**：`iam:user:create`

**请求 `CreateUserRequest`**

| 字段 | 类型 | 必填 | 校验 |
|---|---|---|---|
| `username` | string | 是 | 3–64，字母/数字/`_.-` |
| `password` | string | 是 | 8–72，字母+数字 |
| `nickname` | string | 是 | 1–64 |
| `email` | string? | 否 | 邮箱格式，≤128 |
| `phone` | string? | 否 | 6–32，数字/`+-` |
| `roleIds` | long[] | 否 | 只允许启用角色 |

**响应**：201 + `UserDetail`

**说明**：创建成功后 `mustChangePassword = true`，用户首次登录必须修改密码。

**错误**：`IAM_USERNAME_EXISTS` 409、`IAM_INVALID_ROLE_IDS` 400。

### 5.4 `PUT /api/v1/admin/iam/users/{id}`

**权限**：`iam:user:update`

**请求 `UpdateUserRequest`**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `nickname` | string | 是 | 1–64 |
| `email` | string? | 是（可为 null） | ≤128；null 表示清空 |
| `phone` | string? | 是（可为 null） | ≤32；null 表示清空 |
| `version` | int | 是 | 乐观锁版本 |

**响应**：200 + `UserDetail`

**错误**：`IAM_USER_NOT_FOUND` 404、`COMMON_OPTIMISTIC_LOCK` 409。

### 5.5 `PATCH /api/v1/admin/iam/users/{id}/status`

**权限**：`iam:user:status`

**请求 `UpdateUserStatusRequest`**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `status` | enum | 是 | `ENABLED` / `DISABLED` |
| `version` | int | 是 | 乐观锁版本 |

**响应**：200 + `UserDetail`

**说明**：禁用时立即吊销该用户全部 token，并清理权限缓存。

### 5.6 `POST /api/v1/admin/iam/users/{id}/reset-password`

**权限**：`iam:user:reset-password`

**请求 `ResetPasswordRequest`**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `newPassword` | string | 是 | 8–72，字母+数字 |
| `version` | int | 是 | 乐观锁版本 |

**响应**：204；设置 `mustChangePassword = 1`，吊销全部 token。

### 5.7 `GET /api/v1/admin/iam/users/{id}/roles`

**权限**：`iam:user:list`

**响应 `UserRolesResponse`**

| 字段 | 类型 | 说明 |
|---|---|---|
| `roles` | `RoleBrief[]` | 已分配角色 |

### 5.8 `PUT /api/v1/admin/iam/users/{id}/roles`

**权限**：`iam:user:assign-role`

**请求 `AssignRolesRequest`**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `roleIds` | long[] | 是 | 全量替换；只允许启用角色 |

**响应**：204

**错误**：`IAM_USER_NOT_FOUND` 404、`IAM_INVALID_ROLE_IDS` 400。

> 关联集合采用幂等全量替换，不使用 `version`；并发覆盖为最后一次写入生效。

### 5.9 `DELETE /api/v1/admin/iam/users/{id}/sessions`

**权限**：`iam:user:force-logout`

**响应**：204；删除该用户全部 token。

### 5.10 `DELETE /api/v1/admin/iam/users/{id}/sessions/{device}`

**权限**：`iam:user:force-logout`

**路径参数**：`device` 为 `WEB` / `APP` / `MINI`。

**响应**：204；无会话时同样返回 204。

## 6. 角色管理

### 6.1 `GET /api/v1/admin/iam/roles`

**权限**：`iam:role:list`

**查询参数**：`page`、`size`、`sort`（`sort`、`createdAt`、`code`）、`keyword`（角色名/编码前缀）、`status`。

**响应**：`PageResult<RoleSummary>`

`RoleSummary`：`id`、`code`、`name`、`description`、`status`、`sort`、`createdAt`。

### 6.2 `GET /api/v1/admin/iam/roles/{id}`

**权限**：`iam:role:list`

**响应 `RoleDetail`**：`RoleSummary` + `updatedAt`、`version`。

### 6.3 `POST /api/v1/admin/iam/roles`

**权限**：`iam:role:create`

**请求 `CreateRoleRequest`**

| 字段 | 类型 | 必填 | 校验 |
|---|---|---|---|
| `code` | string | 是 | 大写 snake_case，2–64 |
| `name` | string | 是 | 1–64 |
| `description` | string? | 否 | ≤255 |
| `status` | enum | 否 | 默认 `ENABLED` |
| `sort` | int | 否 | 默认 0 |

**响应**：201 + `RoleDetail`

**错误**：`IAM_ROLE_CODE_EXISTS` 409。

### 6.4 `PUT /api/v1/admin/iam/roles/{id}`

**权限**：`iam:role:update`

**请求 `UpdateRoleRequest`**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `name` | string | 是 | 1–64 |
| `description` | string? | 是（可为 null） | ≤255；null 表示清空 |
| `status` | enum | 是 | `ENABLED` / `DISABLED` |
| `sort` | int | 是 | 排序 |
| `version` | int | 是 | 乐观锁版本 |

**响应**：200 + `RoleDetail`

**错误**：`IAM_ROLE_NOT_FOUND` 404、`IAM_ROLE_PROTECTED` 409、`COMMON_OPTIMISTIC_LOCK` 409。

**说明**：`code` 不可修改；`SUPER_ADMIN` 不允许禁用；角色状态变更后清理受影响用户的权限缓存。

### 6.5 `DELETE /api/v1/admin/iam/roles/{id}`

**权限**：`iam:role:delete`

**响应**：204；应用服务校验无用户引用并清理角色-资源。

**错误**：`IAM_ROLE_IN_USE` 409、`IAM_ROLE_PROTECTED` 409。

### 6.6 `GET /api/v1/admin/iam/roles/{id}/resources`

**权限**：`iam:role:list`

**响应 `RoleResourcesResponse`**

| 字段 | 类型 | 说明 |
|---|---|---|
| `resourceIds` | long[] | 已授权资源 ID |

### 6.7 `PUT /api/v1/admin/iam/roles/{id}/resources`

**权限**：`iam:role:assign-resource`

**请求 `AssignResourcesRequest`**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `resourceIds` | long[] | 是 | 全量替换 |

**响应**：204

**错误**：`IAM_ROLE_NOT_FOUND` 404、`IAM_INVALID_RESOURCE_IDS` 400。

> 关联集合采用幂等全量替换，不使用 `version`；并发覆盖为最后一次写入生效。

## 7. 资源管理

> 资源不提供运行时创建/删除端点；新增与删除由 Flyway 迁移维护，后台只更新展示属性与启停状态。

### 7.1 `GET /api/v1/admin/iam/resources/tree`

**权限**：`iam:resource:list`

**查询参数**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `status` | enum | 否 | `ENABLED` / `DISABLED`；不传返回全部 |
| `type` | enum | 否 | `DIRECTORY` / `MENU` / `BUTTON` / `API` |

**响应**：`ResourceNode[]`（树形，`children` 递归）

`ResourceNode`：`id`、`parentId`、`type`、`code`、`name`、`path`、`component`、`icon`、`sort`、`visible`、`status`、`children`。

> 资源树是系统资源的有界集合，不做分页。

### 7.2 `GET /api/v1/admin/iam/resources/{id}`

**权限**：`iam:resource:list`

**响应 `ResourceDetail`**：`ResourceNode` 去掉 `children`，增加 `createdAt`、`updatedAt`、`version`。

### 7.3 `PUT /api/v1/admin/iam/resources/{id}`

**权限**：`iam:resource:update`

**请求 `UpdateResourceRequest`**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `name` | string | 是 | 1–64 |
| `parentId` | long | 是 | 0 或 `DIRECTORY` / `MENU` 资源 |
| `sort` | int | 是 | 排序 |
| `icon` | string? | 是（可为 null） | ≤64；null 表示清空 |
| `visible` | boolean | 是 | 是否显示 |
| `version` | int | 是 | 乐观锁版本 |

**响应**：200 + `ResourceDetail`

**错误**：`IAM_RESOURCE_NOT_FOUND` 404、`IAM_RESOURCE_PARENT_INVALID` 400、`COMMON_OPTIMISTIC_LOCK` 409。

**说明**：`type`、`code`、`path`、`component` 由迁移维护，不在请求 DTO 中；父级必须为根（0）、`DIRECTORY` 或 `MENU`，`BUTTON` / `API` 不能作为父级且自身必须有非 0 父级，且不能形成环。

### 7.4 `PATCH /api/v1/admin/iam/resources/{id}/status`

**权限**：`iam:resource:status`

**请求 `UpdateResourceStatusRequest`**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `status` | enum | 是 | `ENABLED` / `DISABLED` |
| `version` | int | 是 | 乐观锁版本 |

**响应**：200 + `ResourceDetail`

**错误**：`IAM_RESOURCE_NOT_FOUND` 404、`IAM_RESOURCE_HAS_ENABLED_CHILDREN` 409、`COMMON_OPTIMISTIC_LOCK` 409。

**说明**：资源状态变更后清理受影响用户的权限缓存。停用目录/菜单前校验不存在启用子资源；存在则返回 `IAM_RESOURCE_HAS_ENABLED_CHILDREN` 409。

## 8. 系统参数

> 系统参数不提供运行时创建/删除端点；新增与删除由 Flyway 迁移维护，后台只更新参数值。

### 8.1 `GET /api/v1/admin/system/configs`

**权限**：`sys:config:list`

**查询参数**：`page`、`size`、`sort`（`configKey`、`updatedAt`）、`group`、`keyword`（key 前缀）。

**响应**：`PageResult<ConfigSummary>`

`ConfigSummary`：`id`、`configKey`、`configValue`、`configGroup`、`publicFlag`、`description`、`updatedAt`、`version`。

### 8.2 `GET /api/v1/admin/system/configs/{id}`

**权限**：`sys:config:list`

**响应 `ConfigDetail`**：`ConfigSummary` + `createdAt`、`createdBy`、`updatedBy`。

### 8.3 `PUT /api/v1/admin/system/configs/{id}`

**权限**：`sys:config:update`

**请求 `UpdateConfigRequest`**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `configValue` | string | 是 | ≤4000；已知 key 按类型校验 |
| `description` | string? | 是（可为 null） | ≤255；null 表示清空 |
| `publicFlag` | boolean | 是 | 是否公开 |
| `version` | int | 是 | 乐观锁版本 |

**响应**：200 + `ConfigDetail`

**错误**：`SYS_CONFIG_NOT_FOUND` 404、`SYS_CONFIG_VALUE_INVALID` 400、`SYS_CONFIG_PUBLIC_FORBIDDEN` 400、`COMMON_OPTIMISTIC_LOCK` 409。

**说明**：

- `configKey` 不可修改。
- `publicFlag = 1` 仅允许 `system.*` 参数；认证、审计参数必须私有。
- 修改写入操作日志，并立即删除对应系统参数缓存。

## 9. 审计日志

### 9.1 `GET /api/v1/admin/audit/operation-logs`

**权限**：`audit:operation:list`

**查询参数**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `page` / `size` / `sort` | | 否 | `sort` 白名单：`createdAt` |
| `startTime` / `endTime` | string | 否 | 本地 ISO 时间 |
| `userId` | long | 否 | 操作人 ID |
| `username` | string | 否 | 操作人用户名前缀 |
| `module` | string | 否 | `iam` / `system` / `audit` |
| `action` | string | 否 | 动作编码 |
| `success` | boolean | 否 | 是否成功 |

> 日志表按写入时间排序；`durationMs` 仅作为展示字段，不作为排序字段，避免大表无索引排序。

**响应**：`PageResult<OperationLogSummary>`

`OperationLogSummary`：`id`、`traceId`、`userId`、`username`、`module`、`action`、`requestMethod`、`requestUri`、`success`、`ip`、`durationMs`、`createdAt`。

### 9.2 `GET /api/v1/admin/audit/operation-logs/{id}`

**权限**：`audit:operation:list`

**响应 `OperationLogDetail`**：`OperationLogSummary` + `method`、`requestParams`、`errorMessage`、`userAgent`。

### 9.3 `GET /api/v1/admin/audit/login-logs`

**权限**：`audit:login:list`

**查询参数**：`page`、`size`、`sort`（`createdAt`）、`startTime`、`endTime`、`userId`、`username`、`action`、`success`、`ip`。

**响应**：`PageResult<LoginLogSummary>`

`LoginLogSummary`：`id`、`traceId`、`userId`、`username`、`action`、`loginType`、`device`、`ip`、`success`、`failureReason`、`createdAt`。

### 9.4 `GET /api/v1/admin/audit/login-logs/{id}`

**权限**：`audit:login:list`

**响应 `LoginLogDetail`**：`LoginLogSummary` + `userAgent`。

## 10. 公开接口

### 10.1 `GET /api/v1/public/system/config`

**权限**：公开

**响应 `PublicSiteConfig`**

| 字段 | 类型 | 说明 |
|---|---|---|
| `systemName` | string | 系统名称 |
| `logoUrl` | string | Logo 地址，可能为空 |

**说明**：只返回 `public_flag = 1` 且属于 `system.*` 的参数；不暴露其他配置。站点配置为单例资源，因此路径使用单数 `config`。

## 11. 错误码清单

| 错误码 | HTTP | 说明 |
|---|---|---|
| `COMMON_VALIDATION_FAILED` | 400 | 参数校验失败 |
| `COMMON_BAD_REQUEST` | 400 | 请求参数错误 |
| `COMMON_NOT_FOUND` | 404 | 资源不存在 |
| `COMMON_CONFLICT` | 409 | 数据冲突 |
| `COMMON_OPTIMISTIC_LOCK` | 409 | 数据已被修改，请刷新后重试 |
| `COMMON_TOO_MANY_REQUESTS` | 429 | 请求过于频繁 |
| `COMMON_INTERNAL_ERROR` | 500 | 服务器内部错误 |
| `AUTH_UNAUTHENTICATED` | 401 | 未登录 |
| `AUTH_TOKEN_INVALID` | 401 | 登录凭证无效 |
| `AUTH_TOKEN_EXPIRED` | 401 | 登录已过期 |
| `AUTH_BAD_CREDENTIALS` | 401 | 用户名或密码错误 |
| `AUTH_OLD_PASSWORD_INVALID` | 400 | 当前密码不正确 |
| `AUTH_TOO_MANY_ATTEMPTS` | 429 | 尝试次数过多，请稍后再试 |
| `AUTH_FORBIDDEN` | 403 | 无权限 |
| `AUTH_CSRF_INVALID` | 403 | CSRF 校验失败 |
| `AUTH_PASSWORD_CHANGE_REQUIRED` | 403 | 必须先修改密码 |
| `IAM_USER_NOT_FOUND` | 404 | 用户不存在 |
| `IAM_USERNAME_EXISTS` | 409 | 用户名已存在 |
| `IAM_ROLE_NOT_FOUND` | 404 | 角色不存在 |
| `IAM_ROLE_CODE_EXISTS` | 409 | 角色编码已存在 |
| `IAM_ROLE_IN_USE` | 409 | 角色已被用户引用 |
| `IAM_ROLE_PROTECTED` | 409 | 内置角色受保护（不可删除、禁用或修改 code） |
| `IAM_INVALID_ROLE_IDS` | 400 | 角色列表包含无效项 |
| `IAM_RESOURCE_NOT_FOUND` | 404 | 资源不存在 |
| `IAM_RESOURCE_PARENT_INVALID` | 400 | 目标父级不存在或会形成环 |
| `IAM_RESOURCE_HAS_ENABLED_CHILDREN` | 409 | 资源存在启用子资源，不能停用 |
| `IAM_INVALID_RESOURCE_IDS` | 400 | 资源列表包含无效项 |
| `SYS_CONFIG_NOT_FOUND` | 404 | 系统参数不存在 |
| `SYS_CONFIG_VALUE_INVALID` | 400 | 系统参数值不合法 |
| `SYS_CONFIG_PUBLIC_FORBIDDEN` | 400 | 该参数不允许公开 |
| `AUDIT_LOG_NOT_FOUND` | 404 | 日志不存在 |

校验错误响应：

```json
{
  "code": "COMMON_VALIDATION_FAILED",
  "message": "参数校验失败",
  "data": {
    "fieldErrors": [
      { "field": "username", "message": "长度必须在 3 到 64 之间" }
    ]
  }
}
```

## 12. OpenAPI 约定

- 使用 SpringDoc 生成 OpenAPI 3。
- 标签：`Auth`、`User`、`Role`、`Resource`、`SystemConfig`、`AuditLog`、`Public`。
- 安全方案：
  - `cookieAuth`：apiKey，位置 cookie，名称 `KEEL_TOKEN`。
  - `bearerAuth`：HTTP bearer，`bearerFormat: opaque`。
- 管理端接口默认声明 `cookieAuth` 与 `bearerAuth`；公开端点（如 `auth/login`、`auth/csrf`）与公开接口声明 `security: []`。
- 通用 schema：`ApiResponse<T>`、`PageResult<T>`、`FieldError`、各错误响应。
- 每个端点必须有摘要、参数说明、响应示例。
- 分组：
  - `admin`：`/api/v1/admin/**`
  - `public`：`/api/v1/public/**`
- 前端从最新快照生成 TypeScript 类型；CI 校验生成物与 `openapi.json` 一致。

## 13. 前端对接约定

- 启动调用 `/auth/me`；401 进入登录页，403 展示无权限。
- 登录前先获取 CSRF token；所有写请求带 `X-XSRF-TOKEN`。
- 登录成功后不保存 token；依赖 `KEEL_TOKEN` Cookie。
- 登录成功后默认跳转第一个有权限菜单；不提供仪表盘路由。
- 资源与系统参数页面不提供新增/删除入口，仅提供编辑、启停和参数值更新。
- 菜单树来自 `/auth/me`；静态路由使用 `meta.permission` 校验。
- `v-permission` 指令优先判断 `superAdmin`，再判断权限码。
- `mustChangePassword = true` 时强制跳转改密页，仅允许改密、登出、`/me`。
- 改密成功后 token 已吊销，前端清理登录态并跳转登录页。
- 强制下线或 token 失效统一按 401 处理。
- 时间按本地时间直接展示，不做时区换算。

## 14. 评审清单

- [ ] 路径带 `/api/v1`，资源名复数、kebab-case。
- [ ] HTTP 方法与状态码符合语义。
- [ ] 响应为 `{ code, message, data }`，无 `traceId` 字段。
- [ ] 错误码已注册、绑定 HTTP 状态、未复用。
- [ ] 请求 / 响应为显式 DTO，校验完整。
- [ ] 分页、排序、筛选有上限与白名单。
- [ ] 鉴权注解与权限码正确。
- [ ] 敏感信息未进入响应或日志。
- [ ] OpenAPI 有摘要、示例与安全方案。
- [ ] 前端类型已从最新快照生成。

## 15. 评审确认结论

本轮评审的接口范围已定稿：

| 事项 | 结论 |
|---|---|
| 资源/参数生命周期 | 迁移维护；不提供创建/删除端点 |
| 资源停用 | 存在启用子资源时返回 409 |
| 仪表盘 | 不提供 |
| `SUPER_ADMIN` | 不允许禁用 |
| `API` 资源类型 | 预留类型，不种子化 |

## 16. 关联文档

- `01-需求与功能模块梳理.md`
- `02-技术选型与整体架构.md`
- `03-数据库设计.md`
- `05-实施计划与工程规范.md`
- `06-设计方案评审结果.md`
- `../rules/api-rules.md`
