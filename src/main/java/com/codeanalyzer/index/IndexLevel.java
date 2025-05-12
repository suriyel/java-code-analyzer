package com.codeanalyzer.index;

/**
 * 索引级别枚举
 */
public enum IndexLevel {
    FILE,       // 文件级
    CLASS,      // 类级
    INTERFACE,  // 接口级
    METHOD,     // 方法级
    FIELD,      // 字段级
    SNIPPET,    // 代码片段级
    ALL         // 所有级别
}
