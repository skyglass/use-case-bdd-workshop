package ai.unifiedprocess.petclinic.visit.domain;

/**
 * Raised when UC-009 BR-001 is violated.
 */
public class BlankVisitDescriptionException extends RuntimeException {

    public BlankVisitDescriptionException() {
        super("Visit description is required");
    }
}
