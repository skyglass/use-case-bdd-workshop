package ai.unifiedprocess.petclinic.pet.application;

import ai.unifiedprocess.petclinic.owner.domain.Owner;
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
 * UC-007 application service. Adds a pet to an existing owner after applying
 * the pet-name domain policy.
 */
@Service
public class AddPetToOwnerUseCase {

    private final OwnerRepository ownerRepository;
    private final PetRepository petRepository;
    private final PetNamePolicy petNamePolicy;

    public AddPetToOwnerUseCase(
            OwnerRepository ownerRepository,
            PetRepository petRepository,
            PetNamePolicy petNamePolicy) {
        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;
        this.petNamePolicy = petNamePolicy;
    }

    public Optional<Owner> findOwner(Integer ownerId) {
        return ownerRepository.findById(ownerId);
    }

    public List<PetType> availablePetTypes() {
        return petRepository.findAllTypes();
    }

    @Transactional
    public AddedPet add(AddPetCommand command) {
        ownerRepository.findById(command.ownerId())
                .orElseThrow(() -> new IllegalArgumentException("Owner " + command.ownerId() + " not found"));
        petNamePolicy.requireUniqueName(command.ownerId(), command.name(), null);
        Pet pet = Pet.addToOwner(command.ownerId(), command.name(), command.birthDate(), command.type());
        Pet addedPet = petRepository.save(pet);
        return new AddedPet(addedPet.id());
    }

    public record AddPetCommand(Integer ownerId, String name, LocalDate birthDate, PetType type) {
    }

    public record AddedPet(Integer petId) {
    }
}
