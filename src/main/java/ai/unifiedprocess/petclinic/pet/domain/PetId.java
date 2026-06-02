package ai.unifiedprocess.petclinic.pet.domain;

import ai.unifiedprocess.petclinic.core.domain.ValueObject;

public record PetId(Integer value) implements ValueObject {

    public PetId {
        if (value == null || value < 1) {
            throw new IllegalArgumentException("Pet id must be a positive integer");
        }
    }
}
