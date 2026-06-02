package ai.unifiedprocess.petclinic.owner.domain;

import ai.unifiedprocess.petclinic.core.domain.ValueObject;

public record OwnerId(Integer value) implements ValueObject {

    public OwnerId {
        if (value == null || value < 1) {
            throw new IllegalArgumentException("Owner id must be a positive integer");
        }
    }
}
