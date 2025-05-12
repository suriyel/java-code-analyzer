package com.codeanalyzer.index;

import com.codeanalyzer.ast.CodeEntity;
import com.codeanalyzer.ast.IntermediateRepresentation;
import com.codeanalyzer.ast.ParsedProjectStructure;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 索引管理模块 - 实现多级索引管理
 * 特点:
 * 1. 多级索引结构，支持文件级、类级、方法级、代码片段级检索
 * 2. 支持关系检索（实现、继承、引用）
 * 3. 支持注释语义检索
 * 4. 内置查询优化，支持多种查询模式
 */
public class IndexManager implements AutoCloseable {
    // 索引目录
    private final Path indexPath;
    // Lucene索引目录
    private final Directory directory;
    // 索引分析器
    private final Analyzer analyzer;
    // 索引写入器
    private IndexWriter indexWriter;
    // 索引读取器（延迟初始化）
    private IndexReader indexReader;
    // 索引搜索器（延迟初始化）
    private IndexSearcher indexSearcher;
    // 缓存常用的索引读取器和搜索器
    private final Map<String, Object> searchCache = new ConcurrentHashMap<>();

    // 索引字段定义
    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_PATH = "path";
    public static final String FIELD_PACKAGE = "package";
    public static final String FIELD_CLASS = "class";
    public static final String FIELD_METHOD = "method";
    public static final String FIELD_CONTENT = "content";
    public static final String FIELD_JAVADOC = "javadoc";
    public static final String FIELD_MODIFIERS = "modifiers";
    public static final String FIELD_RELATIONS = "relations";
    public static final String FIELD_PARAMS = "params";
    public static final String FIELD_RETURN = "returnType";
    public static final String FIELD_FIELD_TYPE = "fieldType";
    public static final String FIELD_SNIPPET = "snippet";

    /**
     * 初始化索引管理器
     * @param indexPath 索引存储路径
     * @throws IOException 如果索引目录无法创建
     */
    public IndexManager(String indexPath) throws IOException {
        this.indexPath = Paths.get(indexPath);
        this.directory = FSDirectory.open(this.indexPath);
        this.analyzer = new StandardAnalyzer();

        // 配置索引写入器
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        config.setRAMBufferSizeMB(256.0);
        config.setUseCompoundFile(true);
        config.setCommitOnClose(true);

        this.indexWriter = new IndexWriter(directory, config);
    }

