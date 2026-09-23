# 外卖配送平台

项目当前处于环境准备阶段。技术方案见 `docs/外卖配送平台技术方案.md`。

## IDEA 构建

使用 IntelliJ IDEA 打开项目根目录并导入根 `pom.xml`。IDEA 会自动识别 `backend` Maven 模块和 `frontend` Node/TypeScript 模块。后端使用 Java 21，构建命令：

```bash
mvn test
```

## Manjaro 开发环境

首次配置在项目根目录执行：

```bash
./scripts/setup-manjaro.sh
```

该脚本会：

1. 安装 OpenJDK 21、Maven、Docker、Docker Compose、Docker Buildx 和构建工具。
2. 复用 Node.js 20+；当前环境已有 Node.js 24 时不会重复安装。
3. 将默认 JDK 设置为 Java 21，并配置 `JAVA_HOME`。
4. 启用 Docker 服务，并把当前用户加入 `docker` 组。
5. 从 `.env.example` 生成带随机密钥的本地 `.env`。
6. 拉取 MySQL 8、Redis 7 和 MinIO 镜像；官方源失败时自动切换 DaoCloud 镜像源重试。
7. 启动 MySQL 8、Redis 7 和 MinIO。

脚本需要输入一次 sudo 密码。加入 `docker` 组后，当前终端执行 `exec newgrp docker` 立即刷新组权限；也可注销并重新登录。

```bash
exec newgrp docker
```

检查环境：

```bash
./scripts/verify-env.sh
```

安装并验证项目级依赖：

```bash
./scripts/install-project-deps.sh
```

后端位于 `backend`，使用 Spring Boot 3.4.1、Java 21 和 Maven；前端位于 `frontend`，使用 Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router、Axios、ECharts 和高德地图 Loader。依赖安装脚本会显式使用 `.mvn/settings.xml`，并在镜像源失败时回退 Maven Central；随后分别执行后端打包和前端构建。

后端启动时会自动查找项目根目录或父目录的 `.env`，因此 IDEA 直接运行 `DeliveryApplication` 不需要额外配置环境变量。命令行环境变量优先于 `.env`。

管理基础服务：

```bash
docker compose ps
docker compose logs -f mysql redis minio
docker compose down
```

## 本地服务

| 服务 | 地址 | 用途 |
|---|---|---|
| MySQL 8 | `127.0.0.1:3306` | 主数据库 |
| Redis 7 | `127.0.0.1:6379` | 缓存、Token 和实时状态 |
| MinIO API | `http://127.0.0.1:9000` | 对象存储 API |
| MinIO Console | `http://127.0.0.1:9001` | 对象存储管理界面 |

数据库、Redis、MinIO 和 JWT 的本地密钥保存在 `.env`，该文件不会提交到 Git。高德地图密钥可在 `.env` 中补充。

## 镜像拉取失败

安装脚本优先使用 Docker Hub 和 Quay 官方镜像。如果网络无法访问官方源，会自动把 `.env` 切换为：

- MySQL：`docker.m.daocloud.io/library/mysql:8.4`
- Redis：`docker.m.daocloud.io/library/redis:7.4-alpine`
- MinIO：`quay.m.daocloud.io/minio/minio:latest`

切换镜像源不会删除 MySQL、Redis 或 MinIO 的已有数据卷。仍失败时，检查系统代理或 DNS 后重新执行安装脚本。
