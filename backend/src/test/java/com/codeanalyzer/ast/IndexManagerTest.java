package com.codeanalyzer.ast;

import com.codeanalyzer.index.IndexLevel;
import com.codeanalyzer.index.IndexManager;
import com.codeanalyzer.index.SearchResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static com.codeanalyzer.ast.Utils.writeString;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 索引管理器测试
 */
class IndexManagerTest {

    private IndexManager indexManager;
    private ASTParser parser;
    private ParsedProjectStructure structure;

    @TempDir
    Path tempDir;

    @TempDir
    Path indexDir;

    @BeforeEach
    void setUp() throws Exception {
        parser = new ASTParser(Arrays.asList(tempDir), 1);

        // 创建一个简单的Java类
        Path filePath = tempDir.resolve("TestClass.java");
        writeString(filePath,
                "/**\n" +
                        " * 测试数据库连接的类\n" +
                        " */\n" +
                        "public class TestClass {\n" +
                        "    private String connectionString;\n" +
                        "    \n" +
                        "    /**\n" +
                        "     * 初始化数据库连接\n" +
                        "     */\n" +
                        "    public void initDatabaseConnection(String connString) {\n" +
                        "        this.connectionString = connString;\n" +
                        "    }\n" +
                        "    \n" +
                        "    /**\n" +
                        "     * 执行查询\n" +
                        "     */\n" +
                        "    public void executeQuery(String query) {\n" +
                        "        // Connect to database and execute\n" +
                        "    }\n" +
                        "}\n");

        // 解析项目
        structure = parser.parseProject(tempDir);

        // 创建索引管理器
        indexManager = new IndexManager(indexDir.toString());

        // 构建索引
        indexManager.buildIndex(structure);
    }

    @Test
    void testBasicSearch() throws Exception {
        // 测试全文搜索
        List<SearchResult> results = indexManager.search("database connection", IndexLevel.ALL, 10);

        // 验证结果
        assertFalse(results.isEmpty(), "搜索结果不应为空");

        // 至少有一个结果应该包含TestClass
        boolean foundTestClass = false;
        for (SearchResult result : results) {
            if (result.getName().equals("TestClass") || result.getName().equals("initDatabaseConnection")) {
                foundTestClass = true;
                break;
            }
        }
        assertTrue(foundTestClass, "应该找到TestClass或其方法");
    }

    @Test
    void testMethodSearch() throws Exception {
        // 测试方法级搜索
        List<SearchResult> results = indexManager.search("executeQuery", IndexLevel.METHOD, 10);

        // 验证结果
        assertFalse(results.isEmpty(), "搜索结果不应为空");

        // 结果应该是executeQuery方法
        SearchResult result = results.get(0);
        assertEquals("executeQuery", result.getName());
        assertEquals("METHOD", result.getType());
    }

    @Test
    void testSemanticSearch() throws Exception {
        // 测试语义搜索（基于JavaDoc）
        List<SearchResult> results = indexManager.semanticSearch("数据库连接", 10);

        // 验证结果
        assertFalse(results.isEmpty(), "搜索结果不应为空");

        // 结果应该包含JavaDoc中包含"数据库连接"的实体
        boolean foundJavadoc = false;
        for (SearchResult result : results) {
            String javadoc = (String)result.getAttribute("javadoc");
            if (javadoc != null && javadoc.contains("数据库连接")) {
                foundJavadoc = true;
                break;
            }
        }
        assertTrue(foundJavadoc, "应该找到JavaDoc中包含'数据库连接'的实体");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (indexManager != null) {
            indexManager.close();
        }
        if (parser != null) {
            parser.shutdown();
        }
    }
}
