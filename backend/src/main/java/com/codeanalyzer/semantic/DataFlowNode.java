package com.codeanalyzer.semantic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 数据流节点
 */
public class DataFlowNode {
    private final String methodId;
    private final Map<String, String> inputs = new HashMap<>();    // 参数名 -> 类型
    private final Map<String, String> outputs = new HashMap<>();   // 名称（如"return"）-> 类型
    private final Set<String> connections = new HashSet<>();       // 连接到的其他节点

    public DataFlowNode(String methodId) {
        this.methodId = methodId;
    }

    public void addInput(String name, String type) {
        inputs.put(name, type);
    }

    public void addOutput(String name, String type) {
        outputs.put(name, type);
    }

    public void addConnection(String targetMethodId) {
        connections.add(targetMethodId);
    }

    public String getMethodId() {
        return methodId;
    }

    public Map<String, String> getInputs() {
        return inputs;
    }

    public Map<String, String> getOutputs() {
        return outputs;
    }

    public Set<String> getConnections() {
        return connections;
    }
}