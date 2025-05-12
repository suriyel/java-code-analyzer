package com.codeanalyzer.ast;

import com.codeanalyzer.semantic.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.codeanalyzer.ast.Utils.writeString;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 语义分析器测试
 */
class SemanticAnalyzerTest {

    private ASTParser parser;
    private ParsedProjectStructure structure;
    private SemanticAnalyzer semanticAnalyzer;

    @TempDir
    Path tempDir;

    @TempDir
    Path semanticDir;

    @BeforeEach
    void setUp() throws Exception {
        parser = new ASTParser(Arrays.asList(tempDir), 1);

        // 创建一些Java类
        // 1. Calculator类
        Path calculatorPath = tempDir.resolve("Calculator.java");
        writeString(calculatorPath,
                "/**\n" +
                        " * 简单计算器类\n" +
                        " */\n" +
                        "public class Calculator {\n" +
                        "    /**\n" +
                        "     * 加法运算\n" +
                        "     */\n" +
                        "    public int add(int a, int b) {\n" +
                        "        return a + b;\n" +
                        "    }\n" +
                        "    \n" +
                        "    /**\n" +
                        "     * 减法运算\n" +
                        "     */\n" +
                        "    public int subtract(int a, int b) {\n" +
                        "        return a - b;\n" +
                        "    }\n" +
                        "    \n" +
                        "    /**\n" +
                        "     * 乘法运算\n" +
                        "     */\n" +
                        "    public int multiply(int a, int b) {\n" +
                        "        return a * b;\n" +
                        "    }\n" +
                        "}\n");

        // 2. CalculatorUser类（引用Calculator）
        Path userPath = tempDir.resolve("CalculatorUser.java");
        writeString(userPath,
                "/**\n" +
                        " * 计算器使用者\n" +
                        " */\n" +
                        "public class CalculatorUser {\n" +
                        "    private Calculator calculator = new Calculator();\n" +
                        "    \n" +
                        "    /**\n" +
                        "     * 计算两数之和\n" +
                        "     */\n" +
                        "    public int sum(int a, int b) {\n" +
                        "        return calculator.add(a, b);\n" +
                        "    }\n" +
                        "    \n" +
                        "    /**\n" +
                        "     * 计算两数之差\n" +
                        "     */\n" +
                        "    public int difference(int a, int b) {\n" +
                        "        return calculator.subtract(a, b);\n" +
                        "    }\n" +
                        "}\n");

        // 3. SimilarCalculator类（与Calculator相似）
        Path similarPath = tempDir.resolve("SimilarCalculator.java");
        writeString(similarPath,
                "/**\n" +
                        " * 另一个计算器实现\n" +
                        " */\n" +
                        "public class SimilarCalculator {\n" +
                        "    /**\n" +
                        "     * 加法运算\n" +
                        "     */\n" +
                        "    public int add(int x, int y) {\n" +
                        "        return x + y;\n" +
                        "    }\n" +
                        "    \n" +
                        "    /**\n" +
                        "     * 减法运算\n" +
                        "     */\n" +
                        "    public int subtract(int x, int y) {\n" +
                        "        return x - y;\n" +
                        "    }\n" +
                        "}\n");

        // 解析项目
        structure = parser.parseProject(tempDir);

        // 创建语义分析器
        semanticAnalyzer = new SemanticAnalyzer(semanticDir.toString());

        // 分析项目
        semanticAnalyzer.analyzeProject(structure);
    }

    @Test
    void testCallGraph() throws Exception {
        // 测试调用图 - CalculatorUser.sum方法应该调用Calculator.add
        List<String> callees = semanticAnalyzer.findRelatedMethods("CalculatorUser#sum", "callees");

        // 验证结果
        assertFalse(callees.isEmpty(), "被调用方法列表不应为空");
        assertTrue(callees.contains("add") || callees.contains("Calculator#add"),
                "CalculatorUser.sum应该调用Calculator.add");
    }

    @Test
    void testDataFlow() throws Exception {
        // 测试数据流 - Calculator.add方法的参数和返回值
        DataFlowNode node = semanticAnalyzer.findDataFlowNode("Calculator#add");

        // 验证结果
        assertNotNull(node, "应该找到Calculator.add方法的数据流节点");

        // 验证输入参数
        Map<String, String> inputs = node.getInputs();
        assertEquals(2, inputs.size(), "应该有两个输入参数");
        assertTrue(inputs.containsKey("a") && inputs.containsKey("b"), "参数应该是a和b");

        // 验证输出参数
        Map<String, String> outputs = node.getOutputs();
        assertTrue(outputs.containsKey("return"), "应该有返回值");
        assertEquals("int", outputs.get("return"), "返回值类型应该是int");
    }

    @Test
    void testCodeSimilarity() throws Exception {
        // 测试代码相似度 - Calculator.add和SimilarCalculator.add应该相似
        List<CodeSimilarityPair> similarMethods = semanticAnalyzer.findSimilarMethods("Calculator#add", 0.7);

        // 验证结果
        assertFalse(similarMethods.isEmpty(), "相似方法列表不应为空");

        // 至少有一个相似方法应该是SimilarCalculator.add
        boolean foundSimilar = false;
        for (CodeSimilarityPair pair : similarMethods) {
            String otherMethod = pair.getMethod1Id().equals("Calculator#add") ?
                    pair.getMethod2Id() : pair.getMethod1Id();
            if (otherMethod.equals("SimilarCalculator#add") || otherMethod.contains("add")) {
                foundSimilar = true;
                assertTrue(pair.getSimilarity() >= 0.7, "相似度应该大于等于0.7");
                break;
            }
        }
        assertTrue(foundSimilar, "应该找到与Calculator.add相似的方法");
    }

    @Test
    void testConceptExtraction() throws Exception {
        // 测试概念提取 - "计算器"概念应该被提取
        List<ConceptEntityResult> conceptResults = semanticAnalyzer.findEntitiesByConcept("计算器");

        // 验证结果
        assertFalse(conceptResults.isEmpty(), "概念相关实体列表不应为空");

        // 至少有一个实体应该是Calculator或CalculatorUser
        boolean foundCalculator = false;
        for (ConceptEntityResult result : conceptResults) {
            if (result.getEntityId().contains("Calculator")) {
                foundCalculator = true;
                break;
            }
        }
        assertTrue(foundCalculator, "应该找到与'计算器'概念相关的实体");
    }

    @Test
    void testCodeQuality() {
        // 测试代码质量分析
        List<QualityIssue> issues = semanticAnalyzer.getQualityIssues(null);

        // 验证结果
        assertNotNull(issues, "质量问题列表不应为空");

        // 打印一些问题（仅用于调试）
        for (QualityIssue issue : issues) {
            System.out.println(issue.getEntityId() + ": " +
                    issue.getType() + " - " +
                    issue.getSeverity() + " - " +
                    issue.getMessage());
        }
    }

    @AfterEach
    void tearDown() {
        if (parser != null) {
            parser.shutdown();
        }
    }
}
