# Shiyi Docker 部署指南

> ⚠️ **重要提醒**: 文档中的域名 `example.com`、`admin.example.com` 等仅为示例，请根据实际情况替换为你自己的域名。所有域名示例文件名为 `*.template`，复制后去掉 `.template` 再修改。
> ⚠️ **真实部署的配置（带你的域名+密码）请不要提交到Git！** `sites-enabled/*`、`.env` 已在根 `.gitignore` 中被忽略（仅保留 `*.template` 模板）。

本项目支持使用 Docker 进行快速部署，包含后端 Java 应用、MySQL、Redis 和 Nginx 反向代理。

## 目录结构

```
Shiyi-Website/
├── Dockerfile              # 后端应用镜像（生产用 SerialGC，适配 <4GB 机器）
├── docker-compose.yml     # 服务编排
└── docker/
    ├── .env.example       # 环境变量示例 (复制为 .env 后修改)
    ├── nginx/
    │   ├── nginx.conf               # Nginx 主配置
    │   ├── conf.d/
    │   │   └── shiyi-backend.conf   # 后端代理配置
    │   └── sites-enabled/
    │       └── example.com.template # 四站点Nginx配置模板（复制去掉.template后改成你的域名）
    ├── mysql/
    │   └── init/
    │       └── shiyi.sql   # ✅ 数据库初始化脚本（建库+建表，UTF-8 字符集）
    └── html/               # 前端静态文件目录（每个站点一个子目录）
        └── .gitkeep
```

## 前置要求

- Docker >= 20.10
- Docker Compose >= 2.0
- 域名已解析到服务器（用于 HTTPS）

## 部署步骤

### 1. 配置环境变量

```bash
cd docker
cp .env.example .env
```

编辑 `.env` 文件，**必须修改以下配置**：

```bash
# ============================================
# MySQL配置 (必须修改)
# ============================================
MYSQL_ROOT_PASSWORD=your_root_password_here
MYSQL_PASSWORD=your_password_here

# ============================================
# JWT密钥 (必须修改!)
# ============================================
# 使用随机字符串生成，至少32位
# 可以使用: openssl rand -base64 32
JWT_SECRET_KEY=your-very-long-random-secret-key-here-change-this

# ============================================
# 阿里云OSS配置 (必须修改)
# ============================================
ALIOSS_ENDPOINT=oss-cn-shanghai.aliyuncs.com
ALIOSS_ACCESS_KEY_ID=your-access-key-id
ALIOSS_ACCESS_KEY_SECRET=your-access-key-secret
ALIOSS_BUCKET_NAME=your-bucket-name

# ============================================
# 邮件配置 (必须修改)
# ============================================
EMAIL_PERSONAL=YourName
EMAIL_FROM=your-email@example.com

# ============================================
# 访客登录验证码 (必须修改)
# ============================================
VISITOR_VERIFY_CODE=123456

# ============================================
# 网站URL配置 (替换为你的实际域名)
# ============================================
WEBSITE_TITLE=YourSiteTitle
WEBSITE_HOME=https://example.com            # 博客内含 /home 路由，可不单独部署主站
WEBSITE_ADMIN=https://admin.example.com
WEBSITE_CV=https://cv.example.com
WEBSITE_BLOG=https://blog.example.com
```

### 2. 导入数据库（✅ 用 docker/mysql/init/shiyi.sql，不是 init.sql！）

首次部署 MySQL 容器会**自动执行** `docker/mysql/init/shiyi.sql`（建库 `shiyi` + 建表，UTF-8）。
如果要手动初始化：

```bash
# 启动 MySQL 容器
docker compose up -d mysql

# 等待 15~30s 让 MySQL 初始化完成（首次会自动执行 shiyi.sql）
docker compose logs -f mysql | grep -i "ready for connections"

# 确认数据库已创建+表已建：
docker exec -it shiyi-mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "SHOW DATABASES; USE shiyi; SHOW TABLES;"
```

> ✅ **不需要手动 `CREATE DATABASE` 或 `GRANT`**，shiyi.sql 开头已执行：
> ```sql
> drop database if exists shiyi;
> create database shiyi character set utf8mb4 collate utf8mb4_unicode_ci;
> ```

