package com.codeanalyzer.semantic;

import com.codeanalyzer.ast.CodeEntity;
import com.codeanalyzer.ast.IntermediateRepresentation;
import com.codeanalyzer.ast.ParsedProjectStructure;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 高级语义分析模块 - 基于代码的语义理解和分析
 * 特点:
 * 1. 方法调用图分析 - 跟踪方法间调用关系
 * 2. 数据流分析 - 跟踪数据在方法间的流动
 * 3. 代码相似度分析 - 检测相似或重复代码
 * 4. 关键字抽取 - 从代码和注释中提取关键概念
 * 5. 代码质量评估 - 检测潜在问题和优化机会
 */
public class SemanticAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(SemanticAnalyzer.class);

    // 路径常量
    private static final String CALL_GRAPH_INDEX = "call_graph";
    private static final String DATA_FLOW_INDEX = "data_flow";
    private static final String CODE_SIMILARITY_INDEX = "code_similarity";
    private static final String CONCEPT_INDEX = "concept";

    // 索引基本路径
    private final Path indexBasePath;

    // 解析后的项目结构
    private ParsedProjectStructure projectStructure;

    // 调用图
    private final CallGraph callGraph = new CallGraph();

    // 数据流分析器
    private final DataFlowAnalyzer dataFlowAnalyzer = new DataFlowAnalyzer();

    // 代码相似度分析器
    private final CodeSimilarityAnalyzer similarityAnalyzer = new CodeSimilarityAnalyzer();

    // 概念提取器
    private final ConceptExtractor conceptExtractor = new ConceptExtractor();

    // 代码质量分析器
    private final CodeQualityAnalyzer qualityAnalyzer = new CodeQualityAnalyzer();

    /**
     * 初始化语义分析器
     * @param indexPath 索引存储路径
     */
    public SemanticAnalyzer(String indexPath) {
        this.indexBasePath = Paths.get(indexPath);
    }

    /**
     * 分析项目语义
     * @param projectStructure 解析后的项目结构
     */
    public void analyzeProject(ParsedProjectStructure projectStructure) throws IOException {
        this.projectStructure = projectStructure;

        // 1. 构建方法调用图
        buildCallGraph();

        // 2. 执行数据流分析
        analyzeDataFlow();

        // 3. 分析代码相似度
        analyzeCodeSimilarity();

        // 4. 提取概念关键字
        extractConcepts();

        // 5. 分析代码质量
        analyzeCodeQuality();

        logger.info("项目语义分析完成");
    }

    /**
     * 构建方法调用图
     */
    private void buildCallGraph() throws IOException {
        logger.info("开始构建方法调用图...");

        // 获取所有方法实体
        List<CodeEntity> methodEntities = projectStructure.getEntities().stream()
                .filter(entity -> entity.getType().toString().equals("METHOD"))
                .collect(Collectors.toList());

        // 为每个方法构建调用关系
        for (CodeEntity methodEntity : methodEntities) {
            String methodId = methodEntity.getParentName() + "#" + methodEntity.getName();
            Set<String> calledMethods = methodEntity.getMethodCalls();

            for (String calledMethod : calledMethods) {
                // 尝试查找被调用方法的全限定名
                String fullCalledMethodId = resolveMethodCall(methodEntity, calledMethod);
                // 简化处理，实际应从符号表解析
                callGraph.addCall(methodId, fullCalledMethodId);
            }
        }

        // 保存调用图索引
        saveCallGraphIndex();

        logger.info("方法调用图构建完成，共 {} 个方法节点和 {} 个调用边",
                callGraph.getNodes().size(), callGraph.getEdgeCount());
    }

    // 尝试解析方法调用的全限定名
    private String resolveMethodCall(CodeEntity caller, String calledMethodName) {
        // 1. 首先查找同一类中的方法
        String callerClass = caller.getParentName();

        // 遍历所有方法实体，寻找匹配的方法
        for (CodeEntity entity : projectStructure.getEntities()) {
            if (entity.getType().toString().equals("METHOD") &&
                    entity.getName().equals(calledMethodName)) {

                // 返回格式：ClassName#methodName
                return entity.getParentName() + "#" + calledMethodName;
            }
        }

        // 2. 如果找不到完整匹配，至少保留类名信息（如果存在）
        // 查找是否有引用的字段类型
        for (CodeEntity entity : projectStructure.getEntities()) {
            if (entity.getType().toString().equals("FIELD") &&
                    entity.getParentName().equals(callerClass)) {

                String fieldType = entity.getFieldType();
                // 如果找到匹配的字段类型，使用该类型作为前缀
                if (fieldType != null && !fieldType.isEmpty()) {
                    return fieldType + "#" + calledMethodName;
                }
            }
        }

        // 3. 如果无法解析，使用原始名称
        return calledMethodName;
    }

    /**
     * 执行数据流分析
     */
    private void analyzeDataFlow() throws IOException {
        logger.info("开始数据流分析...");

        // 获取所有方法实体
        List<CodeEntity> methodEntities = projectStructure.getEntities().stream()
                .filter(entity -> entity.getType().toString().equals("METHOD"))
                .collect(Collectors.toList());

        // 分析每个方法的数据流
        for (CodeEntity methodEntity : methodEntities) {
            String methodId = methodEntity.getParentName() + "#" + methodEntity.getName();

            // 获取方法参数
            Map<String, String> parameters = methodEntity.getParameters();

            // 创建数据流节点
            DataFlowNode node = new DataFlowNode(methodId);

            // 添加参数作为输入
            for (Map.Entry<String, String> param : parameters.entrySet()) {
                node.addInput(param.getKey(), param.getValue());
            }

            // 添加返回值作为输出
            if (methodEntity.getReturnType() != null && !methodEntity.getReturnType().equals("void")) {
                node.addOutput("return", methodEntity.getReturnType());
            }

            // 添加到数据流分析器
            dataFlowAnalyzer.addNode(node);
        }

        // 基于调用图连接数据流
        for (CallGraphNode caller : callGraph.getNodes().values()) {
            for (String callee : caller.getCallees()) {
                dataFlowAnalyzer.connectNodes(caller.getMethodId(), callee);
            }
        }

        // 保存数据流分析结果
        saveDataFlowIndex();

        logger.info("数据流分析完成，共分析 {} 个方法", dataFlowAnalyzer.getNodeCount());
    }

    /**
     * 分析代码相似度
     */
    private void analyzeCodeSimilarity() throws IOException {
        logger.info("开始代码相似度分析...");

        // 获取所有方法实体
        List<IntermediateRepresentation> methodIRs = projectStructure.getIrMap().values().stream()
                .filter(ir -> ir.getType().equals("METHOD"))
                .collect(Collectors.toList());

        // 为每个方法计算特征向量
        for (IntermediateRepresentation ir : methodIRs) {
            // 计算方法特征向量
            similarityAnalyzer.computeFeatureVector(ir.getId(), ir.getText());
        }

        // 计算方法间相似度
        similarityAnalyzer.computeSimilarities();

        // 检测可能的重复代码
        List<CodeSimilarityPair> duplicates = similarityAnalyzer.findPotentialDuplicates(0.8); // 80%相似度阈值

        // 保存相似度分析结果
        saveSimilarityIndex(duplicates);

        logger.info("代码相似度分析完成，发现 {} 对潜在重复代码", duplicates.size());
    }

    /**
     * 提取概念关键字
     */
    private void extractConcepts() throws IOException {
        logger.info("开始提取概念关键字...");

        // 从所有JavaDoc和注释中提取关键概念
        for (IntermediateRepresentation ir : projectStructure.getIrMap().values()) {
            String javadoc = (String)ir.getAttribute("javadoc");
            if (javadoc != null && !javadoc.isEmpty()) {
                conceptExtractor.processText(ir.getId(), javadoc, ConceptSource.JAVADOC);
            }

            // 从方法名、类名等标识符中提取概念
            conceptExtractor.processText(ir.getId(), ir.getName(), ConceptSource.IDENTIFIER);

            // 处理方法体或字段内容
            conceptExtractor.processText(ir.getId(), ir.getText(), ConceptSource.CODE);
        }

        // 聚合和排名关键概念
        Map<String, Set<ConceptOccurrence>> concepts = conceptExtractor.rankConcepts();

        // 保存概念索引
        saveConceptIndex(concepts);

        logger.info("概念提取完成，共提取 {} 个关键概念", concepts.size());
    }

    /**
     * 分析代码质量
     */
    private void analyzeCodeQuality() {
        logger.info("开始代码质量分析...");

        // 分析每个代码实体
        for (CodeEntity entity : projectStructure.getEntities()) {
            switch (entity.getType().toString()) {
                case "CLASS":
                    qualityAnalyzer.analyzeClass(entity);
                    break;
                case "METHOD":
                    qualityAnalyzer.analyzeMethod(entity);
                    break;
                case "FIELD":
                    qualityAnalyzer.analyzeField(entity);
                    break;
            }
        }

        // 获取潜在问题列表
        List<QualityIssue> issues = qualityAnalyzer.getIssues();

        logger.info("代码质量分析完成，发现 {} 个潜在问题", issues.size());
    }

    /**
     * 保存调用图索引
     */
    private void saveCallGraphIndex() throws IOException {
        Path indexPath = indexBasePath.resolve(CALL_GRAPH_INDEX);

        try (Directory directory = FSDirectory.open(indexPath)) {
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

            try (IndexWriter writer = new IndexWriter(directory, config)) {
                // 为每个调用关系创建索引文档
                for (CallGraphNode node : callGraph.getNodes().values()) {
                    for (String callee : node.getCallees()) {
                        Document doc = new Document();
                        doc.add(new StringField("caller", node.getMethodId(), Field.Store.YES));
                        doc.add(new StringField("callee", callee, Field.Store.YES));
                        doc.add(new TextField("callInfo", node.getMethodId() + " calls " + callee, Field.Store.NO));

                        writer.addDocument(doc);
                    }
                }

                writer.commit();
            }
        }
    }

    /**
     * 保存数据流索引
     */
    private void saveDataFlowIndex() throws IOException {
        Path indexPath = indexBasePath.resolve(DATA_FLOW_INDEX);

        try (Directory directory = FSDirectory.open(indexPath)) {
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

            try (IndexWriter writer = new IndexWriter(directory, config)) {
                // 为每个数据流节点创建索引文档
                for (DataFlowNode node : dataFlowAnalyzer.getAllNodes()) {
                    Document doc = new Document();
                    doc.add(new StringField("methodId", node.getMethodId(), Field.Store.YES));

                    // 添加输入参数
                    StringBuilder inputSb = new StringBuilder();
                    for (Map.Entry<String, String> input : node.getInputs().entrySet()) {
                        String fieldName = "input_" + input.getKey();
                        doc.add(new StringField(fieldName, input.getValue(), Field.Store.YES));
                        inputSb.append(input.getKey()).append(":").append(input.getValue()).append(" ");
                    }
                    doc.add(new TextField("inputs", inputSb.toString(), Field.Store.NO));

                    // 添加输出参数
                    StringBuilder outputSb = new StringBuilder();
                    for (Map.Entry<String, String> output : node.getOutputs().entrySet()) {
                        String fieldName = "output_" + output.getKey();
                        doc.add(new StringField(fieldName, output.getValue(), Field.Store.YES));
                        outputSb.append(output.getKey()).append(":").append(output.getValue()).append(" ");
                    }
                    doc.add(new TextField("outputs", outputSb.toString(), Field.Store.NO));

                    // 添加连接
                    for (String connection : node.getConnections()) {
                        doc.add(new StringField("connection", connection, Field.Store.YES));
                    }

                    writer.addDocument(doc);
                }

                writer.commit();
            }
        }
    }

    /**
     * 保存相似度索引
     */
    private void saveSimilarityIndex(List<CodeSimilarityPair> pairs) throws IOException {
        Path indexPath = indexBasePath.resolve(CODE_SIMILARITY_INDEX);

        try (Directory directory = FSDirectory.open(indexPath)) {
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

            try (IndexWriter writer = new IndexWriter(directory, config)) {
                // 为每个相似度对创建索引文档
                for (CodeSimilarityPair pair : pairs) {
                    Document doc = new Document();
                    doc.add(new StringField("method1", pair.getMethod1Id(), Field.Store.YES));
                    doc.add(new StringField("method2", pair.getMethod2Id(), Field.Store.YES));
                    doc.add(new StringField("similarity", String.valueOf(pair.getSimilarity()), Field.Store.YES));

                    // 可以用于文本搜索的字段
                    doc.add(new TextField("info",
                            "Similar methods: " + pair.getMethod1Id() + " and " + pair.getMethod2Id() +
                                    " with similarity " + pair.getSimilarity(),
                            Field.Store.NO));

                    writer.addDocument(doc);
                }

                writer.commit();
            }
        }
    }

    /**
     * 保存概念索引
     */
    private void saveConceptIndex(Map<String, Set<ConceptOccurrence>> concepts) throws IOException {
        Path indexPath = indexBasePath.resolve(CONCEPT_INDEX);

        try (Directory directory = FSDirectory.open(indexPath)) {
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

            try (IndexWriter writer = new IndexWriter(directory, config)) {
                // 为每个概念创建索引文档
                for (Map.Entry<String, Set<ConceptOccurrence>> entry : concepts.entrySet()) {
                    String concept = entry.getKey();
                    Set<ConceptOccurrence> occurrences = entry.getValue();

                    Document doc = new Document();
                    doc.add(new StringField("concept", concept, Field.Store.YES));
                    doc.add(new StringField("frequency", String.valueOf(occurrences.size()), Field.Store.YES));

                    // 添加出现位置
                    for (ConceptOccurrence occurrence : occurrences) {
                        doc.add(new StringField("entityId", occurrence.getEntityId(), Field.Store.YES));
                        doc.add(new StringField("source", occurrence.getSource().toString(), Field.Store.YES));
                    }

                    // 添加相关概念
                    Set<String> relatedConcepts = conceptExtractor.getRelatedConcepts(concept);
                    if (relatedConcepts != null) {
                        for (String related : relatedConcepts) {
                            doc.add(new StringField("related", related, Field.Store.YES));
                        }
                    }

                    writer.addDocument(doc);
                }

                writer.commit();
            }
        }
    }

    /**
     * 查询与某个方法相关的方法调用
     * @param methodId 方法ID
     * @param direction 方向（"callers"或"callees"）
     * @return 相关方法列表
     */
    public List<String> findRelatedMethods(String methodId, String direction) throws Exception {
        Path indexPath = indexBasePath.resolve(CALL_GRAPH_INDEX);
        List<String> result = new ArrayList<>();

        try (Directory directory = FSDirectory.open(indexPath);
             IndexReader reader = DirectoryReader.open(directory)) {

            IndexSearcher searcher = new IndexSearcher(reader);
            QueryParser parser = new QueryParser("callInfo", new StandardAnalyzer());

            // 根据方向确定查询字段
            String field = direction.equals("callers") ? "callee" : "caller";
            String returnField = direction.equals("callers") ? "caller" : "callee";

            Query query = parser.parse(field + ":" + QueryParser.escape(methodId));
            TopDocs docs = searcher.search(query, 100);

            // 处理结果
            for (ScoreDoc scoreDoc : docs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                result.add(doc.get(returnField));
            }
        }

        return result;
    }

    /**
     * 查询数据流
     * @param methodId 方法ID
     * @return 数据流节点
     */
    public DataFlowNode findDataFlowNode(String methodId) throws Exception {
        return dataFlowAnalyzer.getNode(methodId);
    }

    /**
     * 查询相似方法
     * @param methodId 方法ID
     * @param minSimilarity 最小相似度
     * @return 相似方法列表
     */
    public List<CodeSimilarityPair> findSimilarMethods(String methodId, double minSimilarity) throws Exception {
        Path indexPath = indexBasePath.resolve(CODE_SIMILARITY_INDEX);
        List<CodeSimilarityPair> result = new ArrayList<>();

        try (Directory directory = FSDirectory.open(indexPath);
             IndexReader reader = DirectoryReader.open(directory)) {

            IndexSearcher searcher = new IndexSearcher(reader);
            QueryParser parser = new QueryParser("info", new StandardAnalyzer());

            // 构建查询
            Query query = parser.parse("method1:" + QueryParser.escape(methodId) +
                    " OR method2:" + QueryParser.escape(methodId));
            TopDocs docs = searcher.search(query, 100);

            // 处理结果
            for (ScoreDoc scoreDoc : docs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                String method1 = doc.get("method1");
                String method2 = doc.get("method2");
                double similarity = Double.parseDouble(doc.get("similarity"));

                if (similarity >= minSimilarity) {
                    String otherMethod = method1.equals(methodId) ? method2 : method1;
                    result.add(new CodeSimilarityPair(methodId, otherMethod, similarity));
                }
            }
        }

        return result;
    }

    /**
     * 查询概念相关的代码实体
     * @param concept 概念
     * @return 相关实体列表
     */
    public List<ConceptEntityResult> findEntitiesByConcept(String concept) throws Exception {
        Path indexPath = indexBasePath.resolve(CONCEPT_INDEX);
        List<ConceptEntityResult> result = new ArrayList<>();

        try (Directory directory = FSDirectory.open(indexPath);
             IndexReader reader = DirectoryReader.open(directory)) {

            IndexSearcher searcher = new IndexSearcher(reader);
            QueryParser parser = new QueryParser("concept", new StandardAnalyzer());

            // 构建查询
            Query query = parser.parse("concept:" + QueryParser.escape(concept));
            TopDocs docs = searcher.search(query, 100);

            // 处理结果
            if (docs.scoreDocs.length > 0) {
                Document doc = searcher.doc(docs.scoreDocs[0].doc);

                // 获取所有实体ID
                String[] entityIds = doc.getValues("entityId");
                String[] sources = doc.getValues("source");

                for (int i = 0; i < entityIds.length; i++) {
                    ConceptSource source = ConceptSource.valueOf(sources[i]);
                    result.add(new ConceptEntityResult(entityIds[i], concept, source));
                }
            }
        }

        return result;
    }

    /**
     * 获取代码质量问题
     * @param entityId 实体ID（可选，为null则返回所有问题）
     * @return 质量问题列表
     */
    public List<QualityIssue> getQualityIssues(String entityId) {
        if (entityId == null) {
            return qualityAnalyzer.getIssues();
        } else {
            return qualityAnalyzer.getIssues().stream()
                    .filter(issue -> issue.getEntityId().equals(entityId))
                    .collect(Collectors.toList());
        }
    }
}