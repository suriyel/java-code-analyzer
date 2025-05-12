package com.codeanalyzer.config;

/**
 * 操作性能指标
 */
public class OperationMetric {
    private final String operationId;
    private final String operationType;
    private final long startTime;
    private long endTime;

    public OperationMetric(String operationId, String operationType, long startTime) {
        this.operationId = operationId;
        this.operationType = operationType;
        this.startTime = startTime;
    }

    public String getOperationId() { return operationId; }
    public String getOperationType() { return operationType; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
}