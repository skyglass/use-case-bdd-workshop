package ai.unifiedprocess.petclinic.owner.domain;

import ai.unifiedprocess.petclinic.core.domain.ValueObject;

public record TelephoneNumber(String value) implements ValueObject {

    public TelephoneNumber {
        if (value == null || !value.trim().matches("\\d{10}")) {
            throw new IllegalArgumentException("Telephone must be exactly 10 digits");
        }
        value = value.trim();
    }
}
