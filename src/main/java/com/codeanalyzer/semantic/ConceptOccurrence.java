package com.codeanalyzer.semantic;

import java.util.Objects;

/**
 * 概念出现
 */
public class ConceptOccurrence {
    private final String entityId;
    private final String concept;
    private final ConceptSource source;

    public ConceptOccurrence(String entityId, String concept, ConceptSource source) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConceptOccurrence that = (ConceptOccurrence) o;
        return Objects.equals(entityId, that.entityId) &&
                Objects.equals(concept, that.concept) &&
                source == that.source;
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityId, concept, source);
    }
}
