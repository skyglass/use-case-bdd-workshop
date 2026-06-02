package ai.unifiedprocess.petclinic.vet.domain;

import ai.unifiedprocess.petclinic.core.domain.ValueObject;

public record Specialty(String name) implements ValueObject {

    public Specialty {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Specialty name is required");
        }
        name = name.trim();
    }
}
