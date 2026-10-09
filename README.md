# 外卖配送平台

项目包含 Spring Boot 后端、Vue 3 前端、MySQL、Redis 和 MinIO，当前实现覆盖顾客、商家、骑手和管理员四类角色的第一阶段业务闭环。技术方案见 `docs/外卖配送平台技术方案.md`。

## 访问地址

开发模式默认地址：

| 页面 | 地址 |
|---|---|
| 登录页 | `http://localhost:5173/login` |
| 注册页 | `http://localhost:5173/register` |
| 顾客首页 | `http://localhost:5173/home` |
| 移动端首页 | `http://localhost:5173/m/home` |
| 后端 API | `http://127.0.0.1:8080` |
| Swagger UI | `http://127.0.0.1:8080/swagger-ui.html` |
| 后端健康检查 | `http://127.0.0.1:8080/actuator/health` |

生产预览时登录页为 `http://localhost:4173/login`。

手机浏览器访问任意电脑端地址（如 `http://localhost:5173/home`）会自动跳转到内容一致的移动版（`/m/*`，底部 Tab 导航）；也可直接访问 `/m/home`，电脑浏览器同样可以打开 `/m/*` 预览移动版。移动端覆盖顾客点餐闭环、商家接单与菜单管理、骑手工作台与抢单；管理员后台保留电脑端。

首次使用请在注册页创建账户。注册接口默认创建 `CUSTOMER` 账户；商家、骑手和管理员角色需要管理员在后台调整。数据库 seed 用户的密码哈希是占位值，不能直接作为登录凭据。

## 完整运行方法

### 环境要求

- Java 21
- Maven 3.9+
- Node.js 20+ 和 npm
- Docker 和 Docker Compose
- 可用端口：`3306`、`6379`、`8080`、`9000`、`9001`、`5173`

### 1. 初始化本地配置和依赖

在项目根目录执行：

```bash
./scripts/init-env.sh
./scripts/install-project-deps.sh
```

`init-env.sh` 会从 `.env.example` 创建 `.env`，并生成数据库、Redis、MinIO 和 JWT 的本地开发密钥。已有 `.env` 不会被覆盖。

`install-project-deps.sh` 会安装 Maven/npm 依赖，并验证后端打包和前端构建。

### 2. 启动基础设施

```bash
docker compose up -d
./scripts/verify-env.sh
```

查看服务状态：

```bash
docker compose ps
docker compose logs -f mysql redis minio
```

### 3. 初始化数据库

`docker-compose.yml` 只负责启动 MySQL，首次运行需要导入数据库结构和基础数据：

```bash
docker compose exec -T mysql sh -lc 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < sql/init/init.sql
```

该脚本会创建 `food_delivery` 数据库、业务表和基础 seed 数据。重复执行不会重置已经变化的库存、销量或业务状态。

### 4. 启动后端

打开第一个终端，在项目根目录执行：

```bash
cd backend
mvn -s ../.mvn/settings.xml spring-boot:run
```

后端启动后监听 `http://127.0.0.1:8080`。后端启动时会自动查找项目根目录或父目录的 `.env`，命令行环境变量优先于 `.env`。

### 5. 启动前端

打开第二个终端，在项目根目录执行：

```bash
cd frontend
npm run dev
```

前端开发服务器默认监听 `http://localhost:5173`，并代理：

- `/api` 到 `http://127.0.0.1:8080`
- `/ws` 到 `ws://127.0.0.1:8080`

启动完成后直接访问登录页：

```text
http://localhost:5173/login
```

没有账户时访问 `http://localhost:5173/register` 注册。

## 前端开发与验证

前端位于 `frontend`，使用 Vue 3、TypeScript、Vite、Tailwind CSS、Pinia、Vue Router 和 Axios，界面统一采用 Bento Grid 布局。字体使用系统字体栈，不请求 Google Fonts。

```bash
cd frontend
npm run type-check
npm run style-check
npm run build
```

命令说明：

- `npm run type-check`：TypeScript/Vue 类型检查
- `npm run style-check`：Bento Grid 风格与禁止项检查
- `npm run build`：生成生产构建到 `frontend/dist`
- `npm run preview`：本地预览生产构建，默认 `http://localhost:4173`

可通过环境变量调整代理和运行时 API 地址：`VITE_API_BASE_URL`、`VITE_API_PROXY_TARGET`、`VITE_WS_PROXY_TARGET`、`VITE_WS_URL`、`VITE_USE_MOCK`。

## 生产运行

构建后端和前端：

```bash
mvn -s .mvn/settings.xml -pl backend -am package
cd frontend
npm run build
```

启动后端：

```bash
java -jar backend/target/delivery-backend-1.0.0.jar
```

本地预览前端生产构建：

```bash
cd frontend
npm run preview
```

生产部署时需要把 `frontend/dist` 交给静态服务器或 CDN，并配置 SPA fallback，使 `/login`、`/orders` 等前端路由都返回 `index.html`。

## IDEA 构建

使用 IntelliJ IDEA 打开项目根目录并导入根 `pom.xml`。IDEA 会自动识别 `backend` Maven 模块和 `frontend` Node/TypeScript 模块。

后端测试命令：

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

## 本地服务

| 服务 | 地址 | 用途 |
|---|---|---|
| MySQL 8 | `127.0.0.1:3306` | 主数据库 |
| Redis 7 | `127.0.0.1:6379` | 缓存、Token 和实时状态 |
| MinIO API | `http://127.0.0.1:9000` | 对象存储 API |
| MinIO Console | `http://127.0.0.1:9001` | 对象存储管理界面 |

数据库、Redis、MinIO 和 JWT 的本地密钥保存在 `.env`，该文件不会提交到 Git。高德地图密钥可在 `.env` 中补充。

停止基础设施：

```bash
docker compose down
```

## 镜像拉取失败

安装脚本优先使用 Docker Hub 和 Quay 官方镜像。如果网络无法访问官方源，会自动把 `.env` 切换为：

- MySQL：`docker.m.daocloud.io/library/mysql:8.4`
- Redis：`docker.m.daocloud.io/library/redis:7.4-alpine`
- MinIO：`quay.m.daocloud.io/minio/minio:latest`

切换镜像源不会删除 MySQL、Redis 或 MinIO 的已有数据卷。仍失败时，检查系统代理或 DNS 后重新执行安装脚本。
