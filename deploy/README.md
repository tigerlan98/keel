# 部署资源

本目录存放本地依赖服务与同域部署配置。

| 文件 | 说明 |
|---|---|
| `docker-compose.yml` | 默认启动 MySQL 8.4 与 Redis 7.4；`--profile full` 启动完整栈 |
| `nginx/nginx.conf` | 同域入口：`/admin/`、`/portal/`、`/api/`、`/health` |
| `docker-compose.env.example` | 环境变量模板，提交到版本库 |
| `docker-compose.env` | 本地真实密码，已被 `.gitignore` 忽略 |

## 只启动依赖服务（日常开发）

```powershell
cd deploy
Copy-Item docker-compose.env.example docker-compose.env
# 编辑 docker-compose.env，填写真实密码
docker compose --env-file docker-compose.env up -d
```

从仓库根执行：

```powershell
docker compose --env-file deploy/docker-compose.env -f deploy/docker-compose.yml up -d
```

## 完整同域部署（`--profile full`）

前置条件：使用 JDK 25 构建后端 jar。

```powershell
cd backend
mvn verify
```

在 `deploy/docker-compose.env` 中填写数据库密码与 Redis 密码：

```dotenv
BACKEND_PORT=8080
HTTP_PORT=80
```

初始管理员由 Flyway 种子创建（用户名 `admin`），默认密码为开发脚手架固定值，首次登录后请立即修改。

启动完整栈：

```powershell
cd deploy
docker compose --env-file docker-compose.env --profile full up -d --build
```

访问入口：

| 路径 | 内容 |
|---|---|
| `http://127.0.0.1/admin/` | 管理端 |
| `http://127.0.0.1/portal/` | 门户 |
| `http://127.0.0.1/api/v1/public/system/config` | 公开配置 |
| `http://127.0.0.1/health` | 后端健康检查 |

说明：

- 后端镜像使用 `backend/keel-bootstrap/target/keel-bootstrap-1.0.0.jar`，需先执行 `mvn verify` 或 `mvn package`。
- Admin / Portal 在镜像内以 `/admin/`、`/portal/` 为 base 构建，由 Nginx 同域代理。
- 初始管理员由全局种子迁移创建，仅做 `WHERE NOT EXISTS` 插入，不会覆盖已有账号。
- Nginx 传递 `X-Forwarded-For`，后端启用 `forward-headers-strategy=framework`，登录锁定按真实来源 IP 计数。

停止：

```powershell
cd deploy
docker compose --env-file docker-compose.env --profile full down
```

## 其他说明

- 容器统一使用 `Asia/Shanghai` 时区。
- MySQL 与 Redis 仅绑定 `127.0.0.1`，不对局域网暴露。
- 数据卷位于 `D:/Docker/volumes/`，删除容器不会删除数据。
- 禁止提交 `docker-compose.env` 或任何真实密码。
