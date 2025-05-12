package com.codeanalyzer.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存管理器
 */
public class CacheManager {
    // 缓存项
    private static class CacheEntry<T> {
        private final T value;
        private final long expirationTime;

        public CacheEntry(T value, long expirationTimeMs) {
            this.value = value;
            this.expirationTime = System.currentTimeMillis() + expirationTimeMs;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }

        public T getValue() {
            return value;
        }
    }

    // 缓存存储
    private final Map<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();

    // 缓存统计
    private long hits = 0;
    private long misses = 0;

    /**
     * 从缓存获取值
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key) {
        CacheEntry<?> entry = cache.get(key);

        if (entry == null) {
            misses++;
            return Optional.empty();
        }

        if (entry.isExpired()) {
            cache.remove(key);
            misses++;
            return Optional.empty();
        }

        hits++;
        return Optional.of((T)entry.getValue());
    }

    /**
     * 将值放入缓存
     */
    public <T> void put(String key, T value, long expirationTimeMs) {
        cache.put(key, new CacheEntry<>(value, expirationTimeMs));
    }

    /**
     * 从缓存移除值
     */
    public void remove(String key) {
        cache.remove(key);
    }

    /**
     * 清空缓存
     */
    public void clear() {
        cache.clear();
    }

    /**
     * 清理过期缓存
     */
    public void cleanupExpired() {
        List<String> expiredKeys = new ArrayList<>();

        for (Map.Entry<String, CacheEntry<?>> entry : cache.entrySet()) {
            if (entry.getValue().isExpired()) {
                expiredKeys.add(entry.getKey());
            }
        }

        for (String key : expiredKeys) {
            cache.remove(key);
        }
    }

    /**
     * 获取缓存大小
     */
    public int size() {
        return cache.size();
    }

    /**
     * 获取缓存命中率
     */
    public double getHitRate() {
        long total = hits + misses;
        return total > 0 ? (double)hits / total : 0.0;
    }

    /**
     * 获取缓存命中次数
     */
    public long getHits() {
        return hits;
    }

    /**
     * 获取缓存未命中次数
     */
    public long getMisses() {
        return misses;
    }

    /**
     * 重置统计信息
     */
    public void resetStats() {
        hits = 0;
        misses = 0;
    }
}
