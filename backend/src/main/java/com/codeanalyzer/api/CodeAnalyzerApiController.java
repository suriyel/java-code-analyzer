package com.codeanalyzer.api;

import com.codeanalyzer.CodeAnalyzerSystem;
import com.codeanalyzer.ast.ParsedProjectStructure;
import com.codeanalyzer.index.IndexLevel;
import com.codeanalyzer.index.SearchResult;
import com.codeanalyzer.semantic.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * API控制器
 */
@RestController
@RequestMapping("/api/v1")
public class CodeAnalyzerApiController {

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    // 项目ID -> 分析系统实例
    private final Map<String, CodeAnalyzerSystem> analyzerSystems = new ConcurrentHashMap<>();

    // 项目ID -> 语义分析器
    private final Map<String, SemanticAnalyzer> semanticAnalyzers = new ConcurrentHashMap<>();

    // 上传的项目路径
    private final Path projectsDir;

    // 索引存储路径
    private final Path indexBaseDir;

    /**
     * 初始化控制器
     */
    @Autowired
    public CodeAnalyzerApiController(@Value("${analyzer.projects.dir:./projects}") String projectsDir,
                                     @Value("${analyzer.index.dir:./indexes}") String indexBaseDir) {
        this.projectsDir = Paths.get(projectsDir);
        this.indexBaseDir = Paths.get(indexBaseDir);

        // 创建必要的目录
        createDirectories();
    }

    /**
     * 创建必要的目录
     */
    @PostConstruct
    private void createDirectories() {
        try {
            Files.createDirectories(projectsDir);
            Files.createDirectories(indexBaseDir);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create required directories", e);
        }
    }

