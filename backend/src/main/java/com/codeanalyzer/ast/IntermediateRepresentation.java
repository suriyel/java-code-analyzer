package com.codeanalyzer.ast;

import java.util.*;

/**
 * 中间表示 - 统一的代码实体表示，用于索引和检索
 */
public class IntermediateRepresentation {
    private String id;
    private String name;
    private String type;
    private String path;
    private String text; // 用于全文检索
    private final Map<String, Object> attributes = new HashMap<>();
    private final Map<String, Set<String>> relationships = new HashMap<>();

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    // 属性管理
    public void addAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    // 关系管理
    public void addRelationship(String type, Set<String> targets) {
        relationships.put(type, new HashSet<>(targets));
    }

    public void addRelationship(String type, String target) {
        relationships.computeIfAbsent(type, k -> new HashSet<>()).add(target);
    }

    public Set<String> getRelationship(String type) {
        return relationships.getOrDefault(type, Collections.emptySet());
    }

    public Map<String, Set<String>> getRelationships() {
        return relationships;
    }
}
