package com.codeanalyzer.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 性能监控器
 */
public class PerformanceMonitor {
    // 各操作的耗时统计
    private final List<OperationMetric> metrics = new ArrayList<>();

    /**
     * 开始记录操作耗时
     * @param operationName 操作名称
     * @return 操作ID
     */
    public String startOperation(String operationName) {
        String operationId = operationName + "-" + System.nanoTime();
        metrics.add(new OperationMetric(operationId, operationName, System.nanoTime()));
        return operationId;
    }

    /**
     * 结束操作耗时记录
     * @param operationId 操作ID
     */
    public void endOperation(String operationId) {
        long endTime = System.nanoTime();
        for (OperationMetric metric : metrics) {
            if (metric.getOperationId().equals(operationId)) {
                metric.setEndTime(endTime);
                break;
            }
        }
    }

    /**
     * 获取操作耗时
     * @param operationId 操作ID
     * @return 耗时（毫秒）
     */
    public long getOperationDuration(String operationId) {
        for (OperationMetric metric : metrics) {
            if (metric.getOperationId().equals(operationId) && metric.getEndTime() > 0) {
                return TimeUnit.NANOSECONDS.toMillis(metric.getEndTime() - metric.getStartTime());
            }
        }
        return -1; // 未找到或操作未结束
    }

    /**
     * 获取指定类型操作的平均耗时
     * @param operationType 操作类型
     * @return 平均耗时（毫秒）
     */
    public double getAverageOperationTime(String operationType) {
        long totalTime = 0;
        int count = 0;

        for (OperationMetric metric : metrics) {
            if (metric.getOperationType().equals(operationType) && metric.getEndTime() > 0) {
                totalTime += (metric.getEndTime() - metric.getStartTime());
                count++;
            }
        }

        return count > 0 ? TimeUnit.NANOSECONDS.toMillis(totalTime) / (double)count : 0;
    }

    /**
     * 清空性能指标
     */
    public void clearMetrics() {
        metrics.clear();
    }

    /**
     * 获取所有性能指标
     */
    public List<OperationMetric> getAllMetrics() {
        return new ArrayList<>(metrics);
    }

    /**
     * 打印性能报告
     */
    public void printPerformanceReport() {
        System.out.println("===== 性能报告 =====");

        // 按操作类型分组统计
        Map<String, List<Long>> operationTimes = new HashMap<>();

        for (OperationMetric metric : metrics) {
            if (metric.getEndTime() > 0) {
                long duration = metric.getEndTime() - metric.getStartTime();
                operationTimes.computeIfAbsent(metric.getOperationType(), k -> new ArrayList<>())
                        .add(duration);
            }
        }

        // 打印每种操作类型的统计信息
        for (Map.Entry<String, List<Long>> entry : operationTimes.entrySet()) {
            String operationType = entry.getKey();
            List<Long> times = entry.getValue();

            long totalTime = 0;
            long minTime = Long.MAX_VALUE;
            long maxTime = 0;

            for (Long time : times) {
                totalTime += time;
                minTime = Math.min(minTime, time);
                maxTime = Math.max(maxTime, time);
            }

            double avgTime = times.isEmpty() ? 0 : totalTime / (double)times.size();

            System.out.println("操作类型: " + operationType);
            System.out.println("  次数: " + times.size());
            System.out.println("  平均耗时: " + TimeUnit.NANOSECONDS.toMillis((long)avgTime) + " ms");
            System.out.println("  最小耗时: " + TimeUnit.NANOSECONDS.toMillis(minTime) + " ms");
            System.out.println("  最大耗时: " + TimeUnit.NANOSECONDS.toMillis(maxTime) + " ms");
            System.out.println("  总耗时: " + TimeUnit.NANOSECONDS.toMillis(totalTime) + " ms");
            System.out.println();
        }

        System.out.println("====================");
    }
}