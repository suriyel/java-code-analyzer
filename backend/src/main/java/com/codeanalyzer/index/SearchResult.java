package com.codeanalyzer.index;

import java.util.HashMap;
import java.util.Map;

/**
 * 搜索结果类
 */
public class SearchResult {
    private String id;
    private String name;
    private String type;
    private String path;
    private float score;
    private final Map<String, Object> attributes = new HashMap<>();

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public float getScore() { return score; }
    public void setScore(float score) { this.score = score; }

    public void addAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return "SearchResult{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", path='" + path + '\'' +
                ", score=" + score +
                ", attributes=" + attributes +
                '}';
    }
}
