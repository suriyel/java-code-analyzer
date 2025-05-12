package com.codeanalyzer;

import com.codeanalyzer.ast.ASTParser;
import com.codeanalyzer.ast.ParsedProjectStructure;
import com.codeanalyzer.index.IndexLevel;
import com.codeanalyzer.index.IndexManager;
import com.codeanalyzer.index.SearchResult;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Java代码分析系统 - 集成AST解析和索引管理模块
 */
public class CodeAnalyzerSystem {
    private final ASTParser astParser;
    private final IndexManager indexManager;

    /**
     * 初始化代码分析系统
     * @param sourcePaths 源代码路径
     * @param indexPath 索引存储路径
     * @param threadCount 解析线程数
     */
    public CodeAnalyzerSystem(List<Path> sourcePaths, String indexPath, int threadCount) throws Exception {
        // 初始化AST解析器
        this.astParser = new ASTParser(sourcePaths, threadCount);

        // 初始化索引管理器
        this.indexManager = new IndexManager(indexPath);
    }

    /**
     * 分析指定项目
     * @param projectPath 项目根目录
     */
    public void analyzeProject(Path projectPath) throws Exception {
        System.out.println("开始分析项目: " + projectPath);

        // 1. 解析项目结构
        ParsedProjectStructure projectStructure = astParser.parseProject(projectPath);
        System.out.println("解析完成，共提取 " + projectStructure.getEntities().size() + " 个代码实体");

        // 2. 构建索引
        indexManager.buildIndex(projectStructure);
        System.out.println("索引构建完成");
    }

    /**
     * 全文检索
     * @param query 查询字符串
     * @param level 索引级别
     * @param maxResults 最大结果数
     */
    public List<SearchResult> search(String query, IndexLevel level, int maxResults) throws Exception {
        List<SearchResult> results = indexManager.search(query, level, maxResults);
        System.out.println("搜索 '" + query + "' 匹配 " + results.size() + " 个结果");
        return results;
    }

    /**
     * 关系检索
     * @param relationType 关系类型
     * @param target 目标名称
     * @param maxResults 最大结果数
     */
    public List<SearchResult> searchByRelation(String relationType, String target, int maxResults) throws Exception {
        List<SearchResult> results = indexManager.searchByRelation(relationType, target, maxResults);
        System.out.println("关系 '" + relationType + ":" + target + "' 匹配 " + results.size() + " 个结果");
        return results;
    }

    /**
     * 语义检索
     * @param semanticQuery 语义查询
     * @param maxResults 最大结果数
     */
    public List<SearchResult> semanticSearch(String semanticQuery, int maxResults) throws Exception {
        List<SearchResult> results = indexManager.semanticSearch(semanticQuery, maxResults);
        System.out.println("语义搜索 '" + semanticQuery + "' 匹配 " + results.size() + " 个结果");
        return results;
    }

    /**
     * 高级检索
     * @param queryBuilder 查询构建器
     * @param maxResults 最大结果数
     */
    public List<SearchResult> advancedSearch(IndexManager.QueryBuilder queryBuilder, int maxResults) throws Exception {
        List<SearchResult> results = indexManager.advancedSearch(queryBuilder, maxResults);
        System.out.println("高级搜索匹配 " + results.size() + " 个结果");
        return results;
    }

    /**
     * 关闭系统，释放资源
     */
    public void close() throws Exception {
        astParser.shutdown();
        indexManager.close();
        System.out.println("系统已关闭");
    }

    /**
     * 打印搜索结果
     */
    public static void printSearchResults(List<SearchResult> results) {
        System.out.println("========== 搜索结果 ==========");
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            System.out.println((i + 1) + ". [" + result.getType() + "] " + result.getName() +
                    " (得分: " + result.getScore() + ")");
            System.out.println("   路径: " + result.getPath());

            // 打印JavaDoc摘要
            String javadoc = (String)result.getAttribute("javadoc");
            if (javadoc != null && !javadoc.isEmpty()) {
                // 截取前100个字符作为摘要
                String summary = javadoc.length() > 100 ?
                        javadoc.substring(0, 100) + "..." : javadoc;
                System.out.println("   JavaDoc: " + summary);
            }

            // 打印类型特定信息
            switch (result.getType()) {
                case "CLASS":
                case "INTERFACE":
                case "ENUM":
                    System.out.println("   包名: " + result.getAttribute("package"));
                    break;

                case "METHOD":
                    System.out.println("   所属类: " + result.getAttribute("class"));
                    System.out.println("   返回类型: " + result.getAttribute("returnType"));

                    // 打印参数信息
                    @SuppressWarnings("unchecked")
                    Map<String, String> params = (Map<String, String>)result.getAttribute("parameters");
                    if (params != null && !params.isEmpty()) {
                        System.out.println("   参数: " + params);
                    }
                    break;

                case "FIELD":
                    System.out.println("   所属类: " + result.getAttribute("class"));
                    System.out.println("   字段类型: " + result.getAttribute("fieldType"));
                    break;

                case "snippet":
                    System.out.println("   所属方法: " + result.getAttribute("method"));
                    System.out.println("   所属类: " + result.getAttribute("class"));
                    System.out.println("   代码片段: " + result.getAttribute("snippet"));
                    break;
            }

            System.out.println("-----------------------------");
        }
        System.out.println("==============================");
    }

    /**
     * 使用示例
     */
    public static void main(String[] args) {
        try {
            // 源代码路径
            List<Path> sourcePaths = Arrays.asList(
                    Paths.get("/path/to/your/java/project/src/main/java"),
                    Paths.get("/path/to/your/java/project/src/test/java")
            );

            // 索引存储路径
            String indexPath = "/path/to/your/index/directory";

            // 创建系统实例
            CodeAnalyzerSystem system = new CodeAnalyzerSystem(sourcePaths, indexPath, 4);

            // 分析项目
            system.analyzeProject(Paths.get("/path/to/your/java/project"));

            // 全文检索示例
            List<SearchResult> results1 = system.search("connection database", IndexLevel.CLASS, 10);
            printSearchResults(results1);

            // 关系检索示例 - 查找所有实现特定接口的类
            List<SearchResult> results2 = system.searchByRelation("IMPLEMENTS", "Serializable", 10);
            printSearchResults(results2);

            // 语义检索示例 - 基于JavaDoc内容
            List<SearchResult> results3 = system.semanticSearch("handle concurrent connections", 10);
            printSearchResults(results3);

            // 高级检索示例
            IndexManager.QueryBuilder queryBuilder = new IndexManager.QueryBuilder(new org.apache.lucene.analysis.standard.StandardAnalyzer())
                    .ofType("METHOD")
                    .and(IndexManager.FIELD_NAME, "save*")
                    .or(IndexManager.FIELD_NAME, "update*")
                    .and(IndexManager.FIELD_JAVADOC, "database");

            List<SearchResult> results4 = system.advancedSearch(queryBuilder, 10);
            printSearchResults(results4);

            // 关闭系统
            system.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}