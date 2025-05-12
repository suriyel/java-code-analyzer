package com.codeanalyzer.semantic;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据流分析器
 */
public class DataFlowAnalyzer {
    private final Map<String, DataFlowNode> nodes = new HashMap<>();

    /**
     * 添加数据流节点
     */
    public void addNode(DataFlowNode node) {
        nodes.put(node.getMethodId(), node);
    }

    /**
     * 连接两个节点
     */
    public void connectNodes(String sourceId, String targetId) {
        DataFlowNode source = nodes.get(sourceId);
        DataFlowNode target = nodes.get(targetId);

        if (source != null && target != null) {
            source.addConnection(targetId);

            // 检查参数类型匹配（简化实现）
            // 实际上应该进行详细的参数传递分析
        }
    }

    /**
     * 获取节点数量
     */
    public int getNodeCount() {
        return nodes.size();
    }

    /**
     * 获取特定节点
     */
    public DataFlowNode getNode(String methodId) {
        return nodes.get(methodId);
    }

    /**
     * 获取所有节点
     */
    public Collection<DataFlowNode> getAllNodes() {
        return nodes.values();
    }
}