    /**
     * 上传并分析项目
     */
    @PostMapping("/projects")
    public ResponseEntity<ProjectResponse> uploadProject(@RequestParam("file") MultipartFile file) {
        try {
            // 生成项目ID
            String projectId = UUID.randomUUID().toString();

            // 创建项目目录
            Path projectDir = projectsDir.resolve(projectId);
            Files.createDirectories(projectDir);

            // 保存上传的文件
            Path zipFile = projectDir.resolve("project.zip");
            file.transferTo(zipFile.toFile());

            // 解压项目
            Path sourceDir = projectDir.resolve("src");
            Files.createDirectories(sourceDir);
            unzipProject(zipFile, sourceDir);

            // 异步分析项目
            CompletableFuture.runAsync(() -> analyzeProject(projectId, sourceDir), executorService);

            // 返回项目ID
            ProjectResponse response = new ProjectResponse();
            response.setProjectId(projectId);
            response.setStatus("PROCESSING");
            response.setMessage("Project upload successful. Analysis started.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ProjectResponse(null, "ERROR", "Error uploading project: " + e.getMessage()));
        }
    }

    /**
     * 获取项目状态
     */
    @GetMapping("/projects/{projectId}")
    public ResponseEntity<ProjectResponse> getProjectStatus(@PathVariable String projectId) {
        if (!analyzerSystems.containsKey(projectId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ProjectResponse(projectId, "NOT_FOUND", "Project not found"));
        }

        ProjectResponse response = new ProjectResponse();
        response.setProjectId(projectId);
        response.setStatus("READY");
        response.setMessage("Project analysis completed");

        return ResponseEntity.ok(response);
    }

    /**
     * 全文检索
     */
    @GetMapping("/projects/{projectId}/search")
    public ResponseEntity<List<SearchResult>> search(
            @PathVariable String projectId,
            @RequestParam String query,
            @RequestParam(defaultValue = "ALL") IndexLevel level,
            @RequestParam(defaultValue = "10") int maxResults) {
        try {
            CodeAnalyzerSystem system = getAnalyzerSystem(projectId);
            if (system == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            List<SearchResult> results = system.search(query, level, maxResults);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 关系检索
     */
    @GetMapping("/projects/{projectId}/search/relation")
    public ResponseEntity<List<SearchResult>> searchByRelation(
            @PathVariable String projectId,
            @RequestParam String relationType,
            @RequestParam String target,
            @RequestParam(defaultValue = "10") int maxResults) {
        try {
            CodeAnalyzerSystem system = getAnalyzerSystem(projectId);
            if (system == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            List<SearchResult> results = system.searchByRelation(relationType, target, maxResults);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 语义检索
     */
    @GetMapping("/projects/{projectId}/search/semantic")
    public ResponseEntity<List<SearchResult>> semanticSearch(
            @PathVariable String projectId,
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int maxResults) {
        try {
            CodeAnalyzerSystem system = getAnalyzerSystem(projectId);
            if (system == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            List<SearchResult> results = system.semanticSearch(query, maxResults);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 高级语义分析 - 查询调用关系
     */
    @GetMapping("/projects/{projectId}/semantic/calls")
    public ResponseEntity<List<String>> findRelatedMethods(
            @PathVariable String projectId,
            @RequestParam String methodId,
            @RequestParam(defaultValue = "callees") String direction) {
        try {
            SemanticAnalyzer semanticAnalyzer = getSemanticAnalyzer(projectId);
            if (semanticAnalyzer == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            List<String> methods = semanticAnalyzer.findRelatedMethods(methodId, direction);
            return ResponseEntity.ok(methods);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 高级语义分析 - 查询数据流
     */
    @GetMapping("/projects/{projectId}/semantic/dataflow")
    public ResponseEntity<DataFlowNode> findDataFlowNode(
            @PathVariable String projectId,
            @RequestParam String methodId) {
        try {
            SemanticAnalyzer semanticAnalyzer = getSemanticAnalyzer(projectId);
            if (semanticAnalyzer == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            DataFlowNode node = semanticAnalyzer.findDataFlowNode(methodId);
            if (node == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            return ResponseEntity.ok(node);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 高级语义分析 - 查询相似方法
     */
    @GetMapping("/projects/{projectId}/semantic/similar")
    public ResponseEntity<List<CodeSimilarityPair>> findSimilarMethods(
            @PathVariable String projectId,
            @RequestParam String methodId,
            @RequestParam(defaultValue = "0.7") double minSimilarity) {
        try {
            SemanticAnalyzer semanticAnalyzer = getSemanticAnalyzer(projectId);
            if (semanticAnalyzer == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            List<CodeSimilarityPair> pairs = semanticAnalyzer.findSimilarMethods(methodId, minSimilarity);
            return ResponseEntity.ok(pairs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 高级语义分析 - 查询概念
     */
    @GetMapping("/projects/{projectId}/semantic/concepts")
    public ResponseEntity<List<ConceptEntityResult>> findEntitiesByConcept(
            @PathVariable String projectId,
            @RequestParam String concept) {
        try {
            SemanticAnalyzer semanticAnalyzer = getSemanticAnalyzer(projectId);
            if (semanticAnalyzer == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            List<ConceptEntityResult> results = semanticAnalyzer.findEntitiesByConcept(concept);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 高级语义分析 - 获取代码质量问题
     */
    @GetMapping("/projects/{projectId}/semantic/quality")
    public ResponseEntity<List<QualityIssue>> getQualityIssues(
            @PathVariable String projectId,
            @RequestParam(required = false) String entityId) {
        try {
            SemanticAnalyzer semanticAnalyzer = getSemanticAnalyzer(projectId);
            if (semanticAnalyzer == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            List<QualityIssue> issues = semanticAnalyzer.getQualityIssues(entityId);
            return ResponseEntity.ok(issues);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 删除项目
     */
    @DeleteMapping("/projects/{projectId}")
    public ResponseEntity<ApiResponse> deleteProject(@PathVariable String projectId) {
        try {
            CodeAnalyzerSystem system = analyzerSystems.remove(projectId);
            if (system != null) {
                system.close();
            }

            // 删除项目目录
            Path projectDir = projectsDir.resolve(projectId);
            if (Files.exists(projectDir)) {
                deleteDirectory(projectDir.toFile());
            }

            // 删除索引目录
            Path indexDir = indexBaseDir.resolve(projectId);
            if (Files.exists(indexDir)) {
                deleteDirectory(indexDir.toFile());
            }

            return ResponseEntity.ok(new ApiResponse("success", "Project deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("error", "Error deleting project: " + e.getMessage()));
        }
    }

    /**
     * 获取分析器系统实例
     */
    private CodeAnalyzerSystem getAnalyzerSystem(String projectId) {
        return analyzerSystems.get(projectId);
    }

    /**
     * 获取语义分析器实例
     */
    private SemanticAnalyzer getSemanticAnalyzer(String projectId) {
        return semanticAnalyzers.get(projectId);
    }

    /**
     * 异步分析项目
     */
    private void analyzeProject(String projectId, Path sourceDir) {
        try {
            // 源代码路径
            List<Path> sourcePaths = Arrays.asList(sourceDir);

            // 索引存储路径
            Path indexDir = indexBaseDir.resolve(projectId);
            Files.createDirectories(indexDir);

            // 创建分析系统
            CodeAnalyzerSystem system = new CodeAnalyzerSystem(
                    sourcePaths,
                    indexDir.toString(),
                    4 // 线程数
            );

            // 分析项目
            system.analyzeProject(sourceDir);

            // 创建语义分析器
            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(
                    indexDir.resolve("semantic").toString()
            );

            // 获取解析结构
            ParsedProjectStructure projectStructure = system.getProjectStructure();

            // 执行语义分析
            semanticAnalyzer.analyzeProject(projectStructure);

            // 保存实例
            analyzerSystems.put(projectId, system);
            semanticAnalyzers.put(projectId, semanticAnalyzer);

        } catch (Exception e) {
            // 记录错误
            e.printStackTrace();
        }
    }

    /**
     * 解压项目
     */
    private void unzipProject(Path zipFile, Path destDir) throws Exception {
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(zipFile.toFile())) {
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                java.util.zip.ZipEntry entry = entries.nextElement();
                Path entryDest = destDir.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(entryDest);
                } else {
                    Files.createDirectories(entryDest.getParent());
                    Files.copy(zip.getInputStream(entry), entryDest);
                }
            }
        }
    }

    /**
     * 递归删除目录
     */
    private boolean deleteDirectory(File directory) {
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directory.delete();
    }
}
