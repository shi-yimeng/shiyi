# 构建阶段
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# 复制pom文件并下载依赖
COPY Backend/pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并构建
COPY Backend/ .
RUN mvn clean package -DskipTests -B

# 运行阶段
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="shiyi"

# 创建非root用户
RUN addgroup -g 1000 appgroup && \
    adduser -u 1000 -G appgroup -s /bin/sh -D appuser

WORKDIR /app

# 复制构建好的JAR包
COPY --from=builder /build/Shiyi-server/target/Shiyi-server-1.0-SNAPSHOT.jar ./shiyi.jar

# 创建日志目录
RUN mkdir -p /app/logs && \
    chown -R appuser:appgroup /app

# 切换到非root用户
USER appuser

# JVM参数：4GB内存机器推荐（G1GC + 各区域硬上限，避免非堆内存暴涨OOM）
ENV JAVA_OPTS="-XX:+UseG1GC \
  -Xms768m -Xmx768m \
  -XX:MaxMetaspaceSize=256m \
  -XX:MaxDirectMemorySize=128m \
  -XX:ReservedCodeCacheSize=64m \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/app/logs \
  -XX:+ExitOnOutOfMemoryError"

# 暴露端口
EXPOSE 5922

# 启动命令
# spring.profiles.active 由 docker-compose 环境变量 SPRING_PROFILES_ACTIVE 传入
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar shiyi.jar"]
