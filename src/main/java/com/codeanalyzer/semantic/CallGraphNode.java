package com.codeanalyzer.semantic;

import java.util.HashSet;
import java.util.Set;

/**
 * 调用图节点
 */
public class CallGraphNode {
    private final String methodId;
    private final Set<String> callees = new HashSet<>();
    private final Set<String> callers = new HashSet<>();

    public CallGraphNode(String methodId) {
        this.methodId = methodId;
    }

    public void addCallee(String calleeId) {
        callees.add(calleeId);
    }

    public void addCaller(String callerId) {
        callers.add(callerId);
    }

    public String getMethodId() {
        return methodId;
    }

    public Set<String> getCallees() {
        return callees;
    }

    public Set<String> getCallers() {
        return callers;
    }
}