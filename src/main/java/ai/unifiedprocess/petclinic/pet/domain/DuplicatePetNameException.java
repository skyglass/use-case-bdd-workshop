package ai.unifiedprocess.petclinic.pet.domain;

/**
 * Raised when a pet name would violate UC-007/UC-008 BR-001.
 */
public class DuplicatePetNameException extends RuntimeException {

    public DuplicatePetNameException(String name) {
        super("Pet name already exists for this owner: " + name);
    }
}
