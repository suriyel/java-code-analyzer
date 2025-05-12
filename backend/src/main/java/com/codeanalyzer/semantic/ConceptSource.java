package com.codeanalyzer.semantic;

/**
 * 概念来源
 */
public enum ConceptSource {
    JAVADOC,     // 来自JavaDoc注释
    IDENTIFIER,  // 来自标识符（类名、方法名等）
    CODE         // 来自代码内容
}
