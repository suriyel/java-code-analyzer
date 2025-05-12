package com.codeanalyzer.semantic;

/**
 * 代码相似度对
 */
public class CodeSimilarityPair {
    private final String method1Id;
    private final String method2Id;
    private final double similarity;

    public CodeSimilarityPair(String method1Id, String method2Id, double similarity) {
        this.method1Id = method1Id;
        this.method2Id = method2Id;
        this.similarity = similarity;
    }

    public String getMethod1Id() {
        return method1Id;
    }

    public String getMethod2Id() {
        return method2Id;
    }

    public double getSimilarity() {
        return similarity;
    }
}
