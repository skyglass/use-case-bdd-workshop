package ai.unifiedprocess.petclinic.visit.domain;

import ai.unifiedprocess.petclinic.core.domain.ValueObject;

public record VisitId(Integer value) implements ValueObject {

    public VisitId {
        if (value == null || value < 1) {
            throw new IllegalArgumentException("Visit id must be a positive integer");
        }
    }
}
