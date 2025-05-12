# Java代码解析与索引系统

一个高性能Java代码解析与索引系统，支持代码搜索、语义分析和质量评估。

## 项目概述

本项目包含两个主要部分：

1. **Backend** - Java后端服务，提供代码解析、索引和API服务
2. **Frontend** - React前端应用，提供用户界面和交互体验

## 主要功能

- **代码解析**：解析Java源代码，提取代码结构和关系
- **代码搜索**：支持全文搜索、语义搜索和关系搜索
- **语义分析**：提供调用图分析、数据流分析、相似代码检测等功能
- **代码质量**：分析代码质量并提供评分和问题列表

## 项目结构

```
java-code-analyzer/
├── backend/                # Java后端项目
│   ├── src/
│   │   ├── main/java/      # Java源代码
│   │   └── test/java/      # 测试代码
│   └── pom.xml             # Maven配置
└── frontend/               # React前端项目
    ├── public/             # 静态资源
    ├── src/                # React源代码
    └── package.json        # NPM配置
```

## 技术栈

**后端**：
- Java 8+
- Spring Boot 2.5+
- Apache Lucene 8.11+
- JavaParser 3.20+

**前端**：
- React 18
- Tailwind CSS
- Vite

## 开发环境设置

### 后端设置

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

### 前端设置

```bash
cd frontend
npm install
npm run dev
```

## 部署

### 部署选项1：独立部署

1. **后端部署**：
```bash
cd backend
mvn clean package
java -jar target/java-code-analyzer-1.0.0.jar
```

2. **前端部署**：
```bash
cd frontend
npm run build
# 将dist目录内容部署到Web服务器
```

### 部署选项2：整合部署

如果希望通过后端提供前端静态资源：

1. 构建前端：
```bash
cd frontend
npm run build
```

2. 将前端构建结果复制到后端静态资源目录：
```bash
mkdir -p backend/src/main/resources/static
cp -r frontend/dist/* backend/src/main/resources/static/
```

3. 构建并运行后端：
```bash
cd backend
mvn clean package
java -jar target/java-code-analyzer-1.0.0.jar
```

## API文档

启动后端服务后，可通过以下地址访问API文档：
http://localhost:8080/swagger-ui

## 贡献

欢迎贡献代码！请先Fork项目，然后创建Pull Request提交变更。

## 许可证

[MIT License](LICENSE)