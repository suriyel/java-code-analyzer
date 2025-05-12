package com.codeanalyzer.semantic;

import java.util.*;

/**
 * 概念提取器
 */
class ConceptExtractor {
    // 概念 -> 出现位置
    private final Map<String, Set<ConceptOccurrence>> conceptOccurrences = new HashMap<>();

    // 概念 -> 相关概念
    private final Map<String, Set<String>> relatedConcepts = new HashMap<>();

    // 停用词列表
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "an", "the", "and", "or", "but", "if", "then", "else", "when",
            "at", "by", "for", "with", "about", "against", "between", "into",
            "through", "during", "before", "after", "above", "below", "from",
            "up", "down", "in", "out", "on", "off", "over", "under", "again",
            "further", "then", "once", "here", "there", "all", "any", "both",
            "each", "few", "more", "most", "other", "some", "such", "no", "nor",
            "not", "only", "own", "same", "so", "than", "too", "very", "s", "t",
            "can", "will", "just", "don", "should", "now"
    ));

    /**
     * 处理文本，提取概念
     */
    public void processText(String entityId, String text, ConceptSource source) {
        if (text == null || text.isEmpty()) {
            return;
        }

        // 分词
        String[] tokens = text.toLowerCase()
                .replaceAll("[^a-z0-9_]", " ")
                .split("\\s+");

        // 提取单个词的概念
        for (String token : tokens) {
            if (!token.isEmpty() && token.length() > 2 && !STOP_WORDS.contains(token)) {
                addConcept(entityId, token, source);
            }
        }

        // 提取多词短语（双词）
        // 这里简化实现，实际应使用更复杂的短语提取算法
        for (int i = 0; i < tokens.length - 1; i++) {
            String token1 = tokens[i];
            String token2 = tokens[i + 1];

            if (!token1.isEmpty() && !token2.isEmpty() &&
                    !STOP_WORDS.contains(token1) && !STOP_WORDS.contains(token2)) {
                String phrase = token1 + " " + token2;
                addConcept(entityId, phrase, source);

                // 建立词之间的关联
                addRelatedConcept(token1, token2);
                addRelatedConcept(token2, token1);
            }
        }
    }

    /**
     * 添加概念
     */
    private void addConcept(String entityId, String concept, ConceptSource source) {
        ConceptOccurrence occurrence = new ConceptOccurrence(entityId, concept, source);
        conceptOccurrences.computeIfAbsent(concept, k -> new HashSet<>()).add(occurrence);
    }

    /**
     * 添加相关概念
     */
    private void addRelatedConcept(String concept1, String concept2) {
        relatedConcepts.computeIfAbsent(concept1, k -> new HashSet<>()).add(concept2);
    }

    /**
     * 获取相关概念
     */
    public Set<String> getRelatedConcepts(String concept) {
        return relatedConcepts.get(concept);
    }

    /**
     * 对概念进行排名
     */
    public Map<String, Set<ConceptOccurrence>> rankConcepts() {
        // 按出现次数排序
        Map<String, Set<ConceptOccurrence>> sortedConcepts = new LinkedHashMap<>();
        conceptOccurrences.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
                .forEach(e -> sortedConcepts.put(e.getKey(), e.getValue()));

        return sortedConcepts;
    }
}
