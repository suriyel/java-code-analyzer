package com.codeanalyzer.ast;

import com.codeanalyzer.api.CodeAnalyzerApiApplication;
import com.codeanalyzer.api.ProjectResponse;
import com.codeanalyzer.index.SearchResult;
import com.codeanalyzer.semantic.QualityIssue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.codeanalyzer.ast.Utils.writeString;
import static org.junit.jupiter.api.Assertions.*;

/**
 * REST API测试
 */
@SpringBootTest(
        classes = CodeAnalyzerApiApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(properties = {
        "analyzer.projects.dir=${java.io.tmpdir}/code-analyzer-test/projects",
        "analyzer.index.dir=${java.io.tmpdir}/code-analyzer-test/indexes"
})
class CodeAnalyzerApiControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @TempDir
    static Path testProjectDir;

    private String projectId;

    @BeforeAll
    static void setupProject() throws IOException {
        // 创建测试项目
        createTestProject(testProjectDir);

        // 创建ZIP文件
        createProjectZip(testProjectDir);
    }

    @BeforeEach
    void setUp() throws Exception {
        // 确保测试目录存在 - 使用JDK 8兼容方法
        Path projectsDir = Paths.get(System.getProperty("java.io.tmpdir"), "code-analyzer-test", "projects");
        Path indexesDir = Paths.get(System.getProperty("java.io.tmpdir"), "code-analyzer-test", "indexes");

        try {
            Files.createDirectories(projectsDir);
            Files.createDirectories(indexesDir);
        } catch (IOException e) {
            throw new RuntimeException("无法创建测试目录", e);
        }

        // 上传项目
        uploadProject();

        // 轮询等待项目分析完成
        ResponseEntity<ProjectResponse> response;
        int maxRetries = 30;  // 最多等待30次
        int retryInterval = 1000;  // 每次间隔1秒
        int retryCount = 0;

        do {
            Thread.sleep(retryInterval);
            response = restTemplate.getForEntity(
                    "/api/v1/projects/{projectId}",
                    ProjectResponse.class,
                    projectId);
            retryCount++;

            System.out.println("等待项目分析完成，当前状态: " +
                    (response.getBody() != null ? response.getBody().getStatus() : "unknown") +
                    ", 重试次数: " + retryCount);

        } while (retryCount < maxRetries &&
                response.getBody() != null &&
                !"READY".equals(response.getBody().getStatus()));

        if (retryCount >= maxRetries) {
            throw new RuntimeException("等待项目分析超时");
        }

        // 确认项目状态为READY
        assertEquals("READY", response.getBody().getStatus(),
                "项目分析应该已经完成");
    }

    @Test
    void testProjectStatus() {
        // 获取项目状态
        ResponseEntity<ProjectResponse> response = restTemplate.getForEntity(
                "/api/v1/projects/{projectId}",
                ProjectResponse.class,
                projectId);

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("READY", response.getBody().getStatus());
    }

    @Test
    void testSearch() {
        // 执行搜索
        ResponseEntity<List<SearchResult>> response = restTemplate.exchange(
                "/api/v1/projects/{projectId}/search?query={query}&level={level}&maxResults={maxResults}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<SearchResult>>() {},
                projectId, "calculator", "ALL", 10);

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isEmpty(), "搜索结果不应为空");
    }

    @Test
    void testSemanticSearch() {
        // 执行语义搜索
        ResponseEntity<List<SearchResult>> response = restTemplate.exchange(
                "/api/v1/projects/{projectId}/search/semantic?query={query}&maxResults={maxResults}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<SearchResult>>() {},
                projectId, "计算器", 10);

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isEmpty(), "搜索结果不应为空");
    }

    @Test
    void testFindRelatedMethods() {
        // 查找相关方法
        ResponseEntity<List<String>> response = restTemplate.exchange(
                "/api/v1/projects/{projectId}/semantic/calls?methodId={methodId}&direction={direction}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<String>>() {},
                projectId, "CalculatorUser#sum", "callees");

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isEmpty(), "相关方法列表不应为空");
    }

    @Test
    void testGetQualityIssues() {
        // 获取质量问题
        ResponseEntity<List<QualityIssue>> response = restTemplate.exchange(
                "/api/v1/projects/{projectId}/semantic/quality",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<QualityIssue>>() {},
                projectId);

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isEmpty(), "质量问题列表不应为空");
    }

    @AfterEach
    void tearDown() {
        // 删除项目
        restTemplate.delete("/api/v1/projects/{projectId}", projectId);
    }

    /**
     * 创建测试项目
     */
    private static void createTestProject(Path projectDir) throws IOException {
        // 创建项目目录结构
        Path srcDir = projectDir.resolve("src");
        Files.createDirectories(srcDir);

        // 创建一些Java类
        // 1. Calculator类
        Path calculatorPath = srcDir.resolve("Calculator.java");
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
                        "}\n");

        // 2. CalculatorUser类
        Path userPath = srcDir.resolve("CalculatorUser.java");
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
                        "}\n");
    }

    /**
     * 创建项目ZIP文件
     */
    private static void createProjectZip(Path projectDir) throws IOException {
        Path zipFile = projectDir.resolve("project.zip");

        try (FileOutputStream fos = new FileOutputStream(zipFile.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            // 添加src目录
            addDirectoryToZip(projectDir.resolve("src").toFile(), "src", zos);
        }
    }

    /**
     * 递归添加目录到ZIP
     */
    private static void addDirectoryToZip(File directory, String basePath, ZipOutputStream zos) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                String entryPath = basePath + "/" + file.getName();
                if (file.isDirectory()) {
                    addDirectoryToZip(file, entryPath, zos);
                } else {
                    zos.putNextEntry(new ZipEntry(entryPath));
                    Files.copy(file.toPath(), zos);
                    zos.closeEntry();
                }
            }
        }
    }

    /**
     * 上传项目
     */
    private void uploadProject() throws IOException {
        // 创建MultipartFile
        Path zipFile = testProjectDir.resolve("project.zip");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(zipFile.toFile()));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // 执行上传
        ResponseEntity<ProjectResponse> response = restTemplate.postForEntity(
                "/api/v1/projects",
                requestEntity,
                ProjectResponse.class);

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getProjectId());

        projectId = response.getBody().getProjectId();
    }
}