    /**
     * 构建索引
     * @param projectStructure 解析后的项目结构
     * @throws IOException 索引构建异常
     */
    public void buildIndex(ParsedProjectStructure projectStructure) throws IOException {
        // 清空现有索引
        indexWriter.deleteAll();

        // 文件级索引（按文件路径分组）
        Map<String, List<IntermediateRepresentation>> fileGroups = new HashMap<>();

        // 获取所有中间表示
        Map<String, IntermediateRepresentation> irMap = projectStructure.getIrMap();

        // 按文件路径分组
        for (IntermediateRepresentation ir : irMap.values()) {
            String filePath = extractFilePath(ir.getPath());
            fileGroups.computeIfAbsent(filePath, k -> new ArrayList<>()).add(ir);
        }

        // 构建文件级索引
        for (Map.Entry<String, List<IntermediateRepresentation>> entry : fileGroups.entrySet()) {
            String filePath = entry.getKey();
            List<IntermediateRepresentation> entities = entry.getValue();

            // 创建文件级文档
            Document fileDoc = new Document();
            fileDoc.add(new StringField(FIELD_ID, "file:" + filePath, Field.Store.YES));
            fileDoc.add(new StringField(FIELD_TYPE, "file", Field.Store.YES));
            fileDoc.add(new StringField(FIELD_PATH, filePath, Field.Store.YES));

            // 文件内容全文索引
            StringBuilder fileContent = new StringBuilder();
            for (IntermediateRepresentation ir : entities) {
                fileContent.append(ir.getText()).append(" ");
            }
            fileDoc.add(new TextField(FIELD_CONTENT, fileContent.toString(), Field.Store.NO));

            // 添加到索引
            indexWriter.addDocument(fileDoc);
        }

        // 构建类/接口/枚举级索引
        List<IntermediateRepresentation> typeIRs = irMap.values().stream()
                .filter(ir -> ir.getType().equals("CLASS") ||
                        ir.getType().equals("INTERFACE") ||
                        ir.getType().equals("ENUM"))
                .collect(Collectors.toList());

        for (IntermediateRepresentation ir : typeIRs) {
            Document typeDoc = createDocument(ir);
            indexWriter.addDocument(typeDoc);
        }

        // 构建方法级索引
        List<IntermediateRepresentation> methodIRs = irMap.values().stream()
                .filter(ir -> ir.getType().equals("METHOD"))
                .collect(Collectors.toList());

        for (IntermediateRepresentation ir : methodIRs) {
            Document methodDoc = createDocument(ir);
            indexWriter.addDocument(methodDoc);
        }

        // 构建字段级索引
        List<IntermediateRepresentation> fieldIRs = irMap.values().stream()
                .filter(ir -> ir.getType().equals("FIELD"))
                .collect(Collectors.toList());

        for (IntermediateRepresentation ir : fieldIRs) {
            Document fieldDoc = createDocument(ir);
            indexWriter.addDocument(fieldDoc);
        }

        // 代码片段级索引 - 分割方法体为代码片段
        int snippetId = 0;
        for (IntermediateRepresentation ir : methodIRs) {
            String methodText = ir.getText();
            // 分割为代码片段（此处简化处理，实际应按语法结构分割）
            List<String> snippets = splitToSnippets(methodText);

            for (int i = 0; i < snippets.size(); i++) {
                String snippet = snippets.get(i);
                if (snippet.trim().isEmpty()) continue;

                Document snippetDoc = new Document();
                snippetDoc.add(new StringField(FIELD_ID, "snippet:" + (snippetId++), Field.Store.YES));
                snippetDoc.add(new StringField(FIELD_TYPE, "snippet", Field.Store.YES));
                snippetDoc.add(new StringField(FIELD_METHOD, ir.getId(), Field.Store.YES));
                snippetDoc.add(new StringField(FIELD_CLASS, (String)ir.getAttribute("className"), Field.Store.YES));
                snippetDoc.add(new TextField(FIELD_SNIPPET, snippet, Field.Store.YES));

                indexWriter.addDocument(snippetDoc);
            }
        }

        // 提交索引
        indexWriter.commit();
    }

    /**
     * 从IR创建Lucene文档
     */
    private Document createDocument(IntermediateRepresentation ir) {
        Document doc = new Document();

        // 基本字段
        doc.add(new StringField(FIELD_ID, ir.getId(), Field.Store.YES));
        doc.add(new StringField(FIELD_NAME, ir.getName(), Field.Store.YES));
        doc.add(new StringField(FIELD_TYPE, ir.getType(), Field.Store.YES));
        doc.add(new StringField(FIELD_PATH, ir.getPath(), Field.Store.YES));

        // 全文内容
        doc.add(new TextField(FIELD_CONTENT, ir.getText(), Field.Store.NO));

        // 添加JavaDoc（用于语义检索）
        String javadoc = (String)ir.getAttribute("javadoc");
        if (javadoc != null && !javadoc.isEmpty()) {
            doc.add(new TextField(FIELD_JAVADOC, javadoc, Field.Store.YES));
        }

        // 添加修饰符
        Object modifiersObj = ir.getAttribute("modifiers");
        if (modifiersObj != null && modifiersObj instanceof String[]) {
            String[] modifiers = (String[])modifiersObj;
            for (String modifier : modifiers) {
                doc.add(new StringField(FIELD_MODIFIERS, modifier, Field.Store.YES));
            }
        }

        // 添加关系
        Map<String, Set<String>> relationships = ir.getRelationships();
        if (relationships != null && !relationships.isEmpty()) {
            for (Map.Entry<String, Set<String>> entry : relationships.entrySet()) {
                String relationType = entry.getKey();
                for (String target : entry.getValue()) {
                    // 存储关系类型:目标格式
                    doc.add(new StringField(FIELD_RELATIONS, relationType + ":" + target, Field.Store.YES));
                }
            }
        }

        // 根据类型添加特定字段
        switch (ir.getType()) {
            case "CLASS":
            case "INTERFACE":
            case "ENUM":
                String packageName = (String)ir.getAttribute("package");
                if (packageName != null) {
                    doc.add(new StringField(FIELD_PACKAGE, packageName, Field.Store.YES));
                }
                break;

            case "METHOD":
                String className = (String)ir.getAttribute("className");
                if (className != null) {
                    doc.add(new StringField(FIELD_CLASS, className, Field.Store.YES));
                }

                String returnType = (String)ir.getAttribute("returnType");
                if (returnType != null) {
                    doc.add(new StringField(FIELD_RETURN, returnType, Field.Store.YES));
                }

                // 参数
                Object paramsObj = ir.getAttribute("parameters");
                if (paramsObj != null && paramsObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> params = (Map<String, String>)paramsObj;
                    for (Map.Entry<String, String> param : params.entrySet()) {
                        // 参数格式: 名称:类型
                        doc.add(new StringField(FIELD_PARAMS, param.getKey() + ":" + param.getValue(), Field.Store.YES));
                    }
                }
                break;

            case "FIELD":
                String fieldClassName = (String)ir.getAttribute("className");
                if (fieldClassName != null) {
                    doc.add(new StringField(FIELD_CLASS, fieldClassName, Field.Store.YES));
                }

                String fieldType = (String)ir.getAttribute("fieldType");
                if (fieldType != null) {
                    doc.add(new StringField(FIELD_FIELD_TYPE, fieldType, Field.Store.YES));
                }
                break;
        }

        return doc;
    }

