package com.codeanalyzer.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * 性能优化工具类
 */
public class PerformanceOptimizer {
    // 配置管理器
    private final ConfigurationManager configManager;
    // 内存管理器
    private final MemoryManager memoryManager;

    public PerformanceOptimizer() {
        this.configManager = ConfigurationManager.getInstance();
        this.memoryManager = new MemoryManager(85); // 85%内存使用率阈值
    }

    /**
     * 根据系统资源动态调整配置
     */
    public void optimizeConfigurations() {
        // 获取可用处理器数量
        int availableProcessors = Runtime.getRuntime().availableProcessors();

        // 计算最佳线程数（一般为核心数+1）
        int optimalThreadCount = availableProcessors + 1;
        configManager.setConfig("parser.threadCount", String.valueOf(optimalThreadCount));

        // 根据可用内存调整索引缓冲区大小
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();

        // 分配最大内存的1/4作为索引缓冲区
        int ramBufferSize = (int)(maxMemory / (4 * 1024 * 1024));
        // 确保至少64MB，最多1GB
        ramBufferSize = Math.max(64, Math.min(ramBufferSize, 1024));
        configManager.setConfig("index.ramBufferSizeMB", String.valueOf(ramBufferSize));

        // 根据内存使用情况决定是否使用复合索引文件
        boolean useCompoundFile = maxMemory < 4L * 1024 * 1024 * 1024; // 4GB
        configManager.setConfig("index.useCompoundFile", String.valueOf(useCompoundFile));

        // 调整缓存大小
        int cacheSize = (int)(maxMemory / (16 * 1024)); // 根据内存大小调整
        configManager.setConfig("cache.maxEntries", String.valueOf(cacheSize));

        System.out.println("性能配置已优化:");
        System.out.println("- 解析线程数: " + optimalThreadCount);
        System.out.println("- 索引缓冲区大小: " + ramBufferSize + "MB");
        System.out.println("- 使用复合索引文件: " + useCompoundFile);
        System.out.println("- 缓存大小: " + cacheSize + " 条目");
    }

    /**
     * 监控系统资源并在必要时释放
     */
    public void monitorAndFreeResources() {
        // 检查内存使用情况
        if (memoryManager.isMemoryOverThreshold()) {
            System.out.println("内存使用率过高，释放资源...");

            // 清理缓存
            configManager.getCacheManager().cleanupExpired();

            // 建议垃圾回收
            memoryManager.requestGC();

            // 打印内存使用情况
            memoryManager.printMemoryUsage();
        }
    }

    /**
     * 打印性能报告
     */
    public void printPerformanceReport() {
        // 打印性能指标
        configManager.getPerformanceMonitor().printPerformanceReport();

        // 打印缓存统计
        CacheManager cacheManager = configManager.getCacheManager();
        System.out.println("===== 缓存统计 =====");
        System.out.println("缓存大小: " + cacheManager.size() + " 条目");
        System.out.println("命中次数: " + cacheManager.getHits());
        System.out.println("未命中次数: " + cacheManager.getMisses());
        System.out.println("命中率: " + String.format("%.2f%%", cacheManager.getHitRate() * 100));
        System.out.println("====================");

        // 打印内存使用情况
        memoryManager.printMemoryUsage();
    }

    /**
     * 执行基准测试
     */
    public void runBenchmark(String projectPath, int iterations) throws Exception {
        System.out.println("开始基准测试...");

        // 创建多个查询
        String[] queries = {
                "database connection",
                "thread safe",
                "exception handling",
                "file io",
                "serialization"
        };

        // 创建AST解析器和索引管理器
        Path path = Paths.get(projectPath);
        List<Path> sourcePaths = Collections.singletonList(path);
        com.codeanalyzer.ast.ASTParser parser = new com.codeanalyzer.ast.ASTParser(
                sourcePaths,
                configManager.getParserThreadCount()
        );

        // 解析项目
        String parseOpId = configManager.getPerformanceMonitor().startOperation("Project Parse");
        com.codeanalyzer.ast.ParsedProjectStructure structure = parser.parseProject(path);
        configManager.getPerformanceMonitor().endOperation(parseOpId);

        // 创建索引管理器
        com.codeanalyzer.index.IndexManager indexManager = new com.codeanalyzer.index.IndexManager(
                "/tmp/benchmark_index"
        );

        // 构建索引
        String indexOpId = configManager.getPerformanceMonitor().startOperation("Index Build");
        indexManager.buildIndex(structure);
        configManager.getPerformanceMonitor().endOperation(indexOpId);

        // 执行查询测试
        for (int i = 0; i < iterations; i++) {
            for (String query : queries) {
                String searchOpId = configManager.getPerformanceMonitor().startOperation("Search: " + query);
                indexManager.search(query, com.codeanalyzer.index.IndexLevel.ALL, 10);
                configManager.getPerformanceMonitor().endOperation(searchOpId);
            }

            // 语义搜索测试
            String semanticOpId = configManager.getPerformanceMonitor().startOperation("Semantic Search");
            indexManager.semanticSearch("handle concurrent connections", 10);
            configManager.getPerformanceMonitor().endOperation(semanticOpId);

            // 关系搜索测试
            String relationOpId = configManager.getPerformanceMonitor().startOperation("Relation Search");
            indexManager.searchByRelation("IMPLEMENTS", "Serializable", 10);
            configManager.getPerformanceMonitor().endOperation(relationOpId);
        }

        // 关闭资源
        parser.shutdown();
        indexManager.close();

        // 打印性能报告
        printPerformanceReport();

        System.out.println("基准测试完成");
    }

    /**
     * 使用示例
     */
    public static void main(String[] args) {
        try {
            PerformanceOptimizer optimizer = new PerformanceOptimizer();

            // 优化配置
            optimizer.optimizeConfigurations();

            // 运行基准测试
            optimizer.runBenchmark("/path/to/project", 5);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}