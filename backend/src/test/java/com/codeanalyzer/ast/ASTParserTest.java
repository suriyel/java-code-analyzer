package com.codeanalyzer.ast;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

import static com.codeanalyzer.ast.Utils.writeString;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AST解析器测试
 */
class ASTParserTest {

    private ASTParser parser;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        parser = new ASTParser(Arrays.asList(tempDir), 4);
    }

    @Test
    void testBasicClassParsing() throws IOException {
        // 创建一个简单的Java类
        String className = "SimpleClass";
        Path filePath = tempDir.resolve(className + ".java");
        writeString(filePath,
                "/**\n" +
                        " * 一个简单的类\n" +
                        " */\n" +
                        "public class " + className + " {\n" +
                        "    private String name;\n" +
                        "    \n" +
                        "    /**\n" +
                        "     * 获取名称\n" +
                        "     */\n" +
                        "    public String getName() {\n" +
                        "        return name;\n" +
                        "    }\n" +
                        "}\n");

        // 解析项目
        ParsedProjectStructure structure = parser.parseProject(tempDir);

        // 验证结果
        assertNotNull(structure);

        // 至少应该有一个类、一个方法和一个字段
        assertTrue(structure.getEntities().size() >= 3);

        // 验证类实体
        boolean foundClass = false;
        for (CodeEntity entity : structure.getEntities()) {
            if (entity.getType().toString().equals("CLASS") && entity.getName().equals(className)) {
                foundClass = true;

                // 验证JavaDoc
                assertTrue(entity.getJavadoc().contains("一个简单的类"));
                break;
            }
        }
        assertTrue(foundClass, "应该找到类" + className);

        // 验证方法实体
        boolean foundMethod = false;
        for (CodeEntity entity : structure.getEntities()) {
            if (entity.getType().toString().equals("METHOD") && entity.getName().equals("getName")) {
                foundMethod = true;

                // 验证返回类型
                assertEquals("String", entity.getReturnType());
                // 验证JavaDoc
                assertTrue(entity.getJavadoc().contains("获取名称"));
                break;
            }
        }
        assertTrue(foundMethod, "应该找到方法getName");

        // 验证字段实体
        boolean foundField = false;
        for (CodeEntity entity : structure.getEntities()) {
            if (entity.getType().toString().equals("FIELD") && entity.getName().equals("name")) {
                foundField = true;

                // 验证字段类型
                assertEquals("String", entity.getFieldType());
                break;
            }
        }
        assertTrue(foundField, "应该找到字段name");
    }

    @Test
    void testInheritanceRelationship() throws IOException {
        // 创建一个父类
        Path parentPath = tempDir.resolve("Parent.java");
        writeString(parentPath,
                "public class Parent {\n" +
                        "    public void parentMethod() {}\n" +
                        "}\n");

        // 创建一个子类
        Path childPath = tempDir.resolve("Child.java");
        writeString(childPath,
                "public class Child extends Parent {\n" +
                        "    @Override\n" +
                        "    public void parentMethod() {\n" +
                        "        super.parentMethod();\n" +
                        "    }\n" +
                        "}\n");

        // 解析项目
        ParsedProjectStructure structure = parser.parseProject(tempDir);

        // 验证结果
        boolean foundRelationship = false;
        for (CodeEntity entity : structure.getEntities()) {
            if (entity.getType().toString().equals("CLASS") && entity.getName().equals("Child")) {
                // 验证继承关系
                assertTrue(entity.getRelationships().containsKey(RelationType.EXTENDS));
                Set<String> parents = entity.getRelationships().get(RelationType.EXTENDS);
                assertTrue(parents.contains("Parent"));
                foundRelationship = true;
                break;
            }
        }
        assertTrue(foundRelationship, "应该找到Child类继承Parent的关系");
    }

    @AfterEach
    void tearDown() {
        parser.shutdown();
    }
}