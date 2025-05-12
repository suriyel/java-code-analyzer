package com.codeanalyzer.ast;

import java.util.*;

/**
 * 解析项目结构 - 存储解析结果
 */
public class ParsedProjectStructure {
    // 存储所有提取的代码实体
    private final List<CodeEntity> entities = new ArrayList<>();
    // 存储实体对应的中间表示
    private final Map<String, IntermediateRepresentation> irMap = new HashMap<>();
    // 实体间引用关系图
    private final Map<String, Set<String>> referenceGraph = new HashMap<>();

    /**
     * 添加代码实体
     */
    public void addEntity(CodeEntity entity, IntermediateRepresentation ir) {
        entities.add(entity);
        irMap.put(ir.getId(), ir);
    }

    /**
     * 构建实体间的关系图
     */
    public void buildRelationships() {
        // 实现类/接口继承关系
        for (CodeEntity entity : entities) {
            if (entity.getType() == EntityType.CLASS || entity.getType() == EntityType.INTERFACE) {
                Map<RelationType, Set<String>> relationships = entity.getRelationships();

                // 处理继承关系
                if (relationships.containsKey(RelationType.EXTENDS)) {
                    for (String parent : relationships.get(RelationType.EXTENDS)) {
                        addReference(entity.getName(), parent, RelationType.EXTENDS);
                    }
                }

                // 处理实现关系
                if (relationships.containsKey(RelationType.IMPLEMENTS)) {
                    for (String iface : relationships.get(RelationType.IMPLEMENTS)) {
                        addReference(entity.getName(), iface, RelationType.IMPLEMENTS);
                    }
                }
            }
        }

        // 方法调用关系
        for (CodeEntity entity : entities) {
            if (entity.getType() == EntityType.METHOD) {
                for (String calledMethod : entity.getMethodCalls()) {
                    // 尝试解析被调用方法的全限定名
                    // 这里简化实现，实际应该从符号表中解析
                    addReference(entity.getName(), calledMethod, RelationType.CALLS);
                }
            }
        }
    }

    /**
     * 添加引用关系
     */
    private void addReference(String source, String target, RelationType type) {
        String refKey = source + "->" + target + ":" + type.name();
        referenceGraph.computeIfAbsent(source, k -> new HashSet<>()).add(refKey);

        // 在中间表示中也添加关系
        for (IntermediateRepresentation ir : irMap.values()) {
            if (ir.getName().equals(source)) {
                ir.addRelationship(type.name(), target);
                break;
            }
        }
    }

    /**
     * 获取解析出的所有实体
     */
    public List<CodeEntity> getEntities() {
        return entities;
    }

    /**
     * 获取实体对应的中间表示
     */
    public Map<String, IntermediateRepresentation> getIrMap() {
        return irMap;
    }

    /**
     * 获取引用关系图
     */
    public Map<String, Set<String>> getReferenceGraph() {
        return referenceGraph;
    }
}