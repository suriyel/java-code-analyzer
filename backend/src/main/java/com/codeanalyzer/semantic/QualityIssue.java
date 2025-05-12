package com.codeanalyzer.semantic;

/**
 * 代码质量问题
 */
public class QualityIssue {
    private final String entityId;
    private final QualityIssueType type;
    private final QualitySeverity severity;
    private final String message;

    public QualityIssue(String entityId, QualityIssueType type, QualitySeverity severity, String message) {
        this.entityId = entityId;
        this.type = type;
        this.severity = severity;
        this.message = message;
    }

    public String getEntityId() {
        return entityId;
    }

    public QualityIssueType getType() {
        return type;
    }

    public QualitySeverity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }
}