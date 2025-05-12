package com.codeanalyzer.semantic;

/**
 * 概念实体结果
 */
public class ConceptEntityResult {
    private final String entityId;
    private final String concept;
    private final ConceptSource source;

    public ConceptEntityResult(String entityId, String concept, ConceptSource source) {
        this.entityId = entityId;
        this.concept = concept;
        this.source = source;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getConcept() {
        return concept;
    }

    public ConceptSource getSource() {
        return source;
    }
}
