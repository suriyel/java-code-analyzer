# 后端 Dockerfile
FROM openjdk:8-jdk-alpine

# 工作目录
WORKDIR /app

# 复制Maven构建文件
COPY target/java-code-analyzer-1.0.0.jar app.jar

# 创建必要的目录
RUN mkdir -p /app/projects /app/indexes /app/logs

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java","-jar","/app/app.jar"]