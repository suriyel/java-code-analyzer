package com.codeanalyzer.semantic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 调用图实现
 */
public class CallGraph {
    private final Map<String, CallGraphNode> nodes = new HashMap<>();

    /**
     * 添加调用关系
     */
    public void addCall(String callerId, String calleeId) {
        // 确保两个节点都存在
        CallGraphNode caller = nodes.computeIfAbsent(callerId, CallGraphNode::new);
        CallGraphNode callee = nodes.computeIfAbsent(calleeId, CallGraphNode::new);

        // 添加调用关系
        caller.addCallee(calleeId);
        callee.addCaller(callerId);
    }

    /**
     * 获取所有节点
     */
    public Map<String, CallGraphNode> getNodes() {
        return nodes;
    }

    /**
     * 获取调用边数量
     */
    public int getEdgeCount() {
        int count = 0;
        for (CallGraphNode node : nodes.values()) {
            count += node.getCallees().size();
        }
        return count;
    }

    /**
     * 获取特定方法的调用者
     */
    public Set<String> getCallers(String methodId) {
        CallGraphNode node = nodes.get(methodId);
        return node != null ? node.getCallers() : Collections.emptySet();
    }

    /**
     * 获取特定方法调用的方法
     */
    public Set<String> getCallees(String methodId) {
        CallGraphNode node = nodes.get(methodId);
        return node != null ? node.getCallees() : Collections.emptySet();
    }
}
