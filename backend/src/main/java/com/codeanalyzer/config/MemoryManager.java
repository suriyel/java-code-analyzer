package com.codeanalyzer.config;

/**
 * 内存管理器
 */
public class MemoryManager {
    // 内存使用阈值（百分比）
    private final int memoryThreshold;

    public MemoryManager(int memoryThreshold) {
        this.memoryThreshold = memoryThreshold;
    }

    /**
     * 获取当前内存使用率
     */
    public double getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();

        return (double)usedMemory / maxMemory * 100.0;
    }

    /**
     * 检查是否超过内存阈值
     */
    public boolean isMemoryOverThreshold() {
        return getMemoryUsage() > memoryThreshold;
    }

    /**
     * 请求垃圾回收
     */
    public void requestGC() {
        System.gc();
    }

    /**
     * 打印内存使用情况
     */
    public void printMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = allocatedMemory - freeMemory;

        System.out.println("===== 内存使用情况 =====");
        System.out.println("最大可用内存: " + formatSize(maxMemory));
        System.out.println("已分配内存: " + formatSize(allocatedMemory));
        System.out.println("已使用内存: " + formatSize(usedMemory));
        System.out.println("空闲内存: " + formatSize(freeMemory));
        System.out.println("内存使用率: " + String.format("%.2f%%", (double)usedMemory / maxMemory * 100));
        System.out.println("========================");
    }

    /**
     * 格式化内存大小
     */
    private String formatSize(long size) {
        final long KB = 1024;
        final long MB = KB * 1024;
        final long GB = MB * 1024;

        if (size >= GB) {
            return String.format("%.2f GB", (double)size / GB);
        } else if (size >= MB) {
            return String.format("%.2f MB", (double)size / MB);
        } else if (size >= KB) {
            return String.format("%.2f KB", (double)size / KB);
        } else {
            return size + " B";
        }
    }
}
