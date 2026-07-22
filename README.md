<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5-green?style=flat-square&logo=springboot" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/Vue-3.5-brightgreen?style=flat-square&logo=vuedotjs" alt="Vue">
  <img src="https://img.shields.io/badge/Vite-7-blueviolet?style=flat-square&logo=vite" alt="Vite">
  <img src="https://img.shields.io/badge/MySQL-8.0-blue?style=flat-square&logo=mysql" alt="MySQL">
  <img src="https://img.shields.io/badge/Redis-7-red?style=flat-square&logo=redis" alt="Redis">
  <img src="https://img.shields.io/badge/license-MIT-blue?style=flat-square" alt="License">
</p>

# Shiyi · 师忆 — 个人全栈博客系统

一套基于 **Spring Boot 3 + Vue 3** 的个人网站全栈解决方案。
包含博客、后台管理、个人主页、在线简历 4 个前端子站 + 统一后端服务。

> 在线演示：[blog.zhaoxinyi.fun](https://blog.zhaoxinyi.fun) · [zhaoxinyi.fun](https://zhaoxinyi.fun) · [cv.zhaoxinyi.fun](https://cv.zhaoxinyi.fun)

---

## ✨ 功能特性

### 博客端 (Frontend-Blog)
- Markdown 文章渲染 + 全文搜索 / 分类 / 标签 / 归档 / TOC 目录
- 评论系统（嵌套回复、Markdown、悄悄话、邮件通知）
- 留言板 / 友情链接 / 文章点赞 / RSS 订阅
- **暗黑模式**（跟随系统 + 手动切换）/ **响应式设计**（移动端完美适配）
- 访客指纹识别（无需登录即可评论 / 点赞）
- 🎵 音乐播放器（进度条点击跳转 / 拖拽 / 悬停预览 / 移动端触控）

### 管理端 (Frontend-Admin)
- 文章管理（Markdown 编辑器、封面上传、分类 / 标签管理）
- 评论 / 留言审核 · 友情链接 / 音乐管理
- 个人信息 / 社交媒体管理
- 📊 访客统计 + 数据看板（ECharts）
- 系统配置 · 操作日志

### 个人主页 (Frontend-Home) · 在线简历 (Frontend-Cv)
- 简洁大气的单页设计 · 响应式布局 · 适合分享

---

## 🏗 技术架构

```
┌────────────────────────────────────────────────────────────┐
│                    Nginx 反向代理 (80/443)                  │
│  blog.*   home.*   cv.*   admin.*   ->  4 个前端 dist       │
└──────┬──────────┬──────────┬──────────┬─────────────────────┘
       │          │          │          │
       ▼          ▼          ▼          ▼
   Blog Vue    Home Vue    Cv Vue   Admin Vue
       │          │          │          │
       └──────────┴──────────┴──────────┘
                      │   /api/* + WebSocket
                      ▼
           ┌──────────────────────┐
           │   Spring Boot 3.x    │   默认端口: 5922（如遇占用可改5923）
           │   JDK 21 · MyBatis   │
           └───────┬────────┬─────┘
                   │        │
                   ▼        ▼
            ┌──────────┐ ┌──────────┐
            │ MySQL 8+ │ │  Redis   │
            └──────────┘ └──────────┘
                 │
                 ▼
          Aliyun OSS (图片/文件)
```

### 后端技术栈

| 技术 | 说明 |
|---|---|
| Spring Boot 3.5 | 应用框架（Java 21 · 虚拟线程已开启）|
| MyBatis + PageHelper | ORM + 物理分页 |
| MySQL 8 + Druid | 数据库 + 连接池（默认 Druid 监控生产已关）|
| Redis + Spring Cache | 缓存 + Session |
| JWT (JJWT 0.12) | 无状态认证授权 |
| Knife4j OpenAPI 3 | 开发环境 API 文档（生产默认关闭）|
| Aliyun OSS SDK | 对象存储 |
| WebSocket | 实时在线人数 |
| Bucket4j | 接口限流（令牌桶）|
| CommonMark + Jsoup | Markdown 解析 + HTML 清洗 |
| Thumbnailator + WebP | 图片压缩自动转 WebP |
| Spring Mail | 邮件发送（QQ 邮箱 SMTP）|

### 前端技术栈

| 技术 | 说明 |
|---|---|
| Vue 3.5 + Vite 7 | 前端框架 + 构建 |
| Vue Router 4 · Pinia 3 | 路由 · 状态管理（持久化）|
| Element Plus | UI 组件库 |
| md-editor-v3 | Markdown 编辑 / 预览 |
| ECharts 5 | 管理端数据可视化 |
| Axios · Sass | HTTP 客户端 · CSS 预处理器 |

---

## 📁 项目结构

```
Shiyi-Website/
├── Backend/                          # Spring Boot 后端
│   ├── Shiyi-common/               # 公共模块（工具类/常量/异常）
│   ├── Shiyi-pojo/                 # 实体/DTO/VO
│   └── Shiyi-server/               # 主服务（Controller/Service/Mapper）
│       └── src/main/resources/
│           ├── application.yml                # 基础配置（生产默认关监控省内存）
│           ├── application-dev.yml.template   # 开发环境模板（复制改名填密码）
│           ├── application-prod.yml.template  # 生产环境模板（复制改名填密码）
│           ├── application-docker.yml         # Docker 部署专用配置（可提交）
│           ├── database/shiyi.sql           # 数据库建表脚本
│           └── mapper/*.xml                   # MyBatis XML
├── Frontend-Blog/                   # 博客前端（首页/文章/归档/留言/搜索/个人信息/home路由）
├── Frontend-Admin/                  # 管理后台
├── Frontend-Cv/                     # 在线简历
├── Dockerfile                       # 后端 Docker 多阶段构建
├── CONTRIBUTING.md                  # 贡献指南
└── README.md
```

---

## 🚀 快速开始（本地开发）

### 环境要求

| 环境 | 版本要求 |
|---|---|
| JDK | 21 LTS 或更高 |
| Maven | 3.9+ |
| Node.js | 20.19+ / 22.12+ |
| 包管理器 | pnpm 9+ （推荐）或 npm |
| MySQL | 8.0+ |
| Redis | 7.0+ |

### 1. 克隆 + 初始化数据库

```bash
git clone https://github.com/<your-github-username>/Shiyi-Website.git
cd Shiyi-Website

# 创建数据库并导入建表脚本（数据库名可自定义，示例用shiyi）
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS shiyi DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p shiyi < Backend/Shiyi-server/src/main/resources/database/shiyi.sql
```

### 2. 配置后端

```bash
cd Backend/Shiyi-server/src/main/resources
# 如果从全新安装，复制模板改密码即可：
# cp application-dev.yml.template application-dev.yml
# cp application-prod.yml.template application-prod.yml
```

修改 `application-dev.yml`，至少改以下配置：
- MySQL 账号密码（`shiyi.datasource.*`）
- Redis 密码（`shiyi.redis.*`）
- （可选）阿里云 OSS / QQ 邮箱 / JWT 自定义密钥

### 3. 启动后端

```bash
cd Backend
mvn clean package -DskipTests

# 开发环境（开启 Druid SQL 监控 /doc.html 文档）
java -jar Shiyi-server/target/Shiyi-server-1.0-SNAPSHOT.jar --spring.profiles.active=dev

# 开发环境默认入口：
#   API:    http://localhost:5922
#   Druid 监控:  http://localhost:5922/druid   (admin / dev123456)
#   Knife4j 文档: http://localhost:5922/doc.html
```

### 4. 启动前端（任意子站可独立开发）

```bash
# 以博客端为例，其余 3 个同理
cd Frontend-Blog
pnpm install      # 或 npm install
pnpm dev          # 默认 http://localhost:5173
```

开发模式下 Vite 会自动把 `/api` 代理到 `http://localhost:5922`。

### 5. 端口总览

| 服务 | 默认地址 | 说明 |
|---|---|---|
| 博客端（含个人信息/home路由） | http://localhost:5173 | 推荐开发入口 |
| 管理后台 | http://localhost:5174 | — |
| 在线简历 | http://localhost:5176 | — |
| 后端 API | http://localhost:5922 | **如5922被占用，启动时加** `-Dserver.port=5923`（当前线上生产实际用5923）|

---

## 🖥 服务器配置建议（单 ECS 部署所有服务）

> ⚠️ 本项目默认所有进程（Java/MySQL/Redis/Nginx/宝塔面板/4个Python uWSGI站点）装在同一台服务器上。
> ⚠️ **实测：2核1.6GB（共享型最低配）加3GB Swap后可稳定跑日PV<500**；**强烈建议 2C8G**（稳定不折腾）。

### 方案对比

| 方案 | 配置 | 预估年成本 | 适用场景 |
|---|---|---|---|
| 🟢 **低配极限** | **2核 1.6GB · 40G SSD · 1M带宽 + 3GB Swap** | ¥200~400 / 年（共享型活动价） | ⭐ **当前生产实测可用** · 日PV < 500 · 纯个人博客 |
| 🟡 **⭐ 推荐（均衡）** | **2核 8GB · 60G ESSD · 3-5M带宽** | ¥1000~1800 / 年 | 日PV 500~5000 · 长期稳定 · 不折腾 |
| 🟠 中间档 | 2核 4GB · 40G SSD · 1-3M带宽 | ¥300~600 / 年（新用户活动价） | 日PV < 500 · 比1.6GB从容 |
| 🔴 高性能（商业） | 4核 16GB + 云RDS + 云Redis | ¥6000+ / 年 | 日PV过万 / SEO引流 / 商业化 |

---

### 🟢 真实生产：2核1.6GB 极限优化（当前服务器实测）

> 2026-07-22 健康检查实测：`MemTotal: 1.6GiB · Swap: 3GiB (2G /swapfile + 1.1G /www/swap) · Java堆=320MB · MariaDB=100MB`
> 必须**完整执行以下5步**，否则 OOM/SSHD 挂死/网站打不开：

1. **加 Swap 3GB**（防止偶发峰值 OOM — 当前已做，保留）
   ```bash
   # 已有就跳过，执行下面这个调 swappiness
   sysctl vm.swappiness=20      # 1.6GB机器要让系统更早用Swap，避免突发OOM
   echo 'vm.swappiness=20' >> /etc/sysctl.conf
   ```
2. **Java 启动参数（1.6GB机器用！！不是 4GB 的参数！）**
   > ⚠️ **小堆(<512MB)必用 SerialGC！G1GC 会因分区表+ remembered set 额外多占100MB+非堆内存，1.6GB机器反而更容易OOM。**
   > 当前实际部署：systemd service `/etc/systemd/system/shiyi.service`（旧名feitwnd.service）或宝塔Java项目管理器。
   ```bash
   # 复制到「JVM参数」框（已测：内存最稳，GC停顿在50ms内对博客完全够用）：
   -XX:+UseSerialGC \
   -Xms192m -Xmx320m \
   -XX:MaxMetaspaceSize=140m \
   -XX:MaxDirectMemorySize=40m \
   -XX:ReservedCodeCacheSize=32m \
   -XX:+HeapDumpOnOutOfMemoryError \
   -XX:HeapDumpPath=/opt/shiyi/logs \
   -XX:+ExitOnOutOfMemoryError \
   -Dspring.profiles.active=prod \
   -Dfile.encoding=UTF-8
   ```
   > 对比之前误写的 G1GC + Xmx288m：SerialGC + Xmx320m 在 1.6GB 机器上可用堆更多，总内存占用反而小 50~100MB
3. **MariaDB 缩内存（100MB → 40MB）**
   ```ini
   # 宝塔 → 软件商店 → MariaDB 10.x → 配置修改 → 加到 [mysqld] 块末尾
   [mysqld]
   innodb_buffer_pool_size = 128M   # 必须缩小（1.6GB机器别用512M！）
   innodb_buffer_pool_instances = 1
   key_buffer_size = 8M
   performance_schema = OFF         # 直接省 50~100MB
   max_connections = 40
   table_open_cache = 400
   table_definition_cache = 400
   ```
4. **宝塔卸载不用的插件（省100MB+）**
   - 「Node.js版本管理器 / Node项目管理器」不用就卸载（省 87MB node_manager）
   - 4 个 Python Django 站点（uWSGI 端口9001-9004）如果只用到1-2个，直接停掉不用的（每站省30-60MB）
5. 生产已默认关闭 Druid 监控 + Knife4j（`application.yml` 已写死），不用改

---

### 🟡 2C4G 机器优化（以后有钱升级到4GB时用）

如果升级到 **2核4GB**，把上面参数调大就行：

1. **Swap 保留 2G**（防止偶发峰值）
2. **Java 启动参数**
   ```bash
   -XX:+UseG1GC \
   -Xms768m -Xmx768m \
   -XX:MaxMetaspaceSize=256m \
   -XX:MaxDirectMemorySize=128m \
   -XX:ReservedCodeCacheSize=64m \
   -XX:+HeapDumpOnOutOfMemoryError \
   -XX:+ExitOnOutOfMemoryError \
   -Dspring.profiles.active=prod
   ```
   > Docker 部署已在 Dockerfile 写好，直接 build 即可。
3. **MySQL 8.0 限制内存**
   ```ini
   [mysqld]
   innodb_buffer_pool_size = 512M
   performance_schema = OFF
   max_connections = 80
   ```
4. 内存清理 + 监控脚本同下

---

## 📦 生产部署

### 后端打包

```bash
cd Backend
mvn clean package -DskipTests
# 产物：Shiyi-server/target/Shiyi-server-1.0-SNAPSHOT.jar (约69MB)
```

### 前端打包（4 个都要打）

```bash
# 博客端
cd Frontend-Blog && pnpm install && pnpm build
# 产出 dist/  ->  上传服务器 /www/wwwroot/blog.你的域名/

# 其他 3 个同理：Frontend-Admin / Frontend-Home / Frontend-Cv
```

### Nginx 配置参考（博客端示例）

```nginx
server {
    listen 80;
    server_name blog.yourdomain.com;
    root /www/wwwroot/blog.yourdomain.com/dist;
    index index.html;

    # SPA 路由 404 修复
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 静态资源 30 天强缓存
    location ~* \.(js|css|png|jpg|jpeg|webp|gif|ico|svg|woff2?|ttf|eot)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
        access_log off;
    }

    gzip on;
    gzip_comp_level 6;
    gzip_types text/plain text/css application/json application/javascript text/xml;

    # 反向代理 Java 后端
    location /api/ {
        proxy_pass http://127.0.0.1:5922/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
    }

    # WebSocket（在线人数功能）
    location /api/ws/online {
        proxy_pass http://127.0.0.1:5922/ws/online;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

其他子站（home / admin / cv）配置类似，改 `server_name` 和 `root` 路径即可。

### ⚠️ 生产必须改的配置

| 项 | 建议 |
|---|---|
| `JWT_SECRET` 环境变量 | 用随机 32+ 字符串覆盖默认密钥（`export JWT_SECRET=xxxx` 或 systemd Environment）|
| 数据库账号密码 | 不要用默认 `shiyi / Shiyi_2026!Db` |
| Redis 密码 | 给 Redis 设置 `requirepass` |
| 防火墙 / 安全组 | 只放行 22 / 80 / 443；数据库/Redis 不对外暴露 |

---

## 🧪 已修复的 Bug / 代码改进

| 模块 | 问题 | 修复方式 |
|---|---|---|
| Blog 音乐进度条 | 点击 / 拖拽 / 悬停 / 移动端触控不响应 | 用原生 DOM 事件重写进度条交互逻辑，独立变量控制显示 |
| Blog 音乐进度条 | 拖拽卡顿 + tooltip 显示 0:00 | displayPct 直接赋值 + hoverPct/hoverTimeStr 实时更新 |
| 后端 YAML | ⚠️ **致命 Bug**：`spring.mail` 缩进到 `knife4j` 下 + `spring:` 顶级key定义两次 → QQ邮箱SMTP完全失效 + datasource/redis 丢失！ | 缩进修复 + 把 mail 合并到唯一的 spring 块 |
| 后端 OOM / 内存管理 | Dockerfile 默认 ZGC + Xmx2G + 非堆无上限，2C4G吃爆 | 改 G1GC + 堆 768M 固定 + Metaspace/Direct/CodeCache 各区硬上限 + OOM自动退出 |
| 后端 OOM / 内存管理 | Druid 监控面板 + Knife4j 生产常驻多占 300MB+ | 生产 `application.yml` 默认关 + 开发 `application-dev.yml` 单独开 |
| 后端安全 | JWT Secret 硬编码到 Git | 改 `${JWT_SECRET:默认值}`，生产通过环境变量覆盖 |
| 数据库连接池 | Druid 初始/最小/最大 5/5/20 个人博客浪费 | 缩小为 2/2/10，减少常驻内存 |
| 前端 Lint | ESLint 9.x flat config 不支持 `.eslintignore` → dist/ 压缩产物全报错 | `eslint.config.js` 开头加全局 `ignores` 块，明确忽略 dist/coverage/node_modules |
| 部署文档 | 之前所有文档默认 4GB，实际服务器只有 **1.6GB** 导致优化参数全错 | 2026-07-22 重新体检后新增「1.6GB真实生产极限优化」操作手册 |
| 服务器运维 | 无自动内存监控/清理 → 凌晨偶发内存占满 SSHD 挂死 | 部署 `clean_memory.sh + watch_memory.sh` + Cron（每5分钟监控 + 每日/每月清理） |

---

## 🔐 上传 GitHub 前的安全检查清单（⭐ 必过！）

> 按照下面的顺序过一遍，避免把密码/密钥/服务器IP泄漏到公开仓库。

| 序号 | 检查项 | 命令/方法 | 结果 |
|---|---|---|---|
| 1 | 🚫 **不要提交 `.env`**（含 SSH 密码 / MySQL 密码 / JWT Secret） | 用 `.gitignore` 已忽略，确认 `git status` 不出现 `.env` | ⚠️ 当前项目根目录外的 `.env` 不要复制进仓库 |
| 2 | 🚫 **不要提交真实使用的 application-dev.yml / application-prod.yml**（它们带真实密码） | 已在 `.gitignore` 忽略；只提交 `.template` 和基础 application.yml（占位符） | ✅ 模板文件已加入白名单 |
| 3 | 🔍 **全局扫描敏感关键字**（在 Git bash 或 PowerShell 执行） | 仓库根目录执行：<br>`grep -r --include="*.yml" --include="*.yaml" --include="*.properties" -E "password:|secret:|requirepass|Authorization: Bearer|<YOUR_" . 2>/dev/null` | 命中后把真实值换成占位符或环境变量 |
| 4 | 🚫 **不要提交 `target/`、`node_modules/`、`dist/`** | `.gitignore` 已忽略，首次 commit 前执行 `git clean -ndX` 预览要忽略的文件 | 不要用 `git add -f` 强行加 |
| 5 | 🚫 **不要提交运维用的 Python SSH 脚本**（含服务器 IP/密码） | 这些脚本放在项目外的 `scripts/` 目录（当前项目工作区的 `scripts/` 在 Shiyi-Website 仓库外），不要复制进仓库 | 若要公开请全部把 IP/密码改成 `xxx` 占位符 |
| 6 | ⚠️ 检查 `pom.xml` / `package.json` 里的名字是否是你想要的 | 当前 `artifactId=ShiyiBlogBackend`、`groupId=cc.shiyi`；前端 `name=feitwnd-*` 或 `shiyi-*` 可在对应 package.json 改 | 不强制但建议统一 |
| 7 | ✅ 最后检查 Git 暂存区清单 | `git status` / `git diff --cached --name-only` 确认没有上面6类文件 | 没问题再 commit |

---

## 🤝 贡献

欢迎提 Issue 和 Pull Request！提交 PR 前请：
- 前端：`pnpm lint` 确保 0 错误
- 后端：`mvn clean package -DskipTests` 确保编译通过

## 📄 License

[MIT License](LICENSE)

---

⭐ 如果这个项目帮到你，欢迎 Star 支持一下！