### 3. 放入前端静态文件（3个目录！Frontend-Home 不存在：个人信息路由已整合进 Frontend-Blog）

> 📌 **实际项目只有3个前端工程**：Frontend-Blog（含 /home 个人信息路由）、Frontend-Admin、Frontend-Cv。
>
> 构建每个前端：`cd Frontend-xxx && pnpm build`，产物在 `dist/`。

```bash
# ----------博客（含个人信息/home路由，推荐作为主站入口）----------
mkdir -p html/blog.example.com/html
cp -r ../Frontend-Blog/dist/* html/blog.example.com/html/

# ----------简历----------
mkdir -p html/cv.example.com/html
cp -r ../Frontend-Cv/dist/* html/cv.example.com/html/

# ----------管理后台----------
mkdir -p html/admin.example.com/html
cp -r ../Frontend-Admin/dist/* html/admin.example.com/html/

#（可选）如果你想把 example.com 根域名也用博客内容：
mkdir -p html/example.com/html
cp -r ../Frontend-Blog/dist/* html/example.com/html/
```

### 4. 配置 Nginx 站点

```bash
cd nginx/sites-enabled
# 复制模板去掉 .template 再改成你的域名
cp example.com.template yourdomain.com.conf

# 打开 yourdomain.com.conf，全局把 4 处 example.com 替换为你的真实域名（1次查找替换即可）
#   example.com         → 你的主域名
#   blog.example.com    → blog.你的域名
#   cv.example.com      → cv.你的域名
#   admin.example.com   → admin.你的域名
```

### 5. 构建并启动服务

```bash
# 返回 docker 上级目录，构建并启动所有服务
cd ../..
docker compose up -d --build

# 查看运行状态
docker compose ps

# 查看日志（后端/NGINX有问题先看这个）
docker compose logs -f backend
docker compose logs -f nginx
```

### 6. 配置 HTTPS（可选）

#### 使用 Certbot 自动配置

```bash
# 进入 Nginx 容器
docker exec -it shiyi-nginx sh

# 安装 Certbot
apk add --no-cache certbot py3-certbot-nginx

# 申请证书（需要域名已解析到服务器）
certbot --nginx -d example.com -d www.example.com
certbot --nginx -d blog.example.com
certbot --nginx -d cv.example.com
certbot --nginx -d admin.example.com
```

#### 手动配置 HTTPS

编辑 `docker/nginx/sites-enabled/yourdomain.com.conf`，在对应 `server {}` 块里添加 SSL 配置：

```nginx
server {
    listen 443 ssl http2;
    server_name example.com;

    ssl_certificate /etc/letsencrypt/live/example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/example.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;

    # ... 其他 location 配置保持不变
}

# HTTP 重定向到 HTTPS
server {
    listen 80;
    server_name example.com;
    return 301 https://$server_name$request_uri;
}
```

## 环境变量说明

| 变量名 | 必填 | 说明 | 默认值 |
|--------|------|------|--------|
| `MYSQL_ROOT_PASSWORD` | 是 | MySQL root 密码 | - |
| `MYSQL_PASSWORD` | 是 | MySQL shiyi 用户密码 | - |
| `REDIS_PASSWORD` | 否 | Redis 密码（无需密码则留空） | - |
| `JWT_SECRET_KEY` | 是 | JWT 签名密钥，建议至少32位随机字符串 | - |
| `JWT_TTL` | 否 | JWT 过期时间(毫秒) | 86400000 (24小时) |
| `ALIOSS_ENDPOINT` | 是 | 阿里云 OSS 地域节点 | - |
| `ALIOSS_ACCESS_KEY_ID` | 是 | 阿里云 AccessKey ID | - |
| `ALIOSS_ACCESS_KEY_SECRET` | 是 | 阿里云 AccessKey Secret | - |
| `ALIOSS_BUCKET_NAME` | 是 | 阿里云 OSS Bucket 名称 | - |
| `EMAIL_PERSONAL` | 是 | 邮件发送者昵称 | - |
| `EMAIL_FROM` | 是 | 邮件发送者邮箱 | - |
| `VISITOR_VERIFY_CODE` | 是 | 访客登录验证码 | - |
| `WEBSITE_TITLE` | 否 | 网站标题 | - |
| `WEBSITE_HOME` | 否 | 首页地址（博客端含/home路由，可与BLOG共用同一个） | - |
| `WEBSITE_ADMIN` | 否 | 管理后台地址 | - |
| `WEBSITE_CV` | 否 | 简历地址 | - |
| `WEBSITE_BLOG` | 否 | 博客地址 | - |

