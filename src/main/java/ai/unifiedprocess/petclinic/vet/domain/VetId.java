package ai.unifiedprocess.petclinic.vet.domain;

import ai.unifiedprocess.petclinic.core.domain.ValueObject;

public record VetId(Integer value) implements ValueObject {

    public VetId {
        if (value == null || value < 1) {
            throw new IllegalArgumentException("Vet id must be a positive integer");
        }
    }
}
