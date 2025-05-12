package com.codeanalyzer.semantic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 代码相似度分析器
 */
public class CodeSimilarityAnalyzer {
    // 方法ID -> 特征向量
    private final Map<String, double[]> featureVectors = new HashMap<>();

    // 方法对 -> 相似度
    private final Map<String, Double> similarities = new HashMap<>();

    /**
     * 计算方法的特征向量
     * 这里使用简化的TF-IDF实现
     */
    public void computeFeatureVector(String methodId, String methodText) {
        // 分词
        String[] tokens = methodText.toLowerCase()
                .replaceAll("[^a-z0-9_]", " ")
                .split("\\s+");

        // 统计词频
        Map<String, Integer> termFrequency = new HashMap<>();
        for (String token : tokens) {
            if (!token.isEmpty()) {
                termFrequency.put(token, termFrequency.getOrDefault(token, 0) + 1);
            }
        }

        // 计算特征向量（这里简化为词频）
        // 实际应使用TF-IDF或更复杂的特征
        double[] vector = new double[termFrequency.size()];
        String[] terms = termFrequency.keySet().toArray(new String[0]);

        for (int i = 0; i < terms.length; i++) {
            vector[i] = termFrequency.get(terms[i]);
        }

        // 归一化
        double norm = 0;
        for (double v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);

        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }

        featureVectors.put(methodId, vector);
    }

    /**
     * 计算所有方法之间的相似度
     */
    public void computeSimilarities() {
        String[] methodIds = featureVectors.keySet().toArray(new String[0]);

        // 计算每对方法的相似度
        for (int i = 0; i < methodIds.length; i++) {
            for (int j = i + 1; j < methodIds.length; j++) {
                String id1 = methodIds[i];
                String id2 = methodIds[j];

                double similarity = computeCosineSimilarity(featureVectors.get(id1), featureVectors.get(id2));

                // 保存相似度
                String pairKey = id1 + "_" + id2;
                similarities.put(pairKey, similarity);
            }
        }
    }

    /**
     * 计算两个向量的余弦相似度
     */
    private double computeCosineSimilarity(double[] vector1, double[] vector2) {
        // 如果向量长度不同，使用较短的长度
        int length = Math.min(vector1.length, vector2.length);

        double dotProduct = 0;
        for (int i = 0; i < length; i++) {
            dotProduct += vector1[i] * vector2[i];
        }

        return dotProduct;
    }

    /**
     * 查找潜在的重复代码
     */
    public List<CodeSimilarityPair> findPotentialDuplicates(double threshold) {
        List<CodeSimilarityPair> result = new ArrayList<>();

        for (Map.Entry<String, Double> entry : similarities.entrySet()) {
            if (entry.getValue() >= threshold) {
                String[] ids = entry.getKey().split("_");
                result.add(new CodeSimilarityPair(ids[0], ids[1], entry.getValue()));
            }
        }

        return result;
    }
}
