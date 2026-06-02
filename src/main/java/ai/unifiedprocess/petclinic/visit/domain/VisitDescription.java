package ai.unifiedprocess.petclinic.visit.domain;

import ai.unifiedprocess.petclinic.core.domain.ValueObject;

public record VisitDescription(String value) implements ValueObject {

    public VisitDescription {
        if (value == null || value.trim().isEmpty()) {
            throw new BlankVisitDescriptionException();
        }
        value = value.trim();
    }
}
