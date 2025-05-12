# Java代码解析与索引系统 - 后端

这是Java代码解析与索引系统的后端部分，基于Java 8和Spring Boot构建。

## 功能特点

- AST解析：解析Java源代码，提取代码结构和关系
- 代码索引：构建多级索引，支持高效检索
- 语义分析：分析代码语义，支持调用图、数据流等分析
- 代码质量：评估代码质量，识别潜在问题
- REST API：提供完整的REST API，支持前端交互

## 技术栈

- Java 8+
- Spring Boot 2.5+
- Apache Lucene 8.11+
- JavaParser 3.20+
- Maven 3.6+

## 开发环境设置

### 前提条件

- JDK 8+
- Maven 3.6+

### 编译和运行

```bash
# 编译项目
mvn clean compile

# 运行应用
mvn spring-boot:run
```

服务器将在 http://localhost:8080 启动。

### 构建可执行JAR

```bash
mvn clean package
```

构建产物将输出到 `target` 目录。

### 运行JAR包

```bash
java -jar target/java-code-analyzer-1.0.0.jar
```

## 项目结构

```
src/main/java/com/codeanalyzer/
├── api/           # REST API相关类
├── ast/           # AST解析相关类
├── config/        # 配置相关类
├── index/         # 索引相关类
└── semantic/      # 语义分析相关类
```

## API文档

启动应用后，可通过以下URL访问Swagger API文档：

http://localhost:8080/swagger-ui

## 配置

主要配置位于 `src/main/resources/application.properties` 文件中，可配置项包括：

- 服务器端口
- 上传文件大小限制
- 索引目录
- 线程数
- 缓存设置
- 日志级别

## 部署

### 作为独立服务运行

```bash
java -jar target/java-code-analyzer-1.0.0.jar
```

### 配置为系统服务

可将应用配置为systemd服务：

1. 创建服务文件：

```bash
sudo nano /etc/systemd/system/java-code-analyzer.service
```

2. 添加以下内容：

```
[Unit]
Description=Java Code Analyzer Service
After=network.target

[Service]
User=your-user
WorkingDirectory=/path/to/application
ExecStart=/usr/bin/java -jar /path/to/application/java-code-analyzer-1.0.0.jar
SuccessExitStatus=143
TimeoutStopSec=10
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

3. 启用并启动服务：

```bash
sudo systemctl enable java-code-analyzer
sudo systemctl start java-code-analyzer
```