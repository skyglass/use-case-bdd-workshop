package ai.unifiedprocess.petclinic.visit.application;

import ai.unifiedprocess.petclinic.owner.domain.OwnerRepository;
import ai.unifiedprocess.petclinic.pet.domain.Pet;
import ai.unifiedprocess.petclinic.pet.domain.PetRepository;
import ai.unifiedprocess.petclinic.visit.domain.Visit;
import ai.unifiedprocess.petclinic.visit.domain.VisitDescriptionPolicy;
import ai.unifiedprocess.petclinic.visit.domain.VisitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * UC-009 application service. Prepares and completes the visit-booking
 * lifecycle for a pet that belongs to the supplied owner.
 */
@Service
public class BookVisitForPetUseCase {

    private final OwnerRepository ownerRepository;
    private final PetRepository petRepository;
    private final VisitRepository visitRepository;
    private final VisitDescriptionPolicy visitDescriptionPolicy;

    public BookVisitForPetUseCase(
            OwnerRepository ownerRepository,
            PetRepository petRepository,
            VisitRepository visitRepository,
            VisitDescriptionPolicy visitDescriptionPolicy) {
        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;
        this.visitRepository = visitRepository;
        this.visitDescriptionPolicy = visitDescriptionPolicy;
    }

    public Optional<VisitBookingForm> prepare(Integer ownerId, Integer petId) {
        if (ownerRepository.findById(ownerId).isEmpty()) {
            return Optional.empty();
        }
        return petRepository.findById(petId)
                .filter(pet -> pet.ownerId().equals(ownerId))
                .map(pet -> new VisitBookingForm(pet, visitRepository.findByPetId(pet.id())));
    }

    @Transactional
    public BookedVisit book(BookVisitCommand command) {
        visitDescriptionPolicy.requireDescription(command.description());
        prepare(command.ownerId(), command.petId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pet " + command.petId() + " does not belong to owner " + command.ownerId()));
        LocalDate date = command.visitDate() == null ? LocalDate.now() : command.visitDate();
        Visit visit = Visit.bookForPet(command.petId(), date, command.description());
        Visit bookedVisit = visitRepository.save(visit);
        return new BookedVisit(bookedVisit.id());
    }

    public record VisitBookingForm(Pet pet, List<Visit> previousVisits) {
    }

    public record BookVisitCommand(Integer ownerId, Integer petId, LocalDate visitDate, String description) {
    }

    public record BookedVisit(Integer visitId) {
    }
}
