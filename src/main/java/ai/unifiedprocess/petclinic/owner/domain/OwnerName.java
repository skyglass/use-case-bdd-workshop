package ai.unifiedprocess.petclinic.owner.domain;

import ai.unifiedprocess.petclinic.core.domain.ValueObject;

public record OwnerName(String firstName, String lastName) implements ValueObject {

    public OwnerName {
        firstName = requireText(firstName, "First name");
        lastName = requireText(lastName, "Last name");
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
