package ai.unifiedprocess.petclinic.pet.domain;

import ai.unifiedprocess.petclinic.core.domain.ValueObject;

public record PetName(String value) implements ValueObject {

    public PetName {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Pet name is required");
        }
        value = value.trim();
    }

    public boolean sameNameIgnoringCase(PetName other) {
        return other != null && value.equalsIgnoreCase(other.value);
    }
}
