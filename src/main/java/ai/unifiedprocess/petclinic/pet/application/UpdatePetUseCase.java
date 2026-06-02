package ai.unifiedprocess.petclinic.pet.application;

import ai.unifiedprocess.petclinic.owner.domain.OwnerRepository;
import ai.unifiedprocess.petclinic.pet.domain.Pet;
import ai.unifiedprocess.petclinic.pet.domain.PetNamePolicy;
import ai.unifiedprocess.petclinic.pet.domain.PetRepository;
import ai.unifiedprocess.petclinic.pet.domain.PetType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * UC-008 application service. Loads a pet in owner context and updates it
 * after enforcing sibling-name uniqueness.
 */
@Service
public class UpdatePetUseCase {

    private final OwnerRepository ownerRepository;
    private final PetRepository petRepository;
    private final PetNamePolicy petNamePolicy;

    public UpdatePetUseCase(
            OwnerRepository ownerRepository,
            PetRepository petRepository,
            PetNamePolicy petNamePolicy) {
        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;
        this.petNamePolicy = petNamePolicy;
    }

    public List<PetType> availablePetTypes() {
        return petRepository.findAllTypes();
    }

    public Optional<Pet> findPetForOwner(Integer ownerId, Integer petId) {
        if (ownerRepository.findById(ownerId).isEmpty()) {
            return Optional.empty();
        }
        return petRepository.findById(petId)
                .filter(pet -> pet.ownerId().equals(ownerId));
    }

    @Transactional
    public void update(UpdatePetCommand command) {
        Pet pet = findPetForOwner(command.ownerId(), command.petId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pet " + command.petId() + " does not belong to owner " + command.ownerId()));
        petNamePolicy.requireUniqueName(command.ownerId(), command.name(), command.petId());
        petRepository.save(pet.changeDetails(command.name(), command.birthDate(), command.type()));
    }

    public record UpdatePetCommand(
            Integer ownerId,
            Integer petId,
            String name,
            LocalDate birthDate,
            PetType type) {
    }
}
