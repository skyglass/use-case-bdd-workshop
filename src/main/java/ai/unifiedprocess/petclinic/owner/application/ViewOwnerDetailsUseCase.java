package ai.unifiedprocess.petclinic.owner.application;

import ai.unifiedprocess.petclinic.owner.domain.Owner;
import ai.unifiedprocess.petclinic.owner.domain.OwnerRepository;
import ai.unifiedprocess.petclinic.pet.domain.Pet;
import ai.unifiedprocess.petclinic.pet.domain.PetRepository;
import ai.unifiedprocess.petclinic.visit.domain.Visit;
import ai.unifiedprocess.petclinic.visit.domain.VisitRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * UC-005 application service. Loads the owner aggregate view with pets and
 * visits in the ordering required by the use case.
 */
@Service
public class ViewOwnerDetailsUseCase {

    private final OwnerRepository ownerRepository;
    private final PetRepository petRepository;
    private final VisitRepository visitRepository;

    public ViewOwnerDetailsUseCase(
            OwnerRepository ownerRepository,
            PetRepository petRepository,
            VisitRepository visitRepository) {
        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;
        this.visitRepository = visitRepository;
    }

    public Optional<OwnerDetails> findDetails(Integer ownerId) {
        return ownerRepository.findById(ownerId)
                .map(owner -> new OwnerDetails(owner, petsFor(owner.id())));
    }

    private List<PetDetails> petsFor(Integer ownerId) {
        return petRepository.findByOwnerId(ownerId).stream()
                .map(pet -> new PetDetails(pet, visitRepository.findByPetId(pet.id())))
                .toList();
    }

    public record OwnerDetails(Owner owner, List<PetDetails> pets) {
    }

    public record PetDetails(Pet pet, List<Visit> visits) {
    }
}