## 常用命令

```bash
# 启动服务
docker compose up -d

# 停止服务
docker compose down

# 重启服务
docker compose restart

# 查看日志
docker compose logs -f backend    # 后端日志
docker compose logs -f nginx       # Nginx 日志
docker compose logs -f mysql       # MySQL 日志
docker compose logs -f redis       # Redis 日志

# 进入容器
docker exec -it shiyi-backend sh
docker exec -it shiyi-mysql mysql -uroot -p
docker exec -it shiyi-redis redis-cli

# 重新构建后端
docker compose build backend
docker compose up -d backend
```

## 数据持久化

- MySQL 数据: `mysql_data` 命名卷（或 `./docker/mysql/data`，以 compose 实际配置为准）
- Redis 数据: `redis_data` 命名卷
- Nginx 日志: `nginx_logs` 命名卷
- 前端静态文件: `docker/html/` 目录（**不要在容器里改，改宿主机目录**）
- 后端日志: `docker/app/logs/` 目录

## 注意事项

1. **数据库初始化**: 首次启动 MySQL 容器会自动执行 `docker/mysql/init/shiyi.sql`。若要重置数据库，删除 MySQL 数据卷后再 `up`。
2. **敏感信息**: `.env`、`sites-enabled/*.conf`（去掉.template后的真实配置文件）包含敏感信息，已被 `.gitignore` 忽略，**请勿提交到版本控制**。
3. **JVM内存配置（⭐重要！和裸机部署一致）**：
   - Dockerfile 里默认用：`UseSerialGC -Xms192m -Xmx320m -XX:MaxMetaspaceSize=140m`（适配1.6G-2G小机器；和主 README「1.6GB真实生产优化」保持一致）
   - 如果机器内存 **≥4GB**，可升级为 G1GC：`-XX:+UseG1GC -Xms512m -Xmx1024m`
4. **安全建议**:
   - 修改默认后端端口（5922），在 docker-compose.yml 的 `ports` 段改
   - MySQL / Redis 都用强密码
   - 只对外暴露 80/443，数据库端口不要映射到公网
   - 配置服务器防火墙（ufw / firewalld / 宝塔安全组）
5. **前端 API 地址**: 前端构建时 `.env.production` 里的 `VITE_API_BASE_URL` 必须指向你部署好的后端域名（`https://blog.你的域名/api` 或 Nginx 反代后的 `/api` 路径）。

## 故障排查

### 后端启动失败

```bash
# 查看后端日志（99%的问题看这个就知道了）
docker compose logs --tail=300 backend

# 常见原因 & 解决：
#   ❌ "Communications link failure" → MySQL 没启动完 or 配置的 host/port/user/password 错
#      → 等30秒再 up，或者检查 .env 里的 MYSQL_* 变量
#   ❌ "Unable to connect to Redis" → 检查 REDIS_HOST / REDIS_PASSWORD
#   ❌ "OutOfMemoryError: Java heap space" → Xmx太小，给大一点（比如从320m加到448m）

# 检查从后端容器能不能连 MySQL 3306：
docker exec -it shiyi-backend sh -c "nc -zv mysql 3306; nc -zv redis 6379"
```

### Nginx 502 Bad Gateway

```bash
# 99% = 后端容器没在跑 or 5922端口没起来
docker compose ps                     # 看 backend 是否 Up
docker compose logs --tail=100 backend # 看后端启动日志

# 检查 Nginx 配置语法是否正确（改完配置一定要跑）
docker exec shiyi-nginx nginx -t
# 修改后热加载，不用重启容器：
docker exec shiyi-nginx nginx -s reload
```

### 数据库连接问题 / 表不存在

```bash
# 检查 MySQL 是否就绪，日志里是否出现 "ready for connections"
docker compose logs mysql | tail -n 50

# 检查数据库 shiyi 是否建好了、表是否齐全（共约20-30张表）
docker exec -it shiyi-mysql mysql -uroot -p -e "USE shiyi; SHOW TABLES;"
```