    /**
     * 从路径提取文件路径
     */
    private String extractFilePath(String path) {
        // 从实体路径中提取文件路径
        // 例如: com/example/MyClass -> com/example/MyClass.java
        return path.contains("/") ? path.substring(0, path.lastIndexOf('/')) + ".java" : path + ".java";
    }

    /**
     * 简单分割代码片段（仅用于示例）
     * 实际实现应使用AST分析的语法结构
     */
    private List<String> splitToSnippets(String methodText) {
        // 简单按行分组，每3行一个片段
        String[] lines = methodText.split("\n");
        List<String> snippets = new ArrayList<>();

        StringBuilder currentSnippet = new StringBuilder();
        int lineCount = 0;

        for (String line : lines) {
            currentSnippet.append(line).append("\n");
            lineCount++;

            if (lineCount >= 3) {
                snippets.add(currentSnippet.toString());
                currentSnippet = new StringBuilder();
                lineCount = 0;
            }
        }

        if (lineCount > 0) {
            snippets.add(currentSnippet.toString());
        }

        return snippets;
    }

    /**
     * 初始化/刷新搜索相关对象
     */
    private synchronized void refreshSearcher() throws IOException {
        if (indexReader != null) {
            indexReader.close();
        }

        // 重新打开索引读取器和搜索器
        indexReader = DirectoryReader.open(directory);
        indexSearcher = new IndexSearcher(indexReader);

        // 清空缓存
        searchCache.clear();
    }

