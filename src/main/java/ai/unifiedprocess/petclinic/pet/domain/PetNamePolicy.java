package ai.unifiedprocess.petclinic.pet.domain;

import org.springframework.stereotype.Service;

/**
 * Domain service for UC-007/UC-008 BR-001.
 */
@Service
public class PetNamePolicy {

    private final PetRepository petRepository;

    public PetNamePolicy(PetRepository petRepository) {
        this.petRepository = petRepository;
    }

    public void requireUniqueName(Integer ownerId, String name, Integer excludePetId) {
        PetName petName = new PetName(name);
        if (petRepository.existsByOwnerAndName(ownerId, petName.value(), excludePetId)) {
            throw new DuplicatePetNameException(petName.value());
        }
    }
}
