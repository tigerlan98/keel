# 部署资源

本目录存放本地依赖服务与后续部署配置。当前包含：

| 文件 | 说明 |
|---|---|
| `docker-compose.yml` | 本地 MySQL 8.4 与 Redis 7.4 |
| `docker-compose.env.example` | 环境变量模板，提交到版本库 |
| `docker-compose.env` | 本地真实密码，已被 `.gitignore` 忽略 |

## 首次使用

```powershell
cd deploy
Copy-Item docker-compose.env.example docker-compose.env
# 编辑 docker-compose.env，填写真实密码
docker compose --env-file docker-compose.env up -d
```

## 从仓库根执行

```powershell
docker compose --env-file deploy/docker-compose.env -f deploy/docker-compose.yml up -d
```

## 停止

```powershell
cd deploy
docker compose --env-file docker-compose.env down
```

## 说明

- 容器统一使用 `Asia/Shanghai` 时区。
- MySQL 与 Redis 仅绑定 `127.0.0.1`，不对局域网暴露。
- 数据卷位于 `D:/Docker/volumes/`，删除容器不会删除数据。
- 禁止提交 `docker-compose.env` 或任何真实密码。