    /**
     * 执行多级索引搜索
     * @param queryStr 查询字符串
     * @param level 索引级别：file, class, method, field, snippet
     * @param maxResults 最大结果数
     * @return 搜索结果列表
     */
    public List<SearchResult> search(String queryStr, IndexLevel level, int maxResults) throws Exception {
        if (indexSearcher == null) {
            refreshSearcher();
        }

        // 根据级别确定搜索字段
        String[] searchFields;
        String typeFilter = null;

        switch (level) {
            case FILE:
                searchFields = new String[]{FIELD_CONTENT, FIELD_PATH};
                typeFilter = "file";
                break;
            case CLASS:
                searchFields = new String[]{FIELD_NAME, FIELD_CONTENT, FIELD_JAVADOC};
                typeFilter = "CLASS";
                break;
            case INTERFACE:
                searchFields = new String[]{FIELD_NAME, FIELD_CONTENT, FIELD_JAVADOC};
                typeFilter = "INTERFACE";
                break;
            case METHOD:
                searchFields = new String[]{FIELD_NAME, FIELD_CONTENT, FIELD_JAVADOC, FIELD_PARAMS, FIELD_RETURN};
                typeFilter = "METHOD";
                break;
            case FIELD:
                searchFields = new String[]{FIELD_NAME, FIELD_CONTENT, FIELD_JAVADOC, FIELD_FIELD_TYPE};
                typeFilter = "FIELD";
                break;
            case SNIPPET:
                searchFields = new String[]{FIELD_SNIPPET};
                typeFilter = "snippet";
                break;
            case ALL:
            default:
                searchFields = new String[]{FIELD_NAME, FIELD_CONTENT, FIELD_JAVADOC};
                typeFilter = null;
                break;
        }

        // 构建查询
        MultiFieldQueryParser parser = new MultiFieldQueryParser(searchFields, analyzer);
        Query query = parser.parse(queryStr);

        // 如果需要按类型过滤
        if (typeFilter != null) {
            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
            booleanQuery.add(query, BooleanClause.Occur.MUST);
            booleanQuery.add(new TermQuery(new Term(FIELD_TYPE, typeFilter)), BooleanClause.Occur.MUST);
            query = booleanQuery.build();
        }

        // 执行搜索
        TopDocs topDocs = indexSearcher.search(query, maxResults);

        // 转换结果
        List<SearchResult> results = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = indexSearcher.doc(scoreDoc.doc);

            SearchResult result = new SearchResult();
            result.setId(doc.get(FIELD_ID));
            result.setName(doc.get(FIELD_NAME));
            result.setType(doc.get(FIELD_TYPE));
            result.setPath(doc.get(FIELD_PATH));
            result.setScore(scoreDoc.score);

            // 根据文档类型添加特定字段
            switch (doc.get(FIELD_TYPE)) {
                case "CLASS":
                case "INTERFACE":
                case "ENUM":
                    result.addAttribute("package", doc.get(FIELD_PACKAGE));
                    break;

                case "METHOD":
                    result.addAttribute("class", doc.get(FIELD_CLASS));
                    result.addAttribute("returnType", doc.get(FIELD_RETURN));

                    // 获取所有参数
                    String[] params = doc.getValues(FIELD_PARAMS);
                    if (params != null && params.length > 0) {
                        Map<String, String> paramMap = new LinkedHashMap<>();
                        for (String param : params) {
                            String[] parts = param.split(":");
                            if (parts.length == 2) {
                                paramMap.put(parts[0], parts[1]);
                            }
                        }
                        result.addAttribute("parameters", paramMap);
                    }
                    break;

                case "FIELD":
                    result.addAttribute("class", doc.get(FIELD_CLASS));
                    result.addAttribute("fieldType", doc.get(FIELD_FIELD_TYPE));
                    break;

                case "snippet":
                    result.addAttribute("snippet", doc.get(FIELD_SNIPPET));
                    result.addAttribute("method", doc.get(FIELD_METHOD));
                    result.addAttribute("class", doc.get(FIELD_CLASS));
                    break;
            }

            // 添加JavaDoc
            String javadoc = doc.get(FIELD_JAVADOC);
            if (javadoc != null) {
                result.addAttribute("javadoc", javadoc);
            }

            // 添加修饰符
            String[] modifiers = doc.getValues(FIELD_MODIFIERS);
            if (modifiers != null && modifiers.length > 0) {
                result.addAttribute("modifiers", Arrays.asList(modifiers));
            }

            // 添加关系
            String[] relations = doc.getValues(FIELD_RELATIONS);
            if (relations != null && relations.length > 0) {
                Map<String, Set<String>> relationMap = new HashMap<>();
                for (String relation : relations) {
                    String[] parts = relation.split(":");
                    if (parts.length == 2) {
                        relationMap.computeIfAbsent(parts[0], k -> new HashSet<>()).add(parts[1]);
                    }
                }
                result.addAttribute("relationships", relationMap);
            }

            results.add(result);
        }

