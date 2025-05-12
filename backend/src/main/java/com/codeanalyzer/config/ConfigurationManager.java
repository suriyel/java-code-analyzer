package com.codeanalyzer.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * 系统配置管理类
 * 支持：
 * 1. 从配置文件加载配置
 * 2. 缓存控制
 * 3. 性能监控
 * 4. 内存管理
 */
public class ConfigurationManager {
    // 单例模式
    private static ConfigurationManager instance;

    // 配置项
    private final Properties config = new Properties();

    // 性能监控
    private final PerformanceMonitor performanceMonitor = new PerformanceMonitor();

    // 缓存管理器
    private final CacheManager cacheManager = new CacheManager();

    // 私有构造函数
    private ConfigurationManager() {
        // 加载默认配置
        loadDefaultConfig();
    }

    /**
     * 获取单例实例
     */
    public static synchronized ConfigurationManager getInstance() {
        if (instance == null) {
            instance = new ConfigurationManager();
        }
        return instance;
    }

    /**
     * 加载默认配置
     */
    private void loadDefaultConfig() {
        // 解析线程数
        config.setProperty("parser.threadCount", String.valueOf(Runtime.getRuntime().availableProcessors()));

        // 索引配置
        config.setProperty("index.ramBufferSizeMB", "256");
        config.setProperty("index.useCompoundFile", "true");

        // 缓存配置
        config.setProperty("cache.maxEntries", "10000");
        config.setProperty("cache.expirationTimeMinutes", "60");

        // 查询配置
        config.setProperty("query.maxResults", "100");
        config.setProperty("query.timeout", "10000"); // 毫秒
    }

    /**
     * 从文件加载配置
     * @param configFile 配置文件路径
     */
    public void loadConfig(File configFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(configFile)) {
            config.load(fis);
        }
    }

    /**
     * 获取整数配置
     */
    public int getIntConfig(String key, int defaultValue) {
        String value = config.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取布尔配置
     */
    public boolean getBooleanConfig(String key, boolean defaultValue) {
        String value = config.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * 获取字符串配置
     */
    public String getStringConfig(String key, String defaultValue) {
        return config.getProperty(key, defaultValue);
    }

    /**
     * 设置配置项
     */
    public void setConfig(String key, String value) {
        config.setProperty(key, value);
    }

    /**
     * 获取性能监控器
     */
    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }

    /**
     * 获取缓存管理器
     */
    public CacheManager getCacheManager() {
        return cacheManager;
    }

    /**
     * 获取解析线程数
     */
    public int getParserThreadCount() {
        return getIntConfig("parser.threadCount", Runtime.getRuntime().availableProcessors());
    }

    /**
     * 获取索引内存缓冲区大小
     */
    public int getIndexRamBufferSizeMB() {
        return getIntConfig("index.ramBufferSizeMB", 256);
    }

    /**
     * 是否使用复合索引文件
     */
    public boolean useCompoundIndexFile() {
        return getBooleanConfig("index.useCompoundFile", true);
    }

    /**
     * 获取查询超时时间
     */
    public int getQueryTimeoutMs() {
        return getIntConfig("query.timeout", 10000);
    }

    /**
     * 获取最大查询结果数
     */
    public int getMaxQueryResults() {
        return getIntConfig("query.maxResults", 100);
    }
}