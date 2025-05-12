package com.codeanalyzer.semantic;

/**
 * 质量问题类型
 */
enum QualityIssueType {
    LONG_METHOD,             // 方法过长
    LARGE_CLASS,             // 类过大
    TOO_MANY_PARAMETERS,     // 参数过多
    MISSING_JAVADOC,         // 缺少JavaDoc
    INCONSISTENT_NAMING,     // 命名不一致
    POTENTIAL_NULL_POINTER,  // 潜在空指针
    COMPLEX_CONDITION,       // 复杂条件
    DEAD_CODE,               // 无效代码
    DUPLICATE_CODE           // 重复代码
}
