package com.codeanalyzer.semantic;

import com.codeanalyzer.ast.CodeEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 代码质量分析器
 */
public class CodeQualityAnalyzer {
    // 质量问题列表
    private final List<QualityIssue> issues = new ArrayList<>();

    // 一些质量阈值
    private static final int MAX_METHOD_LINES = 30;
    private static final int MAX_CLASS_METHODS = 20;
    private static final int MAX_PARAMETERS = 5;

    /**
     * 分析类质量
     */
    public void analyzeClass(CodeEntity entity) {
        // 检查JavaDoc
        if (entity.getJavadoc().isEmpty()) {
            addIssue(entity.getParentName() + "." + entity.getName(),
                    QualityIssueType.MISSING_JAVADOC,
                    QualitySeverity.WARNING,
                    "Class missing JavaDoc documentation");
        }

        // 其他类质量检查...
    }

    /**
     * 分析方法质量
     */
    public void analyzeMethod(CodeEntity entity) {
        // 检查方法长度
        entity.getRange().ifPresent(range -> {
            int lines = range.end.line - range.begin.line + 1;
            if (lines > MAX_METHOD_LINES) {
                addIssue(entity.getParentName() + "#" + entity.getName(),
                        QualityIssueType.LONG_METHOD,
                        QualitySeverity.WARNING,
                        "Method is too long (" + lines + " lines)");
            }
        });

        // 检查参数数量
        if (entity.getParameters().size() > MAX_PARAMETERS) {
            addIssue(entity.getParentName() + "#" + entity.getName(),
                    QualityIssueType.TOO_MANY_PARAMETERS,
                    QualitySeverity.WARNING,
                    "Method has too many parameters (" + entity.getParameters().size() + ")");
        }

        // 检查JavaDoc
        if (entity.getJavadoc().isEmpty()) {
            addIssue(entity.getParentName() + "#" + entity.getName(),
                    QualityIssueType.MISSING_JAVADOC,
                    QualitySeverity.INFO,
                    "Method missing JavaDoc documentation");
        }

        // 其他方法质量检查...
    }

    /**
     * 分析字段质量
     */
    public void analyzeField(CodeEntity entity) {
        // 检查命名规范
        String fieldName = entity.getName();
        if (Character.isUpperCase(fieldName.charAt(0)) && !entity.getModifiers().contains(com.github.javaparser.ast.Modifier.STATIC)) {
            addIssue(entity.getParentName() + "." + entity.getName(),
                    QualityIssueType.INCONSISTENT_NAMING,
                    QualitySeverity.INFO,
                    "Non-static field name starts with an uppercase letter");
        }

        // 其他字段质量检查...
    }

    /**
     * 添加质量问题
     */
    private void addIssue(String entityId, QualityIssueType type, QualitySeverity severity, String message) {
        issues.add(new QualityIssue(entityId, type, severity, message));
    }

    /**
     * 获取质量问题列表
     */
    public List<QualityIssue> getIssues() {
        return issues;
    }

    /**
     * 计算代码质量总分（0-100）
     */
    public int calculateQualityScore() {
        // 基础分100
        int score = 100;

        // 根据问题扣分
        for (QualityIssue issue : issues) {
            switch (issue.getSeverity()) {
                case ERROR:
                    score -= 10;
                    break;
                case WARNING:
                    score -= 5;
                    break;
                case INFO:
                    score -= 1;
                    break;
            }
        }

        // 确保分数在0-100范围内
        return Math.max(0, Math.min(100, score));
    }
}