        return results;
    }

    /**
     * 按关系进行搜索（例如查找所有实现特定接口的类）
     * @param relationType 关系类型
     * @param target 目标名称
     * @param maxResults 最大结果数
     * @return 搜索结果列表
     */
    public List<SearchResult> searchByRelation(String relationType, String target, int maxResults) throws Exception {
        if (indexSearcher == null) {
            refreshSearcher();
        }

        // 构建关系查询
        String relationValue = relationType + ":" + target;
        Query query = new TermQuery(new Term(FIELD_RELATIONS, relationValue));

        // 执行搜索
        TopDocs topDocs = indexSearcher.search(query, maxResults);

        // 转换结果
        List<SearchResult> results = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = indexSearcher.doc(scoreDoc.doc);

            SearchResult result = new SearchResult();
            result.setId(doc.get(FIELD_ID));
            result.setName(doc.get(FIELD_NAME));
            result.setType(doc.get(FIELD_TYPE));
            result.setPath(doc.get(FIELD_PATH));
            result.setScore(scoreDoc.score);

            // 添加关系
            String[] relations = doc.getValues(FIELD_RELATIONS);
            if (relations != null && relations.length > 0) {
                Map<String, Set<String>> relationMap = new HashMap<>();
                for (String relation : relations) {
                    String[] parts = relation.split(":");
                    if (parts.length == 2) {
                        relationMap.computeIfAbsent(parts[0], k -> new HashSet<>()).add(parts[1]);
                    }
                }
                result.addAttribute("relationships", relationMap);
            }

            results.add(result);
        }

        return results;
    }

    /**
     * 进行语义检索，主要基于JavaDoc和注释
     * @param semanticQuery 语义查询
     * @param maxResults 最大结果数
     * @return 搜索结果列表
     */
    public List<SearchResult> semanticSearch(String semanticQuery, int maxResults) throws Exception {
        if (indexSearcher == null) {
            refreshSearcher();
        }

        // 构建语义查询（主要针对JavaDoc）
        QueryParser parser = new QueryParser(FIELD_JAVADOC, analyzer);
        Query query = parser.parse(semanticQuery);

        // 执行搜索
        TopDocs topDocs = indexSearcher.search(query, maxResults);

        // 转换结果
        List<SearchResult> results = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = indexSearcher.doc(scoreDoc.doc);

            SearchResult result = new SearchResult();
            result.setId(doc.get(FIELD_ID));
            result.setName(doc.get(FIELD_NAME));
            result.setType(doc.get(FIELD_TYPE));
            result.setPath(doc.get(FIELD_PATH));
            result.setScore(scoreDoc.score);

            // 添加JavaDoc
            String javadoc = doc.get(FIELD_JAVADOC);
            if (javadoc != null) {
                result.addAttribute("javadoc", javadoc);
            }

            results.add(result);
        }

        return results;
    }

    /**
     * 进行组合查询
     * @param queryBuilder 查询构建器
     * @param maxResults 最大结果数
     * @return 搜索结果列表
     */
    public List<SearchResult> advancedSearch(QueryBuilder queryBuilder, int maxResults) throws Exception {
        if (indexSearcher == null) {
            refreshSearcher();
        }

        // 执行搜索
        TopDocs topDocs = indexSearcher.search(queryBuilder.build(), maxResults);

        // 转换结果
        List<SearchResult> results = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = indexSearcher.doc(scoreDoc.doc);

            SearchResult result = new SearchResult();
            result.setId(doc.get(FIELD_ID));
            result.setName(doc.get(FIELD_NAME));
            result.setType(doc.get(FIELD_TYPE));
            result.setPath(doc.get(FIELD_PATH));
            result.setScore(scoreDoc.score);

            // 根据类型添加其他字段
            addTypeSpecificFields(result, doc);

            results.add(result);
        }

        return results;
    }

    /**
     * 根据文档类型添加特定字段
     */
    private void addTypeSpecificFields(SearchResult result, Document doc) {
        switch (doc.get(FIELD_TYPE)) {
            case "CLASS":
            case "INTERFACE":
            case "ENUM":
                result.addAttribute("package", doc.get(FIELD_PACKAGE));
                break;

            case "METHOD":
                result.addAttribute("class", doc.get(FIELD_CLASS));
                result.addAttribute("returnType", doc.get(FIELD_RETURN));

                // 获取所有参数
                String[] params = doc.getValues(FIELD_PARAMS);
                if (params != null && params.length > 0) {
                    Map<String, String> paramMap = new LinkedHashMap<>();
                    for (String param : params) {
                        String[] parts = param.split(":");
                        if (parts.length == 2) {
                            paramMap.put(parts[0], parts[1]);
                        }
                    }
                    result.addAttribute("parameters", paramMap);
                }
                break;

            case "FIELD":
                result.addAttribute("class", doc.get(FIELD_CLASS));
                result.addAttribute("fieldType", doc.get(FIELD_FIELD_TYPE));
                break;

            case "snippet":
                result.addAttribute("snippet", doc.get(FIELD_SNIPPET));
                result.addAttribute("method", doc.get(FIELD_METHOD));
                result.addAttribute("class", doc.get(FIELD_CLASS));
                break;
        }

        // 添加JavaDoc
        String javadoc = doc.get(FIELD_JAVADOC);
        if (javadoc != null) {
            result.addAttribute("javadoc", javadoc);
        }

        // 添加修饰符
        String[] modifiers = doc.getValues(FIELD_MODIFIERS);
        if (modifiers != null && modifiers.length > 0) {
            result.addAttribute("modifiers", Arrays.asList(modifiers));
        }

        // 添加关系
        String[] relations = doc.getValues(FIELD_RELATIONS);
        if (relations != null && relations.length > 0) {
            Map<String, Set<String>> relationMap = new HashMap<>();
            for (String relation : relations) {
                String[] parts = relation.split(":");
                if (parts.length == 2) {
                    relationMap.computeIfAbsent(parts[0], k -> new HashSet<>()).add(parts[1]);
                }
            }
            result.addAttribute("relationships", relationMap);
        }
    }

    /**
     * 关闭索引管理器，释放资源
     */
    @Override
    public void close() throws IOException {
        if (indexReader != null) {
            indexReader.close();
        }

        if (indexWriter != null) {
            indexWriter.close();
        }

        directory.close();
    }

    /**
     * 提交索引变更
     */
    public void commit() throws IOException {
        indexWriter.commit();
    }

    /**
     * 查询构建器 - 用于构建高级查询
     */
    public static class QueryBuilder {
        private final BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
        private final Analyzer analyzer;

        public QueryBuilder(Analyzer analyzer) {
            this.analyzer = analyzer;
        }

        /**
         * 添加与条件
         */
        public QueryBuilder and(String field, String value) throws Exception {
            QueryParser parser = new QueryParser(field, analyzer);
            queryBuilder.add(parser.parse(value), BooleanClause.Occur.MUST);
            return this;
        }

        /**
         * 添加或条件
         */
        public QueryBuilder or(String field, String value) throws Exception {
            QueryParser parser = new QueryParser(field, analyzer);
            queryBuilder.add(parser.parse(value), BooleanClause.Occur.SHOULD);
            return this;
        }

        /**
         * 添加非条件
         */
        public QueryBuilder not(String field, String value) throws Exception {
            QueryParser parser = new QueryParser(field, analyzer);
            queryBuilder.add(parser.parse(value), BooleanClause.Occur.MUST_NOT);
            return this;
        }

        /**
         * 添加精确匹配条件
         */
        public QueryBuilder term(String field, String value) {
            queryBuilder.add(new TermQuery(new Term(field, value)), BooleanClause.Occur.MUST);
            return this;
        }

        /**
         * 添加前缀匹配条件
         */
        public QueryBuilder prefix(String field, String prefix) {
            queryBuilder.add(new PrefixQuery(new Term(field, prefix)), BooleanClause.Occur.MUST);
            return this;
        }

        /**
         * 添加范围查询条件
         */
        public QueryBuilder range(String field, String start, String end) {
            queryBuilder.add(new TermRangeQuery(field, new BytesRef(start), new BytesRef(end), true, true),
                    BooleanClause.Occur.MUST);
            return this;
        }

        /**
         * 添加通配符查询
         */
        public QueryBuilder wildcard(String field, String pattern) {
            queryBuilder.add(new WildcardQuery(new Term(field, pattern)), BooleanClause.Occur.MUST);
            return this;
        }

        /**
         * 添加类型过滤
         */
        public QueryBuilder ofType(String type) {
            queryBuilder.add(new TermQuery(new Term(FIELD_TYPE, type)), BooleanClause.Occur.MUST);
            return this;
        }

        /**
         * 添加关系查询
         */
        public QueryBuilder hasRelation(String relationType, String target) {
            String relationValue = relationType + ":" + target;
            queryBuilder.add(new TermQuery(new Term(FIELD_RELATIONS, relationValue)), BooleanClause.Occur.MUST);
            return this;
        }

        /**
         * 构建最终查询
         */
        public Query build() {
            return queryBuilder.build();
        }
    }
}