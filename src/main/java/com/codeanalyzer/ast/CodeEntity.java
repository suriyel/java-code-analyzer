package com.codeanalyzer.ast;

import com.github.javaparser.Range;
import com.github.javaparser.ast.Modifier;

import java.util.*;

/**
 * 代码实体 - 存储解析出的Java代码结构元素
 */
public class CodeEntity {
    private final String name;
    private final EntityType type;
    private final String parentName; // 包名或类名
    private final Optional<Range> range;
    private final String javadoc;

    // 通用属性
    private final Set<Modifier> modifiers = new HashSet<>();
    private final Map<RelationType, Set<String>> relationships = new HashMap<>();

    // 方法特有属性
    private String returnType;
    private final Map<String, String> parameters = new LinkedHashMap<>(); // 保持参数顺序
    private final Set<String> methodCalls = new HashSet<>();

    // 字段特有属性
    private String fieldType;
    private Optional<String> initializer = Optional.empty();

    // 枚举特有属性
    private final Set<String> enumConstants = new LinkedHashSet<>();

    public CodeEntity(String name, EntityType type, String parentName,
                      com.github.javaparser.Range range, String javadoc) {
        this.name = name;
        this.type = type;
        this.parentName = parentName;
        this.range = Optional.ofNullable(range);
        this.javadoc = javadoc;
    }

    // 添加关系
    public void addRelationship(RelationType type, String target) {
        relationships.computeIfAbsent(type, k -> new HashSet<>()).add(target);
    }

    // 添加修饰符
    public void addModifiers(com.github.javaparser.ast.NodeList<com.github.javaparser.ast.Modifier> mods) {
        mods.forEach(modifiers::add);
    }

    // 添加参数
    public void addParameter(String name, String type) {
        parameters.put(name, type);
    }

    // 添加方法调用
    public void addMethodCall(String methodName) {
        methodCalls.add(methodName);
    }

    // 添加枚举常量
    public void addEnumConstant(String constantName) {
        enumConstants.add(constantName);
    }

    // Getters
    public String getName() { return name; }
    public EntityType getType() { return type; }
    public String getParentName() { return parentName; }
    public Optional<com.github.javaparser.Range> getRange() { return range; }
    public String getJavadoc() { return javadoc; }
    public Set<com.github.javaparser.ast.Modifier> getModifiers() { return modifiers; }
    public Map<RelationType, Set<String>> getRelationships() { return relationships; }
    public Map<String, String> getParameters() { return parameters; }
    public Set<String> getMethodCalls() { return methodCalls; }
    public Set<String> getEnumConstants() { return enumConstants; }

    // 根据实体类型获取特定属性
    public String getPackageName() {
        return type == EntityType.CLASS || type == EntityType.INTERFACE || type == EntityType.ENUM ?
                parentName : "";
    }

    // Setters
    public void setReturnType(String returnType) { this.returnType = returnType; }
    public String getReturnType() { return returnType; }

    public void setType(String fieldType) { this.fieldType = fieldType; }
    public String getFieldType() { return fieldType; }

    public void setInitializer(String initializer) { this.initializer = Optional.of(initializer); }
    public Optional<String> getInitializer() { return initializer; }
}
