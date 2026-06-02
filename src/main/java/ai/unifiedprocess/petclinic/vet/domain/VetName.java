package ai.unifiedprocess.petclinic.vet.domain;

import ai.unifiedprocess.petclinic.core.domain.ValueObject;

public record VetName(String firstName, String lastName) implements ValueObject {

    public VetName {
        firstName = requireText(firstName, "First name");
        lastName = requireText(lastName, "Last name");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
