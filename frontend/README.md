# Java代码解析与索引系统 - 前端

这是Java代码解析与索引系统的前端部分，基于React和Tailwind CSS构建。

## 功能特点

- 项目上传：支持上传ZIP格式的Java项目
- 代码搜索：提供全文搜索、语义搜索和关系搜索功能
- 语义分析：提供调用图分析、数据流分析、相似代码检测等功能
- 代码质量：分析代码质量并提供评分和问题列表

## 开发环境设置

### 前提条件

- Node.js v16+
- npm v8+ 或 yarn v1.22+

### 安装依赖

```bash
# 使用npm
npm install

# 或使用yarn
yarn
```

### 开发服务器

```bash
# 使用npm
npm run dev

# 或使用yarn
yarn dev
```

开发服务器将在 http://localhost:3000 启动。

### 生产构建

```bash
# 使用npm
npm run build

# 或使用yarn
yarn build
```

构建产物将输出到 `dist` 目录。

## 项目结构

```
src/
├── components/     # React组件
│   ├── CodeQualityPanel.jsx
│   ├── CodeSearchPanel.jsx
│   ├── ProjectUploader.jsx
│   └── SemanticAnalysisPanel.jsx
├── App.jsx         # 主应用组件
├── index.jsx       # 入口文件
└── styles.css      # 样式文件
```

## 与后端通信

前端通过REST API与后端通信，API基础路径为：`/api/v1/`。

在开发环境中，API请求通过Vite的代理功能转发到后端服务（默认为http://localhost:8080）。

## 部署

1. 构建前端项目
```bash
npm run build
```

2. 将`dist`目录中的文件部署到Web服务器，或者配合后端一起部署

### 与后端一起部署

您可以将前端构建结果复制到后端项目的静态资源目录，例如：

```bash
cp -r dist/* ../backend/src/main/resources/static/
```

然后通过后端服务器提供前端静态资源。