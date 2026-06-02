package ai.unifiedprocess.petclinic.pet.domain;

import ai.unifiedprocess.petclinic.core.domain.ValueObject;

/**
 * Reference value selected when creating or updating a pet.
 */
public record PetType(Integer id, String name) implements ValueObject {

    public PetType {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("Pet type id must be a positive integer");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Pet type name is required");
        }
        name = name.trim();
    }

    @Override
    public String toString() {
        return name;
    }
}
